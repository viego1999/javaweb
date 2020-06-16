package s17201321.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;

public class AnalysisTorrent {

    public static Map<Object,Object> getTorrentMsg(FileInputStream fis){
        Map<Object,Object> maps = new LinkedHashMap<>();
        try {
            if (fis!=null){
                BufferedReader bf = new BufferedReader(new InputStreamReader(fis));
                String trackerMsg = bf.readLine();
                String filenameMsg = bf.readLine();
                String fileSizeMsg = bf.readLine();
                String fileLength = bf.readLine();
                String blockNumMsg = bf.readLine();
                String vm = bf.readLine();

                maps.put("trackerIp",trackerMsg.split(":=")[1]);
                maps.put("trackerPort",trackerMsg.split(":=")[2]);
                maps.put("filename",filenameMsg.split(":=")[1]);
                maps.put("fileSize",fileSizeMsg.split(":=")[1]);
                maps.put("fileLength",fileLength.split(":=")[1]);
                maps.put("blockMaxNum",blockNumMsg.split(":=")[1]);
                maps.put("validate",vm.split(":=")[1]);

                bf.close();

                return maps;
            }
        }catch (Exception e){
            System.out.println("analysis error:"+e);
            return null;
        }
        return maps;
    }
}
