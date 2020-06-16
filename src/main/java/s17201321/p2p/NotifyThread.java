package s17201321.p2p;

import s17201321.entity.FileBlock;

import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

public class NotifyThread extends Thread{
    private Socket socket;
    private List<FileBlock> fileBlocks;

    public NotifyThread(Socket socket,List<FileBlock> fileBlocks){
        this.socket = socket;
        this.fileBlocks = fileBlocks;
    }

    public void run(){
        try {
            OutputStream os = socket.getOutputStream();


        }catch (Exception e){
            System.out.println("notify error:"+e);
        }
    }
}
