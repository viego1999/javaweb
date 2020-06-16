package s17201321.p2p;

import s17201321.entity.Lock;
import s17201321.trackerserver.TrackerServer;
import s17201321.util.DataUtils;
import s17201321.util.FileUtils;

import java.io.*;
import java.net.Socket;
import java.util.Map;

/**
 * The NormalDLRunnable is class to deal a normal download request
 * @author 17201321-吴新悦
 */
public class NormalDlRunnable implements Runnable{
    private Socket socket;
    private Map<String,byte[]> fileMaps;
    private int no;

    public NormalDlRunnable(){

    }

    public NormalDlRunnable(Socket socket,Map<String,byte[]> fileMaps,int no){
        this.socket = socket;
        this.fileMaps = fileMaps;
        this.no = no;
    }

    @Override
    public void run() {
        try {
            System.out.println("start normal download...!");
            InputStream is = socket.getInputStream();
            OutputStream os = null;
            OutputStream outputStream = socket.getOutputStream();

            if (is.available()>0) {
                byte[] fb = new byte[1024];
                byte[] lb = new byte[1024];
                int fl = is.read(fb);
                int ll = is.read(lb);
                String filename = new String(fb).trim();
                String downloadLocation = new String(lb).trim();
                String direction = downloadLocation.replace(filename,"");

                byte[] msg = new byte[1024];
                DataUtils.stringToBytes("发送完成",msg);

                byte[] data = FileUtils.getDestinyFile(filename,fileMaps);

                assert data != null;
                System.out.println("文件大小为："+FileUtils.formatFileSize(data.length));
                outputStream.write(msg);
                outputStream.write(data);
                outputStream.flush();
                socket.shutdownOutput();

                BufferedReader bf = new BufferedReader(new InputStreamReader(is));
                String result = bf.readLine();
                System.out.println(result);

                is.close();
                socket.close();

                FileUtils.addDataToFile("D://copy/"+filename+"\n","datasource/peers/peer"+no+".txt");

                FileOutputStream fos = new FileOutputStream("datasource/history/downloadhistory.txt",true);
                fos.write(("peer"+no+"下载了文件:D://copy/"+ filename+"\n").getBytes());
                fos.flush();
                fos.close();

                System.out.println("normal download successfully!");

                synchronized (Lock.getLock()) {
                    TrackerServer ts = TrackerServer.getTrackerServer();
                    ts.setDownloadNum(ts.getDownloadNum() - 1);
                    System.out.println("下载数量--:"+ts.getDownloadNum());
                }
            }
        }catch (Exception e){
            synchronized (Lock.getLock()) {
                TrackerServer ts = TrackerServer.getTrackerServer();
                ts.setDownloadNum(ts.getDownloadNum() - 1);
                System.out.println("下载数量--:"+ts.getDownloadNum());
            }
            System.out.println("normal download error:"+e);
        }
    }
}
