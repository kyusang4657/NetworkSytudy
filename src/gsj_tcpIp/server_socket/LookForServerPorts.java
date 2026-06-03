package gsj_tcpIp.server_socket;

import java.io.*;
import java.net.*;

public class LookForServerPorts {
    public static void main(String[] args){
        for(int i = 0; i < 65535; i++){
            try(ServerSocket serverSocket = new ServerSocket(i)){
            }catch(IOException e){
                System.out.println(i + " ��° ��Ʈ�� Ư�������� ����� �Դϴ�.");
            }
        }
    }
}
