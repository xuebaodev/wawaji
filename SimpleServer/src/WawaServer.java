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
import org.json.JSONObject;

//handle msg from wawaji.
//娃娃机信息简单版。
class MachineInfo
{
	//MAC地址:唯一标识一台娃娃机。这是来自于安卓板的以太网端口。你应该以此作为数据库的主键字段
	public String mac;//identify for wawa machine.
	
	//名称(方便管理娃娃机的。并无实际意义，你可以把里面放的娃娃作为该娃娃机的名称，方便你看到就知道是哪台娃娃机)
	public String name;//for people use.

	//标识当前娃娃机状态：在线 离线 故障 正忙。你应该处理好这些状态切换。比如有玩家在玩的时候，你应该标识为正忙，并且通知这个房间里面的所有人，如果要玩，请预约。并返回排队的人数
	public int    state;//free busying offline//joke. this sample is without database.so ,let the offline machine goto hell. but, you can't do it like me .
	
	//房间里面的用户。这个简单版的服务器没有用到。事实上，你应该将进入此房间的用户想办法保存起来，然后娃娃机状态变更时，通知他们
	public Map<Socket, Integer> user_in_room;//
	
	//实际通信的socket句柄。你懂的。
	public Socket socket;//socket for machine.
	
	//正在玩的玩家socket。简单版的服务器存的是socket。因为我并不知道用什么来标识用户。目前没有登录，没有用户ID。所以只能用socket。实际应该是用户ID。然后根据此ID找到socket执行转发
	public Socket current_player;
	
	//存储当前这台娃娃机的工作线程。方便优雅的退出。--比如说，当30秒不心跳的时候。你需要把这个线程退出的。-我现在做到对象的Clear里面。
	public Thread runningThread;//my running thread. drop when no heart beat or something.
	
	//上次心跳时间。用来检查心跳超时
	public long last_heartbeattime = 0;//30s not hear beat. dead. but i don't do this logic to make this code simple.you do it yourself.
	
	//当超时或者娃娃机正常下线的时候，我们要做些清理工作。-如果是实际的应用场景，你还应该把在里面的玩家'踢'出来
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

public class WawaServer {
	private Thread newThread;

	Map<String, MachineInfo> all_machines;//

	ServerSocket listenSocket;

