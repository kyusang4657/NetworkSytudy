package gsj_tcpIp.server_socket;

import java.io.*;
import java.net.*;
import java.util.Date;

public class DayTimeServer {
    public final static int daytimeport = 13;
    public static void main(String[] args) {
        //13�� ��Ʈ���� Ŭ���̾�Ʈ ������ ��ٸ��� ���� ���� ����
        try(ServerSocket theServer = new ServerSocket(daytimeport)){
            //���ѹݺ��� ���� accept()�� �ݺ��ϸ鼭, ������ ���� ���¸� ������ �� �ִ�.
            while(true){
                try(Socket theSocket = theServer.accept();
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(theSocket.getOutputStream()))){
                        Date now = new Date();

                        writer.write(now.toString());
                        writer.newLine();
                        writer.flush();

                }catch(IOException e){
                        System.out.println(e);
                }
            }
        }catch(IOException e){
            System.out.println(e);
        }
    }
}
