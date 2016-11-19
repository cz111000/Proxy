package per.cz.proxy;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Created by 橙子 on 2016/11/13.
 */
public class Http {
    private BufferedReader in;
    private BufferedWriter out;
    private InputStream proxyIn;
    private OutputStream proxyOut;
    private boolean connect = false;

    public Http(InputStream in, OutputStream out) {
        this.in = new BufferedReader(new InputStreamReader(in));
        this.out = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(out)));
    }

    public Type doJob() {
        try {
            String line;
            String host = "";
            int port = 0;
            while (!((line = in.readLine()).isEmpty()))
                if (line.startsWith("CONNECT")) {
                    connect = true;
                    out.write("HTTP/1.1 200 Connection Established\r\n\r\n");
                    out.flush();
                } else if (line.startsWith("Host:")) {
                    host = getHost(line);
                    port = getPort(line);
                }
            //(请求方式 HOST PORT)分析完毕
            Socket proxySocket = new Socket(host, port);
            proxySocket.setKeepAlive(true);
            proxySocket.setSoTimeout(3000);
            proxyIn = proxySocket.getInputStream();
            proxyOut = proxySocket.getOutputStream();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return Type.TCP;
    }

    private String getHost(String data) {
        String temp = data.substring("Host:".length());
        temp = temp.replaceAll(" ", "");
        int begin = temp.lastIndexOf(":");
        if (begin != -1)
            temp = temp.substring(0, begin);
        return temp;
    }

    private int getPort(String data) {
        data = data.substring("Host:".length());
        data = data.replaceAll(" ", "");
        int temp = data.indexOf(":");
        if (temp != -1)
            temp = Integer.valueOf(data.substring(temp + 1));
        else
            temp = 80;
        return temp;
    }

    public InputStream getProxyIn() {
        return proxyIn;
    }

    public OutputStream getProxyOut() {
        return proxyOut;
    }

    public boolean isHttpMode() {
        return !connect;
    }

}