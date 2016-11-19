package per.cz.proxy;

import java.io.*;
import java.net.*;

/**
 * Created by 橙子 on 2016/11/8.
 */
public class ProxyThread extends Thread {
    private Socket socket;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private InputStream proxyIn;
    private OutputStream proxyOut;
    private boolean httpMode = false;

    public ProxyThread(Socket socket) throws IOException {
        this.socket = socket;
        in = new BufferedInputStream(socket.getInputStream());
        out = new BufferedOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        try {
            Type type = Type.UNKNOWN;
            in.mark(1);
            switch (in.read()) {
                case 0x4:
                    //SOCKS4代理
                    socket.setSoTimeout(3000);
                    Socks4 socks4 = new Socks4(in, out);
                    type = socks4.doJob();
                    proxyIn = socks4.getProxyIn();
                    proxyOut = socks4.getProxyOut();
                    break;
                case 0x5:
                    //SOCKS5代理
                    socket.setSoTimeout(3000);
                    Socks5 socks5 = new Socks5(in, out);
                    type = socks5.doJob();
                    proxyIn = socks5.getProxyIn();
                    proxyOut = socks5.getProxyOut();
                    break;
                case 0x43:
                    //CONNECT
                case 0x47:
                    //GET
                case 0x50:
                    //POST
                    //HTTP代理
                    in.reset();
                    in.mark(409600);
                    Http http = new Http(in, out);
                    type = http.doJob();
                    proxyIn = http.getProxyIn();
                    proxyOut = http.getProxyOut();
                    httpMode = http.isHttpMode();
                    if (httpMode)
                        in.reset();
                    break;
                default:
                    break;
            }
            switch (type) {
                case FTP:
                case TCP:
                    PipeThread request = new PipeThread(in, proxyOut);
                    PipeThread response = new PipeThread(proxyIn, out);
                    request.setHttpMode(httpMode);
                    if (httpMode) {
                        request.run();
                        response.run();
                    }else {
                        response.setHttpMode(httpMode);
                        request.start();
                        response.start();
                        request.join();
                        response.join();
                        //此处start和join顺序不能改变
                    }
                    break;
                case UDP:
                    new PipeThread(in, proxyOut).start();
                    break;
                case UNKNOWN:
                    break;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                socket.close();
                proxyIn.close();
                proxyOut.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
