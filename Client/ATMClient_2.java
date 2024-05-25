import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ATMClient_2 extends JFrame implements ActionListener {
    private JTextField userIdField;
    private JPasswordField passwordField;
    private JButton loginButton_id;
    private JButton connectButton;
    private String ip;
    private JTextField iptextFiled;
    private JButton checkBalanceButton;
    private JButton withdrawButton;
    private JButton logoutButton;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;
    private boolean loggedIn = false; // 登录状态标记



    public ATMClient_2() {
        super("ATM Client");

        // 创建界面组件
        userIdField = new JTextField(20);
        passwordField = new JPasswordField(20);
        iptextFiled = new JTextField(20);
        loginButton_id = new JButton("Loginid");
        connectButton = new JButton("connect");
        checkBalanceButton = new JButton("Check Balance");
        withdrawButton = new JButton("Withdraw");
        logoutButton = new JButton("Logout");

        // 设置布局面板
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // 添加用户名和密码输入框及登录按钮
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("User ID:"), gbc);
        gbc.gridx = 1;
        add(userIdField, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        add(passwordField, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(new JLabel("Server IP:"), gbc);
        gbc.gridx = 1;
        add(iptextFiled, gbc);
        gbc.gridx = 1;
        gbc.gridy = 3;
        add(loginButton_id, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        add(connectButton,gbc);

        // 添加操作按钮
        gbc.gridx = 0;
        gbc.gridy = 4;
        add(checkBalanceButton, gbc);
        gbc.gridx = 1;
        add(withdrawButton, gbc);
        gbc.gridx = 2;
        add(logoutButton, gbc);

        // 设置窗口属性
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);

        loginButton_id.addActionListener(this);
        connectButton.addActionListener(this);
        checkBalanceButton.addActionListener(this);
        withdrawButton.addActionListener(this);
        logoutButton.addActionListener(this);

    }



    public void  actionPerformed(ActionEvent e) {
        if (e.getSource() == loginButton_id) {
            if(connected == true){
                String userId = userIdField.getText();
                char[] passwordChars = passwordField.getPassword();
                String password = new String(passwordChars);

                // 发送登录请求给服务器
                sendMessage("HELO " + userId);
                sendMessage("PASS "+password);
            }else{
                System.out.println("请先连接");
            }


        } else if(e.getSource() == connectButton){
            //建立socket连接
            ip = iptextFiled.getText();
            try{
                socket = new Socket(ip, 2525);
                //建立输出流向服务端发送消息
                out = new PrintWriter(socket.getOutputStream(), true);
                //输入流接受消息
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                connected = true;
                System.out.println("连接成功");
            }catch(IOException esm){
                esm.printStackTrace();
            }
        } else if (e.getSource() == checkBalanceButton ) {
            //检查余额
            if(loggedIn == true){
                sendMessage("BALA");
            }else{
                System.out.println("请先登录");
            }
            // 发送查询余额请求给服务器

        } else if (e.getSource() == withdrawButton) {
            //取钱
            if(loggedIn==true){
                String amount = JOptionPane.showInputDialog(this, "Enter amount to withdraw:");
                sendMessage("WDRA " + amount);
            }else{
                System.out.println("请先登录");
            }

        } else if (e.getSource() == logoutButton && loggedIn == true) {
            // 发送退出请求给服务器
            sendMessage("BYE");
            try {
                // 关闭套接字和输入输出流
                in.close();
                out.close();
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            System.exit(0); // 退出客户端程序
        }
    }

    private void sendMessage(String message) {
        out.println(message);

        try {
            String response = in.readLine();

            if(message.startsWith("BAL")){
                System.out.println("当前余额为：" + response.substring(5));
            }else if(message.startsWith("HE")){
                if(response.startsWith("401")){
                    System.out.println("密码或用户名错误");
                }

            }else if(message.startsWith("PASS")){
                if(response.startsWith("401")){
                    System.out.println("密码或用户名错误");
                }else if(response.startsWith("525")){
                    System.out.println("登录成功");
                    loggedIn = true;
                }
            }else if(message.startsWith("WD")){
                if(response.startsWith("401")){
                    System.out.println("余额不足或输入有误");
                }
                else if(response.startsWith("AMNT")){
                    System.out.println("取钱成功");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ATMClient_2());

    }
}
