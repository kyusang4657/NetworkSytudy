package gsj_network.Ch07;

import java.io.*;
import java.net.*;

public class LookForServerPorts {
    public static void main(String[] args){
        for(int i = 0; i < 65535; i++){
            try(ServerSocket serverSocket = new ServerSocket(i)){
            }catch(IOException e){
                System.out.println(i + " 번째 포트는 특정서버가 사용중 입니다.");
            }
        }
    }
}
