package client;

import java.io.IOException;

public class MessageThread extends Thread {
	private Protocol protocol;
	private String message;

	public MessageThread(Protocol protocol, String message) {
		this.protocol = protocol;
		this.message = message;
	}

	@Override
	public void run() {
		try {
			protocol.sendMessage(message);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
