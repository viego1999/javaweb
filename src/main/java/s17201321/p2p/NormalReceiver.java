package s17201321.p2p;

import s17201321.util.DataUtils;
import s17201321.util.FileUtils;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 用来接收正常数据的线程处理类
 * @see java.lang.Thread
 * @author 17201321-吴新悦
 */
public class NormalReceiver extends Thread{
    private Socket socket;
    private String filename;
    private String direction;
    private int no;

    public NormalReceiver(Socket socket,String filename,String direction,int no){
        this.socket = socket;
        this.filename = filename;
        this.direction = direction;
        this.no = no;
    }

    @Override
    public void run(){
        PrintWriter pw = null;
        System.out.println(socket.isOutputShutdown());
        try {
            InputStream is = socket.getInputStream();
            pw = new PrintWriter(socket.getOutputStream());
            byte[] s = new byte[1024];
            int l = is.read(s);
            if (new String(s).trim().equals("发送完成")) {
                OutputStream os = null;
                File file = new File(direction + filename);
                StringBuilder ultimate = FileUtils.getUltimateName(file, direction, filename);
                os = new FileOutputStream(direction + ultimate);
                List<byte[]> list = new ArrayList<>();
                int allLen = 0;
                int len = 0;
                byte[] flush = new byte[1024];
                while ((len=is.read(flush))!=-1){
                    allLen+=len;
                    os.write(flush,0,len);
                    list.add(Arrays.copyOfRange(flush,0,len));
                }

                byte[] data = new byte[allLen];
                System.out.println("文件大小为："+FileUtils.fileSizeByteToM((long) data.length));
                DataUtils.getAllBytes(data, list);
                //os.write(data);
                os.flush();
                os.close();
                System.out.println("正常下载完成！");
                pw.println("下载完成");
                pw.flush();

                socket.close();
            }else {
                System.out.println("未传送完数据。");
            }
        } catch (IOException e) {
            try {
                pw = new PrintWriter(socket.getOutputStream());
                pw.println("下载失败");
                pw.flush();
                socket.shutdownOutput();
            } catch (IOException ioException) {
                System.out.println("IO normalReceiver:"+e);
            }
            System.out.println("NormalReceiver occur error:"+e);
        }
    }
}
