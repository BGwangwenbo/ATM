import java.io.*;
import java.net.*;
import java.sql.*;
import java.time.LocalDateTime;

import static jdk.nashorn.internal.objects.NativeString.substr;
import static jdk.nashorn.internal.objects.NativeString.substring;

public class Server {
    public static void main(String[] args) {
        try {
            // 创建ServerSocket，监听指定端口
            ServerSocket serverSocket = new ServerSocket(2525);

            System.out.println("服务器已启动，等待客户端连接...");

            // 循环接收客户端连接
            while (true) {
                // 等待客户端连接
                Socket clientSocket = serverSocket.accept();

                LocalDateTime dateTime = LocalDateTime.now();
                System.out.println("客户端已连接：" + clientSocket.getInetAddress().getHostAddress()+"  "+dateTime);
                // 创建新线程或处理器处理客户端通信
                ClientHandler handler = new ClientHandler(clientSocket);
                handler.start(); // 启动处理器线程
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// 客户端处理器类，用于处理单个客户端的通信
class ClientHandler extends Thread {
    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        Connection conn=null;
        PreparedStatement stmt;
        //监视操作次数
        int operate_times=0;
        //Statement sql;
        ResultSet rs;
        String url = "jdbc:mysql://127.0.0.1:3306/atm_sever";
        String username = "root";
        String password = "superhunstein";

        try {
            //连接数据库
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("连接成功");
        }catch(Exception f){
            System.out.println("数据库连接失败");
        }
        try{
            //获取链接
            conn = DriverManager.getConnection(url, username, password);
            System.out.println("获取成功");
        }catch(Exception f){
            System.out.println("链接获取失败");
        }

        try {
            // 获取输入流，用于接收客户端发送的数据
            BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            DataOutputStream  outToClient = new DataOutputStream(clientSocket.getOutputStream());

            // 读取客户端发送的数据
            String clientData;
            String userID = null;
            String passwd;
            while ((clientData = input.readLine()) != null) {
                if(operate_times>20){
                    //记录异常操作
                    LocalDateTime dateTime = LocalDateTime.now();
                    if (conn != null) {
                        stmt = conn.prepareStatement("insert into error_logs values('" + dateTime + "',?,?)");
                        stmt.setString(1, "error frequent operate!");
                        stmt.setString(2, clientSocket.getInetAddress().getHostAddress());
                        int ok_insert = stmt.executeUpdate();
                    }
                    outToClient.writeBytes(("error operate????????????????????????????????\n"));
                    outToClient.flush();
                    // 关闭流和套接字
                    input.close();
                    outToClient.close();
                    clientSocket.close();


                    System.out.println("客户端连接已断开  " + dateTime + "\n\n\n");

                }
                System.out.println("客户端消息：" + clientData);
                String headData=substr(clientData,0,3);
                switch(headData){
                    case "HEL":
                        operate_times++;
                        userID=substring(clientData,5);
                        outToClient.writeBytes("500 AUTH REQUIRE\n");
                        System.out.println("服务端消息：500 AUTH REQUIRE");
                        outToClient.flush();
                        break;
                    case "PAS":
                        operate_times++;
                        passwd=substring(clientData,5);
                        try{
                            if(conn!=null){
                                stmt=conn.prepareStatement("select * from info where ID=? and passwd=?");
                                stmt.setString(1,userID);
                                stmt.setString(2,passwd);
                                rs=stmt.executeQuery();

                                if(rs.next()){
                                    outToClient.writeBytes("525 OK!\n");
                                    System.out.println("服务端消息：525 OK!");
                                    outToClient.flush();
                                }else{
                                    outToClient.writeBytes(("401 ERROR!\n"));
                                    System.out.println("服务端消息：401 ERROR!");
                                    outToClient.flush();
                                }
                            }
                        }catch(Exception e){
                            System.out.println("statement建立失败");
                        }
                        break;
                    case "BAL":
                        operate_times++;
                        try{
                            if(conn!=null){
                                stmt=conn.prepareStatement("select balance from info where ID=?");
                                stmt.setString(1,userID);
                                rs=stmt.executeQuery();

                                if(rs.next()) {
                                    double bala = rs.getDouble("balance");
                                    outToClient.writeBytes("AMNT:" + bala + "\n");
                                    System.out.println("服务端消息：AMNT:" + bala);
                                    outToClient.flush();
                                }else{
                                    outToClient.writeBytes("401 ERROR!\n");
                                    System.out.println("服务端消息：401 ERROR!");
                                    outToClient.flush();
                                }
                            }
                        }catch(Exception e){
                            System.out.println("statement建立失败");
                        }
                        break;
                    case "WDR":
                        operate_times++;
                        String amountStr=substring(clientData,5);
                        try {
                            double amount = Double.parseDouble(amountStr);
                            try {
                                if (conn != null) {
                                    stmt=conn.prepareStatement("select balance from info where ID=?");
                                    stmt.setString(1,userID);
                                    rs = stmt.executeQuery();

                                    rs.next();
                                    double bala = rs.getDouble("balance");
                                    double newBala = bala - amount;
                                    if (newBala >= 0 && amount > 0 ) {//UPDATE 表名称 SET 更新字段1=更新值1,更新字段2=更新值2,...[WHERE 更新条件(s)];
                                        stmt=conn.prepareStatement("update info set balance=? where ID=?");
                                        stmt.setDouble(1,newBala);
                                        stmt.setString(2,userID);
                                        int ok_update = stmt.executeUpdate();
                                        //存储
                                        LocalDateTime dateTime = LocalDateTime.now();
                                        stmt=conn.prepareStatement("insert into record_money(ID,money,datetime) values(?,?,'" + dateTime + "')");
                                        stmt.setString(1,userID);
                                        stmt.setDouble(2,amount);
                                        int ok_insert = stmt.executeUpdate();
                                        outToClient.writeBytes("525 OK!\n");
                                        System.out.println("服务端消息：525 OK!");
                                        outToClient.flush();
                                    } else if(newBala<0){//余额不足
                                        outToClient.writeBytes("401 ERROR!\n");
                                        System.out.println("服务端消息：401 ERROR!");
                                        outToClient.flush();
                                    }else{//非法输入，取出金额为负，你这是给我存钱呢？？？
                                        LocalDateTime dateTime = LocalDateTime.now();
                                        stmt=conn.prepareStatement("insert into error_logs values('" + dateTime + "',?,?)");
                                        stmt.setString(1,"error negative input");
                                        stmt.setString(2,clientSocket.getInetAddress().getHostAddress());
                                        int ok_insert = stmt.executeUpdate();
                                        outToClient.writeBytes("401 ERROR!\n");
                                        System.out.println("服务端消息：401 ERROR!");
                                        outToClient.flush();
                                    }

                                }
                            } catch (Exception e) {
                                System.out.println("statement建立失败");
                            }
                            break;
                        }catch(Exception f){
                            try {
                                LocalDateTime dateTime = LocalDateTime.now();
                                if (conn != null) {
                                    stmt = conn.prepareStatement("insert into error_logs values('" + dateTime + "',?,?)");
                                    stmt.setString(1, "error string input");
                                    stmt.setString(2, clientSocket.getInetAddress().getHostAddress());
                                    int ok_insert = stmt.executeUpdate();
                                    outToClient.writeBytes("401 ERROR!\n");
                                    System.out.println("服务端消息：401 ERROR!");
                                    outToClient.flush();
                                    break;
                                }
                            }catch(Exception e){
                                System.out.println("statement建立失败,错误日志无法存储");
                            }
                        }
                    case "BYE":
                        operate_times++;
                        outToClient.writeBytes("BYE\n");
                        System.out.println("服务端消息：BYE");
                        outToClient.flush();//清除输出流缓存，
                        // 关闭流和套接字
                        input.close();
                        outToClient.close();
                        clientSocket.close();
                        break;
                    default:
                        outToClient.writeBytes("401 ERROR!\n");
                        System.out.println("服务端消息：401 ERROR!");
                        outToClient.flush();
                }
            }
        } catch (Exception e) {
            LocalDateTime dateTime = LocalDateTime.now();
            System.out.println("客户端连接已断开  "+dateTime+"\n\n\n");
        }
    }
}
