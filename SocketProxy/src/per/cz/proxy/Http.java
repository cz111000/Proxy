package per.cz.proxy;

import java.io.*;
import java.net.Socket;

/**
 * Created by 橙子 on 2016/11/13.
 */
public class Http {
    private Socket proxySocket;
    private ClientInputStream in;
    private BufferedOutputStream out;
    private String host;
    private int port;
    private boolean socketExchanged;
    private boolean sslConnect;

    public Http(ClientInputStream in, BufferedOutputStream out) {
        this.in = in;
        this.out = out;
        sslConnect = false;
        socketExchanged = false;
    }

    public Type doJob() throws IOException {
        try {
            String line;
            while (!((line = in.readLine(true)).isEmpty())) {
                if (line.startsWith("CONNECT")) {
                    sslConnect = true;
                    out.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes());
                    out.flush();
                } else if (line.startsWith("Host:")) {
                    String host = getHost(line);
                    int port = getPort(line);
                    if (this.host == null || !this.host.equals(host) || this.port != port) {
                        proxySocket = new Socket(host, port);
                        if (this.host != null) {
                            // 切换host
                            socketExchanged = true;
                        }
                    }
                    this.host = host;
                    this.port = port;
                }
            }
            if (socketExchanged) {
                BufferedInputStream proxyIn = new BufferedInputStream(proxySocket.getInputStream());
                Pipe pipeS2C = new Pipe(proxyIn, out);
                pipeS2C.start();
            }
        } catch (Exception e) {
            out.write("HTTP/1.1 400 Bad Request\r\n".getBytes());
            out.write(e.getLocalizedMessage().getBytes());
            out.write("\r\n".getBytes());
        }
        return Type.TCP;
    }

    private String getHost(String data) {
        String host = data.substring("Host:".length());
        host = host.replaceAll(" ", "");
        int begin = host.lastIndexOf(":");
        if (begin != -1)
            host = host.substring(0, begin);
        return host;
    }

    private int getPort(String data) {
        data = data.substring("Host:".length());
        data = data.replaceAll(" ", "");
        int port = data.indexOf(":");
        if (port != -1)
            port = Integer.valueOf(data.substring(port + 1));
        else if (sslConnect)
            port = 443;
        else
            port = 80;
        return port;
    }

    public Socket getProxySocket() throws IOException {
        return proxySocket;
    }

    public boolean isSocketExchanged() {
        boolean temp = socketExchanged;
        socketExchanged = false;
        return temp;
    }
}