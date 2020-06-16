package s17201321.p2p;

import s17201321.entity.FileBlock;
import s17201321.entity.Lock;
import s17201321.entity.P2pLock;
import s17201321.entity.PeerMsg;
import s17201321.trackerserver.TrackerServer;
import s17201321.util.AnalysisTorrent;
import s17201321.util.DataUtils;
import s17201321.util.FileUtils;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class PeerDLRunnable extends P2pLock implements Runnable {
    private static Socket sockets;
    private String localHost;
    private int port;
    private static String filename;
    private static String direction;
    private int no;
    private List<PeerMsg> pms;
    private static List<FileBlock> fileBlocks;
    private static int blockNums;
    private static final Object lock2 = new Object();

    public PeerDLRunnable(Socket socket,String localHost,int port,String filenames,String directions,int no,List<PeerMsg> pms){
        sockets = socket;
        this.localHost = localHost;
        this.port = port;
        filename = filenames;
        direction = directions;
        this.no = no;
        this.pms = pms;
        fileBlocks = new ArrayList<>();
    }

    @Override
    public void run() {
        try {
            System.out.println("Peer DL Runnable socket is closed? "+sockets.isClosed());

            System.out.println(pms);

            FileInputStream fis = new FileInputStream("datasource/torrents/"+filename+".torrent");
            Map<Object,Object> msg = AnalysisTorrent.getTorrentMsg(fis);
            assert msg != null;
            String validate = (String) msg.get("validate");
            long allLen = Long.parseLong((String) msg.get("fileLength"));
            int blockNum = pms.size();
            blockNums = blockNum;
            int average = (int) (allLen/blockNum);
            int rest = (int) (allLen%blockNum);

            for (int i = 0;i<pms.size();i++) {
                Socket s = new Socket(pms.get(i).getIp(),pms.get(i).getPort());
                if (i==pms.size()-1)
                    new RequestDownloadThread(s,validate,i*average,
                            (i+1)*average-1+rest,i+1).start();
                else
                    new RequestDownloadThread(s,validate,i*average,
                            (i+1)*average-1,i+1).start();
            }

            System.out.println("PeerDL启动wait()，等待文件装载！");
            Thread.sleep(300);

        } catch (IOException | InterruptedException e) {
            System.out.println("无该种子文件的peer！");
            e.printStackTrace();
            System.out.println("PeerDLRunnable occur error:"+e);
        }
    }

    private static class RequestDownloadThread extends Thread{
        private Socket socket;
        private String validate;
        private int startIndex;
        private int endIndex;
        private int no;

        public RequestDownloadThread(Socket socket,String validate,int startIndex,int endIndex,int no){
            this.socket = socket;
            this.validate = validate;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.no = no;
        }

        @Override
        public void  run(){
            try {
                OutputStream os = socket.getOutputStream();
                PrintWriter pw = new PrintWriter(os,true);

                pw.println(validate);
                pw.println(startIndex);
                pw.println(endIndex);

                os.flush();
                socket.shutdownOutput();

                synchronized (lock) {
                    System.out.println("启动wait()，等待peer发送数据！");
                    System.out.println("启动start()，开始接收数据！");
                    new ReceiveThread(socket, validate, no).start();
                }
            }catch (Exception e){
                System.out.println("request download error:"+e);
            }
        }
    }

    private static class ReceiveThread extends Thread{
        private Socket socket;
        private String validate;
        private int no;

        public ReceiveThread(Socket socket,String validate,int no){
            this.socket = socket;
            this.validate = validate;
            this.no = no;
        }

        public void run(){
            List<byte[]> dataList = new LinkedList<>();
            long allLen = 0;
            try {
                InputStream is = socket.getInputStream();
                BufferedReader bf = new BufferedReader(new InputStreamReader(is));
                int len = 0;
                byte[] flush = new byte[1024];
                while ((len=is.read(flush))!=-1){
                    allLen+=len;
                    dataList.add(Arrays.copyOfRange(flush,0,len));
                }

                byte[] data = new byte[(int) allLen];
                DataUtils.getAllBytes(data,dataList);

                FileBlock fileBlock = new FileBlock(DataUtils.getNameBySHA(validate), data, no, validate);
                fileBlocks.add(fileBlock);

                synchronized (lock2){
                    if (fileBlocks.size()==blockNums){
                        System.out.println("已接收完所有文件块，notify PeerDL");

                        System.out.println("文件装载成功，PeerDL开始启动！");
                        System.out.println("fileBlocks size:" + fileBlocks.size());
                        if (FileUtils.createFileByBlock(fileBlocks,direction+"/",filename)){
                            System.out.println("下载成功！");

                            FileUtils.addDataToFile(DataUtils.getNameBySHA(validate)+"\n",filename);

                            FileOutputStream fos = new FileOutputStream("datasource/history/downloadhistory.txt",true);
                            fos.write(("peer"+no+"下载了文件:"+DataUtils.getNameBySHA(validate)+"\n").getBytes());
                            fos.flush();
                            fos.close();

                            PrintWriter pw = new PrintWriter(sockets.getOutputStream());
                            pw.println("下载完成");
                            pw.flush();
                            sockets.shutdownOutput();

                            socket.close();
                        }else {
                            System.out.println("下载失败！");
                        }
                    }else {
                        System.out.println("未接接收所有文件块，继续接收...");
                    }
                }
                sleep(100);
            } catch (Exception e) {
                System.out.println("receive occur error:" + e);
            }
        }
    }
}
