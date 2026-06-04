package gsj_project.filetransfer;

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
                System.out.println("\n" + "Receive client request" + message);
                String response = """
                        {
                            "type": "ACK",
                            "message": "Request received"
                        }
                        """;
                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

                DatagramPacket sendPacket = new DatagramPacket(
                        responseBytes,
                        responseBytes.length,
                        receivePacket.getSocketAddress()
                );

                socket.send(sendPacket);
                System.out.println("ACK response transmission complete");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
