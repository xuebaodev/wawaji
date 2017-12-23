
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class PlayerInfo
{
	public Socket socket;//my socket now is my key.Should use other ID or somewhat to fill the clientMap. but now ,this...
	public String in_room_mac;//which room am i enter?
	public int state;//am i playing ? watching? or what.
	
	public Thread runningThread;//my running thread. drop when no heart beat or something.
	
	public void Clear() 
	{
		if( socket != null ) 
		{
			try {
				socket.close();
				socket = null;
			} catch (IOException d) {
				d.printStackTrace();
			}
		}
		
		if( runningThread != null) 
		{
			runningThread.interrupt(); runningThread = null;
		}
	}
}

public class ClientServer {

	// all connected player is in this map
	Map<Socket, PlayerInfo> all_clients;// in fact this map's key should be userID. but this simple server don't know
										// what it is.

	private Thread newThread;
	ServerSocket serverSocket;
	boolean showldStop = false;

	public void Start(int np) {
		showldStop = false;
		all_clients = new HashMap<Socket, PlayerInfo>();
		newThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					serverSocket = new ServerSocket();
					serverSocket.bind(new InetSocketAddress("0.0.0.0", np));

					while (showldStop == false) {
						Socket cur_socket = serverSocket.accept();
						String ip = cur_socket.getRemoteSocketAddress().toString();
						System.out.println("clientip" + ip + "has connected.");

						new HandlerThread(cur_socket);
					}

					System.out.println("listen is exit at" + np);

				} catch (Exception e) {
					System.out.println("listen is exit at" + np);
					// e.printStackTrace();
				}
			}
		});
		newThread.start();
	}

	public void Stop() {
		try {
			serverSocket.close();
			serverSocket = null;
		} catch (IOException a) {
		}

		showldStop = true;
		if (newThread != null) {
			newThread.interrupt();
			newThread = null;
		}
		
		//遍历一次 把所有客户全关掉。
		synchronized (all_clients) {
			Iterator<Map.Entry<Socket, PlayerInfo>> iter = all_clients.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<Socket, PlayerInfo> me = iter.next();
				me.getValue().Clear();
			}
		}
		
		all_clients.clear();
	}

	private int ReadDataUnti(byte[] datas, int expect_len, InputStream is) {
		int readCount = 0;

		while (readCount < expect_len) {
			try {
				int recv_len = is.read(datas, readCount, expect_len - readCount);
				if (recv_len <= 0) {

					return -1;
				} else
					readCount += recv_len;
			} catch (IOException e) {
				return -1;
			}
		}

		return readCount;
	}

	boolean check_com_data(byte[] data, int len) {
		int check_total = 0;
		// check sum
		for (int i = 0; i < len; i++) {
			if (i >= 6 && i < len - 1)
				check_total += (data[i] & 0xff);
		}

		if (data[0] != (byte) (~data[3] & 0xff) && data[1] != (byte) (~data[4] & 0xff)
				&& data[2] != (byte) (~data[5] & 0xff))
			return false;

		if (check_total % 100 != data[len - 1]) {
			return false;
		}

		return true;
	}

	public void TranlsateToPlayer(Socket sock, byte[] da) {

		if(sock == null) return;
		
		PlayerInfo curClient = null;
		synchronized (all_clients) {
			curClient = all_clients.get(sock);
		}

		if (curClient == null)
			return;
		
		try {
			DataOutputStream out = new DataOutputStream(sock.getOutputStream());
			out.write(da, 0, da.length);
			out.flush();
		} catch (IOException ioe) {
			System.out.println("client new DataOutputStream Excepiton");
		}
	}

	private class HandlerThread implements Runnable {
		PlayerInfo pi = null;
		
		DataOutputStream out = null;

		public HandlerThread(Socket client) {
			pi = new PlayerInfo();
			pi.socket = client;
			
			try {
				out = new DataOutputStream(pi.socket.getOutputStream());
			} catch (IOException ioe) {
			}

			pi.runningThread = new Thread(this);
			pi.runningThread.start();
		}

		public void run() {
			while (showldStop == false) {
				try {
					InputStream reader = pi.socket.getInputStream();

					byte[] bHead = new byte[7];
					int count = ReadDataUnti(bHead, 7, reader);
					if (count != 7) {
						System.out.println("Read head != 7.Socket close.");
						break;
					}

					if ((bHead[0] & 0xff) != 0xfe) {
						System.out.println("Invalid Head.Socket close.");
						continue;
					}

					int data_length = bHead[6] & 0xff;// byte2Int(bHead, 6, 7);

					byte datas[] = new byte[data_length - 7];
					int data_recved_len = ReadDataUnti(datas, data_length - 7, reader);
					while (data_recved_len != data_length - 7) {
						break;
					}

					byte total_data[] = new byte[data_length];
					System.arraycopy(bHead, 0, total_data, 0, 7);
					System.arraycopy(datas, 0, total_data, 7, data_length - 7);

					if (check_com_data(total_data, data_length) == false) {
						System.out.println("Checksum Data Failed. skip.");
						continue;
					}

					if (total_data[7] == 0x01) // login and req room list//note: you should send other cmd to handle
												// login.
					{
						// make this client login.
						synchronized (all_clients) {
							all_clients.put(pi.socket, pi);
						}

						if (SimpleApp.wserver != null) {
							String strRoomList = SimpleApp.wserver.MakeRoomList();
							int data_len = 9 + strRoomList.getBytes().length;

							byte msg_content[] = new byte[data_len];
							msg_content[0] = (byte) 0xfe;
							msg_content[1] = (byte) (0);
							msg_content[2] = (byte) (0);
							msg_content[3] = (byte) ~msg_content[0];
							msg_content[4] = (byte) ~msg_content[1];
							msg_content[5] = (byte) ~msg_content[2];
							msg_content[6] = (byte) (msg_content.length);
							msg_content[7] = (byte) 0x1;

							System.arraycopy(strRoomList.getBytes(), 0, msg_content, 9, strRoomList.getBytes().length);

							System.out.println("room list len." + data_len);
							out.write(msg_content, 0, msg_content.length);
							out.flush();

						}
					} else if (total_data[7] == 0x02) // enter room
					{
						String strMAC = new String(total_data, 8, 12);
						pi.in_room_mac = strMAC;

						System.out.println("clinet enter room " + strMAC);
					} else if (total_data[7] == 0x03) // leave room
					{
						SimpleApp.wserver.processPlayerLeave(pi.in_room_mac, pi.socket);
						pi.in_room_mac = "";
						

						System.out.println("clinet leave room ");
					} else if (total_data[7] == 0x31)// player start play.
					{
						// check if the room is free .if not reply not ok.
						// else reply ok.--i will skip this step. directly reply ok.
						boolean bOK = SimpleApp.wserver.processPlayerStartNewGame(pi.in_room_mac, pi.socket);
						if (bOK == true) {
							// decide should grasp or not. then send to wawaji.
							// now i skip .directly send to wawaji . you do it.
							SimpleApp.wserver.TranlsateToWawaji(pi.in_room_mac, total_data);

							out.write(total_data, 0, total_data.length);
							out.flush();

						} else {
							// should do your logic notify result to client. i skip. you do it.
						}

					}
					else{//default translate other data to wawaji. not handle
						SimpleApp.wserver.TranlsateToWawaji(pi.in_room_mac, total_data);
					}
					// cmd from client. maybe get room . enter room .exit room or play operation.

					// if(total_data[7] == 0x35)
					// {
					// Send(total_data);
					// }
					// else
					// if(SimpleApp.wserver != null)
					// {
					// SimpleApp.wserver.Send(total_data);
					// }

					/*
					 * Message message = Message.obtain(); message.what = 10; message.arg1 =
					 * data_length; message.obj = total_data; if(handler != null)
					 * handler.sendMessage(message);
					 */

				} catch (Exception e) {
					e.printStackTrace();
					break;
				}
			}

			SimpleApp.wserver.processPlayerLeave(pi.in_room_mac, pi.socket);
			
			synchronized (all_clients) {
				all_clients.remove(pi.socket);
			}

			pi.Clear();
			pi = null;
		}
	}
}
