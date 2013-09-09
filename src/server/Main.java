package server;

import java.net.ServerSocket;
import java.net.Socket;

public class Main {
	private ServerSocket serverSocket;
	private static final int SERVER_PORT = 43594;
	private boolean listening;
	public static User[] userList;
	public static short SERVER_VERSION = 5;

	public static void main(String[] arguments) {
		new Main();
	}

	public Main() {
		try {
			userList = new User[1001];
			serverSocket = new ServerSocket(SERVER_PORT);
			listening = true;
			write("Server successfully started on port " + SERVER_PORT + ".");
		} catch(Exception e) {
			e.printStackTrace();
			write("Error while starting server: " + e.getMessage() + "");
		}
		waitForConnections();
	}

	private void waitForConnections() {
		while(listening)
			try {
				final Socket currentConnection = serverSocket.accept();
				new Thread() {
					@Override
					public void run() {
						for(int i = 1; i < userList.length; i++)
							if(userList[i] == null) {
								userList[i] = new User(currentConnection, i);
								System.gc();
								break;
							}
					}
				}.start();
			} catch(Exception e) {}
	}

	public static void write(String out) {
		System.out.println(out);
	}
}
