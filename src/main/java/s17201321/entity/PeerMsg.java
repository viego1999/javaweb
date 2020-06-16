package s17201321.entity;

import java.io.Serializable;

public class PeerMsg implements Serializable {
    private String ip;
    private int port;
    private int no;

    public PeerMsg(String ip, int port,int no) {
        this.ip = ip;
        this.port = port;
        this.no = no;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getNo() {
        return no;
    }

    public void setNo(int no) {
        this.no = no;
    }
}
