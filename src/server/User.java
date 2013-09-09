package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Calendar;

public class User {
	public static final int LOGIN_SUCCESS = 0, LOGIN_ACCOUNT_ALREADY_ON = 1, LOGIN_INCORRECT_DATA = 2, LOGIN_INCORRECT_VERSION = 3, LOGIN_SERVER_FULL = 4;
	private Socket userSocket;
	public DataInputStream in;
	public DataOutputStream out;
	public int userId;
	public int userRights;
	public String username;
	public String password;
	private String customColor;
	public int clientVersion;
	public Protocol protocol;
	public long pingStartTime = System.currentTimeMillis();
	public boolean recievedLastPing = true;

	public User(Socket _userSocket, int _userId) {
		try {
			// We just accepted this client, we must get the information.
			userSocket = _userSocket;
			userId = _userId;
			write("Connection from: " + userSocket.getInetAddress().toString().substring(1));
			in = new DataInputStream(userSocket.getInputStream());
			out = new DataOutputStream(userSocket.getOutputStream());
			protocol = new Protocol(this);
			protocol.readUserData();
			if(Main.SERVER_VERSION != clientVersion)
				protocol.writeByte(LOGIN_INCORRECT_VERSION);
			else
				protocol.writeByte(LOGIN_SUCCESS);
			username = "User " + userId;
			password = "";
			startMessegeHandler();
			startPingSender();
		} catch(Exception e) {
			e.printStackTrace();
			closeConnection(false);
		}
	}

	private static String removeHTML(String s) {
		StringBuffer out = new StringBuffer();
		for(int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if(c > 127 || c == '"' || c == '<' || c == '>')
				out.append("&#" + (int)c + ";");
			else
				out.append(c);
		}
		return out.toString();
	}

	private void startMessegeHandler() {
		new Thread() {
			@Override
			public void run() {
				try {
					protocol.sendUserData();
					sendMessageToAll(username + " has connected to the server.");
					updateOnlineList();
					byte packetID = 0;
					while((packetID = in.readByte()) != -1)
						switch(packetID) {
							case Protocol.PACKET_RECIEVE_MESSAGE:
								String message = protocol.readString();
								if(message.startsWith("/"))
									parseCommand(message.substring(1));
								else
									sendMessageToAll(getTimeStamp() + " <font color=\"" + getUsernameColor() + "\">" + username + "</font>: " + removeHTML(message).replaceAll("\n", "<br>"));
							break;
							case Protocol.PACKET_RECIEVE_PING_BACK:
								int pingTime = (int)(System.currentTimeMillis() - pingStartTime);
								protocol.sendFinishedPingPacket(pingTime);
								recievedLastPing = true;
							break;
						}
					closeConnection(true);
				} catch(Exception e) {
					closeConnection(true);
				}
			}
		}.start();
	}

	public void parseCommand(String command) throws Exception {
		System.out.println(command);
		if(command.toLowerCase().startsWith("user")) {
			if(command.length() > 5) {
				for(int i = 0; i < Main.userList.length; i++)
					if(Main.userList[i] != null)
						if(Main.userList[i].username.equalsIgnoreCase(command.substring(5))) {
							sendMessage("Username already in use.");
							return;
						}
				username = removeHTML(command.substring(5));
				sendMessage("Username changed to: " + username);
				protocol.sendUserData();
				updateOnlineList();
			} else
				sendMessage("Try as: /user NAME");
		} else if(command.toLowerCase().startsWith("pass")) {
			if(command.length() > 5) {
				password = command.substring(5);
				sendMessage("Password changed to: " + password);
			} else
				sendMessage("Try as: /pass NAME");
		} else if(command.toLowerCase().startsWith("rights")) {
			if(command.length() > 7) {
				userRights = Integer.parseInt(command.substring(7));
				sendMessage("Rights changed to: " + userRights);
			} else
				sendMessage("Try as: /rights NAME");
		} else if(command.toLowerCase().startsWith("setcolor"))
			if(command.length() > 9) {
				customColor = command.substring(9);
				sendMessage("Color changed to: " + customColor);
				updateOnlineList();
			} else
				sendMessage("Try as: /setcolor F0F00F");
	}

	public String getTimeStamp() {
		Calendar cal = Calendar.getInstance();
		String AM_PM = null;
		String extraNumMinute = "";
		String extraNumSecond = "";
		if(cal.get(Calendar.AM_PM) == 0)
			AM_PM = "AM";
		else if(cal.get(Calendar.AM_PM) == 1)
			AM_PM = "PM";
		if(cal.get(Calendar.MINUTE) >= 0 && cal.get(Calendar.MINUTE) <= 9)
			extraNumMinute = "0";
		if(cal.get(Calendar.SECOND) >= 0 && cal.get(Calendar.SECOND) <= 9)
			extraNumSecond = "0";
		return "[" + cal.get(Calendar.HOUR) + ":" + extraNumMinute + cal.get(Calendar.MINUTE) + ":" + extraNumSecond + cal.get(Calendar.SECOND) + AM_PM + "]";
	}

	private void startPingSender() {
		new Thread() {
			@Override
			public void run() {
				while(true)
					try {
						if(recievedLastPing) {
							recievedLastPing = false;
							pingStartTime = System.currentTimeMillis();
							protocol.sendPingPacket();
						}
						Thread.sleep(2000);
					} catch(Exception e) {
						break;
					}
			}
		}.start();
	}

	private void closeConnection(boolean announceDisconnection) {
		Main.userList[userId] = null;
		write("Disconnected.");
		try {
			out.close();
			in.close();
		} catch(Exception e) {}
		if(userSocket != null)
			if(!userSocket.isClosed())
				try {
					userSocket.close();
				} catch(Exception e) {}
		if(username != null && announceDisconnection) {
			sendMessageToAll(username + " has disconnected from the server.");
			updateOnlineList();
		}
		out = null;
		in = null;
		userSocket = null;
	}

	public String getUsernameColor() {
		String returnValue = "#FF0000";
		switch(userRights) {
			case 0: // Regular
				returnValue = "#FF0000";
			break;
			case 1: // Moderator
				returnValue = "#00FF00";
			break;
			case 2: // Administrator
				returnValue = "#FF0000";
			break;
			case 3: // Custom color allowed.
				returnValue = "#" + customColor;
			break;
		}
		return returnValue;
	}

	public void sendMessage(String data) throws Exception {
		protocol.sendMessage(data);
	}

	private void write(String out) {
		Main.write("[User " + userId + " - " + username + "]: " + out);
	}

	public static final void updateOnlineList() {
		try {
			for(int i = 0; i < Main.userList.length; i++)
				if(Main.userList[i] != null) {
					Main.userList[i].protocol.sendDeleteUserList();
					for(int i2 = 0; i2 < Main.userList.length; i2++)
						if(Main.userList[i2] != null)
							Main.userList[i].protocol.sendUserListData("<html><font color=\"" + Main.userList[i2].getUsernameColor() + "\">" + Main.userList[i2].username + "</font></html>");
				}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public static final void sendMessageToAll(String out) {
		for(User user : Main.userList)
			if(user != null)
				try {
					user.sendMessage(out);
				} catch(Exception e) {
					e.printStackTrace();
				}
	}
}
