package s17201321.p2p;

import s17201321.trackerserver.TrackerServer;
import s17201321.util.DataUtils;
import s17201321.util.FileUtils;
import s17201321.util.TorrentUtils;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * The DownloadRunnable is used to download file uploaded by Peers
 * @author 17201321-吴新悦
 */
public class DownloadRunnable implements Runnable{
    private static final Logger logger = Logger.getLogger(DownloadRunnable.class.toString());
    private Socket socket;
    private String filename;

    public DownloadRunnable(Socket socket){
        this.socket = socket;
    }

    public DownloadRunnable(Socket socket, String filename) {
        this.socket = socket;
        this.filename = filename;
    }

    public void run(){
        try {
            logger.info("TrackerServer "+socket.getInetAddress()+":"+socket.getPort()+" Download file start...");
            System.out.println("TrackerServer download file start...");
            System.out.println("D://copy/"+getFilename());
            InputStream is = new BufferedInputStream(socket.getInputStream());

            File file = new File("D://copy/"+getFilename());
            String direction = "D://copy/";
            StringBuilder sb = FileUtils.getUltimateName(file,direction,getFilename());// 避免有重名文件，进行判断处理,获取最终文件名
            file = new File("D://copy/"+sb);

            FileOutputStream fos = new FileOutputStream(file);
            OutputStream os = new BufferedOutputStream(fos);

            System.out.println("available is:"+is.available());
            List<byte[]> list = new ArrayList<>();
            int totalLen = 0;
            int len = 0;
            byte[] flush = new byte[1024];
            while ((len=is.read(flush))!=-1){
                totalLen+=len;
                fos.write(flush,0,len);
                list.add(Arrays.copyOfRange(flush,0,len));
            }

            byte[] allData = new byte[totalLen];
            DataUtils.getAllBytes(allData,list);
            TrackerServer.getTrackerServer().addFileToMap(sb.toString(),allData);

            System.out.println("download file(s) size is:"+FileUtils.fileSizeByteToM((long)totalLen));

            TorrentUtils.createTorrent(sb.toString(),filename,allData);// 创建种子文件

            OutputStream outputStream = socket.getOutputStream();
            outputStream.write("上传完成！".getBytes());

            FileUtils.addSHAData(filename);// 向SHA表增加文件数据

            fos.flush();
            fos.close();
            os.flush();
            os.close();
            //socket.close();
            System.out.println("TrackerServer download success...");
            Thread.sleep(1000);
        }catch (IOException | InterruptedException e){
            logger.info("下载失败，error:"+e);
            System.out.println("下载失败，error:"+e);
        }
    }

    public String getFilename(){
        try {
            return filename.substring(filename.lastIndexOf("/")+1).trim();
        }catch (Exception e){
            System.out.println("文件名解析失败："+e);
            return filename;
        }
    }
}
