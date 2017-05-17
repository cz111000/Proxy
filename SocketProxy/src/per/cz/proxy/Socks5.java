package per.cz.proxy;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
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
    private DatagramSocket clientDatagramSocket = null;
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
        //ATYPE
        byte[] temp;
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
                temp = new byte[2];
                in.read(temp);
                port = ByteBuffer.wrap(temp).asShortBuffer().get() & 0xFFFF;
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
            switch (type) {
                case UDP:
                    clientDatagramSocket = new DatagramSocket(Proxy.PROXY_PORT);
                    clientDatagramSocket.setSoTimeout(1000 * 60 * 5);
                    break;
                case TCP:
                    proxySocket = new Socket(host, port);
                    break;
            }
            out.write((byte) 0x0);
        } catch (Exception e) {
            out.write((byte) 0x1);
        }
        out.write((byte) 0x0);
        out.write((byte) 0x1);
        out.write(InetAddress.getLocalHost().getAddress());
        out.write((byte) (Proxy.PROXY_PORT & 0xFF00));
        out.write((byte) (Proxy.PROXY_PORT & 0x00FF));
        out.flush();
        //SOCKS Server响应
        if (clientDatagramSocket != null) {
            byte[] buffer = new byte[1024];
            DatagramPacket clientReceivePacket = new DatagramPacket(buffer, buffer.length);
            this.clientDatagramSocket.receive(clientReceivePacket);
            //|  RSV  | FRAG | ATYP | DST.ADDR | DST.PORT | DATA |
            //| 00 00 |   0  | 1/3/4| -------- |   00 80  | ---- |
            int offset = 5;
            switch (buffer[4]) {
                case 0x1:
                    temp = new byte[]{buffer[5], buffer[6], buffer[7], buffer[8]};
                    offset += temp.length;
                    host = InetAddress.getByAddress(temp).getHostAddress();
                    break;
                case 0x3:
                    temp = new byte[buffer[5]];
                    offset++;
                    System.arraycopy(buffer, 6, temp, 0, temp.length);
                    offset += temp.length;
                    host = new String(temp);
                    break;
                case 0x4:
                    temp = new byte[16];
                    System.arraycopy(buffer, 5, temp, 0, temp.length);
                    offset += temp.length;
                    host = InetAddress.getByAddress(temp).getHostAddress();
                    break;
            }
            temp = new byte[]{buffer[offset + 1], buffer[offset + 2]};
            offset += temp.length;
            port = ByteBuffer.wrap(temp).asShortBuffer().get() & 0xFFFF;
            DatagramSocket serverDatagramSocket = new DatagramSocket();
            serverDatagramSocket.setSoTimeout(1000 * 60 * 5);
            DatagramPacket serverSendPacket = new DatagramPacket(buffer, offset, clientReceivePacket.getLength() - offset, InetAddress.getByName(host), port);
            serverDatagramSocket.send(serverSendPacket);
            DatagramPacket serverReceivePacket = new DatagramPacket(buffer, offset, buffer.length - offset);
            serverDatagramSocket.receive(serverReceivePacket);
            serverDatagramSocket.close();
            DatagramPacket clientSendPacket = new DatagramPacket(buffer, offset + serverReceivePacket.getLength(), clientReceivePacket.getAddress(), clientReceivePacket.getPort());
            this.clientDatagramSocket.send(clientSendPacket);
            this.clientDatagramSocket.close();
        }

        return type;
    }

    public Socket getProxySocket() {
        return proxySocket;
    }
}
