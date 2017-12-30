
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

import org.json.JSONObject;


public class ConfigClientServer {

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

	public void TranlsateToPlayer(Socket sock, byte[] da) {

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
			synchronized (all_clients) {
				all_clients.put(client, pi);
			}
			
			try {
				out = new DataOutputStream(pi.socket.getOutputStream());
			} catch (IOException ioe) {
			}

			new Thread(this).start();
		}

		public void run() {
			while (showldStop == false) {
				try {
					InputStream reader = pi.socket.getInputStream();

					byte[] total_data = new byte[2048];
					int recv_len = reader.read(total_data, 0, 2048);
					if (recv_len <= 0) {
						System.out.println(this.getClass().getName() + "socket read return -1.beacuse:recv_len=" + recv_len + "Close wawaji");
						break;
					}
					
					String strData = new String(total_data, 0, recv_len,"UTF-8");
					
					JSONObject jsonObject = new JSONObject(strData);
					if(jsonObject.has("cmd") == false)//invlaid data
						break;
					
					String strCMD = jsonObject.getString("cmd");
					
					System.out.println("Received From Client:" + strData);
					
					if(strCMD.equals("getlist") )
					{
						String strRoomList="";
						if (SimpleApp.conf_server != null) {
							strRoomList = SimpleApp.conf_server.MakeRoomList();
						}
						
						System.out.println("getlist replay"  + strRoomList);
						// heart beat reply back
						try {
							DataOutputStream out = new DataOutputStream(pi.socket.getOutputStream());
							out.write(strRoomList.getBytes(), 0, strRoomList.getBytes().length);
							out.flush();
						} catch (IOException ioe) {

						}
					}
					else if(strCMD.equals("getconfig"))//get specific machine config
					{
						String strMAC = jsonObject.getString("mac");
						pi.in_room_mac = strMAC;
						SimpleApp.conf_server.TranlsateToWawaji(strMAC, total_data, pi.socket);
					}
					else if(strCMD.equals("applyconfig") )
					{
						String strMAC = jsonObject.getString("mac");
						pi.in_room_mac = strMAC;
						SimpleApp.conf_server.TranlsateToWawaji(strMAC, total_data, pi.socket);
					}
					else if(strCMD.equals("update") ) 
					{
						String strMAC = jsonObject.getString("mac");
						pi.in_room_mac = strMAC;
						SimpleApp.conf_server.TranlsateToWawaji(strMAC, total_data, pi.socket);
					}
				} catch (Exception e) {
					//e.printStackTrace();
					System.out.println("Config client close.I close.");
					break;
				}
			}

			SimpleApp.conf_server.processPlayerLeave(pi.in_room_mac, pi.socket);
			
			synchronized (all_clients) {
				all_clients.remove(pi.socket);
			}

			pi.Clear();
		}
	}
}
