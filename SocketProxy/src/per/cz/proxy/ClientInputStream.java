package per.cz.proxy;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by 橙子 on 2017/5/14.
 */
public class ClientInputStream extends BufferedInputStream {

    private byte[] buffer;
    private int readPosition;
    private int writePosition;
    private String method;
    private int contentLength;
    private Http analysis;

    public ClientInputStream(InputStream inputStream) {
        super(inputStream);
        buffer = new byte[65536];
        clear();
    }

    @Override
    public int read(byte[] b) throws IOException {
        int result;
        if (method.equals("CONNECT")) {
            result = super.read(b);
        } else {
            while (true) {
                int length = writePosition - readPosition;
                for (result = 0; result < length; result++)
                    b[result] = buffer[readPosition++];
                if (result == 0)
                    for (result = 0; result < contentLength; result++)
                        b[result] = (byte) super.read();
                else break;
                if (result == 0) {
                    clear();
                    analysis.doJob();
                } else break;
            }
        }
        return result;
    }

    public String readLine(boolean original) throws IOException {
        int temp;
        String line = "";
        while ((temp = super.read()) != '\r' && temp != -1)
            line += (char) temp;
        if (temp != -1)
            super.read();
        else throw new IOException("return -1");
        if (contentLength == -1)
            contentLength = 0;
        // 是否去除头部首地址(严重影响效率= =)
        if (!original) {
            if (writePosition == 0) {
                String[] split = line.split("\\s");
                line = "";
                method = split[0];
                int index = split[1].indexOf('/', "http://".length());
                if (index != -1)
                    split[1] = split[1].substring(index);
                for (String value : split)
                    line += value + " ";
                line = line.substring(0, line.length() - 1);
            } else if (line.startsWith("Content-Length:") && contentLength == 0) {
                String content = line.substring("Content-Length:".length());
                content = content.replaceAll(" ", "");
                contentLength = Integer.valueOf(content);
            }
        } else if (writePosition == 0)
            method = line.split("\\s")[0];
        for (int i = 0; i < line.length(); i++)
            buffer[writePosition++] = ((byte) line.charAt(i));
        buffer[writePosition++] = (byte) '\r';
        buffer[writePosition++] = (byte) '\n';
        return line;
    }

    public void setAnalysis(Http analysis) {
        this.analysis = analysis;
    }

    public void clear() {
        readPosition = 0;
        writePosition = 0;
        contentLength = -1;
    }
}
