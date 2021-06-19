import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;

public class Client {
    private JFrame frame;
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private Thread threadRead;
    private DefaultListModel<String> model;
    private HashMap<String, ChatScreen> listChat;

    void AddComponentToPane() {
        Container container = frame.getContentPane();
        listChat = new HashMap<>();

        //panelLeft
        JPanel panelLeft = new JPanel(new BorderLayout());
        JButton buttonConfig = new JButton("Config");
        panelLeft.add(buttonConfig, BorderLayout.PAGE_START);
        model = new DefaultListModel<>();
        JList<String> listOnline = new JList<>(model);
        JScrollPane scrollPane = new JScrollPane(listOnline);
        TitledBorder title;
        title = BorderFactory.createTitledBorder("List user online");
        scrollPane.setBorder(title);
        panelLeft.add(scrollPane, BorderLayout.CENTER);
        panelLeft.setMinimumSize(new Dimension(200, 1000));

        //panel right
        JPanel panelRight = new JPanel(new CardLayout());

        // panel right config
        JPanel panelConfig = new JPanel();
        panelConfig.setLayout(new BoxLayout(panelConfig, BoxLayout.Y_AXIS));

        JPanel panelName = new JPanel();
        JLabel name = new JLabel("Your name: ");
        JTextField nameValue = new JTextField("", 20);
        panelName.add(name);
        panelName.add(nameValue);
        panelName.setMinimumSize(new Dimension(0, 0));
        panelConfig.add(panelName);

        // choose server
        JPanel panelChooseServer = new JPanel();
        JLabel labelChooseServer = new JLabel("Choose a server: ");
        panelChooseServer.add(labelChooseServer);
        String HOST = "localhost";
        int PORT = 3000;
        String[] listServer = {HOST + " " + PORT, HOST + " " + 2500};
        JComboBox<String> comboBoxServer = new JComboBox<>(listServer);
        panelChooseServer.add(comboBoxServer);
        panelChooseServer.setMinimumSize(new Dimension(0, 0));
        panelConfig.add(panelChooseServer);
        JLabel state = new JLabel("No connection");
        JButton buttonConnect = new JButton("Connect");
        buttonConnect.setPreferredSize(new Dimension(80, 80));
        buttonConnect.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton buttonDisconnect = new JButton("Disconnect");
        buttonDisconnect.setVisible(false);
        buttonDisconnect.setPreferredSize(new Dimension(80, 80));
        buttonDisconnect.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonConnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (nameValue.getText().equals("")) {
                    JOptionPane.showMessageDialog(frame, "Your name must be not null!!!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    socket = new Socket(HOST, PORT);
                    buttonConnect.setVisible(false);
                    buttonDisconnect.setVisible(true);
                    System.out.println(socket.getPort());

                    InputStream inputStream = socket.getInputStream();
                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    threadRead = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            do {
                                try {
                                    String messageReceive = bufferedReader.readLine();
                                    String[] split = messageReceive.split("--");
                                    switch (split[0]) {
                                        case "chat" -> {
                                            ChatScreen chatScreen = listChat.get(split[2]);
                                            JLabel message = new JLabel("<html>  "+ split[2] + ": " + split[3] + "</html>");
                                            chatScreen.getContent().add(Box.createVerticalStrut(10));
                                            chatScreen.getContent().add(message);
                                            chatScreen.Repaint();
                                        }
                                        case "list" -> {
                                            for (int i = 1; i < split.length; i++) {
                                                model.addElement(split[i]);
                                            }
                                        }
                                        case "add" -> {
                                            if (split[1].equals(nameValue.getText())) {
                                                break;
                                            }
                                            model.addElement(split[1]);
                                        }
                                        case "remove" -> {
                                            if (split[1].equals(nameValue.getText())) {
                                                break;
                                            }
                                            model.removeElement(split[1]);
                                            JOptionPane.showMessageDialog(frame, "This user have been disconnected. This conversation will be destroy");
                                            listChat.remove(split[1]);
                                            CardLayout cardLayout = (CardLayout) panelRight.getLayout();
                                            cardLayout.show(panelRight, "panelConfig");
                                        }
                                        case "connect" -> {
                                            String friend = split[2];
                                            int option = JOptionPane.showConfirmDialog(frame, "Do you allow " + friend + " to chat with you?", "Connect requst", JOptionPane.YES_NO_OPTION);
                                            if (option == JOptionPane.YES_OPTION) {
                                                bufferedWriter.write("ok--" + friend + "--" + nameValue.getText());
                                                bufferedWriter.newLine();
                                                bufferedWriter.flush();
                                               // create chat screen
                                                ChatScreen chatScreen = new ChatScreen(bufferedWriter, nameValue.getText());
                                                JPanel chatPanel = chatScreen.CreateChatScreen();
                                                panelRight.add(chatPanel, friend);
                                                chatScreen.SetTextFriendName(friend);
                                                listChat.put(friend, chatScreen);
                                                CardLayout cardLayout = (CardLayout) panelRight.getLayout();
                                                cardLayout.show(panelRight, friend);
                                            } else {
                                                bufferedWriter.write("no--" + friend + "--" + nameValue.getText());
                                                bufferedWriter.newLine();
                                                bufferedWriter.flush();
                                            }
                                        }
                                        case "ok" -> {
                                            //create chat screen
                                            ChatScreen chatScreen = new ChatScreen(bufferedWriter, nameValue.getText());
                                            JPanel chatPanel = chatScreen.CreateChatScreen();
                                            panelRight.add(chatPanel, split[2]);
                                            chatScreen.SetTextFriendName(split[2]);
                                            listChat.put(split[2], chatScreen);
                                            CardLayout cardLayout = (CardLayout) panelRight.getLayout();
                                            cardLayout.show(panelRight, split[2]);
                                        }
                                        case "no" -> {
                                            JOptionPane.showMessageDialog(frame, split[2] + " refuse your request");
                                        }
                                        case "quit" -> {
                                            bufferedWriter.write("quit");
                                            bufferedWriter.newLine();
                                            bufferedWriter.flush();
                                            buttonDisconnect.setVisible(false);
                                            buttonConnect.setVisible(true);
                                            model.removeAllElements();
                                            listChat.clear();
                                            JOptionPane.showMessageDialog(frame, "Server prepare to close");
                                        }
                                    }
                                } catch (IOException ioException) {
                                    ioException.printStackTrace();
                                    break;
                                }
                            } while (true);
                        }
                    });
                    threadRead.start();

