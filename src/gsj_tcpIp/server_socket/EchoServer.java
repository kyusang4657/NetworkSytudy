package gsj_tcpIp.server_socket;

import java.io.*;
import java.net.*;

public class EchoServer {
    public static void main(String[] args){
        String theLine;

        try(ServerSocket theServer = new ServerSocket(7)){
            while(true){
                try(Socket theSocket = theServer.accept();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(theSocket.getInputStream()));
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(theSocket.getOutputStream()));){

                    while((theLine = reader.readLine()) != null){
                        System.out.println(theLine);

                        writer.write(theLine);
                        writer.newLine();
                        writer.flush();
                    }

                }catch(IOException ioe){
                    System.err.println(ioe);
                }
            }
        }catch(IOException ioe){
            System.err.println(ioe);
        }
    }
}
