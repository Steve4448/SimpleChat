package client;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.LayoutStyle;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

public class Client extends JFrame implements ActionListener, KeyListener, HyperlinkListener {
	public static final int CLIENT_VERSION = 5;
	public static final int LOGIN_SUCCESS = 0, LOGIN_ACCOUNT_ALREADY_ON = 1, LOGIN_INCORRECT_DATA = 2, LOGIN_INCORRECT_VERSION = 3, LOGIN_SERVER_FULL = 4;
	public String username = "n/a";
	public String password = "n/a";
	private JButton clearButton;
	private JLabel dropBoxLabel;
	private JTextArea enterMessageArea;
	private JScrollPane enterMessageAreaScroller;
	private JTextPane incomingMessageArea;
	private JScrollPane incomingMessageAreaScroller;
	private JPanel mainPanel;
	private JLabel pingLabel;
	private JButton sendButton;
	private JList<Object> userList;
	private JScrollPane userListScroller;
	private Socket serverConnection;
	public DataInputStream in;
	public DataOutputStream out;
	private ArrayList<String> usersOnlineList;
	public static ClassLoader loader;
	private HashMap<String, URL> emoteList;
	private Protocol protocol;
	public int offset = 0;

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {}
		new Client();
	}

	public Client() {
		super("Chat Client");
		loader = getClass().getClassLoader();
		try {
			emoteList = EmotionThemeParser.LoadEmotes(loader.getResourceAsStream("resource/theme"));
		} catch(Exception e) {
			e.printStackTrace();
		}
		mainPanel = new JPanel();
		incomingMessageAreaScroller = new JScrollPane();
		incomingMessageArea = new JTextPane();
		userListScroller = new JScrollPane();
		userList = new JList<Object>();
		enterMessageAreaScroller = new JScrollPane();
		enterMessageArea = new JTextArea();
		dropBoxLabel = new JLabel();
		pingLabel = new JLabel();
		clearButton = new JButton();
		sendButton = new JButton();

		incomingMessageArea.setContentType("text/html");
		incomingMessageArea.setEditable(false);

		incomingMessageAreaScroller.setViewportView(incomingMessageArea);

		usersOnlineList = new ArrayList<String>();

		userListScroller.setViewportView(userList);

		enterMessageArea.setColumns(20);
		enterMessageArea.setRows(5);
		enterMessageArea.setLineWrap(true);
		enterMessageAreaScroller.setViewportView(enterMessageArea);

		dropBoxLabel.setText("Drop Box");

		pingLabel.setFont(new Font("Tahoma", Font.PLAIN, 14));
		pingLabel.setText("Ping: Unavailable.");

		clearButton.setText("Clear");
		sendButton.setText("Send");
		GroupLayout mainPanelLayout = new GroupLayout(mainPanel);
		mainPanel.setLayout(mainPanelLayout);
		mainPanelLayout.setHorizontalGroup(mainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(mainPanelLayout.createSequentialGroup().addGap(12, 12, 12).addGroup(mainPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING).addComponent(incomingMessageAreaScroller, GroupLayout.DEFAULT_SIZE, 436, Short.MAX_VALUE).addComponent(enterMessageAreaScroller, GroupLayout.DEFAULT_SIZE, 436, Short.MAX_VALUE)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(mainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false).addComponent(sendButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(clearButton, GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE).addComponent(pingLabel, GroupLayout.PREFERRED_SIZE, 90, GroupLayout.PREFERRED_SIZE).addComponent(dropBoxLabel, GroupLayout.PREFERRED_SIZE, 99, GroupLayout.PREFERRED_SIZE).addComponent(userListScroller, GroupLayout.PREFERRED_SIZE, 180, GroupLayout.PREFERRED_SIZE)).addContainerGap()));
		mainPanelLayout.setVerticalGroup(mainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(mainPanelLayout.createSequentialGroup().addContainerGap().addGroup(mainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(userListScroller, GroupLayout.DEFAULT_SIZE, 264, Short.MAX_VALUE).addComponent(incomingMessageAreaScroller, GroupLayout.DEFAULT_SIZE, 264, Short.MAX_VALUE)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(mainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(enterMessageAreaScroller, GroupLayout.PREFERRED_SIZE, 99, GroupLayout.PREFERRED_SIZE).addGroup(mainPanelLayout.createSequentialGroup().addComponent(dropBoxLabel, GroupLayout.PREFERRED_SIZE, 14, GroupLayout.PREFERRED_SIZE).addGap(3, 3, 3).addComponent(pingLabel, GroupLayout.PREFERRED_SIZE, 21, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(clearButton, GroupLayout.PREFERRED_SIZE, 23, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(sendButton, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE))).addContainerGap()));
		this.add(mainPanel);
		this.setSize(new Dimension(800, 600));
		this.setVisible(true);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		clearButton.addActionListener(this);
		sendButton.addActionListener(this);
		enterMessageArea.addKeyListener(this);
		incomingMessageArea.addHyperlinkListener(this);
		// Connect to the server!
		addMessage("Connecting to server...");
		try {
			serverConnection = new Socket("127.0.0.1", 43594);
			in = new DataInputStream(serverConnection.getInputStream());
			out = new DataOutputStream(serverConnection.getOutputStream());
			protocol = new Protocol(this);
			protocol.sendUserData();
			int returnCode = protocol.readByte();
			switch(returnCode) {
				case LOGIN_SUCCESS:
					startMessageHandler();
					addMessage("Successfully connected to server.");
				break;
				case LOGIN_INCORRECT_VERSION:
					addMessage("Incorrect client version, please update.");
					return;
				case LOGIN_SERVER_FULL:
					addMessage("Server is full, please try again later.");
					return;
			}
		} catch(Exception e) {
			addMessage("ERROR CONNECTING TO SERVER: " + e.getMessage());
			JOptionPane.showMessageDialog(this, "Error connecting to the server.");
			e.printStackTrace();
			return;
		}

	}

	@Override
	public void actionPerformed(ActionEvent aE) {
		String command = aE.getActionCommand();
		if(command.equalsIgnoreCase("send")) {
			if(!enterMessageArea.getText().equals("")) {
				new MessageThread(protocol, enterMessageArea.getText()).start();
				enterMessageArea.setText("");
			}
		} else if(command.equalsIgnoreCase("clear")) {
			enterMessageArea.setText("");
			incomingMessageArea.setText("");
			offset = 0;
		}
	}

	public void startMessageHandler() {
		new Thread() {
			@Override
			public void run() {
				try {
					byte packetID = -1;
					while((packetID = in.readByte()) != -1)
						switch(packetID) {
							case Protocol.PACKET_RECIEVE_DELETE_USER_DATA:
								usersOnlineList.clear();
							break;
							case Protocol.PACKET_RECIEVE_USER_LIST:
								usersOnlineList.add(protocol.readString());
								userList.setListData(usersOnlineList.toArray());
							break;
							case Protocol.PACKET_RECIEVE_MESSAGE:
								addMessage(protocol.readString());
							break;
							case Protocol.PACKET_RECIEVE_PING:
								protocol.sendReturnPing();
							break;
							case Protocol.PACKET_RECIEVE_FINISHED_PING:
								pingLabel.setText("Ping: " + protocol.readInt() + "ms.");
							break;
							case Protocol.PACKET_RECIEVE_USER_DATA:
								System.gc();
								protocol.readUserData();
							break;
						}
					disconnectFromServer("Connection lost with server.");
				} catch(Exception e) {
					e.printStackTrace();
					disconnectFromServer("Connection lost with server.");
				}
			}
		}.start();
	}

	private boolean isLink(String word) {
		return word.startsWith("http://") || word.startsWith("https://") || word.startsWith("www.") || word.indexOf(".org") != -1 || word.indexOf(".net") != -1 || word.indexOf(".com") != -1;
	}

	public void addMessage(String message) {
		try {
			HTMLEditorKit kit = (HTMLEditorKit)incomingMessageArea.getEditorKit();
			StringBuilder output = new StringBuilder();
			StringBuilder word = new StringBuilder();
			for(char c : message.toCharArray())
				if(Character.isWhitespace(c)) {
					if(word.length() > 0) {
						String w = word.toString();
						if(isLink(w.toLowerCase()))
							w = "<a href=\"" + (!w.toLowerCase().startsWith("http://") ? "http://" : "") + w + "\">" + w + "</a>";
						output.append(w);
						word.delete(0, word.length());
					}
					output.append(c);
				} else
					word.append(c);
			if(word.length() > 0) {
				String w = word.toString();
				if(isLink(w.toLowerCase()))
					if(w.endsWith(".gif") || w.endsWith(".png") || w.endsWith(".bmp") || w.endsWith(".jpg") || w.endsWith(".jpeg"))
						w = "<img src=\"" + (!w.toLowerCase().startsWith("http://") ? "http://" : "") + w + "\" />";
					else
						w = "<a href=\"" + (!w.toLowerCase().startsWith("http://") ? "http://" : "") + w + "\">" + w + "</a>";
				output.append(w);
				word.delete(0, word.length());
			}
			String done = output.toString();
			char[] c = done.toCharArray();
			for(int i = 0; i < c.length; i++)
				for(Entry<String, URL> ent : emoteList.entrySet()) {
					String c2 = "";
					for(int i2 = 0; i2 < ent.getKey().length(); i2++) {
						if(i + i2 >= c.length)
							break;
						if(Character.toLowerCase(c[i + i2]) == Character.toLowerCase(ent.getKey().charAt(i2)))
							c2 += c[i + i2];
						else
							break;
					}
					if(c2.toLowerCase().contains(ent.getKey().toLowerCase())) {
						String addedData = "<img src=\"" + ent.getValue().toString() + "\" />";
						done = done.substring(0, i) + addedData + done.substring(i + c2.length());
						i += addedData.length() - 1;
						c = done.toCharArray();
						break;
					}
				}
			int coff = incomingMessageArea.getDocument().getLength();
			kit.insertHTML((HTMLDocument)incomingMessageArea.getDocument(), offset, "<div>" + done + "</div>", offset > 0 ? 2 : 1, 0, Tag.DIV);
			offset += incomingMessageArea.getDocument().getLength() - coff;
			incomingMessageArea.setCaretPosition(incomingMessageArea.getDocument().getLength());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void disconnectFromServer(String logoutMessage) {
		incomingMessageArea.setText("");
		enterMessageArea.setText("");
		offset = 0;
		userList.setListData(new String[] { "" });
		try {
			serverConnection.close();
			serverConnection = null;
		} catch(Exception e) {}
		JOptionPane.showMessageDialog(this, logoutMessage);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if(e.getKeyCode() == KeyEvent.VK_ENTER && e.getModifiers() == 0) {
			if(!enterMessageArea.getText().equals("")) {
				String mes = enterMessageArea.getText().substring(0, enterMessageArea.getText().length() - 1);
				if(!mes.equals(""))
					new MessageThread(protocol, mes).start();
				enterMessageArea.setText("");
			}
		} else if(e.getKeyCode() == KeyEvent.VK_ENTER && e.getModifiers() != 0)
			enterMessageArea.setText(enterMessageArea.getText() + "\n");
	}

	@Override
	public void keyPressed(KeyEvent e) {}

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void hyperlinkUpdate(HyperlinkEvent e) {
		if(e.getEventType() == EventType.ACTIVATED)
			BrowserLauncher.openURL(e.getDescription());
	}
}