                    OutputStream outputStream = socket.getOutputStream();
                    bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
                    bufferedWriter.write(nameValue.getText());
                    bufferedWriter.newLine();
                    bufferedWriter.flush();

                    state.setText("Connect to server: " + HOST + ", port: " + PORT);
                    JOptionPane.showMessageDialog(frame, "Connect successfully!");
                } catch (IOException ioException) {
                    JOptionPane.showMessageDialog(frame, "This server is not open. Please select another server !", "Error", JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        buttonDisconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    bufferedWriter.write("quit");
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                    buttonDisconnect.setVisible(false);
                    buttonConnect.setVisible(true);
                    model.removeAllElements();
                    listChat.clear();
                    state.setText("No connection");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });
        panelConfig.add(buttonConnect);
        buttonConnect.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelConfig.add(buttonDisconnect);
        buttonDisconnect.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelConfig.add(Box.createVerticalStrut(10));
        panelConfig.add(state);
        state.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelConfig.add(Box.createVerticalGlue());
        listOnline.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getClickCount() == 2) {
                    if (listChat.get(listOnline.getSelectedValue()) == null) {
                        System.out.println("Chat" + listOnline.getSelectedValue());
                        try {
                            bufferedWriter.write("connect--" + listOnline.getSelectedValue() + "--" + nameValue.getText());
                            bufferedWriter.newLine();
                            bufferedWriter.flush();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    } else {
                        CardLayout cardLayout = (CardLayout) panelRight.getLayout();
                        cardLayout.show(panelRight, listOnline.getSelectedValue());
                    }
                }
            }
        });

        //link
        panelRight.add(panelConfig, "panelConfig");

        buttonConfig.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CardLayout cardLayout = (CardLayout) panelRight.getLayout();
                cardLayout.show(panelRight, "panelConfig");
            }
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelLeft, panelRight);
        container.add(splitPane);

        // when close window
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                try {
                    bufferedWriter.write("quit");
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                } catch (NullPointerException ignored) {

                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });
    }

    void CreateAndShowGUI() {
        //create and set up window
        frame = new JFrame("Chat - Client");

        AddComponentToPane();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

       // frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.setMinimumSize(new Dimension(600, 500));
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
        Client client = new Client();
        client.Run();
    }
}

