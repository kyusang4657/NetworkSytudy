package gsj_project.filetransfer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.net.SocketTimeoutException;

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
                handlePacket(socket, receivePacket, message);
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
                    }else{
                        System.out.println("Invalid ACK received");
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

    private static void sendMessage(
            DatagramSocket socket,
            String message,
            DatagramPacket targetPacket
    ) throws Exception {
        byte[] buffer = message.getBytes(StandardCharsets.UTF_8);

        DatagramPacket sendPacket = new DatagramPacket(
                buffer,
                buffer.length,
                targetPacket.getAddress(),
                targetPacket.getPort()
        );
        socket.send(sendPacket);
    }
}
