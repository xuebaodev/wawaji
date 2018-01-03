
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

class MachineInfo
{
	public String mac;//identify for wawa machine.
	public String name;//for people use.

	public int    state;//free busying offline//joke. this sample is without database.so ,let the offline machine goto hell. but, you can't do it like me .
	public Map<Socket, Integer> user_in_room;//
	
	public Socket socket;//socket for machine.
	public Socket current_player;
	public Thread runningThread;//my running thread. drop when no heart beat or something.
	
	public long last_heartbeattime = 0;//30s not hear beat. dead. but i don't do this logic to make this code simple.you do it yourself.
	
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

	public void Start(int np) {
		showldStop = false;
		all_machines = new HashMap<String, MachineInfo>();

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
					System.out.println("listen thread is exit at" + np);
					//e.printStackTrace();
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
			Iterator<Map.Entry<String, MachineInfo>> iter = all_machines.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, MachineInfo> me = iter.next();
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
	
	//todo processPlayerEnterRoom ---should notify other to update. but i don't impl that in this free version.
	//todo processPlayerExitRoom ---should notify other to update. but i don't impl that in this free version.
	
	public boolean processPlayerStartPlay(String strMAC, Socket sclient)
	{
		//todo if busy. return false.
		MachineInfo macInfo = all_machines.get( strMAC ); 
		if( macInfo == null) return false;
		
		macInfo.current_player = sclient;
		return true;
	}
	
	void processMsgtoPlayer(String MAC, byte[] data) 
	{
		MachineInfo macInfo = all_machines.get( MAC ); 
		if( macInfo == null) return ;
		
		SimpleApp.cserver.TranlsateToPlayer(macInfo.current_player , data);
	}
	
	   //make the room state to playing.--i skip .you do it.
	   //save the room current player to this socket.
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
		
		
		return true;
	}
	
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
					
					System.out.println("cmd recv:" + total_data[7]);
					
					if ((total_data[7]&0xff)== 0x35) {//heart beat
						String strMAC = new String(total_data, 8, 12);
						System.out.println("wawa heartbeat." + strMAC);
						long now_tw = System.currentTimeMillis();
						MachineInfo tmp = all_machines.get(strMAC);
						if (tmp == null) {
							me.last_heartbeattime = now_tw;
							me.mac = strMAC;
					
							synchronized (all_machines) {
								all_machines.put(strMAC, me);
							}
						} else {
							me.last_heartbeattime = now_tw;
						}

						//heart beat reply back
						try {
							DataOutputStream out = new DataOutputStream(me.socket.getOutputStream());
							out.write(total_data, 0, total_data.length);
							out.flush();
						} catch (IOException ioe) {

						}
					}else if((total_data[7]&0xff)== 0xa0) //视频推流成功
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
						
						if( total_data[7] == 0x34 )//error happend..do your code.change machine state and etc.
						{
							
						}
						
						//translate to current player
						if(me.current_player != null && me.current_player.isClosed() == false)
							SimpleApp.cserver.TranlsateToPlayer(me.current_player , total_data);
						
						if(total_data[7] == 0x33)//game end.//set wawaji to free.
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
			
			System.out.println("[AppServer] "+ me.mac  +"thread exit.");
			
			me.Clear();
			me = null;
		}
	}
}
