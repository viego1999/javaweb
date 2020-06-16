package s17201321.p2p;

import s17201321.util.AnalysisTorrent;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.util.List;

public class BTDownloadRunnable implements Runnable{
    private Socket socket;
    private List<PeerThread> pts;

    public BTDownloadRunnable(){

    }

    public BTDownloadRunnable(Socket socket,List<PeerThread> pts){
        this.socket = socket;
        this.pts = pts;
    }


    @Override
    public void run() {
        try {
            InputStream is = socket.getInputStream();
            byte[] filename_b = new byte[1024];
            byte[] location_b = new byte[1024];
            FileInputStream fis = null;

            if (is.available()>0){
                int fl = is.read(filename_b);
                int ll = is.read(location_b);
                String filename = new String(filename_b).trim();
                String location = new String(location_b).trim();

                fis = new FileInputStream("Torrents/"+filename+".torrent");
                System.out.println("文件可用："+fis.available());

                System.out.println(AnalysisTorrent.getTorrentMsg(fis));

            }

        }catch (Exception e){
            System.out.println("bt download error:"+e);
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public List<PeerThread> getPts() {
        return pts;
    }

    public void setPts(List<PeerThread> pts) {
        this.pts = pts;
    }
}
