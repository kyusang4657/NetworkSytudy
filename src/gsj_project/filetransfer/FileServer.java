package gsj_project.filetransfer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.net.SocketTimeoutException;
import java.io.ByteArrayOutputStream;

public class FileServer {
    private static final int SERVER_PORT = 9000;
    private static final int BUFFER_SIZE = 4096;
    private static final int BLOCK_SIZE = 512;
    private static final int TIMEOUT = 3000;
    private static final Path BASE_DIR = Paths.get("C:/gsj_study/NetworkSytudy/src/gsj_project/filetransfer/");

    public static void main(String[] args) {
        FileServer server = new FileServer();
        server.start();
    }

    public void start() {
        try(DatagramSocket socket = new DatagramSocket(SERVER_PORT)) {
            System.out.println("Start UDP file server. Port: " + SERVER_PORT);

            byte[] buffer = new byte[BUFFER_SIZE];

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                receivePacket.setLength(buffer.length);
                socket.receive(receivePacket);

                String message = packetToMessage(receivePacket);

                System.out.println("Request received:");
                System.out.println(message);

                DatagramPacket requestCopy = copyPacket(receivePacket);
                String messageCopy = message;

                new Thread(() -> {
                    try(DatagramSocket transferSocket = new DatagramSocket()) {
                        handlePacket(transferSocket, requestCopy, messageCopy);
                    }catch(Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void handlePacket(DatagramSocket socket, DatagramPacket receivePacket, String message) {
        Packet packet = PacketUtil.fromJson(message);

        if(packet.getType().equals("RRQ")) {
            String filename = packet.getFilename();

            if(filename == null || filename.isBlank()) {
                sendError(socket, receivePacket, "Filename is required");
                return;
            }

            handleRRQ(socket, receivePacket, filename);
        }else if(packet.getType().equals("WRQ")) {
            String filename = packet.getFilename();
            if(filename == null || filename.isBlank()) {
                sendError(socket, receivePacket, "Filename is required");
                return;
            }
            handleWRQ(socket, receivePacket, filename);
        }else{
            sendError(socket, receivePacket, "Unsupported request type: " + packet.getType());
        }
    }

    private void handleRRQ(DatagramSocket socket, DatagramPacket receivePacket, String filename) {
        Path filePath = BASE_DIR.resolve(filename);

        if(Files.exists(filePath)) {
            sendFile(socket, receivePacket, filePath);
        }else{
            sendError(socket, receivePacket, "File not found: " + filename);
        }
    }

    private void handleWRQ(DatagramSocket socket, DatagramPacket receivePacket, String filename) {
        try{
            String ack = PacketUtil.createACK(0);
            sendMessage(socket, ack, receivePacket);
            System.out.println("WRQ accepted. ACK sent: 0");

            ByteArrayOutputStream fileOutput = new ByteArrayOutputStream();
            int expectedBlock = 1;
            while(true) {
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket dataPacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(dataPacket);

                //받은 데이터를 JSON 문자열로 바꿔주고 그걸 다시 Packet 객체로 비꿈.
                String message = packetToMessage(dataPacket);
                Packet packet = PacketUtil.fromJson(message);

                if(packet.getType().equals("DATA")) {
                    if(packet.getBlock() != expectedBlock) {
                        sendNACK(socket, dataPacket, expectedBlock);
                        continue;
                    }
                    byte[] decodedData = Base64.getDecoder().decode(packet.getData());
                    fileOutput.write(decodedData);

                    sendACK(socket, dataPacket, packet.getBlock());
                    expectedBlock++;
                    System.out.println("Upload DATA received. ACK sent: " + packet.getBlock());

                    if(packet.getDataSize() < BLOCK_SIZE) {
                        break;
                    }
                }else{
                    sendError(socket, dataPacket, "Expected DATA packet");
                    return;
                }
            }

            Path savePath = BASE_DIR.resolve(filename);
            Files.write(savePath, fileOutput.toByteArray());
            System.out.println("Uploaded file saved: " + savePath);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void sendFile(DatagramSocket socket, DatagramPacket receivePacket, Path filePath) {
        try{
            byte[] fileBytes = Files.readAllBytes(filePath);
            int block = 1;

            for(int offset = 0; offset < fileBytes.length; offset += BLOCK_SIZE) {
                int dataSize = Math.min(BLOCK_SIZE, fileBytes.length - offset);

                byte[] chunk = Arrays.copyOfRange(
                        fileBytes,
                        offset,
                        offset + dataSize
                );
                String encodeData = Base64.getEncoder().encodeToString(chunk);
                String response = PacketUtil.createDATA(block, encodeData, dataSize);
                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

                DatagramPacket sendPacket = new DatagramPacket(
                        responseBytes,
                        responseBytes.length,
                        receivePacket.getAddress(),
                        receivePacket.getPort()
                );

                socket.send(sendPacket);
                System.out.println("Data block sent: " + block);

                boolean ackReceived = waitForAck(socket, sendPacket, block);

                if(!ackReceived) {
                    break;
                }

                block++;

                if(dataSize < BLOCK_SIZE) {
                    break;
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private boolean waitForAck(DatagramSocket socket, DatagramPacket sendPacket, int block) {
        try {
            socket.setSoTimeout(TIMEOUT);

            while(true) {
                try {
                    byte[] ackBuffer = new byte[BUFFER_SIZE];
                    DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
                    socket.receive(ackPacket);

                    String ackMessage = packetToMessage(ackPacket);
                    Packet ack = PacketUtil.fromJson(ackMessage);

                    if(ack.getType().equals("ACK") && ack.getBlock() == block) {
                        System.out.println("ACK received: " + block);
                        socket.setSoTimeout(0);
                        return true;
                    } else if(ack.getType().equals("NACK")) {
                        socket.send(sendPacket);
                        System.out.println("NACK received. Resend DATA block: " + block);
                    } else {
                        System.out.println("Invalid response received");
                    }
                }catch(SocketTimeoutException e) {
                    socket.send(sendPacket);
                    System.out.println("ACK timeout. Resend DATA block: " + block);
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
            try {
                socket.setSoTimeout(0);
            }catch(Exception resetException) {
                resetException.printStackTrace();
            }
            return false;
        }
    }

    private void sendError (DatagramSocket socket, DatagramPacket receivePacket, String message) {
        try{
            String response = PacketUtil.createError(message);
            sendMessage(socket, response, receivePacket);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static String packetToMessage(DatagramPacket packet) {
        return new String(
                packet.getData(),
                0,
                packet.getLength(),
                StandardCharsets.UTF_8
        );
    }

    //문자열 JSON을 UPD 패킷으로 만들어서 보내는 메소드
    private static void sendMessage(DatagramSocket socket, String message, DatagramPacket targetPacket) throws Exception {
        byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
        DatagramPacket sendPacket = new DatagramPacket(
                buffer,
                buffer.length,
                targetPacket.getAddress(),
                targetPacket.getPort()
        );
        socket.send(sendPacket);
    }

    private void sendACK(DatagramSocket socket, DatagramPacket receivePacket, int block) throws Exception {
        String ack = PacketUtil.createACK(block);
        sendMessage(socket, ack, receivePacket);
        System.out.println("ACK send: " + block);
    }

    private void sendNACK(DatagramSocket socket, DatagramPacket receivePacket, int expectedBlock) throws Exception {
        String nack = PacketUtil.createNACK(expectedBlock);
        sendMessage(socket, nack, receivePacket);
        System.out.println("NACK send: " + expectedBlock);
    }

    private static DatagramPacket copyPacket(DatagramPacket packet) {
        byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());

        return new DatagramPacket(
                data,
                data.length,
                packet.getAddress(),
                packet.getPort()
        );
    }
}
