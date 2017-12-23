import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SimpleApp {

	public static WawaServer wserver;
	public static ClientServer cserver;
	public static ConfigServer conf_server;
	public static ConfigClientServer conf_clientserver;

	boolean app_should_stop = false;

	public void Start() {
		System.out.println("app start");

		wserver = new WawaServer();
		wserver.Start(7770);

		cserver = new ClientServer();
		cserver.Start(7771);

		conf_server = new ConfigServer();
		conf_server.Start(7776);

		conf_clientserver = new ConfigClientServer();
		conf_clientserver.Start(7778);

		while (app_should_stop == false) {
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
