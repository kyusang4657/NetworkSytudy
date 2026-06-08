package gsj_project.filetransfer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class FileClient {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 9000;
    private static final int BUFFER_SIZE = 4096;
    private static final int BLOCK_SIZE = 512;

    public static void main(String[] args) {
        FileClient client = new FileClient();
        client.downloadFile("test.txt");
    }

    public void downloadFile(String filename) {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress serverAddress = InetAddress.getByName(SERVER_IP);

            sendRRQ(socket, serverAddress, filename);

            Path outputPath = Path.of("C:/gsj_study/NetworkSytudy/src/gsj_project/filetransfer/downloaded_" + filename);
            receiveFile(socket, outputPath);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void sendRRQ(DatagramSocket socket, InetAddress serverAddress, String filename) throws Exception{
        String request = PacketUtil.createRRQ(filename);
        byte[] requestBytes = request.getBytes(StandardCharsets.UTF_8);

        DatagramPacket sendPacket = new DatagramPacket(
                requestBytes,
                requestBytes.length,
                serverAddress,
                SERVER_PORT
        );
        socket.send(sendPacket);
        System.out.println("RRQ request sent successfully");
    }

    private void receiveFile (DatagramSocket socket, Path outputPath) {
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            ByteArrayOutputStream fileOutput = new ByteArrayOutputStream();

            while(true){
                receivePacket.setLength(buffer.length);
                socket.receive(receivePacket);

                String response = packetToMessage(receivePacket);
                Packet packet = PacketUtil.fromJson(response);

                if(packet.getType().equals("DATA")){
                    byte[] decodeData = Base64.getDecoder().decode(packet.getData());
                    fileOutput.write(decodeData);

                    System.out.println("DATA block received: " + packet.getBlock());
                    sendACK(socket, receivePacket, packet.getBlock());

                    if(packet.getDataSize() < BLOCK_SIZE){
                        break;
                    }
                } else if(packet.getType().equals("ERROR")) {
                    System.out.println("Server error: " + packet.getMessage());
                    return;
                }
            }
            Files.write(outputPath, fileOutput.toByteArray());
            System.out.println("File downloaded successfully");
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void sendACK(DatagramSocket socket, DatagramPacket receivePacket, int block) throws Exception{
        String ack = PacketUtil.createACK(block);
        byte[] ackBytes = ack.getBytes(StandardCharsets.UTF_8);
        DatagramPacket ackPacket = new DatagramPacket(
                ackBytes,
                ackBytes.length,
                receivePacket.getAddress(),
                receivePacket.getPort()
        );

        socket.send(ackPacket);
        System.out.println("ACK sent: " + block);
    }

    private static String packetToMessage(DatagramPacket packet) {
        return new String(
                packet.getData(),
                0,
                packet.getLength(),
                StandardCharsets.UTF_8
        );
    }
}
