package gsj_project.filetransfer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

public class FileServer {
    private static final int SERVER_PORT = 9000;
    private static final int BUFFER_SIZE = 4096;

    public static void main(String[] args) {
        try(DatagramSocket socket = new DatagramSocket(SERVER_PORT)){
            System.out.println("Start UDP file server. Port: " + SERVER_PORT);

            byte[] buffer = new byte[BUFFER_SIZE];

            while(true){
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(receivePacket);

                String message = new String(
                        receivePacket.getData(),
                        0,
                        receivePacket.getLength(),
                        StandardCharsets.UTF_8
                );
                System.out.println("Request received:");
                System.out.println(message);

                Packet packet = PacketUtil.fromJson(message);

                if(packet.getType().equals("RRQ")) {
                    String filename = packet.getFilename();

                    Path filePath = Path.of("C:/gsj_study/NetworkSytudy/src/gsj_project/filetransfer/" + filename);
                    String response;

                    if(Files.exists(filePath)) {
                        String fileData = Files.readString(filePath);
                        response = PacketUtil.createDATA(1, fileData);
                    }else{
                        response = PacketUtil.createError("File not found: " + filename);
                    }
                    byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

                    DatagramPacket sendPacket = new DatagramPacket(
                            responseBytes,
                            responseBytes.length,
                            receivePacket.getAddress(),
                            receivePacket.getPort()
                    );
                    socket.send(sendPacket);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
