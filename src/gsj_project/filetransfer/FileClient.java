package gsj_project.filetransfer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class FileClient {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 9000;
    private static final int BUFFER_SIZE = 4096;

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress serverAddress = InetAddress.getByName(SERVER_IP);

            String request = """
                    {
                        "type": "RRQ",
                        "filename": "test.txt"
                    }
                    """;

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
            socket.receive(receivePacket);

            String response = new String(
                    receivePacket.getData(),
                    0,
                    receivePacket.getLength(),
                    StandardCharsets.UTF_8
            );
            System.out.println("Receive server response:");
            System.out.println(response);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
