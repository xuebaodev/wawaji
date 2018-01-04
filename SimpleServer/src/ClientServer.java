
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

					byte[] bHead = new byte[7];
					int count = ReadDataUnti(bHead, 7, reader);//先接收7个字节。获取到数据长度
					if (count != 7) {
						System.out.println("Read head != 7.Socket close.");
						break;
					}

					
					if ((bHead[0] & 0xff) != 0xfe) {//检查数据头是否合法
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

					if (check_com_data(total_data, data_length) == false) {//数据合法性校验
						System.out.println("Checksum Data Failed. skip.");
						continue;
					}

					//简单的登录消息命令---这个你可以随便自己定义。客户端和服务器配合就行了
					//在实际的应用场景，你可以给玩家返回任意格式的信息。我这里只默认构造房间列表返回。--方便简单演示
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

							System.out.println("room list reply:" + strRoomList);
							out.write(msg_content, 0, msg_content.length);
							out.flush();

						}
					} else if (total_data[7] == 0x02) // enter room 
					{
						//玩家进入房间。也就是进入了该娃娃机的列表。该娃娃机状态发生变更时，必须通知这个列表里面的所有人
						//简单版的服务器不做这个功能。然而你们必须要做。
						String strMAC = new String(total_data, 8, 12);
						pi.in_room_mac = strMAC;

						System.out.println("clinet enter room " + strMAC);
					} else if (total_data[7] == 0x03) // leave room
					{
						//玩家离开房间--todo此时wserver必须通知房间里的所有人更新inroom的玩家个数什么的。
						SimpleApp.wserver.processPlayerLeave(pi.in_room_mac, pi.socket);
						pi.in_room_mac = "";
						

						System.out.println("clinet leave room ");
					} else if (total_data[7] == 0x31)// player start play.
					{
						//开局命令--
						//检查是否可以开局并且回应给客户
						// check if the room is free .if not reply not ok.
						// else reply ok.--i will skip this step. directly reply ok.
						boolean bOK = SimpleApp.wserver.processPlayerStartNewGame(pi.in_room_mac, pi.socket);
						if (bOK == true) {//开局成功
							// decide should grasp or not. then send to wawaji.
							// now i skip .directly send to wawaji . you do it.
							SimpleApp.wserver.TranlsateToWawaji(pi.in_room_mac, total_data);//发送开局命令到娃娃机

							//返回给客户端，你开始游戏成功。可以显示操作按钮了
							out.write(total_data, 0, total_data.length);
							out.flush();

						} else {
							// should do your logic notify result to client. i skip. you do it.
						}

					}
					else{//default translate other data to wawaji. not handle
						//其他命令。可能是操作命令什么的，我这里默认转发给娃娃机去处理。
						//实际应用场景中，应该会有其他请求命令过来，比如充值，获取抓取记录什么的，你们要自己处理哈
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
