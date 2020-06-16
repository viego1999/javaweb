package s17201321.trackerserver;

import s17201321.entity.PeerMsg;
import s17201321.util.DataUtils;
import s17201321.util.FileUtils;
import s17201321.p2p.DownloadRunnable;
import s17201321.entity.Lock;
import s17201321.p2p.NormalDlRunnable;
import s17201321.p2p.PeerThread;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The TrackerServer class is a Tracker used to trace and service peers
 * @see java.lang.Thread
 * @author 17201321-吴新悦
 */
public class TrackerServer extends Thread{
    private static final Logger logger = Logger.getLogger(TrackerServer.class.toString());

    private static TrackerServer trackerServer = new TrackerServer();
    public static int downloadNum;
    ServerSocket serverSocket = null;
    private List<PeerThread> peerThreads;
    private List<String> peers;
    private int port;
    private boolean isRun = true;
    private Map<String,byte[]> filesMap;
    private List<String> torrents;

    private TrackerServer(){
        port = 2020;
        peerThreads = new ArrayList<>();
        peers = new ArrayList<>();
        filesMap = new HashMap<>();
        torrents = new ArrayList<>();
        init();
    }

    private TrackerServer(int port){
        this.port = port;
        peerThreads = new ArrayList<>();
        peers = new ArrayList<>();
        filesMap = new HashMap<>();
        torrents = new ArrayList<>();
        init();
    }