	boolean showldStop = false;
	int nport =0;
	public void Start(int np) {
		showldStop = false;
		nport = np;
		
		all_machines = new HashMap<String, MachineInfo>();

		newThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					listenSocket = new ServerSocket();
					listenSocket.bind(new InetSocketAddress("0.0.0.0", nport));//监听本机所有网卡的该端口。别问为什么。。。
					while (showldStop == false) {
						Socket cur_socket = listenSocket.accept();

						String ip = cur_socket.getRemoteSocketAddress().toString();
						System.out.println("wawaji ip" + ip + "has connected.");

						new HandlerThread(cur_socket);
					}

					System.out.println("listen is exit at" + nport);

				} catch (Exception e) {
					System.out.println("listen thread is exit at" + nport);
					//e.printStackTrace();
				}
			}
		});
		newThread.start();

		CheckTimeout();
	}

	//关服务器的时候调用。--然而除非重大更新，否则并不执行这个逻辑。我只是举个例子
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
			Iterator<Map.Entry<String, MachineInfo>> iter = all_machines.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, MachineInfo> me = iter.next();
				me.getValue().Clear();
			}
		}
		
		all_machines.clear();
	}

	//检查所有娃娃机，是否有超过30秒不心跳的情况。如果有，就把该娃娃机下线。
	void processTimeOut(/* Map<String, MachineInfo> list */) {

		if (all_machines.size() <= 0)
			return;

		//System.out.println("cheiking heat beat time..");
		synchronized (all_machines) {
			Iterator<Map.Entry<String, MachineInfo>> iter = all_machines.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, MachineInfo> me = iter.next();

				long now_tw = System.currentTimeMillis();
				if (now_tw - me.getValue().last_heartbeattime > 30000) {

					System.out.println("[AppServer]"+ me.getKey() +"Timeout Remove:" );
					me.getValue().Clear();
					
					iter.remove(); // OK
				}
			}
		}
	}
	
	//todo processPlayerEnterRoom ---should notify other to update. but i don't impl that in this simple version.
	//todo processPlayerExitRoom ---should notify other to update. but i don't impl that in this simple version.
	

	public boolean processPlayerStartPlay(String strMAC, Socket sclient)
	{
		//todo if busy. return false.
		MachineInfo macInfo = all_machines.get( strMAC ); 
		if( macInfo == null) return false;
		
		macInfo.current_player = sclient;
		return true;
	}
	
	//转发消息给当前正在玩的玩家
	void processMsgtoPlayer(String MAC, byte[] data) 
	{
		MachineInfo macInfo = all_machines.get( MAC ); 
		if( macInfo == null) return ;
		
		SimpleApp.cserver.TranlsateToPlayer(macInfo.current_player , data);
	}
	
	   //make the room state to playing.--i skip .you do it.
	   //save the room current player to this socket.
	//玩家请求开始游戏,设置该娃娃机的当前玩家为该玩家。
	//todo  检查娃娃机状态是否为空闲并回应。 我这里直接默认为成功
	public boolean processPlayerStartNewGame(String MAC, Socket client) 
	{
		if (all_machines.size() <= 0)
			return false;
		
		MachineInfo wawaji = null;
		synchronized (all_machines) {
			wawaji = all_machines.get(MAC);
		}
		
		if( wawaji == null )return false;
		
		wawaji.current_player = client;
		
		//娃娃机状态为空闲时--返回成功。 其他状态你们要看情况处理。
		return true;
	}
	
	//
	public void processPlayerLeave(String MAC, Socket client) 
	{
		if (all_machines.size() <= 0)
			return ;
		
		MachineInfo wawaji = null;
		synchronized (all_machines) {
			wawaji = all_machines.get(MAC);
		}
		
		if( wawaji == null )return ;
		if( wawaji.current_player == client)
			wawaji.current_player = null;
		
		//do your logic. i skip. you do. like :notify other people  in the room and etc.
		
	
		return;
	}
	
	
	public String MakeRoomList() 
	{
		if (all_machines.size() <= 0)
			return "[]";
		
		JSONArray jsonArray1 = new JSONArray();
		
		synchronized (all_machines) {
			Iterator<Map.Entry<String, MachineInfo>> iter = all_machines.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, MachineInfo> me = iter.next();
				jsonArray1.put(me.getKey());
			}
		}
		
		String jsonStr = jsonArray1.toString();
		System.out.println("Room list json str" +jsonStr );
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
				
				System.out.println("[AppServer] heartbeat thread exit." );
			}
		});
		thTimer.start();
	}

	private int ReadDataUnti(byte[] datas, int expect_len, InputStream is) {
		int readCount = 0; 

		while (readCount < expect_len) {
			try {
				int recv_len = is.read(datas, readCount, expect_len - readCount);
				if (recv_len <= 0) {
					System.out.println(this.getClass().getName() + "ReadDataUnti. return -1.beacuse:recv_len=" + recv_len);
					return -1;
				} else
					readCount += recv_len;
			} catch (IOException e) {
				System.out.println("ReadDataUnti Exception. return -1.");
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

	public void TranlsateToWawaji(String mac, byte[] da) {
		
		MachineInfo dest_mac = all_machines.get(mac);
		if(dest_mac == null) 
		{
			System.out.println("target mac not exist. not send.");
			return;
		}
		
		try {
			DataOutputStream out = new DataOutputStream(dest_mac.socket.getOutputStream());
			out.write(da, 0, da.length);
			out.flush();
		} catch (IOException ioe) {
			System.out.println("server new DataOutputStream Failed.");
		}
	}

	private class HandlerThread implements Runnable {
	
		MachineInfo me = null;

		public HandlerThread(Socket client) {
			me = new MachineInfo();
			me.socket = client;
			
			me.runningThread = new Thread(this);
			me.runningThread.start();
		}

		public void run() {
			while (showldStop == false) {
				try {
					InputStream reader = me.socket.getInputStream();

					byte[] bHead = new byte[7];
					int count = ReadDataUnti(bHead, 7, reader);
					if (count != 7) {
						System.out.println("Room recv Read head != 7.Socket close.");
						break;
					}

					if ((bHead[0] & 0xff) != 0xfe) {
						System.out.println("Invalid Head.Socket close.");
						break;
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
					
					//命令码
					int data_cmd = total_data[7]&0xff;
					//System.out.println("cmd recv:" + total_data[7]);
					
					if (data_cmd == 0x35) {//心跳消息
						String strMAC = new String(total_data, 8, 12);
						long t1=System.currentTimeMillis();
								
						System.out.println("["+ t1+"] wawa heartbeat." + strMAC);
						long now_tw = System.currentTimeMillis();
						MachineInfo tmp = all_machines.get(strMAC);
						if (tmp == null) {
							//首次心跳。得到数据库里面检查是否存在此娃娃机。存在则加入房间列表。
							//todo你还应该从数据库里面读取娃娃机名称（即房间名称）之类的娃娃机配置信息。玩家要看的
							//我这里不考虑其他情况。所以上来都是加入列表。你可不能这么做。。
							me.last_heartbeattime = now_tw;
							me.mac = strMAC;
							
							//功能添加 首次登陆
					
							synchronized (all_machines) {
								all_machines.put(strMAC, me);
							}
						} else {
							me.last_heartbeattime = now_tw;//取出并更新上次心跳计时
						}

						//heart beat reply back
						try {
							DataOutputStream out = new DataOutputStream(me.socket.getOutputStream());
							out.write(total_data, 0, total_data.length);
							out.flush();
						} catch (IOException ioe) {

						}
					}else if(data_cmd == 0x92)//0x92照原样返回
					{
						try {
							DataOutputStream out = new DataOutputStream(me.socket.getOutputStream());
							out.write(total_data, 0, total_data.length);
							out.flush();
						} catch (IOException ioe) {

						}
					}
					else if(data_cmd== 0xa0) //1.2新增，视频推流成功通知消息
					{
						String strMAC = new String(total_data, 8, 12);
						if((total_data[20]&0xff)== 0x00 )
							System.out.println("娃娃机:" + strMAC +"前置推流失败.");
						else if((total_data[20]&0xff)== 0x01 )
							System.out.println("娃娃机:" + strMAC +"前置推流成功.");
						else if((total_data[20]&0xff)== 0x02 )
							System.out.println("娃娃机:" + strMAC +"前置推流关闭.");
						else if((total_data[20]&0xff)== 0x10 )
							System.out.println("娃娃机:" + strMAC +"后置推流失败.");
						else if((total_data[20]&0xff)== 0x11 )
							System.out.println("娃娃机:" + strMAC +"后置推流成功.");
						else if((total_data[20]&0xff)== 0x12 )
							System.out.println("娃娃机:" + strMAC +"后置推流关闭.");
					} 
					else  {//translate msg to playing player. but you should check if any error happen.
						
						//娃娃机有故障上报。你要标记娃娃机状态为故障-维护中。
						if( data_cmd == 0x37 )//error happend..do your code.change machine state and etc.
						{
							System.out.println("收到娃娃机故障");
						}
						
						if( data_cmd == 0x89 )//摄像头预览故障-推流故障 娃娃机即将重启
						{
							int frontCamstate  = total_data[8]&0xff;
							int backCamstate = total_data[9]&0xff;
							String st_txt = "收到即将重启命令.";
							
							if(frontCamstate == 0 )
								st_txt += "前置正常.";
							else if (frontCamstate == 1)
								st_txt += "前置推流故障.";
							else if(frontCamstate == 2)
								st_txt += "前置缺失.";
							
							if(backCamstate == 0 )
								st_txt += "后置正常.";
							else if (backCamstate == 1)
								st_txt += "后置推流故障.";
							else if(backCamstate == 2)
								st_txt += "后置缺失.";
							
							System.out.println(st_txt);
						}
						
						//translate to current player
						//其他消息，一律转发给当前正在玩的玩家(如果有)
						//todo 这里，如果玩家有抓到娃娃，你服务器端还要处理的。比如中奖的娃娃ID，玩家ID之类的做中奖记录。
						//然后通知后台客服发货什么的。。。这个简单版的服务器就没有做。我们怕功能太复杂了，你们会理不清楚核心逻辑
						if(me.current_player != null && me.current_player.isClosed() == false)
							SimpleApp.cserver.TranlsateToPlayer(me.current_player , total_data);
						
						if( data_cmd == 0x33 )//game end.//set wawaji to free.
						{
							if (all_machines.size() <= 0)
								return ;
							
							me.current_player = null;
						}
					}

					/*
					 * Message message = Message.obtain(); message.what = 10; message.arg1 =
					 * data_length; message.obj = total_data; if(handler != null)
					 * handler.sendMessage(message);
					 */

				} catch (Exception e) {
					//e.printStackTrace();
					System.out.println("[AppServer] Exception!===" + me.mac);
					break;
				}
			}

			synchronized (all_machines) {
				all_machines.remove(me.mac);
			}
			
			System.out.println("[AppServer] "+ me.mac +"thread exit.");
			
			me.Clear();
			me = null;
		}
	}
}
