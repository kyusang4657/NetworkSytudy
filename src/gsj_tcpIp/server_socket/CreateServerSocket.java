package gsj_tcpIp.server_socket;
/*
* 서버 포트만 열었다가 바로 종료하는 코드
* accept()가 없으므로 클라이언트 연결도 안 받고 통신도 안 한다.
*/
import java.io.*;
import java.net.*;

public class CreateServerSocket {
    public static void main(String[] args) {
        if(args.length != 1){
            System.out.println("포트 번호 입력");
            return;
        }
        int port = Integer.parseInt(args[0]);

        try(ServerSocket theServer = new ServerSocket(port)){
            System.out.println(port + "에 바인드된 서버 소켓 객체를 생성하였습니다.");
        }catch(IOException e){
            System.out.println(e);
        }
    }
}