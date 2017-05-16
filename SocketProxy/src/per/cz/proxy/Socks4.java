package per.cz.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Scanner;

/**
 * Created by 橙子 on 2016/11/12.
 */
public class Socks4 {
    private InputStream in;
    private OutputStream out;
    private Socket proxySocket;
    private String host;
    private int port = 80;
    private Type type = Type.UNKNOWN;

    public Socks4(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }

    public Type doJob() throws Exception {
        //SOCKS Client请求
        //| VER | CMD | DST.PORT | DST.ADDR | USERID | NULL |
        //|  4  |  1  |   00 80  | -------- | ------ |  0   |
        //CMD
        if (in.read() == 0x1)
            type = Type.TCP;
        //DST.PORT
        byte[] temp = new byte[2];
        in.read(temp);
        port = ByteBuffer.wrap(temp).asShortBuffer().get() & 0xFFFF;
        //DST.ADDR
        byte[] data = new byte[4];
        in.read(data);
        host = InetAddress.getByAddress(data).getHostAddress();
        //读取至NULL
        while (in.read() != 0x0) ;
        //(HOST PORT)分析完毕
        //| VN | CMD | DST.PORT | DST.ADDR |
        out.write((byte) 0x0);
        try {
            proxySocket = new Socket(host, port);
            out.write((byte) 0x5A);
        } catch (Exception e) {
            out.write((byte) 0x5B);
        }
        out.write(data);
        out.write(temp);
        out.flush();
        //SOCKS Server响应
        return type;
    }

    public Socket getProxySocket() {
        return proxySocket;
    }
}