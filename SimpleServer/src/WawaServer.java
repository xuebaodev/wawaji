import java.awt.List;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import java.applet.AudioClip;
import java.applet.Applet;
import java.io.*;

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
	
	//为了测试丢包率做的变量
	//发送给娃娃机的次数
	public long sendCount = 0;
	
	//娃娃机接收到的次数
	public long recvCount = 0;
	
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

						try {
							DataOutputStream out = new DataOutputStream(cur_socket.getOutputStream());
							out.write("aa".getBytes(), 0, 2);
							out.flush();
						} catch (IOException ioe) {
							System.out.println("server new DataOutputStream Failed.");
						}
						
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
	
	//check the room state if it's ready to play
	//玩家请求开始游戏,设置该娃娃机的当前玩家为该玩家。
	//todo  检查娃娃机状态是否为空闲并回应。 我这里直接默认为成功.对于服务器来说
	//娃娃机的状态分为 离线 故障 游戏中 空闲。服务器应该根据娃娃机心跳情况和故障上报做相应的状态标记。
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
		//if (all_machines.size() <= 0)
		//	return "{\"cmd\":\"reply_roomlist\",\"rooms\":[]}";
		
		JSONObject inf = new JSONObject();
        inf.put("cmd", "reply_roomlist");
		JSONArray jsonArray1 = new JSONArray();
		
		synchronized (all_machines) {
			Iterator<Map.Entry<String, MachineInfo>> iter = all_machines.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, MachineInfo> me = iter.next();
				jsonArray1.put(me.getKey());
			}
		}
		
		inf.put("rooms", jsonArray1);
		String jsonStr = inf.toString();
		System.out.println("Room list json str" +inf );
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
		
		if (check_total % 100 != data[len - 1]) {
			return false;
		}

		return true;
	}

	//玩家发送给娃娃机
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
			dest_mac.sendCount++;
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

					//头校验不通过。丢弃数据
					if (bHead[0] != (byte) (~bHead[3] & 0xff) && bHead[1] != (byte) (~bHead[4] & 0xff)
							&& bHead[2] != (byte) (~bHead[5] & 0xff))
						continue;
			
					//取出长度 继续接收
					int data_length = bHead[6] & 0xff;// byte2Int(bHead, 6, 7);
					byte datas[] = new byte[data_length - 7];
					int data_recved_len = ReadDataUnti(datas, data_length - 7, reader);
					while (data_recved_len != data_length - 7) {
						break;
					}

					byte total_data[] = new byte[data_length];
					System.arraycopy(bHead, 0, total_data, 0, 7);
					System.arraycopy(datas, 0, total_data, 7, data_length - 7);

					//数据校验和
					if (check_com_data(total_data, data_length) == false) {
						System.out.println("Checksum Data Failed. skip.");
						continue;
					}
					
					//根据命令码做相应处理
					int data_cmd = total_data[7]&0xff;
					System.out.printf("cmd:%02X\n",data_cmd );
					/*System.out.print("cmd recv:");
					for(int kk = 0;kk<data_length;kk++) 
					{
						System.out.printf("%02X", total_data[kk] );
					}
					System.out.println();*/
					me.recvCount ++;
					if(data_cmd == 0x31) {//开局结果返回。通知玩家是否开局成功。由于串口数据有丢失率。因此发0x31给娃娃机的时候，最好起一个超时计时器，未收到开局则隔100ms继续发。
						//todo-标记娃娃机的状态为不空闲。不再接受其他玩家的开局申请.--服务器必须做。简单服务器就不做了。
						if(me.current_player != null && me.current_player.isClosed() == false) 
						{
							SimpleApp.cserver.OnGameStartOK(me.current_player);
							me.recvCount ++;
						}
					}else if(data_cmd == 0x33) { //游戏结束。通知玩家和在房间里的所有人。游戏结果.
						//todo 这里，如果玩家有抓到娃娃，你服务器端还要处理的。比如中奖的娃娃ID，玩家ID之类的做中奖记录。
						//然后通知后台客服发货什么的。。。这个简单版的服务器就没有做。我们怕功能太复杂了，你们会理不清楚核心逻辑
						if (all_machines.size() <= 0)
							return ;
						
						if(me.current_player != null && me.current_player.isClosed() == false) 
						{
							int game_res = total_data[8]&0xff;
							SimpleApp.cserver.OnGameEnd(me.current_player, game_res);
						}
						
						me.recvCount --;
						me.current_player = null;
					}else if (data_cmd == 0x35) {//心跳消息
						me.recvCount --;//心跳不计入测试丢包统计
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
					}else if( data_cmd == 0x37 ) {
						//error happend..do your code.change machine state and etc.
						//todo 收到故障。通知房间里所有玩家。同时置娃娃机状态为故障-维护中。此时就不能再接受玩家的开局了。我这里没做处理。但正式是必须要做的。
						me.recvCount --;
						System.out.println("收到娃娃机故障");
					}else if( data_cmd == 0x89 ) {
						//摄像头预览故障-推流故障 通知后台人员查看 具体命令值请看github.com/xuebaodev wiki
						me.recvCount --;
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
					}else if(data_cmd == 0x92) {//0x92照原样返回
						me.recvCount --;
						try {
							DataOutputStream out = new DataOutputStream(me.socket.getOutputStream());
							out.write(total_data, 0, total_data.length);
							out.flush();
						} catch (IOException ioe) {

						}
					}
					else if(data_cmd== 0xa0){ //1.2新增，视频推流成功通知消息
						me.recvCount --;
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
					else{
						//其他消息，视情况进行处理
						/*System.out.println(me.mac + "发:"+me.sendCount + "收" + me.recvCount + "差值" + (me.recvCount- me.sendCount));
						if( me.recvCount - me.sendCount >=10) 
						{
							String imagePath=System.getProperty("user.dir")+"/";
						    try {
					            URL cb;
					            //File f = new File(imagePath+"mario.midi");
					            //File f = new File(imagePath+"1000.ogg");
					            File f = new File(imagePath+"1024.wav");
					            cb = f.toURL();
					            AudioClip aau;
					            aau = Applet.newAudioClip(cb);
					            aau.play();//循环播放 aau.play() 单曲 aau.stop()停止播放
					            aau.loop();
					        } catch (MalformedURLException e) {
					            e.printStackTrace();
					        }
						    
						    System.out.println("发送0x55");
							byte send_buf[] = new byte[9];
							send_buf[0] = (byte)0xfe;
							send_buf[1] = 0;
							send_buf[2] = 0;
							send_buf[3] = (byte)0x01;
							send_buf[4] = (byte)0xff;
							send_buf[5] = (byte)0xff;
							send_buf[6] = (byte)0x09;
							send_buf[7] = (byte)0x55;
							send_buf[8] = (byte)0x5e;
							
							try {
								DataOutputStream out = new DataOutputStream(me.current_player.getOutputStream());
								out.write(send_buf, 0, send_buf.length);
								out.flush();
							} catch (IOException ioe) {
								System.out.println("client new DataOutputStream Excepiton");
							}
						}*/
					}
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
