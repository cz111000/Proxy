package per.cz.proxy;

import java.io.*;
import java.net.*;

/**
 * Created by 橙子 on 2016/11/8.
 */
public class ProxyThread extends Thread {
    private final Socket socket;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private BufferedInputStream proxyIn;
    private BufferedOutputStream proxyOut;
    private Socket proxySocket;

    public ProxyThread(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedInputStream(socket.getInputStream());
        this.out = new BufferedOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        try {
            Http http = null;
            socket.setSoTimeout(1000 * 60 * 5);
            in.mark(1);
            Type type;
            switch (in.read()) {
                case 0x4:
                    //SOCKS4
                    Socks4 socks4 = new Socks4(in, out);
                    type = socks4.doJob();
                    proxySocket = socks4.getProxySocket();
                    break;
                case 0x5:
                    //SOCKS5
                    Socks5 socks5 = new Socks5(in, out);
                    type = socks5.doJob();
                    proxySocket = socks5.getProxySocket();
                    break;
                default:
                    //HTTP
                    in.reset();
                    in = new ClientInputStream(in);
                    http = new Http((ClientInputStream) in, out);
                    ((ClientInputStream) in).setAnalysis(http);
                    type = http.doJob();
                    proxySocket = http.getProxySocket();
                    break;
            }
            if (proxySocket != null) {
                proxySocket.setSoTimeout(1000 * 60 * 5);
                proxyIn = new BufferedInputStream(proxySocket.getInputStream());
                proxyOut = new BufferedOutputStream(proxySocket.getOutputStream());
                switch (type) {
                    case FTP:
                    case TCP:
                        Pipe pipeC2S = new Pipe(in, proxyOut, http);
                        pipeC2S.start();
                        Pipe pipeS2C = new Pipe(proxyIn, out);
                        pipeS2C.start();
                        pipeC2S.join();
                        break;
                    case UDP:
                        new Pipe(in, proxyOut).start();
                        break;
                    case UNKNOWN:
                        break;
                }
            }
        } catch (Exception ignored) {
        } finally {
            try {
                if (!socket.isClosed())
                    socket.close();
                if (proxySocket != null && !proxySocket.isClosed())
                    proxySocket.close();
            } catch (Exception ignored1) {
            }
        }
    }
}
