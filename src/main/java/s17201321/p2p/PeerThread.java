package s17201321.p2p;

import s17201321.entity.FileBlock;
import s17201321.entity.PeerMsg;
import s17201321.util.DataUtils;
import s17201321.util.FileUtils;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author 17201321-吴新悦
 */
public class PeerThread extends Thread{
    private static final Logger logger = Logger.getLogger(PeerThread.class.toString());
    private Socket socket;
    private Socket acceptSocket;
    private int port;
    private String localHost;
    private List<FileBlock> fileBlocks;
    private int No;
    private final Object lock1 = new Object();

    public PeerThread(Socket socket){
        this.socket = socket;
        this.fileBlocks = new ArrayList<>();
        logger.info("peer  start connect to Server"+socket.getInetAddress()+"...");
        System.out.println("peer start connect to Server"+socket.getInetAddress()+" ...");
    }

    public PeerThread(Socket socket,String localHost,int port,int No){
        this.socket = socket;
        this.localHost = localHost;
        this.port = port;
        this.No = No;
        this.fileBlocks = new ArrayList<>();
        logger.info("peer  start connect to Server"+socket.getInetAddress()+"...");
        System.out.println("peer start connect to Server"+socket.getInetAddress()+" ...");
        if (socket.getInetAddress()!=null)
            FileUtils.createPeerFileAndDir(socket.isConnected(),No);
    }

