package s17201321.util;

import s17201321.entity.PeerMsg;
import s17201321.trackerserver.TrackerServer;
import s17201321.encryption.EncryptionUtils;
import s17201321.entity.FileBlock;
import s17201321.entity.Lock;
import s17201321.p2p.PeerThread;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.logging.Logger;

/**
 * The DataUtils is a class to use for dealing some data
 * @author 17201321-吴新悦
 */
public class DataUtils {
    private final static Logger logger = Logger.getLogger(DataUtils.class.toString());

    public static void divideFile(byte[] allData,String filename){
        try {
            System.out.println("start divide data...");
            System.out.println("byte total len :" + allData.length);
            int allLen = allData.length;
            List<PeerThread> pts = TrackerServer.getTrackerServer().getPeerThreads();
            int count = pts.size();
            System.out.println("pt size:" + count);
            int average = allLen / count;
            int rest = allLen - average * count;
            List<byte[]> blocks = getDivideList(count, allData, average, rest);
            System.out.println("block num:"+blocks.size());
            for (int i = 0; i < count; i++) {
                FileBlock fb = new FileBlock(filename,blocks.get(i),i+1, EncryptionUtils.getHash(filename,"SHA"));
                pts.get(i).addFileBlock(fb);
            }
            Lock.getLock2().notifyAll();
        }catch (Exception e){
            logger.severe("divide file error:"+e);
            System.out.println("divide file error:"+e);
        }
    }

    public static List<byte[]> getDivideList(int count,byte[] allData,int average,int rest){
        List<byte[]> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            byte[] bytes;
            if (i==count-1){
                bytes = new byte[average + rest];
                for (int j = 0; j < bytes.length; j++) {
                    bytes[j] = allData[j+i*average];
                }
            }else {
                bytes = new byte[average];
                for (int j = 0; j < bytes.length; j++) {
                    bytes[j] = allData[j+i*average];
                }
            }
            list.add(bytes);
        }
        return list;
    }

    public static void getAllBytes(byte[] allData,List<byte[]> list){
        int i = 0;
        for (byte[] bs:list) {
            for (byte b:bs) {
                if (i<allData.length) {
                    allData[i] = b;
                    i++;
                }
            }
        }
    }

    public static void stringToBytes(String str,byte[] bytes){
        byte[] bs = str.getBytes();
        System.arraycopy(bs, 0, bytes, 0, bs.length);
    }

    public static int returnActualLength(byte[] data) {
        int i = 0;
        for (; i < data.length; i++) {
            if (data[i] == '\0')
                break;
        }
        return i;
    }

    public static List<PeerMsg> getMatchResult(List<PeerThread> pts,String filename){
        List<PeerMsg> pms = new ArrayList<>();
        try {
            FileInputStream fis = new FileInputStream("datasource/torrents/"+filename+".torrent");
            Map<Object,Object> msg = AnalysisTorrent.getTorrentMsg(fis);
            for (PeerThread pt : pts) {
                List<String> list = getPeerFileData("datasource/peers/peer" + pt.getNo() + ".txt");
                Set set = new HashSet(list);
                for (Object s : set) {
                    assert msg != null;
                    if (Objects.equals(EncryptionUtils.getHash((String) s, "SHA"), msg.get("validate"))) {
                        PeerMsg pm = new PeerMsg(pt.getLocalHost(), pt.getPort(),pt.getNo());
                        pms.add(pm);
                    }
                }
            }
        }catch (Exception e){
            System.out.println("getMathResult occur error:"+e);
        }
        return pms;
    }

    public static List<String> getPeerFileData(String filename){
        List<String> fileList = new ArrayList<>();
        try {
            FileInputStream fis = new FileInputStream(filename);
            BufferedReader bf = new BufferedReader(new InputStreamReader(fis));
            String f = null;
            while ((f=bf.readLine())!=null)
                fileList.add(f);

        }catch (Exception e){
            System.out.println("getPeerFileData error:"+e);
        }
        return fileList;
    }

    public static String getNameBySHA(String validate){
        String name = "";
        try {
            FileInputStream fis = new FileInputStream("datasource/SHAtable.info");
            BufferedReader bf = new BufferedReader(new InputStreamReader(fis));
            String str = null;
            List<String> list = new ArrayList<>();
            while ((str=bf.readLine())!=null)
                list.add(str);
            Set<String> set = new HashSet<>(list);
            for (String s:set) {
                if (s.split(":=")[0].equals(validate))
                    name = s.split(":=")[1];
            }
        }catch (Exception e){
            System.out.println("getNameBySHA():"+e);
        }
        return name;
    }

    public static String getCastFilename(String origin){
        String str = null;
        try {
            str = origin.replaceAll("\\\\","/");
            str = str.replaceFirst(":",":/");
        }catch (Exception e){
            System.out.println("getCastFilename:"+e);
        }
        return str;
    }

    public static void main(String[] args) {
        Set<String> set = new HashSet(getPeerFileData("datasource/peers/peer1.txt"));
        System.out.println(set);
    }
}
