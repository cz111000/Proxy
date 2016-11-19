package per.cz.proxy;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Created by 橙子 on 2016/11/12.
 */
public class Socks5 {
    private InputStream in;
    private OutputStream out;
    private Socket proxySocket;
    private InputStream proxyIn;
    private OutputStream proxyOut;
    private String host;
    private int port = 80;
    private Type type = Type.UNKNOWN;

    public Socks5(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }

    public Type doJob() throws Exception {
        //| VER | METHOD | METHODS |
        //|  5  |    0   |    1    |
        //METHOD
        in.read();
        in.read();
        //| VER | METHODS |
        out.write((byte) 0x5);
        out.write((byte) 0x0);
        //SOCKS Server响应
        out.flush();
        //SOCKS Client请求
        //| VER | CMD |  RSV  | ATYP | DST.ADDR | DST.PORT |
        //|  5  |1/2/3|   0   | 1/3/4| -------- |   00 80  |
        if (in.read() != 0x5)
            return type;
        switch (in.read()) {
            case 0x1:
                type = Type.TCP;
                break;
            case 0x2:
                type = Type.FTP;
                break;
            case 0x3:
                type = Type.UDP;
                break;
        }
        //RSV
        in.read();
        byte[] temp;
        //ATYPE
        switch (in.read()) {
            case 0x1:
                temp = new byte[4];
                in.read(temp);
                host = InetAddress.getByAddress(temp).getHostAddress();
                temp = new byte[2];
                in.read(temp);
                port = ByteBuffer.wrap(temp).asShortBuffer().get() & 0xFFFF;
                break;
            case 0x3:
                temp = new byte[in.read()];
                in.read(temp);
                host = new String(temp);
                break;
            case 0x4:
                temp = new byte[16];
                in.read(temp);
                host = InetAddress.getByAddress(temp).getHostAddress();
                temp = new byte[2];
                in.read(temp);
                port = ByteBuffer.wrap(temp).asShortBuffer().get() & 0xFFFF;
                break;
        }
        //(HOST PORT)分析完毕
        //| VER | REP |  RSV  | ATYP | BND.ADDR | BND.PORT |
        out.write((byte) 0x5);
        try {
            proxySocket = new Socket(host, port);
            proxySocket.setSoTimeout(3000);
            proxyIn = proxySocket.getInputStream();
            proxyOut = proxySocket.getOutputStream();
            out.write((byte) 0x0);
        } catch (Exception e) {
            out.write((byte) 0x1);
        }
        out.write((byte) 0x0);
        out.write((byte) 0x1);
        out.write(proxySocket.getLocalAddress().getAddress());
        out.write((byte) (1080 & 0xFF00));
        out.write((byte) (1080 & 0x00FF));
        out.flush();
        //SOCKS Server响应
        return type;
    }

    public InputStream getProxyIn() {
        return proxyIn;
    }

    public OutputStream getProxyOut() {
        return proxyOut;
    }
}
