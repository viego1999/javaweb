package s17201321.util;

import org.junit.Test;
import s17201321.encryption.EncryptionUtils;
import s17201321.entity.FileBlock;
import s17201321.trackerserver.TrackerServer;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;

/**
 * a class to use deal file
 * @author 17201321-吴新悦
 */
public class FileUtils {
    /**
     * 将文件大小由Byte转为MB或者KB
     * @return file size with B to KB
     */
    public static String fileSizeByteToM(Long size) {
        BigDecimal fileSize = new BigDecimal(size);
        BigDecimal param = new BigDecimal(1024);
        int count = 0;
        while(fileSize.compareTo(param) > 0 && count < 5) {
            fileSize = fileSize.divide(param);
            count++;
        }
        DecimalFormat df = new DecimalFormat("#.##");
        String result = df.format(fileSize) + "";
        switch (count) {
            case 0:
                result += "B";break;
            case 1:
                result += "KB";break;
            case 2:
                result += "MB";break;
            case 3:
                result += "GB";break;
            case 4:
                result += "TB";break;
            case 5:
                result += "PB";break;
        }
        return result;
    }

    /**
     * 转换文件大小
     * @return  file(s) size with B to GB
     */
    public static String formatFileSize(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        String wrongSize = "0B";
        if (fileS == 0) {
            return wrongSize;
        }
        if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + "KB";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + "MB";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) + "GB";
        }
        return fileSizeString;
    }

    public static StringBuilder getUltimateName(File file,String direction,String filename){
        StringBuilder sb = new StringBuilder(filename);
        int i = 0;
        while (file.exists()){
            System.out.println(file.getName()+" is already exist!");
            i++;
            sb = new StringBuilder(filename);
            int insertIndex = filename.lastIndexOf(".");
            sb.insert(insertIndex,"("+i+")");
            file = new File(direction+sb);
        }
        return sb;
    }

    public static byte[] getDestinyFile(String filename, Map<String,byte[]> fileMaps){
        if (fileMaps.containsKey(filename))
            return fileMaps.get(filename);
        else{
            System.out.println("未找到指定文件！");
            return null;
        }
    }

    public static File[] getAllFiles(String direction){
        File[] files = new File(direction).listFiles();
        assert files != null;
        System.out.println("文件夹下文件数量有："+files.length);

        return files;
    }

    public static void addDataToFile(String data,String filename){
        File file = new File(filename);
        FileOutputStream fos = null;
        OutputStreamWriter osw = null;

        try {
            if (!file.exists()){
                boolean create = file.createNewFile();
                if (create){
                    System.out.println("file not exist,create file successfully!");
                }
                fos = new FileOutputStream(file);
            }else {
                System.out.println("file have exist!");
                fos = new FileOutputStream(file,true);
            }
            osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            osw.write(data);
            System.out.println("add data to file finish!");
        }catch (Exception e){
            System.out.println("addData occur error:"+e);
        }finally {
            try {
                assert osw != null;
                osw.close();
                fos.close();
            }catch (IOException e){
                System.out.println("Close error:"+e);
            }
        }
    }

    public static void addSHAData(String rawFilename){
        File file = new File("datasource/SHAtable.info");
        FileOutputStream fos = null;
        OutputStreamWriter osw = null;

        try {
            if (!file.exists()){
                boolean create = file.createNewFile();
                if (create){
                    System.out.println("SHAtable not exist,create table successfully!");
                }
                fos = new FileOutputStream(file);
            }else {
                System.out.println("SHAtable have exist!");
                fos = new FileOutputStream(file,true);
            }
            osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            osw.write(EncryptionUtils.getHash(rawFilename,"SHA")+":="+rawFilename+"\n");
        }catch (Exception e){
            System.out.println("addSHAData occur error:"+e);
        }finally {
            try {
                assert osw != null;
                osw.close();
                fos.close();
            }catch (IOException e){
                System.out.println("Close error:"+e);
            }
        }
    }

    public static void createPeerFileAndDir(boolean isConnect,int No){
        if (isConnect){
            File file = new File("datasource/peers/peer"+No+".txt");
            File direction = new File("D://test/peer"+No);
            FileOutputStream fos = null;
            try {
                if (!file.exists()){
                    boolean create = file.createNewFile();
                    if (create){
                        System.out.println("peer"+No+".txt not exist,create table successfully!");
                    }
                    fos = new FileOutputStream(file);
                }else {
                    System.out.println("peer"+No+".txt have exist!");
                    fos = new FileOutputStream(file,true);
                }
                if (!direction.exists()){
                    System.out.println("workstation not exist,now create "+direction.mkdir());
                    System.out.println("create peer station successfully!");
                }
            }catch (Exception e){
                System.out.println("create peer file error:"+e);
            }finally {
                try {
                    assert fos != null;
                    fos.close();
                } catch (IOException e) {
                    System.out.println("close error:"+e);
                }
            }
        }else {
            System.out.println("未连接，创建peer文件失败！");
        }
    }

    public static boolean createFileByBlock(final List<FileBlock> fileBlocks,String direction,String filename){
        File file = new File(direction+filename);
        StringBuilder ultimate = getUltimateName(file,direction,filename);
        FileOutputStream fos = null;
        byte[] data = null;

        if (fileBlocks.size()>0){
            if (fileBlocks.size()>1) {
                Collections.sort(fileBlocks, new Comparator<FileBlock>() {
                    @Override
                    public int compare(FileBlock o1, FileBlock o2) {
                        return Integer.compare(o1.getNo(), o2.getNo());
                    }
                });
                int len = 0;
                List<byte[]> list = new ArrayList<>();
                for (FileBlock f : fileBlocks) {
                    len += f.getData().length;
                    list.add(f.getData());
                }
                data = new byte[len];
                DataUtils.getAllBytes(data, list);

            }else {
                data = fileBlocks.get(0).getData();
            }
            file = new File(direction+ultimate);
            try {
                fos = new FileOutputStream(file);
                fos.write(data);
                fos.flush();
                fos.close();
                return true;
            } catch (IOException e) {
                System.out.println("createFileByBlock error:"+e);
                return false;
            }
        }else {
            System.out.println("fileBlock.size() == 0");
            return false;
        }
    }
}
