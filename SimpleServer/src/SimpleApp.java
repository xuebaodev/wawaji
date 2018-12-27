import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.DataOutputStream ;
import java.io.File ;
import java.io.FileOutputStream ;

public class SimpleApp {

	public static WawaServer wserver;
	public static ClientServer cserver;
	public static ConfigServer conf_server;
	public static ConfigClientServer conf_clientserver;

	boolean app_should_stop = false;

	public void Start() {
		System.out.println("app start");
		
		long aa = System.currentTimeMillis()/1000;
		int bb = (int)aa;
		int c = bb;

		wserver = new WawaServer();//处理娃娃机应用消息的类。你应该在这部分完成：处理娃娃机心跳保活，超时，并维护娃娃机状态(空闲，可用，当前玩家，当前在这个房间里面的玩家等等信息)
		wserver.Start(7770);//此即安卓板所连接的应用服务器端口

		cserver = new ClientServer();//处理玩家app的类。
		cserver.Start(7771);//玩家app所连的端口

		conf_server = new ConfigServer();//此即为配置服务器。此服务器负责娃娃机列表，转发娃娃机参数
		conf_server.Start(7776);//安卓板所连接的配置服务器端口

		conf_clientserver = new ConfigClientServer();//外网配置工具处理类
		conf_clientserver.Start(7778);

		while (app_should_stop == false) {//死循环监听是否输入exit。如果输入exit则，正常的退出。
			try {
				InputStreamReader is_reader = new InputStreamReader(System.in);
				String str = new BufferedReader(is_reader).readLine();
				if (str.equals("exit")) {
				
					if (wserver != null) {
						wserver.Stop();
						wserver = null;
					}

					if (cserver != null) {
						cserver.Stop();
						cserver = null;
					}

					if (conf_server != null) {
						conf_server.Stop();
						conf_server = null;
					}

					if (conf_clientserver != null) {
						conf_clientserver.Stop();
						conf_clientserver = null;
					}

					app_should_stop = true;
				} else
					continue;

			} catch (IOException e) {
				e.printStackTrace();
				app_should_stop = true;
			}
		}

		System.out.println("app exit.");
	}

	public static void main(String[] args) {
		SimpleApp app = new SimpleApp();
		app.Start();
	}
}
