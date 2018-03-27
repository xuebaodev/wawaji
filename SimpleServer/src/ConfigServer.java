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
//配置服务器。这个专门处理娃娃机的配置信息应用和转发
//这个服务器是可选的。当这个功能不启用时，无法通过外网配置娃娃机参数和版本升级。
//你必须亲自到场地去，通过如下手段配置娃娃机参数
//1.给娃娃机接上鼠标和显示器，通过鼠标配置。此方法相当麻烦，机器数量多时，效率不高。
//2.用电脑连接到娃娃机所在的同一个局域网，然后打开局域网配置工具进行查找和配置


//配置信息结构体
class ConfigInfo
{
	//娃娃机的名称--用于方便标识哪台娃娃机。推荐是以里面放的娃娃命名此字段。
	//比如你放的是大灰狼，就设置该娃娃机名称为 大灰狼01 、比卡丘05  大白兔08
	public String name;//wawaji name
	
	//娃娃机的MAC
    public String mac;//it's mac
    
    //这台娃娃机是属于谁的。 如果你们不止管理一个客户的娃娃机，可以根据客户端传过来的userID返回该userID对应的娃娃机列表
    //我们这个服务器就处理这种情况。默认返回所有用户。只是留了这个功能，以便于你们要使用
    public String userID;//belong to who
    
    //娃娃机对应的socket
    public Socket socket;
    
    //上次心跳时间
    public long last_heartbeattime;
    
    //配置工具客户端的连接
    public Socket cur_configer;//娃娃机的消息返回给谁
    
    //同理，处理线程的句柄。
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

//
public class ConfigServer {
	private Thread newThread;

	Map<String, ConfigInfo> all_machines;//娃娃机的列表.配置工具要从这里获取到活的娃娃机列表

	ServerSocket listenSocket;

	boolean showldStop = false;
	int nport = 0;
	public void Start(int np) {
		nport = np;
		showldStop = false;
		all_machines = new HashMap<String, ConfigInfo>();
		
		newThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					listenSocket = new ServerSocket();
					listenSocket.bind(new InetSocketAddress("0.0.0.0", nport));
					while (showldStop == false) {
						Socket cur_socket = listenSocket.accept();

						String ip = cur_socket.getRemoteSocketAddress().toString();
						System.out.println("wawaji ip" + ip + "has connected.");

						new HandlerThread(cur_socket);
					}

					System.out.println("listen is exit at" + nport);

				} catch (Exception e) {
					//e.printStackTrace();
					System.out.println("listen thread is exit at" + nport);
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

	//超时删除娃娃机--实际应用中，应当将该娃娃机的配置状态置为离线。
	//此时，娃娃机可能正常工作，但不可以通过外网去配置它。
	//这种情况通常是:
	//1.长时间断网。(短时的断网RTMP会自动重连，应用服务器也是)
	//2.app退出了。
	//3.由于客户不想开启外网配置的功能，因此重新设置了安卓板的配置地址和端口。
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
		
	//配置者离开
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
	
	//构造设备列表并返回
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

	//检测心跳超时
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
					
					//配置口过来的协议全都是json。所以千万不要把配置口和应用端口混用或者搞反
					
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
						//娃娃机过来的消息，除了心跳以外，全部都无脑转发给配置者就对了。这个配置服务器只起中转的作用
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