    public void run(){
        try {
            serverSocket = new ServerSocket(port,8,InetAddress.getByName("127.0.0.1"));
            int i = 0;
            while (isRun) {
                Socket socket = serverSocket.accept();
                logger.info("peer connect to Server"+socket.getLocalAddress()+"  successfully!");
                System.out.println("peer connect to Server"+socket.getInetAddress()+"  successfully!");
                InputStream is = socket.getInputStream();
                PrintWriter pw = null;

                if (i==0||is.available()<1024) {
                    BufferedReader bf = new BufferedReader(new InputStreamReader(is));

                    String ip = bf.readLine();
                    String port = bf.readLine();
                    String No = bf.readLine();
                    System.out.println(ip + "  " + port + "  " + No);
                    try {
                        if (ip.contains("退出连接")){
                            String ipp = ip.split(":")[1].trim();
                            String portt = ip.split(":")[2].trim();
                            String noo = ip.split(":")[3].trim();
                            System.out.println(ipp+" "+portt+" "+noo);
                            peerThreads.removeIf(pt -> (pt.getNo() + "").equals(noo) && (pt.getPort() + "").equals(portt) && pt.getLocalHost().equals(ipp));

                            System.out.println("退出成功！");
                            i--;
                        }else {
                            boolean isExist = false;
                            for (PeerThread pt : peerThreads) {
                                if ((pt.getNo() + "").equals(No) && (pt.getPort() + "").equals(port) && pt.getLocalHost().equals(ip)) {
                                    isExist = true;
                                    break;
                                }
                            }
                            if (!isExist) {
                                PeerThread pt = new PeerThread(new Socket(), ip, Integer.parseInt(port), Integer.parseInt(No));
                                peerThreads.add(pt);
                                i++;

                                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                                oos.writeObject(filesMap);
                                oos.writeObject(torrents);
                                oos.flush();

                                //socket.shutdownOutput();
                            } else {
                                System.out.println("不是第一次连接，已保存Peer信息！");
                            }
                        }
                    }catch (Exception e){
                        System.out.println("null or error:"+e);
                        System.out.println("不是第一次连接，已保存Peer信息！");
                    }
                }

                int available = is.available();
                System.out.println("server accept request available is :"+available);
                if (available>0) {
                    byte[] request_b = new byte[1024];
                    int rl = is.read(request_b);
                    String request = new String(request_b).trim();
                    System.out.println("请求指令为：" + request);

                    if (request.equals("onload")) {
                        new DealThread(socket).start();

                    } else if (request.equals("normal download")){
                        if (downloadNum<=8) {
                            byte[] no_b = new byte[1024];
                            int nl = is.read(no_b);
                            int no = Integer.parseInt(new String(no_b).trim());

                            System.out.println("normal download!");
                            new Thread(new NormalDlRunnable(socket, filesMap, no)).start();

                            synchronized (Lock.getLock()) {
                                downloadNum++;
                                System.out.println("下载数量++：" + downloadNum);
                            }
                        }else {
                            System.out.println("下载人数超过最大数量限度："+downloadNum);
                        }
                    } else if (request.equals("bt download")){
                        if (downloadNum<=8) {
                            System.out.println("bt download!");
                            byte[] no_b = new byte[1024];
                            byte[] filename_b = new byte[1024];
                            int nl = is.read(no_b);
                            int no = Integer.parseInt(new String(no_b).trim());
                            int fl = is.read(filename_b);
                            String filename = new String(filename_b).trim();
                            System.out.println("请求bt文件为：" + filename + ".torrent");
                            List<PeerMsg> pms = DataUtils.getMatchResult(peerThreads, filename);
                            OutputStream os = socket.getOutputStream();
                            if (pms.size() > 0) {
                                pms.removeIf(pm -> pm.getNo() == no);// 移除请求者本身
                                if (pms.size() > 0) {
                                    System.out.println("已连接peer中找到有" + pms.size() + "个有该资源文件...");
                                    ObjectOutputStream ois = new ObjectOutputStream(socket.getOutputStream());
                                    ois.writeBoolean(true);
                                    ois.writeObject(pms);
                                    ois.flush();

                                    socket.shutdownOutput();

                                    synchronized (Lock.getLock()) {
                                        downloadNum++;
                                        System.out.println("下载数量++：" + downloadNum);
                                    }

                                    BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                    String result = br.readLine();

                                    if (result.contains("下载完成")) {
                                        System.out.println("下载完成");
                                    } else {
                                        System.out.println("下载失败");
                                    }
                                    synchronized (Lock.getLock()) {
                                        downloadNum--;
                                        System.out.println("下载数量--：" + downloadNum);
                                    }
                                } else {
                                    System.out.println("已连接的其它peer中没有用户有该文件资源，进行服务器下载...");
                                    ObjectOutputStream ois = new ObjectOutputStream(socket.getOutputStream());
                                    ois.writeBoolean(false);
                                    ois.writeObject(null);
                                    ois.flush();

                                    new Thread(new NormalDlRunnable(socket, filesMap, no)).start();
                                    synchronized (Lock.getLock()) {
                                        downloadNum++;
                                        System.out.println("下载数量++：" + downloadNum);
                                    }
                                }
                            } else {
                                System.out.println("已连接peer中没有用户有该文件资源，进行服务器下载...");
                                ObjectOutputStream ois = new ObjectOutputStream(socket.getOutputStream());
                                ois.writeBoolean(false);
                                ois.writeObject(null);
                                ois.flush();

                                new Thread(new NormalDlRunnable(socket, filesMap, no)).start();
                                synchronized (Lock.getLock()) {
                                    downloadNum++;
                                    System.out.println("下载数量++：" + downloadNum);
                                }
                            }
                        }else {
                            System.out.println("下载人数超过最大数量限度："+downloadNum);
                        }
                    } else if (request.equals("退出连接")){
                        byte[] ip_b = new byte[1024];
                        byte[] port_b = new byte[1024];
                        byte[] No_b = new byte[1024];

                        int il = is.read(ip_b);
                        int pl = is.read(port_b);
                        int nl = is.read(No_b);

                        String ip = new String(ip_b).trim();
                        String port = new String(port_b).trim();
                        String no = new String(No_b).trim();

                        peerThreads.removeIf(pt -> (pt.getNo() + "").equals(no) && (pt.getPort() + "").equals(port) && pt.getLocalHost().equals(ip));

                        System.out.println("退出成功！");
                    } else {
                        System.out.println("error request!");
                    }
                }
                sleep(100);
            }
        }catch (Exception e){
            logger.log(Level.WARNING,"server run error:"+e);
            logger.severe("server run error:"+e);
        }
    }

