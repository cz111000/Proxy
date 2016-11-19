package per.cz.proxy;

import java.io.*;

/**
 * Created by 橙子 on 2016/11/12.
 */
public class PipeThread extends Thread {
    private InputStream in;
    private OutputStream out;
    private byte[] buffer = new byte[409600];
    private boolean httpMode = false;

    public PipeThread(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        int length;
        int contentLength = 0;
        boolean chunked = false;
        try {
            if (httpMode) {
                String line;
                while (!((line = readLine(in)).isEmpty())) {
                    if (line.startsWith("Content-Length:"))
                        contentLength = Integer.valueOf(line.substring("Content-Length:".length()).replaceAll(" ", ""));
                    else if (line.equals("Transfer-Encoding: chunked"))
                        chunked = true;
                    out.write(line.getBytes());
                    out.write("\r\n".getBytes());
                    out.flush();
                }
                out.write("\r\n".getBytes());
                out.flush();
                if (!chunked && contentLength == 0)
                    return;
            }
            while ((length = in.read(buffer)) != -1) {
                out.write(buffer, 0, length);
                out.flush();
                if (httpMode) {
                    if (chunked
                            && buffer[length - 5] == '0'
                            && buffer[length - 4] == '\r'
                            && buffer[length - 3] == '\n'
                            && buffer[length - 2] == '\r'
                            && buffer[length - 1] == '\n') {
                        break;
                    }
                    if ((contentLength -= length) == 0)
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void setHttpMode(boolean flag) {
        this.httpMode = flag;
    }

    private String readLine(InputStream in) throws IOException {
        int temp;
        int length = 0;
        while ((temp = in.read()) != '\r' && temp != -1)
            buffer[length++] = (byte) temp;
        in.read();  //取出'\n'
        return new String(buffer, 0, length);
    }
}
