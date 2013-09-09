package client;

import java.io.IOException;

public class Protocol {
	public static final byte PACKET_SEND_MESSAGE = 0, PACKET_SEND_PING_BACK = 1;
	public static final byte PACKET_RECIEVE_DELETE_USER_DATA = 0, PACKET_RECIEVE_USER_LIST = 1, PACKET_RECIEVE_MESSAGE = 2, PACKET_RECIEVE_PING = 3, PACKET_RECIEVE_FINISHED_PING = 4, PACKET_RECIEVE_USER_DATA = 5;
	private Client client;

	public Protocol(Client client) {
		this.client = client;
	}

	/*
	 * OUT DATA
	 */
	public synchronized void sendUserData() throws IOException {
		writeByte(Client.CLIENT_VERSION);
	}

	public synchronized void sendReturnPing() throws IOException {
		sendPacketID(PACKET_SEND_PING_BACK);
	}

	public synchronized void sendMessage(String message) throws IOException {
		sendPacketID(PACKET_SEND_MESSAGE);
		writeString(message);
	}

	public synchronized void sendPacketID(byte ID) throws IOException {
		writeByte(ID);
	}

	public synchronized void writeInt(int arg) throws IOException {
		client.out.writeInt(arg);
	}

	public synchronized void writeByte(int arg) throws IOException {
		client.out.writeByte(arg);
	}

	public synchronized void writeString(String arg) throws IOException {
		writeInt(arg.length());
		client.out.writeBytes(arg);
	}

	/*
	 * IN DATA
	 */

	public synchronized void readUserData() throws IOException {
		client.username = readString();
		client.password = readString();
	}

	public synchronized String readString() throws IOException {
		byte[] stringData = new byte[readInt()];
		for(int i = 0; i < stringData.length; i++)
			stringData[i] = (byte)readByte();
		return new String(stringData);
	}

	public synchronized int readInt() throws IOException {
		return client.in.readInt();
	}

	public synchronized int readByte() throws IOException {
		return client.in.readByte();
	}
}
