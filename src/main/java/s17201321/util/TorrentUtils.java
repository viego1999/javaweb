package s17201321.util;

import s17201321.trackerserver.TrackerServer;
import s17201321.encryption.EncryptionUtils;
import s17201321.p2p.PeerThread;

import java.io.*;
import java.net.ServerSocket;
import java.util.*;

/**
 * The TorrentUtils is a util class is used to create torrent file
 * @author 17201321-吴新悦
 */
public class TorrentUtils {

    public static void createTorrent(String filename,String rawFilename,byte[] allData){
        try {
            String torrentFilename = filename.trim();
            File file = new File("datasource/torrents/" +torrentFilename+".torrent");
            System.out.println(file.getPath());
            FileOutputStream fos = new FileOutputStream(file);
            writeData(fos,filename,rawFilename,allData);
            TrackerServer.getTrackerServer().addTorrents(torrentFilename+".torrent");
        }catch (IOException e){
            System.out.println(TorrentUtils.class +".createTorrent "+e);
        }
    }

    public static void writeData(FileOutputStream fos,String filename,String rawFilename,byte[] allData){
        try {
            Map<String, String> messages = new LinkedHashMap<>();
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            ServerSocket ss = TrackerServer.getTrackerServer().getServerSocket();
            List<PeerThread> pts = TrackerServer.getTrackerServer().getPeerThreads();

            messages.put("announce", ss.getInetAddress().getHostAddress() + ":=" + ss.getLocalPort());
            messages.put("filename", filename);
            messages.put("fileSize", FileUtils.fileSizeByteToM((long) allData.length));
            messages.put("fileLength",String.valueOf(allData.length));
            messages.put("blockMaxNum", String.valueOf(pts.size()));
//            int i = 0;
//            for (PeerThread pt : pts) {
//                i++;
//                messages.put("peer" + i, pt.getLocalHost() + ":=" + pt.getPort());
//            }
            messages.put("validate", EncryptionUtils.getHash(rawFilename, "SHA"));

            Set<String> keys = messages.keySet();
            for (String key : keys) {
                osw.write(key + ":=" + messages.get(key) + "\n");
            }
            osw.close();
            fos.close();
        }catch (Exception e){
            System.out.println("writeData error:"+e);
        }
    }

    public static void main(String[] args) {
        Map<String,String> messages = new HashMap<>();
        messages.put("xxx","s");
        messages.put("www","d");

        List<String> list = new ArrayList<>(messages.keySet());
        for (String s:list
             ) {
            System.out.println(s);
        }
    }
}
