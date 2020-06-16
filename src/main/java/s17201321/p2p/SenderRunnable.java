package s17201321.p2p;

import s17201321.encryption.EncryptionUtils;
import s17201321.entity.Lock;
import s17201321.entity.P2pLock;
import s17201321.trackerserver.TrackerServer;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class SenderRunnable extends P2pLock implements Runnable{
    private Socket socket;
    private int No;

    public SenderRunnable(Socket socket,int No){
        this.socket = socket;
        this.No = No;
    }

    public void run(){
        try {
            synchronized (lock) {
                InputStream is = socket.getInputStream();
                OutputStream os = socket.getOutputStream();
                BufferedReader bf = null;
                FileInputStream fis = new FileInputStream("datasource/peers/peer" + No + ".txt");

                bf = new BufferedReader(new InputStreamReader(is));
                String validate = bf.readLine();
                int startIndex = Integer.parseInt(bf.readLine());
                int endIndex = Integer.parseInt(bf.readLine());
                byte[] data = getObjectData(fis, validate, startIndex, endIndex);
                System.out.println(validate + " " + startIndex + " " + endIndex + " " + data.length);
                os.write(data, 0, data.length);

                os.flush();
                System.out.println("查找到数据，发送成功，notify RequestThread！");
                lock.notify();

                socket.shutdownOutput();
            }
        }catch (Exception e){

            System.out.println("Sender occur error:"+e);
        }
    }

    public byte[] getObjectData(FileInputStream fis,String validate,int start,int end){
        BufferedReader bf = new BufferedReader(new InputStreamReader(fis));
        FileInputStream fileInputStream = null;
        int l = end - start + 1;
        byte[] data = new byte[l];

        List<String> list = new ArrayList<>();
        String filename = "";
        String str = null;
        try {
            while (((str=bf.readLine())!=null))
                list.add(str);
            Set<String> set = new HashSet<>(list);
            for (String s:set) {
                if (Objects.equals(EncryptionUtils.getHash(s, "SHA"), validate))
                    filename = s;
            }
            System.out.println("f:"+filename);
            fileInputStream = new FileInputStream(filename);
            byte[] flush = new byte[fileInputStream.available()];
            int len = fileInputStream.read(flush);
            System.arraycopy(flush,start,data,0,l);

        } catch (IOException e) {
            System.out.println("getObjectData occur error:"+e);
        }
        return data;
    }
}
