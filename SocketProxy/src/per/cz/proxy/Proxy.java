package per.cz.proxy;

import java.net.*;
import java.util.concurrent.*;

/**
 * Created by 橙子 on 2016/11/8.
 */
public class Proxy {
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(1080);
            final ExecutorService tpe = Executors.newCachedThreadPool();
            while(true)
            {
                Socket socket = serverSocket.accept();
                socket.setKeepAlive(true);
                tpe.execute(new ProxyThread(socket));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
