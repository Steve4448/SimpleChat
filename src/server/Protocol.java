package server;

import java.io.IOException;

public class Protocol {
	public static final byte PACKET_SEND_DELETE_USER_LIST = 0, PACKET_SEND_USER_LIST = 1, PACKET_SEND_MESSAGE = 2, PACKET_SEND_PING = 3, PACKET_SEND_FINISHED_PING = 4, PACKET_SEND_USER_DATA = 5;
	public static final byte PACKET_RECIEVE_MESSAGE = 0, PACKET_RECIEVE_PING_BACK = 1;
	private User user;

	public Protocol(User user) {
		this.user = user;
	}

	/*
	 * OUT DATA
	 */

	public synchronized void sendUserData() throws IOException {
		sendPacketID(PACKET_SEND_USER_DATA);
		writeString(user.username);
		writeString(user.password);
	}

	public synchronized void sendFinishedPingPacket(int ping) throws IOException {
		sendPacketID(PACKET_SEND_FINISHED_PING);
		writeInt(ping);
	}

	public synchronized void sendPingPacket() throws IOException {
		sendPacketID(PACKET_SEND_PING);
	}

	public synchronized void sendMessage(String message) throws IOException {
		sendPacketID(PACKET_SEND_MESSAGE);
		writeString(message);
	}

	public synchronized void sendUserListData(String data) throws IOException {
		sendPacketID(PACKET_SEND_USER_LIST);
		writeString(data);
	}

	public synchronized void sendDeleteUserList() throws IOException {
		sendPacketID(PACKET_SEND_DELETE_USER_LIST);
	}

	public synchronized void sendPacketID(byte ID) throws IOException {
		writeByte(ID);
	}

	public synchronized void writeInt(int arg) throws IOException {
		user.out.writeInt(arg);
	}

	public synchronized void writeByte(int arg) throws IOException {
		user.out.writeByte(arg);
	}

	public synchronized void writeString(String arg) throws IOException {
		writeInt(arg.length());
		user.out.writeBytes(arg);
	}

	/*
	 * IN DATA
	 */
	public synchronized String readString() throws IOException {
		byte[] stringData = new byte[readInt()];
		for(int i = 0; i < stringData.length; i++)
			stringData[i] = (byte)readByte();
		return new String(stringData);
	}

	public synchronized void readUserData() throws IOException {
		user.clientVersion = readByte();
	}

	public synchronized int readInt() throws IOException {
		return user.in.readInt();
	}

	public synchronized int readByte() throws IOException {
		return user.in.readByte();
	}
}