    @Override
    public void run(){// 等待其它peer来进行下载
        try {
            ServerSocket ss = new ServerSocket(port,8, InetAddress.getByName(localHost));
            while (true) {
                Socket s = ss.accept();
                logger.info("peer request download file from peer"+s.getLocalAddress());
                System.out.println("peer request download file from peer"+s.getLocalAddress());

                new Thread(new SenderRunnable(s,No)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onloadFile(String filename){// 上传文件到服务器
        try {
            if (socket.isConnected()&&!socket.isOutputShutdown()) {
                socket.close();
                socket = new Socket(socket.getInetAddress(), socket.getPort());
            }
        }catch (Exception e){
            System.out.println("onload 1 error:"+e);
        }

        System.out.println("peer request onload file start...");
        if (!socket.isClosed()&&!socket.isOutputShutdown()) {
            try {
                FileInputStream fis = new FileInputStream(filename);
                InputStream is = new BufferedInputStream(fis);
                OutputStream outputStream = socket.getOutputStream();
                OutputStream os = new BufferedOutputStream(outputStream);

                System.out.println("onload file(s) size is:" + FileUtils.formatFileSize(fis.available()));

                byte[] request = new byte[1024];
                byte[] filename_b = new byte[1024];
                DataUtils.stringToBytes("onload", request);
                DataUtils.stringToBytes(filename, filename_b);
                outputStream.write(request);
                outputStream.write(filename_b);

                byte[] flush = new byte[1024];
                int len = 0;
                while ((len = is.read(flush)) != -1) {
                    //System.out.println(len);
                    os.write(flush, 0, len);
                }
                os.flush();
                is.close();
                //socket.close();
                socket.shutdownOutput();

                sleep(100);
                System.out.println("Onload file success...");
                FileUtils.addDataToFile(filename+"\n","datasource/peers/peer"+No+".txt");

                FileOutputStream fos = new FileOutputStream("datasource/history/onloadhistory.txt",true);

                fos.write(("peer"+No+"上传了文件:"+ filename+"\n").getBytes());
                fos.flush();
                fos.close();
                //socket.shutdownOutput();
            } catch (IOException | InterruptedException e) {
                //e.printStackTrace();
                logger.severe("peer onload failed...:" + e);
                System.out.println("peer onload failed...:" + e);
            }
        }else {
            try {
                System.out.println("重新打开连接!");
                socket = new Socket(socket.getInetAddress(),socket.getPort());
                onloadFile(filename);
            }catch (Exception e){
                System.out.println("open connect filed!"+e);
            }
        }
    }

    public void normalDownloadFile(String filename,String direction){
        try {
            if (socket.isConnected()&&!socket.isOutputShutdown()) {
                socket.close();
                socket = new Socket(socket.getInetAddress(), socket.getPort());
            }
        }catch (Exception e){
            System.out.println("onload 1 error:"+e);
        }

        System.out.println("peer request to download file by normal...");
        if (!socket.isClosed()&&!socket.isOutputShutdown()) {
            try {
                OutputStream os = socket.getOutputStream();
                byte[] request = new byte[1024];
                byte[] no_b = new byte[1024];
                byte[] filename_b = new byte[1024];
                byte[] location = new byte[1024];
                DataUtils.stringToBytes("normal download",request);
                DataUtils.stringToBytes(String.valueOf(No),no_b);
                DataUtils.stringToBytes(filename,filename_b);
                DataUtils.stringToBytes(direction+"/" + filename,location);
                os.write(request);
                os.write(no_b);
                os.write(filename_b);
                os.write(location);

                os.flush();
                new NormalReceiver(socket,filename,direction+"/",No).start();

                //socket.shutdownOutput();
            } catch (IOException e) {
                logger.severe("Peer download file error:" + e);
                System.out.println("Peer download file error:" + e);
            }
        }else {
            try {
                System.out.println("重新打开连接！");
                socket = new Socket(socket.getInetAddress(),socket.getPort());
                normalDownloadFile(filename,direction);
            }catch (Exception e){
                System.out.println("open connect filed!"+e);
            }
        }
    }

    public void btDownloadFile(String filename,String direction){
        try {
            if (socket.isConnected()&&!socket.isOutputShutdown()) {
                socket.close();
                socket = new Socket(socket.getInetAddress(), socket.getPort());
            }
        }catch (Exception e){
            System.out.println("onload 1 error:"+e);
        }

        System.out.println("peer request to download file by torrent...");
        if (!socket.isClosed()&&!socket.isOutputShutdown()) {
            try {
                OutputStream os = socket.getOutputStream();
                InputStream is = socket.getInputStream();
                byte[] request = new byte[1024];
                byte[] no_b = new byte[1024];
                byte[] filename_b = new byte[1024];
                byte[] filename_b2 = new byte[1024];
                byte[] location = new byte[1024];
                DataUtils.stringToBytes("bt download",request);
                DataUtils.stringToBytes(String.valueOf(No),no_b);
                DataUtils.stringToBytes(filename,filename_b);
                DataUtils.stringToBytes(filename,filename_b2);
                DataUtils.stringToBytes(direction+"/" + filename,location);
                os.write(request);
                os.write(no_b);
                os.write(filename_b);
                os.write(filename_b2);
                os.write(location);

                os.flush();

                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                boolean result = ois.readBoolean();
                List<PeerMsg> pms = (List<PeerMsg>) ois.readObject();
                System.out.println(result);
                System.out.println(pms);

                if (result) {
                    new Thread(new PeerDLRunnable(socket, localHost, port, filename, direction, No,pms)).start();
                }else {
                    new NormalReceiver(socket,filename,direction+"/",No).start();
                }
//                socket.shutdownOutput();
            } catch (IOException | ClassNotFoundException e) {
                logger.severe("Peer download file error:" + e);
                System.out.println("Peer download file error:" + e);
            }
        }else {
            try {
                System.out.println("重新打开连接！");
                socket = new Socket(socket.getInetAddress(),socket.getPort());
                btDownloadFile(filename,direction);
            }catch (Exception e){
                e.printStackTrace();
                System.out.println("open connect filed!"+e);
            }
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Socket getAcceptSocket() {
        return acceptSocket;
    }

    public void setAcceptSocket(Socket acceptSocket) {
        this.acceptSocket = acceptSocket;
    }

    public String getLocalHost() {
        return localHost;
    }

    public void setLocalHost(String localHost) {
        this.localHost = localHost;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public List<FileBlock> getFileBlocks() {
        return fileBlocks;
    }

    public int getNo() {
        return No;
    }

    public void setNo(int no) {
        No = no;
    }

    public void setFileBlocks(List<FileBlock> fileBlocks) {
        this.fileBlocks = fileBlocks;
    }

    public void addFileBlock(FileBlock fb){
        this.fileBlocks.add(fb);
    }
}
