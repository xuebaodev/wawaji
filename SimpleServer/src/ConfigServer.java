
import java.awt.List;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//handle msg from wawaji.

class ConfigInfo
{
	public String name;//wawaji name
    public String mac;//it's mac
    public String userID;//belong to who
    
    public Socket socket;
    public long last_heartbeattime;
    
    public Socket cur_configer;//娃娃机的消息返回给谁
    
	public Thread runningThread;//my running thread. drop when no heart beat or something.
	
	public void Clear() 
	{
		if( socket != null ) 
		{
			try {
				//System.out.println("Room " + mac + "close.");
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

public class ConfigServer {
	private Thread newThread;

	Map<String, ConfigInfo> all_machines;//

	ServerSocket listenSocket;

	boolean showldStop = false;

	public void Start(int np) {
		showldStop = false;
		all_machines = new HashMap<String, ConfigInfo>();

		newThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					listenSocket = new ServerSocket();
					listenSocket.bind(new InetSocketAddress("0.0.0.0", np));
					while (showldStop == false) {
						Socket cur_socket = listenSocket.accept();

						String ip = cur_socket.getRemoteSocketAddress().toString();
						System.out.println("wawaji ip" + ip + "has connected.");

						new HandlerThread(cur_socket);
					}

					System.out.println("listen is exit at" + np);

				} catch (Exception e) {
					//e.printStackTrace();
					System.out.println("listen thread is exit at" + np);
					
				}
			}
		});
		newThread.start();

		CheckTimeout();
	}

	public void Stop() {
		showldStop = true;
		try {
			listenSocket.close();
			listenSocket = null;
		} catch (IOException a) {
		}

		if (newThread != null) {
			newThread.interrupt();
			newThread = null;
		}
		
		//遍历一次 把所有客户全关掉。
		synchronized (all_machines) {
			Iterator<Map.Entry<String, ConfigInfo>> iter = all_machines.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, ConfigInfo> me = iter.next();
				me.getValue().Clear();
			}
		}
		
		all_machines.clear();
	}

	void processTimeOut(/* Map<String, MachineInfo> list */) {

		if (all_machines.size() <= 0)
			return;

		//System.out.println("cheiking heat beat time..");
		synchronized (all_machines) {
			Iterator<Map.Entry<String, ConfigInfo>> iter = all_machines.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, ConfigInfo> me = iter.next();

				long now_tw = System.currentTimeMillis();
				if (now_tw - me.getValue().last_heartbeattime > 30000) {

					me.getValue().Clear();
					System.out.println("[config server] Timeout Remove：" + me.getKey());
					iter.remove(); // OK
				}
			}
		}
	}
		
	public void processPlayerLeave(String MAC, Socket client) 
	{
		if (all_machines.size() <= 0)
			return ;
		
		ConfigInfo wawaji = null;
		synchronized (all_machines) {
			wawaji = all_machines.get(MAC);
		}
		
		if( wawaji == null )return ;
		if( wawaji.cur_configer == client)
			wawaji.cur_configer = null;
		
		//do your logic. i skip. you do. like :notify other people  in the room and etc.
		
		return;
	}
	
	
	public String MakeRoomList() 
	{
		if (all_machines.size() <= 0)
			return "[]";
		
		
		JSONArray jsonArray1 = new JSONArray();
		
		synchronized (all_machines) {
			Iterator<Map.Entry<String, ConfigInfo>> iter = all_machines.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, ConfigInfo> me = iter.next();
				JSONObject jsonObject2 = new JSONObject();
				jsonObject2.put("mac", me.getValue().mac);
				jsonObject2.put("name", me.getValue().name);
				jsonArray1.put(jsonObject2);
			}
		}
		
		String jsonStr = jsonArray1.toString();
		System.out.println("Room list json str: " +jsonStr );
		return jsonStr;
	}

	void CheckTimeout() // check if any machine is timeout.
	{
		Thread thTimer = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while(showldStop == false) 
					{
						Thread.sleep(5000);
						if(showldStop == true)
							break;
						
						processTimeOut();
					}
					
				} catch (InterruptedException e) {
				}
				
				System.out.println("[ConfigServer]heartbeat thread exit." );
			}
		});
		thTimer.start();
	}


	public void TranlsateToWawaji(String mac, byte[] da, Socket configer) {
		
		ConfigInfo dest_mac = all_machines.get(mac);
		if(dest_mac == null) 
		{
			System.out.println("target mac not exist. not send.");
			return;
		}
		
		dest_mac.cur_configer = configer;
		
		try {
			DataOutputStream out = new DataOutputStream(dest_mac.socket.getOutputStream());
			out.write(da, 0, da.length);
			out.flush();
		} catch (IOException ioe) {
			System.out.println("server new DataOutputStream Failed.");
		}
	}

	private class HandlerThread implements Runnable {
		
		ConfigInfo ci;

		public HandlerThread(Socket client) {
			ci = new ConfigInfo();
			
			ci.socket = client;
			ci.runningThread = new Thread(this);
			ci.runningThread.start();
		}

		public void run() {
			while (showldStop == false) {
				try {
					InputStream reader = ci.socket.getInputStream();

					byte[] total_data = new byte[2048];
					int recv_len = reader.read(total_data, 0, 2048);
					if (recv_len <= 0) {
						System.out.println(this.getClass().getName() + "socket read return -1.beacuse:recv_len=" + recv_len + "Close wawaji");
							// Log.e("is.read返回<=0" , "返回值"+ recv_len);
						break;
					}
					
					String strData = new String(total_data, 0, recv_len,"UTF-8");
					
					System.out.println("Received From Wawaji:" + strData);
					
					JSONObject jsonObject = new JSONObject(strData);
					if( jsonObject.has("cmd") )
					{
						String strCMD = jsonObject.getString("cmd");
						if(strCMD.equals( "heartbeat") )
						{	
							String strMAC = jsonObject.getString("mac");

							long now_tw = System.currentTimeMillis();
							ConfigInfo tmp = all_machines.get(strMAC);
							if (tmp == null) {
								
								String strUserID = jsonObject.getString("userID");//root.getAsJsonPrimitive("userID").getAsString();
								String strName=jsonObject.getString("name");//root.getAsJsonPrimitive("name").getAsString();
								
								ci.last_heartbeattime = now_tw;
								ci.mac = strMAC;
								ci.name = strName;
								ci.userID = strUserID;
								
								synchronized (all_machines) {
									all_machines.put(strMAC, ci);
								}
							} else {
								ci.last_heartbeattime = now_tw;
							}

							// heart beat reply back
							try {
								DataOutputStream out = new DataOutputStream(ci.socket.getOutputStream());
								out.write(total_data, 0, total_data.length);
								out.flush();
							} catch (IOException ioe) {
								ioe.printStackTrace();
							}
						}else //replay to config client
						{
							//dest_mac.cur_configer
							SimpleApp.conf_clientserver.TranlsateToPlayer(ci.cur_configer, total_data);
						}
					}
					else //replay to config client
					{
						//dest_mac.cur_configer
						SimpleApp.conf_clientserver.TranlsateToPlayer(ci.cur_configer, total_data);
					}
				} catch (Exception e) {
					//e.printStackTrace();
					System.out.println("[ConfigServer] Exception!===" + ci.mac);
					break;
				}
			}

			synchronized (all_machines) {
				all_machines.remove(ci.mac);
			}
			
			System.out.println( "[ConfigServer]" + ci.mac +"thread exit.");
			
			ci.Clear();
			ci = null;
		
		}
	}
}
