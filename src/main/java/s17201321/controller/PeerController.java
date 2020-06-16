package s17201321.controller;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import s17201321.p2p.PeerThread;
import s17201321.trackerserver.TrackerServer;
import s17201321.util.DataUtils;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * 主界面类，用来显示peer的客户端界面
 * @author 17201321-吴新悦
 */
public class PeerController extends Application implements Initializable {
    public ImageView bg;
    @FXML
    public Button o_bt;
    @FXML
    public Button nd_bt;
    @FXML
    public Button bt_bt;
    @FXML
    public VBox sfv;
    @FXML
    public VBox tfv;
    public Stage primaryStage;
    private static PeerThread pt;
    private static TrackerServer trackerServer;
    private static Socket socket;
    private static String localHost;
    private static int port;
    private static int No;
    private static Map<String,byte[]> fileMaps;
    private static List<String> torrents;

    public static void main(String[] args) {
        initPeer(args);
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/peer.fxml"));

        Scene scene = new Scene(root,700,500);
        scene.getStylesheets().add(getClass().getResource("/peer.fxml").toExternalForm());

        primaryStage.setTitle("P2P客户端"+No);
        primaryStage.setScene(scene);
        this.primaryStage = primaryStage;
        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("peer"+No+"已退出！");
            if (socket.isClosed()||socket.isOutputShutdown()){
                try {
                    socket = new Socket("127.0.0.1",2020);// 10.86.192.172
                } catch (IOException e) {
                    System.out.println("exit close error:"+e);
                }
            }
            try {
                OutputStream oss = socket.getOutputStream();
                BufferedOutputStream os = new BufferedOutputStream(oss);

                os.write("退出连接:".getBytes());
                os.write((localHost+":").getBytes());
                os.write((port+":").getBytes());
                os.write((No+"").getBytes());

                os.flush();
                os.close();
                socket.close();
            } catch (IOException e) {
                System.out.println("exit error:"+e);
            }
            System.exit(0);
        });
    }

    public static void initPeer(String[] args){
        System.out.println("初始化peer！");
        if (args.length!=3){
            System.out.println("启动程序时传参错误，自动生成peer的ip、port、no");
            autoGenerateMsg();
        }else {
            try {
                localHost = args[0];
                port = Integer.parseInt(args[1]);
                No = Integer.parseInt(args[2]);
                System.out.println("peer信息为，ip:"+localHost+" port:"+port+" No:"+No);
            }catch (Exception e){
                System.out.println("读取启动参数错误，自动生成peer的ip、port、no："+e);
                autoGenerateMsg();
            }
        }
        try {
            trackerServer = TrackerServer.getTrackerServer();
//            fileMaps = trackerServer.getFilesMap();
//            torrents = trackerServer.getTorrents();

            socket = new Socket("127.0.0.1", 2020);
            pt = new PeerThread(socket,localHost,port,No);
            PrintWriter pw = new PrintWriter(socket.getOutputStream());
            pw.println(localHost);
            pw.println(port);
            pw.println(No);
            pw.flush();
            socket.shutdownOutput();
            pt.start();

            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            fileMaps = (Map<String, byte[]>) ois.readObject();
            torrents = (List<String>) ois.readObject();

            System.out.println(fileMaps+"\n"+torrents);

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("initPeer occur error:"+e);
            autoGenerateMsg();
            try {
                socket = new Socket("127.0.0.1", 2020);
                pt = new PeerThread(socket,localHost,port,No);
            } catch (IOException ioException) {
                System.out.println("initPeer occur error:"+ioException);
            }
        }
    }

    public void onload(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,"选择你要上传的文件",
                new ButtonType("取消上传", ButtonBar.ButtonData.NO),
                new ButtonType("选择文件", ButtonBar.ButtonData.YES));
        alert.setTitle("上传文件");
        alert.initOwner(primaryStage);
        alert.showAndWait();
        if (alert.getResult().getButtonData() == ButtonBar.ButtonData.YES) {
            FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showOpenDialog(primaryStage);

            if (null!=file){
                System.out.println("上传文件为："+DataUtils.getCastFilename(file.getPath()));
                String filename = DataUtils.getCastFilename(file.getPath());
                pt.onloadFile(filename);
            }else {
                System.out.println("未选择文件，文件为空！");
            }
        }else {
            System.out.println("取消上传!");
        }
    }

    public void normalDownload(ActionEvent actionEvent) {
        Stage stage = new Stage();
        Pane p = new Pane();
        VBox vBox = new VBox();
        Label label = new Label("请输入你要下载的文件名称\n\n");
        label.setStyle("-fx-font-weight: bold;");
        final TextField tf = new TextField();
        Button button = new Button("确定");
        vBox.getChildren().addAll(label,tf,button);
        vBox.setAlignment(Pos.CENTER);
        p.getChildren().add(vBox);
        final Scene scene = new Scene(p,250,150);
        stage.setScene(scene);
        stage.setTitle("正常文件下载");
        stage.show();
        button.setOnAction(e ->{
            String filename = tf.getText();
            System.out.println("文件名为："+filename);
            if (isValidateFilename(filename)){
                normalDownloadClick(filename);
            }else {
                Alert alert = new Alert(Alert.AlertType.WARNING,"不合法的文件名",
                        new ButtonType("确定", ButtonBar.ButtonData.YES));
                alert.initOwner(stage);
                alert.showAndWait();
            }
            stage.close();
        });
    }

    public void btDownload(ActionEvent actionEvent) {
        Stage stage = new Stage();
        Pane p = new Pane();
        VBox vBox = new VBox();
        Label label = new Label("请输入你要下载的种子名称\n\n");
        label.setStyle("-fx-font-weight: bold;");
        final TextField tf = new TextField();
        Button button = new Button("确定");
        vBox.getChildren().addAll(label,tf,button);
        vBox.setAlignment(Pos.CENTER);
        p.getChildren().add(vBox);
        final Scene scene = new Scene(p,250,150);
        stage.setScene(scene);
        stage.setTitle("种子文件下载");
        stage.show();
        button.setOnAction(e ->{
            String filename = tf.getText();
            System.out.println("种子文件名为："+filename);
            if (isValidateTorrent(filename)){
                btDownloadClick(filename);
            }else {
                Alert alert = new Alert(Alert.AlertType.WARNING,"不合法的种子文件名",
                        new ButtonType("确定", ButtonBar.ButtonData.YES));
                alert.initOwner(stage);
                alert.showAndWait();
            }
            stage.close();
        });
    }

    public static void autoGenerateMsg(){
        int one = (int)(Math.random()*20+1);
        int two = (int)(Math.random()*30+1);
        int _port = 2025+(int)(Math.random()*30+1);
        int no = (int)(Math.random()*100+1);

        localHost = "127.0."+one+"."+two;
        port = _port;
        No = no;

        System.out.println("自动生成成功：ip:"+localHost+",port:"+port+" No:"+No);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        for (String s:torrents) {
            final Label label = new Label(s);
            label.setStyle("-fx-text-fill: blue;-fx-cursor: hand;-fx-font-size: 13");
            label.setPadding(new Insets(2.0,0.0,0.0,0.0));
            label.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    System.out.println(label.getText());
                    btDownloadClick(label.getText());
                }
            });
            tfv.getChildren().add(label);
        }
        Set<String> set = fileMaps.keySet();
        for (String s:set) {
            final Label label = new Label(s);
            label.setStyle("-fx-text-fill: blue;-fx-cursor: hand;-fx-font-size: 13");
            label.setPadding(new Insets(2.0,0.0,0.0,0.0));
            label.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    System.out.println(label.getText());
                    normalDownloadClick(label.getText());
                }
            });
            sfv.getChildren().add(label);
        }
    }

    public void normalDownloadClick(String filename){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,"选择你要下载的存放路径",
                new ButtonType("取消下载", ButtonBar.ButtonData.NO),
                new ButtonType("选择路径", ButtonBar.ButtonData.YES));
        alert.setTitle("正常下载文件");
        alert.initOwner(primaryStage);
        alert.showAndWait();
        if (alert.getResult().getButtonData() == ButtonBar.ButtonData.YES) {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File file = directoryChooser.showDialog(primaryStage);
            if (null!=file){
                System.out.println("存放路径为为："+DataUtils.getCastFilename(file.getPath()));
                String direction = DataUtils.getCastFilename(file.getPath());
                pt.normalDownloadFile(filename,direction);
            }else {
                System.out.println("未选择存放路径，路径为空！");
            }
        }else {
            System.out.println("取消下载!");
        }
    }

    public void btDownloadClick(String filename){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,"选择你要下载的存放路径",
                new ButtonType("取消下载", ButtonBar.ButtonData.NO),
                new ButtonType("选择路径", ButtonBar.ButtonData.YES));
        alert.setTitle("BT下载文件");
        alert.initOwner(primaryStage);
        alert.showAndWait();
        if (alert.getResult().getButtonData() == ButtonBar.ButtonData.YES) {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File file = directoryChooser.showDialog(primaryStage);
            if (null!=file){
                System.out.println("存放路径为为："+DataUtils.getCastFilename(file.getPath()));
                String direction = DataUtils.getCastFilename(file.getPath());
                String realName = filename.replace(".torrent","");
                pt.btDownloadFile(realName,direction);
            }else {
                System.out.println("未选择存放路径，路径为空！");
            }
        }else {
            System.out.println("取消下载!");
        }
    }

    public boolean isValidateFilename(String filename){
        Set<String> set = fileMaps.keySet();
        return set.contains(filename);
    }

    public boolean isValidateTorrent(String filename){
        return torrents.contains(filename);
    }
}
