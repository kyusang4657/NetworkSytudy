package gsj_project.filetransfer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.net.SocketTimeoutException;

public class FileClient {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 9000;
    private static final int BUFFER_SIZE = 4096;
    private static final int BLOCK_SIZE = 512;
    private static final int TIMEOUT = 3000;

    public static void main(String[] args) {
        FileClient client = new FileClient();
    }

    public void downloadFile(String filename, Path outputPath) {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress serverAddress = InetAddress.getByName(SERVER_IP);

            sendRRQ(socket, serverAddress, filename);
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
            int expectedBlock = 1;

            while(true){
                receivePacket.setLength(buffer.length);
                socket.receive(receivePacket);

                String response = packetToMessage(receivePacket);
                Packet packet = PacketUtil.fromJson(response);

                if(packet.getType().equals("DATA")){
                    if(packet.getBlock() != expectedBlock) {
                        sendNACK(socket, receivePacket, expectedBlock);
                        continue;
                    }
                    byte[] decodeData = Base64.getDecoder().decode(packet.getData());
                    fileOutput.write(decodeData);

                    System.out.println("DATA block received: " + packet.getBlock());
                    sendACK(socket, receivePacket, packet.getBlock());
                    expectedBlock++;
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

    private void sendNACK(DatagramSocket socket, DatagramPacket receivePacket, int expectedBlock) throws Exception {
        String nack = PacketUtil.createNACK(expectedBlock);
        byte[] nackBytes = nack.getBytes(StandardCharsets.UTF_8);

        DatagramPacket nackPacket = new DatagramPacket(
                nackBytes,
                nackBytes.length,
                receivePacket.getAddress(),
                receivePacket.getPort()
        );

        socket.send(nackPacket);
        System.out.println("NACK sent: " + expectedBlock);
    }

    private static String packetToMessage(DatagramPacket packet) {
        return new String(
                packet.getData(),
                0,
                packet.getLength(),
                StandardCharsets.UTF_8
        );
    }

    public void uploadFile(Path filePath) {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress serverAddress = InetAddress.getByName(SERVER_IP);

            String filename = filePath.getFileName().toString();

            String request = PacketUtil.createWRQ(filename);
            byte[] requestBytes = request.getBytes(StandardCharsets.UTF_8);

            DatagramPacket sendPacket = new DatagramPacket(
                    requestBytes,
                    requestBytes.length,
                    serverAddress,
                    SERVER_PORT
            );

            socket.send(sendPacket);
            System.out.println("WRQ request sent successfully");

            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(receivePacket);

            String response = packetToMessage(receivePacket);
            Packet packet = PacketUtil.fromJson(response);

            if(packet.getType().equals("ACK") && packet.getBlock() == 0) {
                System.out.println("WRQ accepted by server");
                sendFile(socket, receivePacket, filePath);
            }else if(packet.getType().equals("ERROR")) {
                System.out.println("Server error: " + packet.getMessage());
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    //클라이언트가 서버로 파일을 업로드 할 때 파일 데이터를 보내는 메서드
    private void sendFile(DatagramSocket socket, DatagramPacket receivePacket, Path filePath){
        try {
            byte[] fileBytes = Files.readAllBytes(filePath);
            int block = 1;

            //파일을 512 바이트씩 나눔
            for(int offset = 0; offset < fileBytes.length; offset += BLOCK_SIZE) {
                int dataSize = Math.min(BLOCK_SIZE, fileBytes.length - offset);

                byte[] chunk = java.util.Arrays.copyOfRange(
                        fileBytes,
                        offset,
                        offset + dataSize
                );

                String encodeData = Base64.getEncoder().encodeToString(chunk);
                String dataPacket = PacketUtil.createDATA(block, encodeData, dataSize);
                byte[] dataBytes = dataPacket.getBytes(StandardCharsets.UTF_8);

                DatagramPacket sendPacket = new DatagramPacket(
                        dataBytes,
                        dataBytes.length,
                        receivePacket.getAddress(),
                        receivePacket.getPort()
                );

                socket.send(sendPacket);
                System.out.println("Upload DATA block sent: " + block);

                socket.setSoTimeout(TIMEOUT);

                boolean ackReceived = false;

                while(!ackReceived){
                    try{
                        byte[] ackBuffer = new byte[BUFFER_SIZE];
                        DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
                        socket.receive(ackPacket);

                        String ackMessage = packetToMessage(ackPacket);
                        Packet ack = PacketUtil.fromJson(ackMessage);

                        if(ack.getType().equals("ACK") && ack.getBlock() == block) {
                            System.out.println("Upload ACK received: " + block);
                            ackReceived = true;
                        } else if(ack.getType().equals("NACK")) {
                            socket.send(sendPacket);
                            System.out.println("Upload NACK received. Resend DATA block: " + block);
                        } else {
                            System.out.println("Invalid upload response received");
                        }
                    }catch(SocketTimeoutException e) {
                        socket.send(sendPacket);
                        System.out.println("Upload ACK timeout. Resend DATA block: " + block);
                    }
                }
                socket.setSoTimeout(0);
                block++;

                if(dataSize < BLOCK_SIZE) {
                    break;
                }
            }

            System.out.println("File upload finished");
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
}
