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
import org.json.*;
//玩家信息
class PlayerInfo
{
	//玩家的soket。这里作为关键的ID来使用。
	//实际的生产环境中，用户应该有登录ID 和唯一ID。我这里么有做。但你必须做。
	public Socket socket;//my socket now is my key.Should use other ID or somewhat to fill the clientMap. but now ,this...
	
	//玩家所在的房间列表
	public String in_room_mac;//which room am i enter?
	
	//玩家状态。--我现在没有用到。你也可以不用。
	public int state;//am i playing ? watching? or what.
	
	//服务器用来对这个玩家服务的线程。玩家退出或断网时，要关掉这个线程。
	public Thread runningThread;//my running thread. drop when no heart beat or something.
	
	//todo 玩家的其他信息，比如登录账户，密码，积分 币数 中奖记录 抓娃娃记录 等等信息，这是必须要数据库支持的。
	//根据你们的业务需要去扩展玩家的这些信息。
	
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

//此类负责APP玩家登录，进出房间请求，转发玩家操作到娃娃机。APP玩家的各种操作都和这个类直接交互。
//比如获取房间中奖列表的什么的，都和玩家的应用服务器系列类进行交互
public class ClientServer {

	// all connected player is in this map
	Map<Socket, PlayerInfo> all_clients;// in fact this map's key should be userID. but this simple server don't know
										// what it is.

	private Thread newThread;
	ServerSocket serverSocket;
	boolean showldStop = false;
	int nport = 0;