class ChatScreen {
    // chat screen
    private JPanel chatPanel;
    private JLabel friendNameLabel;
    private JPanel content;
    private JScrollPane chatScroll;
    private BufferedWriter bufferedWriter;
    private String yourName;
    private String friendName;

    ChatScreen(BufferedWriter bufferedWriter, String yourName) {
        this.bufferedWriter = bufferedWriter;
        this.yourName = yourName;
        chatPanel = new JPanel(new BorderLayout());
        friendNameLabel = new JLabel();
        content = new JPanel();
    }

    public JPanel CreateChatScreen() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(friendNameLabel);
        JCheckBox isEnterNewLine = new JCheckBox("Set 'Enter' to send message");
        isEnterNewLine.setSelected(false);
        header.add(isEnterNewLine);
        chatPanel.add(header, BorderLayout.PAGE_START);

        content.setBackground(Color.white);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        chatScroll = new JScrollPane(content);
        content.setBorder(BorderFactory.createCompoundBorder(content.getBorder(), BorderFactory.createEmptyBorder(0, 5, 30, 5)));
        chatPanel.add(chatScroll, BorderLayout.CENTER);
        JPanel chatCellPanel = new JPanel();
        JTextArea chatCell = new JTextArea(1, 20);
        chatCell.setBorder(BorderFactory.createCompoundBorder(chatCell.getBorder(), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JScrollPane chatScroll = new JScrollPane(chatCell, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chatCellPanel.add(chatScroll);
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String mes = chatCell.getText();
                if (mes.equals("")) {
                    return;
                }
                mes = mes.replace("\n", "<br>");
                try {
                    bufferedWriter.write("chat--" + friendName + "--" + yourName + "--" + mes);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                    JLabel message = new JLabel("<html>  " + yourName + ": " + mes + "</html>");
                    chatCell.setText("");
                    content.add(Box.createVerticalStrut(10));
                    content.add(message);
                    Repaint();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });
        chatCellPanel.add(sendButton);
        chatPanel.add(chatCellPanel, BorderLayout.PAGE_END);

        isEnterNewLine.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                KeyListener l = new KeyListener() {
                    @Override
                    public void keyTyped(KeyEvent e) {
                    }

                    @Override
                    public void keyPressed(KeyEvent e) {
                    }

                    @Override
                    public void keyReleased(KeyEvent e) {
                        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                            String mes = chatCell.getText();
                            if (mes.equals("")) {
                                return;
                            }
                            mes = mes.replace("\n", "<br>");
                            try {
                                bufferedWriter.write("chat--" + friendName + "--" + yourName + "--" + mes);
                                bufferedWriter.newLine();
                                bufferedWriter.flush();
                                JLabel message = new JLabel("<html>  " + yourName + ": " + mes + "</html>");
                                content.add(Box.createVerticalStrut(10));
                                content.add(message);
                                Repaint();
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                            chatCell.setText("");
                        }
                    }
                };
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    chatCell.addKeyListener(l);
                }
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                    chatCell.removeKeyListener(l);
                }
            }
        });
        return chatPanel;
    }

    public void Repaint() {
        chatScroll.revalidate();
        chatScroll.repaint();
        JScrollBar scrollBar = chatScroll.getVerticalScrollBar();
        scrollBar.setValue(scrollBar.getMaximum());
    }

    public void SetTextFriendName(String friendName) {
        this.friendName =  friendName;
        friendNameLabel.setText("Chat with: " + friendName);
        friendNameLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    public JPanel getChatPanel() {
        return chatPanel;
    }

    public void setChatPanel(JPanel chatPanel) {
        this.chatPanel = chatPanel;
    }

    public JLabel getFriendNameLabel() {
        return friendNameLabel;
    }

    public void setFriendNameLabel(JLabel friendNameLabel) {
        this.friendNameLabel = friendNameLabel;
    }

    public JPanel getContent() {
        return content;
    }

    public void setContent(JPanel content) {
        this.content = content;
    }

    public JScrollPane getChatScroll() {
        return chatScroll;
    }

    public void setChatScroll(JScrollPane chatScroll) {
        this.chatScroll = chatScroll;
    }

    public BufferedWriter getBufferedWriter() {
        return bufferedWriter;
    }

    public void setBufferedWriter(BufferedWriter bufferedWriter) {
        this.bufferedWriter = bufferedWriter;
    }

    public String getYourName() {
        return yourName;
    }

    public void setYourName(String yourName) {
        this.yourName = yourName;
    }
}
