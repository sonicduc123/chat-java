import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private JFrame frame;
    private ServerSocket serverSocket;
    private JList<String> listUserOnline;
    private List<User> listUser = new ArrayList<User>();
    private DefaultListModel<String> model;
    private Thread serverWaiting;

    void AddComponentToPane() {
        Container container = frame.getContentPane();

        JPanel header = new JPanel();
        JLabel title = new JLabel("Server");
        header.add(title);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JPanel panelConfig = new JPanel();
        JLabel port = new JLabel("Port: ");
        JTextField portValue = new JTextField("", 10);
        panelConfig.add(port);
        panelConfig.add(portValue);
        panelConfig.setMinimumSize(new Dimension(0, 0));
        body.add(panelConfig);
        JLabel state = new JLabel("Server is close");
        JButton serverOpen = new JButton("Open server");
        JButton serverClose = new JButton("Close server");
        serverOpen.setAlignmentX(Component.CENTER_ALIGNMENT);
        serverOpen.setPreferredSize(new Dimension(100, 100));
        serverOpen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int PORT = 0;
                    PORT = Integer.parseInt(portValue.getText());
                    serverSocket = new ServerSocket(PORT);
                    serverClose.setVisible(true);
                    serverOpen.setVisible(false);
                    state.setText("Server is open with port " + portValue.getText());
                    JOptionPane.showMessageDialog(frame, "Open server success");

                    serverWaiting = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            do {
                                if (serverWaiting.isInterrupted()) {
                                    System.out.println("close server");
                                    break;
                                }
                                try {
                                    User user = new User();

                                    Socket socket = serverSocket.accept();
                                    System.out.println(socket.getPort());
                                    // read input stream
                                    InputStream inputStream = socket.getInputStream();
                                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                                    user.setBufferedReader(bufferedReader);
                                    // write output stream
                                    OutputStream outputStream = socket.getOutputStream();
                                    BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
                                    user.setBufferedWriter(bufferedWriter);

                                    user.setName(bufferedReader.readLine());
                                    user.setSocket(socket);
                                    listUser.add(user);
                                    model.addElement(user.getName());
                                    System.out.println(user.getName());
                                    for (User u: listUser) {
                                        u.getBufferedWriter().write("add--" + user.getName());
                                        u.getBufferedWriter().newLine();
                                        u.getBufferedWriter().flush();
                                    }
                                    if (!listUser.isEmpty()) {
                                        bufferedWriter.write("list");
                                        for (User u: listUser) {
                                            if (user.getName().equals(u.getName())) {
                                                continue;
                                            }
                                            bufferedWriter.write("--" + u.getName());
                                        }
                                        bufferedWriter.newLine();
                                        bufferedWriter.flush();
                                    }
                                    Thread readerThread = new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            do {
                                                try {
                                                    String messageReceive = bufferedReader.readLine();
                                                    String[] split = messageReceive.split("--");
                                                    String command = split[0];
                                                    if (command.equals("quit")) {
                                                        for (User u: listUser) {
                                                            u.getBufferedWriter().write("remove--" + user.getName());
                                                            u.getBufferedWriter().newLine();
                                                            u.getBufferedWriter().flush();
                                                        }
                                                        listUser.remove(user);
                                                        model.removeElement(user.getName());
                                                        break;
                                                    }
                                                    String name = split[1];
                                                    int index = model.indexOf(name);
                                                    BufferedWriter bw = listUser.get(index).getBufferedWriter();
                                                    bw.write(messageReceive);
                                                    bw.newLine();
                                                    bw.flush();
                                                } catch (IOException ioException) {
                                                    ioException.printStackTrace();
                                                    break;
                                                }
                                            } while(true);
                                        }
                                    });
                                    readerThread.start();
                                    user.setThreadRead(readerThread);
                                } catch (IOException ioException) {
                                    ioException.printStackTrace();
                                    break;
                                }
                            } while (true);
                        }
                    });
                    serverWaiting.start();
                } catch (NumberFormatException exception) {
                    if (portValue.getText().equals("")) {
                        JOptionPane.showMessageDialog(frame, "Port is required", "Error", JOptionPane.WARNING_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(frame, "Port must be a number", "Error", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });
        serverClose.setAlignmentX(Component.CENTER_ALIGNMENT);
        serverClose.setPreferredSize(new Dimension(100, 100));
        serverClose.setVisible(false);
        serverClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!listUser.isEmpty()) {
                    for (User u: listUser) {
                        try {
                            u.getBufferedWriter().write("quit");
                            u.getBufferedWriter().newLine();
                            u.getBufferedWriter().flush();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }

                    }
                    JOptionPane.showMessageDialog(frame, "Waiting to close connection with user");
                    return;
                }
                try {
                    serverSocket.close();
                    JOptionPane.showMessageDialog(frame, "Close server success");
                    serverOpen.setVisible(true);
                    serverClose.setVisible(false);
                    state.setText("Server is close");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });
        body.add(serverOpen);
        serverOpen.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(serverClose);
        serverClose.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(Box.createVerticalStrut(10));
        body.add(state);
        state.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(Box.createVerticalGlue());
        model = new DefaultListModel<>();
        listUserOnline = new JList<>(model);
        JScrollPane scrollPane = new JScrollPane(listUserOnline);
        TitledBorder titleListUser;
        titleListUser = BorderFactory.createTitledBorder("List user online");
        scrollPane.setBorder(titleListUser);
        body.add(scrollPane);

        container.add(header, BorderLayout.PAGE_START);
        container.add(body, BorderLayout.CENTER);
    }

    void CreateAndShowGUI() {
        //create and set up window
        frame = new JFrame("Chat - Server");

        AddComponentToPane();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.setMinimumSize(new Dimension(500, 500));
        frame.setVisible(true);
    }

    public void Run() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                CreateAndShowGUI();
            }
        });
    }

    public static void main(String[] arg) {
        Server server = new Server();
        server.Run();
    }
}

class User {
    private Socket socket;
    private String name;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private Thread threadRead;

    User() {}

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BufferedReader getBufferedReader() {
        return bufferedReader;
    }

    public void setBufferedReader(BufferedReader bufferedReader) {
        this.bufferedReader = bufferedReader;
    }

    public BufferedWriter getBufferedWriter() {
        return bufferedWriter;
    }

    public void setBufferedWriter(BufferedWriter bufferedWriter) {
        this.bufferedWriter = bufferedWriter;
    }

    public Thread getThreadRead() {
        return threadRead;
    }

    public void setThreadRead(Thread threadRead) {
        this.threadRead = threadRead;
    }
}
