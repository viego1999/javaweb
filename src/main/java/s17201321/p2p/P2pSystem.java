package s17201321.p2p;

import s17201321.trackerserver.TrackerServer;

import java.io.IOException;
import java.net.Socket;

/**
 * The P2pSystem is a system(main) class used to launch TrackServer
 * @author 17201321-吴新悦
 */
public class P2pSystem {

    public static void main(String[] args) throws IOException {
        TrackerServer trackerServer = TrackerServer.getTrackerServer();
        trackerServer.start();

        System.out.println("TrackerServer launch success...");

//        PeerThread pt1 = new PeerThread(new Socket("127.0.0.1", 2020),"127.0.1.1",2021,1);
//        trackerServer.addPeerThread(pt1);
//        pt1.onloadFile("D://test/17201321_吴新悦_2.rar");
//        pt1.normalDownloadFile("book.txt","D://test/peer1");
//        pt1.btDownloadFile("book.txt","D://test/peer1");
//        pt1.start();
//        pt1.onloadFile("D://test/book.txt");
//
//        PeerThread pt2 = new PeerThread(new Socket("127.0.0.1", 2020),"127.0.1.2",2022,2);
//        trackerServer.addPeerThread(pt2);
//        pt2.normalDownloadFile("book3.txt","D://test/peer2");
//        pt2.onloadFile("D://EV录屏文件/20200517_110835.mp4");
//        pt2.btDownloadFile("17201321_吴新悦_2.rar","D://test/peer2");
//        pt2.start();
//
//        PeerThread pt3 = new PeerThread(new Socket("127.0.0.1", 2020),"127.0.1.3",2023,3);
//        trackerServer.addPeerThread(pt3);
//        pt3.onloadFile("17201321_吴新悦_2.rar");
//        pt3.start();
//
//        PeerThread pt4 = new PeerThread(new Socket("127.0.0.4", 2020));
//        pt4.onloadFile("D://test/book1.txt");
//        pt4.start();
//        trackerServer.addPeerThread(pt4);
    }
}
