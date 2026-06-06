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

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress serverAddress = InetAddress.getByName(SERVER_IP);

            String request = PacketUtil.createRRQ("test.txt");
            byte[] requestBytes = request.getBytes(StandardCharsets.UTF_8);
            DatagramPacket sendPacket = new DatagramPacket(
                    requestBytes,
                    requestBytes.length,
                    serverAddress,
                    SERVER_PORT
            );

            socket.send(sendPacket);
            System.out.println("RRQ request sent successfully");

            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

            ByteArrayOutputStream fileOutput = new ByteArrayOutputStream();

            while(true) {
                receivePacket.setLength(buffer.length);
                socket.receive(receivePacket);

                String response = new String(
                        receivePacket.getData(),
                        0,
                        receivePacket.getLength(),
                        StandardCharsets.UTF_8
                );

                Packet packet = PacketUtil.fromJson(response);

                if(packet.getType().equals("DATA")) {
                    byte[] decodedData = Base64.getDecoder().decode(packet.getData());
                    fileOutput.write(decodedData);

                    String ack = PacketUtil.createACK(packet.getBlock());
                    byte[] ackBytes = ack.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket ackPacket = new DatagramPacket(
                            ackBytes,
                            ackBytes.length,
                            receivePacket.getAddress(),
                            receivePacket.getPort()
                    );
                    System.out.println("DATA block received: " + packet.getBlock());
                    socket.send(ackPacket);
                    System.out.println("ACK sent: " + packet.getBlock());

                    if(packet.getDataSize() < 512){
                        break;
                    }

                }else if(packet.getType().equals("ERROR")) {
                    System.out.println("Server error: " + packet.getMessage());
                    return;
                }
            }

            Files.write(
                    Path.of("C:/gsj_study/NetworkSytudy/src/gsj_project/filetransfer/downloaded_test.txt"),
                    fileOutput.toByteArray()
            );

            System.out.println("File downloaded successfully");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