    public void connect(String host,int port){
        try {
            Socket socket = new Socket(host,port);
            socket.setSoTimeout(10000);
            String remoteHost = socket.getInetAddress().getHostAddress();
            int remotePort = socket.getPort();
            logger.info("socket:"+remoteHost+":"+remotePort+" connected!");
            System.out.println("socket:"+remoteHost+":"+remotePort+" connected!");
            peers.add(remoteHost+":"+remotePort);
            PeerThread pt = new PeerThread(socket);
            peerThreads.add(pt);
            pt.start();
        }catch (Exception e){
            logger.info("socket " + host +":"+port+ " can't connect!"+e);
            System.out.println("socket " + host +":"+port+ " can't connect!" + e);
        }
    }

    public void init(){
        System.out.println("服务器初始化...");
        try {
            File[] files = FileUtils.getAllFiles("D://copy");
            for (File f:files) {
                String filename = f.getPath().substring(f.getPath().lastIndexOf("\\")+1);
                InputStream is = new FileInputStream(f.getPath());
                byte[] data = new byte[is.available()];
                int len = is.read(data);
                filesMap.put(filename,data);
            }
            System.out.println("服务器文件初始化成功！");

            File[] tfs = FileUtils.getAllFiles("datasource/torrents");
            for (File f:tfs) {
                String filename = f.getPath().substring(f.getPath().lastIndexOf("\\")+1);
                torrents.add(filename);
            }
            System.out.println("种子文件初始化成功！");
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("init error:"+e);
        }
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public static synchronized TrackerServer getTrackerServer(){
        return trackerServer;
    }

    public static TrackerServer getTrackerServer(int port){
        if (null!=trackerServer) {
            System.out.println("server is already run!");
            return trackerServer;
        }
        else
            return new TrackerServer(port);
    }

    public List<PeerThread> getPeerThreads() {
        return peerThreads;
    }

    public void setPeerThreads(List<PeerThread> peerThreads) {
        this.peerThreads = peerThreads;
    }

    public List<String> getPeers() {
        return peers;
    }

    public void setPeers(List<String> peers) {
        this.peers = peers;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Map<String, byte[]> getFilesMap() {
        return filesMap;
    }

    public void setFilesMap(Map<String, byte[]> filesMap) {
        this.filesMap = filesMap;
    }

    public List<String> getTorrents() {
        return torrents;
    }

    public void setTorrents(List<String> torrents) {
        this.torrents = torrents;
    }

    public void addTorrents(String t){
        this.torrents.add(t);
    }

    public void addFileToMap(String filename, byte[] allData){
        this.filesMap.put(filename,allData);
    }

    public void removeFileFromMap(String filename){
        this.filesMap.remove(filename);
    }

    public synchronized void addPeerThread(PeerThread pt){
        try {
            synchronized (Lock.getLock3()) {
                System.out.println("添加Peer成功!");
                peerThreads.add(pt);
//                Thread.sleep(1000);
            }
        }catch (Exception e){
            System.out.println("addPeerThread error:"+e);
        }
    }

    public void removePeerThread(PeerThread pt){
        peerThreads.remove(pt);
    }

    public int getDownloadNum() {
        return downloadNum;
    }

    public void setDownloadNum(int downloadNum) {
        TrackerServer.downloadNum = Math.max(downloadNum, 0);
    }

    private static class DealThread extends Thread{
        Socket socket;

        public DealThread(Socket socket){
            this.socket = socket;
        }

        public synchronized void run() {
            //synchronized (Lock.getLock3()) {
                try {
                    InputStream is = socket.getInputStream();
                    byte[] filename_b = new byte[1024];
                    int fnl = is.read(filename_b);
                    String filename = new String(filename_b).trim();
                    System.out.println("文件名为："+filename);

                    new Thread(new DownloadRunnable(socket,filename)).start();
                } catch (Exception e) {
                    System.out.println("Deal " + e);
                }
            //}
        }
    }
}
