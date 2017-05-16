package per.cz.proxy;

import java.io.*;

/**
 * Created by 橙子 on 2017/5/13.
 */
public class Pipe extends Thread {
    private BufferedInputStream src;
    private BufferedOutputStream dest;
    private final byte[] buffer;
    private Http analysis;

    public Pipe(BufferedInputStream src, BufferedOutputStream dest) {
        setPriority(Thread.MIN_PRIORITY);
        this.src = src;
        this.dest = dest;
        buffer = new byte[1024 * 1024];
    }

    public Pipe(BufferedInputStream src, BufferedOutputStream dest, Http analysis) {
        this.src = src;
        this.dest = dest;
        buffer = new byte[96000];
        this.analysis = analysis;
    }


    @Override
    public void run() {
        try {
            int length;
            while ((length = src.read(buffer)) != -1) {
                if (analysis != null && analysis.isSocketExchanged()) {
                    // 输出流改变
                    this.dest.close();
                    this.dest = new BufferedOutputStream(analysis.getProxySocket().getOutputStream());
                }
                dest.write(buffer, 0, length);
                dest.flush();
            }
        } catch (Exception ignored) {
        }
        try {
            dest.close();
        } catch (IOException ignored) {
        }
    }
}
