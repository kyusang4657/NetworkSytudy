package gsj_network.gsj_tcpIp;

import java.io.*;
import java.net.*;
import java.util.Date;

public class DayTimeServer {
    public final static int daytimeport = 13;
    public static void main(String[] args) {
        //13번 포트에서 클라이언트 접속을 기다리는 서버 소켓 생성
        try(ServerSocket theServer = new ServerSocket(daytimeport)){
            //무한반복을 통해 accept()를 반복하면서, 서버의 실행 상태를 유지할 수 있다.
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