	public void Start(int np) {
		showldStop = false;
		nport = np;
		all_clients = new HashMap<Socket, PlayerInfo>();
		newThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					serverSocket = new ServerSocket();
					serverSocket.bind(new InetSocketAddress("0.0.0.0", nport));

					while (showldStop == false) {
						Socket cur_socket = serverSocket.accept();
						String ip = cur_socket.getRemoteSocketAddress().toString();
						System.out.println("clientip" + ip + "has connected.");

						new HandlerThread(cur_socket);
					}

					System.out.println("listen is exit at" + nport);

				} catch (Exception e) {
					System.out.println("listen is exit at" + nport);
					// e.printStackTrace();
				}
			}
		});
		newThread.start();
	}

	//仅用于服务器需要停服更新时，优雅的退出。当然，这只是个例子。你可以按你的想法去写
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

	//简单封装，完整接收数据
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

	//数据合法性校验检查
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

	//这个函数处理的是从娃娃机过来的消息，转发给该玩家。或者服务器有什么通知要通知某个玩家的时候--对外调用接口
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
	
	//开局成功-通知玩家。且启用玩家的按钮界面
	public void OnGameStartOK(Socket sock) 
	{
		if(sock == null) return;
		
		PlayerInfo curClient = null;
		synchronized (all_clients) {
			curClient = all_clients.get(sock);
		}

		if (curClient == null)
			return;
		
		try {
			JSONObject inf = new JSONObject();
	        inf.put("cmd", "start_game");
	        inf.put("ret", 1);
			
	        String strRP = inf.toString();
			int data_len = strRP.getBytes().length;

			byte msg_content[] = new byte[data_len+3];
			msg_content[0] = (byte) 0xda;
			msg_content[1] = (byte) ((data_len)/256);
			msg_content[2] = (byte) ((data_len)%256);
			System.arraycopy(strRP.getBytes(), 0, msg_content, 3, strRP.getBytes().length);

			try {
				DataOutputStream out = new DataOutputStream(sock.getOutputStream());
				out.write(msg_content, 0, msg_content.length);
				out.flush();
			} catch (IOException ioe) {
				System.out.println("client new DataOutputStream Excepiton");
			}
		}catch (JSONException e)
        {
            e.printStackTrace();
        }
	}
	
	//游戏结束-通知玩家游戏结果
	public void OnGameEnd(Socket sock, int res) 
	{
		if(sock == null) return;
		
		PlayerInfo curClient = null;
		synchronized (all_clients) {
			curClient = all_clients.get(sock);
		}

		if (curClient == null)
			return;
		
		try {
			JSONObject inf = new JSONObject();
	        inf.put("cmd", "game_ret");
	        inf.put("ret", res);
			
	        String strRP = inf.toString();
			int data_len = strRP.getBytes().length;

			byte msg_content[] = new byte[data_len+3];
			msg_content[0] = (byte) 0xda;
			msg_content[1] = (byte) ((data_len)/256);
			msg_content[2] = (byte) ((data_len)%256);
			System.arraycopy(strRP.getBytes(), 0, msg_content, 3, strRP.getBytes().length);

			try {
				DataOutputStream out = new DataOutputStream(sock.getOutputStream());
				out.write(msg_content, 0, msg_content.length);
				out.flush();
			} catch (IOException ioe) {
				System.out.println("client new DataOutputStream Excepiton");
			}
		}catch (JSONException e)
        {
            e.printStackTrace();
        }
	}

	int g_packget_id = 1;
	byte[] make_com(int... params) {
		byte send_buf[] = new byte[8+params.length];
		send_buf[0] = (byte) 0xfe;
		send_buf[1] = (byte) (g_packget_id);
		send_buf[2] = (byte) (g_packget_id >> 8);
		send_buf[3] = (byte) ~send_buf[0];
		send_buf[4] = (byte) ~send_buf[1];
		send_buf[5] = (byte) ~send_buf[2];
		send_buf[6] = (byte) (8+params.length);
		for (int i = 0; i < params.length; i++) {
			send_buf[7+i] = (byte)(params[i]);
		}

		int sum = 0;
		for (int i = 6; i < (8+params.length - 1); i++) {
			sum += (send_buf[i]&0xff);
		}

		send_buf[8+params.length-1] = (byte)(sum % 100);

		g_packget_id++;
		return send_buf;
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

		//这里必须要注意.如果你觉得这个协议并不合理。你可以自定义自己客户端的全部命令比如
		//{"cmd":"move_left"} {"cmd":"grasp_down"} 然后你在这里转换成娃娃机可识别的操作指令[见文档]发送到娃娃机端(安卓板)即可。
		//所以这个灵活性是服务器端的人配合app端的人开发新协议就可以做到。并不是要死板的遵循我们的协议命令
		//我们的这个例子，APP客户端只是原生发送娃娃机主板会接受的命令，方便熟悉流程。不是说一定要这么去实现。
		public void run() {
			while (showldStop == false) {
				try {
					InputStream reader = pi.socket.getInputStream();

					byte[] bHead = new byte[3];
					int count = ReadDataUnti(bHead, 3, reader);//先接收7个字节。获取到数据长度
					if (count != 3) {
						System.out.println("Read head != 7.Socket close.");
						break;
					}
					
					if ((bHead[0] & 0xff) != 0xda) {//检查数据头是否合法
						System.out.println("Invalid Head.Socket close.");
						continue;
					}

					int data_length = (bHead[1] & 0xff)*256 + bHead[2] & 0xff;// byte2Int(bHead, 6, 7);
					byte datas[] = new byte[data_length];
					int data_recved_len = ReadDataUnti(datas, data_length, reader);
					if (data_recved_len != data_length) {
						break;
					}
					try {
						String jsonString = new String(datas, 0, data_length );
						JSONObject jsCmd = new JSONObject(jsonString);
						System.out.println("jsstr" + jsonString);
						String strCmd = jsCmd.getString("cmd");
						if( strCmd.equals("req_roomlist" )) //获取房间列表--至于其他登录验证什么的，简单服务器就不做了。
						{
							synchronized (all_clients) {
								all_clients.put(pi.socket, pi);
							}

							if (SimpleApp.wserver != null) {
								String strRoomList = SimpleApp.wserver.MakeRoomList();
								int data_len = strRoomList.getBytes().length;

								byte msg_content[] = new byte[data_len+3];
								msg_content[0] = (byte) 0xda;
								msg_content[1] = (byte) ((data_len)/256);
								msg_content[2] = (byte) ((data_len)%256);
								System.arraycopy(strRoomList.getBytes(), 0, msg_content, 3, strRoomList.getBytes().length);
								
								System.out.println("room list reply:" + strRoomList);
								out.write(msg_content, 0, msg_content.length);
								out.flush();
							}
						}
						else if( strCmd.equals("enter_room" ) ) 
						{
							//玩家进入房间。
							//todo 通知房间里面的所有人：XX进来了。
							//todo 加入该房间的玩家列表.该娃娃机状态发生变更时-或XX抓到娃娃时，必须通知这个列表里面的所有人
							//简单版的服务器不做这个功能。然而你们必须要做。
							String strRoomMAC =  jsCmd.getString("mac");
							pi.in_room_mac = strRoomMAC;
							System.out.println("clinet enter room " + strRoomMAC);
						}
						else if(strCmd.equals( "start_game" )) 
						{
							System.out.println("clinet start game ");
							//开局命令--
							//检查是否可以开局并且回应给客户
							// check if the room is free .if not reply not ok.
							// else reply ok.--i will skip this step. directly reply ok.
							boolean bOK = SimpleApp.wserver.processPlayerStartNewGame(pi.in_room_mac, pi.socket);
							if (bOK == true) {//娃娃机状态空闲
								// check if free. if does ,send 0x31
								
								//下发开局指令
								SimpleApp.wserver.TranlsateToWawaji(pi.in_room_mac, make_com(0x31,60,0,0,0,0,0,0,0,0,0,0));//发送开局命令到娃娃机

							} else {//娃娃机非空闲，开局失败-busy. replay start game failed.
								JSONObject inf = new JSONObject();
					            inf.put("cmd", "start_game");
					            inf.put("ret", 0);
								
					            String strRP = inf.toString();
								int data_len = strRP.getBytes().length;

								byte msg_content[] = new byte[data_len+3];
								msg_content[0] = (byte) 0xda;
								msg_content[1] = (byte) ((data_len)/256);
								msg_content[2] = (byte) ((data_len)%256);
								System.arraycopy(strRP.getBytes(), 0, msg_content, 3, strRP.getBytes().length);
							
								out.write(msg_content, 0, msg_content.length);
								out.flush();
							}
						}
						else if(strCmd.equals( "operation" )) 
						{
							int optype =  jsCmd.getInt("type");
							SimpleApp.wserver.TranlsateToWawaji(pi.in_room_mac, make_com(0x32,optype,136,19));
						}
						else if(strCmd.equals( "exit_room" )) 
						{
							//玩家离开房间--todo此时wserver必须通知房间里的所有人更新inroom的玩家个数什么的。
							SimpleApp.wserver.processPlayerLeave(pi.in_room_mac, pi.socket);
							pi.in_room_mac = "";
							
							System.out.println("clinet leave room ");
						}
						else{//other cmd you should make by yourself.you should decide what to do.
							//todo 其他命令。充值啊 获取自己积分啊 获取自己游戏记录啊 抓取记录 设置收获地址啥的。。。自己搭配数据库搞起来
						}
					}catch (JSONException e)
			        {
			            e.printStackTrace();
			        }

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
