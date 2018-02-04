package socks;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.xuebao.rtmpPush.CameraPublishActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.locks.Lock;

import android_serialport_api.ComPort;

//连接配置服务器的类。包含接收 心跳 和重连
public class SockConfig {

    private static final String TAG = "SockConfig";

    public String hostName = "192.168.4.1";
    public int port = 80;
    private Socket socket = null;
    private Handler handler;

    Thread thSocket  = null;
    Thread thHearbeatTimer  = null;

    private boolean ShouldStopNow = false;

    public SockConfig() {
    }

    public void StopNow()
    {
        ShouldStopNow = true;

        if(socket != null)
        {
            try{
                socket.close(); socket = null;
            }catch (IOException e){}
        }
        if(thSocket!= null){
            thSocket.interrupt(); thSocket = null;
        }

        if( thHearbeatTimer != null)
        {
            thHearbeatTimer.interrupt();
            thHearbeatTimer = null;
        }
    }

    void FireHeadbeat()
    {
        if( thHearbeatTimer != null)
        {
            thHearbeatTimer.interrupt();
            thHearbeatTimer = null;
        }

        thHearbeatTimer = new Thread(new Runnable() {
            @Override
            public void run() {

                    while(ShouldStopNow == false)
                    {
                        if(ShouldStopNow == true)
                            break;

                        Log.e(TAG, "心跳.");

                        String strHeadBeat = String.format("{\"userID\":\"%s\",\"mac\":\"%s\",\"cmd\":\"heartbeat\",\"name\":\"%s\"}",
                                VideoConfig.instance.userID, VideoConfig.instance.my_mac,VideoConfig.instance.machine_name);

                        sendMsg(strHeadBeat.getBytes());

                        if(ShouldStopNow == true)
                            break;

                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            continue;
                        }
                    }

                Log.e(TAG, "心跳线程退出");
            }
        });
        thHearbeatTimer.start();
    }

    public void StartWokring(Handler handler, String strHost, int dport)//connect and recv
    {
        this.handler = handler;
        hostName = strHost;
        port = dport;

        ShouldStopNow = false;

        thSocket = new Thread(new ReceiveWatchDog());
        thSocket.start();

        FireHeadbeat();
    }

    public void ApplyNewServer(String strHost, int dport)
    {
        if(hostName.equals(strHost) && port == dport)
            return;

        hostName = strHost;
        port = dport;

        try{
          if(socket != null){  socket.close(); socket = null;}
        }catch (IOException e)
        {

        }
    }

    class ReceiveWatchDog implements Runnable {
        @Override
        public void run() {

            while( ShouldStopNow == false)
            {
                if( hostName.equals("") || port==0)//参数不对。空循环
                {
                    try {
                        Thread.sleep(3000);
                        continue;
                    } catch (InterruptedException ea) {
                        continue;
                    }
                }
                else{
                    try {
                        Log.e(TAG, "try connect====IP:" + hostName + "Port:" + Integer.toString(port));

                        InetAddress addr = InetAddress.getByName(hostName);
                        String domainName = addr.getHostName();//获得主机名
                        String ServerIP = addr.getHostAddress();//获得IP地址

                        socket = new Socket(ServerIP, port);
                        socket.setKeepAlive(true);
                    }catch (IOException e)//连接不成功 重连
                    {
                        Log.e(TAG,"Connect excetion..retry after 3s");
                        try {
                            Thread.sleep(3000);
                            continue;
                        } catch (InterruptedException ea) {
                            continue;
                        }
                    }
                }

                //连接成功立刻心跳
                String strHeadBeat = String.format("{\"userID\":\"%s\",\"mac\":\"%s\",\"cmd\":\"heartbeat\",\"name\":\"%s\"}",
                        VideoConfig.instance.userID, VideoConfig.instance.my_mac,VideoConfig.instance.machine_name);

                sendMsg(strHeadBeat.getBytes());

                while(socket != null && socket.isConnected() && ShouldStopNow == false){
                    try{
                        DataInputStream input = new DataInputStream(socket.getInputStream());
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                        byte jj[] = new byte[2048];
                        int r_len = input.read(jj);
                        if (r_len <= 0)
                        {
                            Log.e(TAG, "收到数据<=0.断开");
                           if(socket!= null) {socket.close(); socket = null;}
                            break;
                        }

                        String aa = new String(jj,0,r_len, "UTF-8");

                        if( aa.indexOf('\u0000') != -1 )
                            aa = aa.substring(0,aa.indexOf('\u0000'));

                        Log.e(TAG,"收到:"+ aa);
                        JSONObject jsonObject = new JSONObject(aa);
                        if(jsonObject.has("cmd") == false)
                        {
                            Log.e("没有CMD","关闭此连接");
                            if(socket!= null){socket.close(); socket = null;}
                            break;
                        }

                        String cmd = jsonObject.getString("cmd");

                        if( cmd.equals("heartbeat") == false)
                            Log.e("jsonfrom公网", aa);

                        if(cmd.equals("getconfig"))
                        {
                            if(jsonObject.has("mac") )
                            {
                                String req_mac = jsonObject.getString("mac");
                                if( req_mac.equals( VideoConfig.instance.my_mac) == false)
                                {
                                    Log.e(TAG,"mac不是本机。不响应获取配置命令");
                                    if( socket!= null){socket.close(); socket = null;}
                                    break;
                                }
                            }

                            Log.e(TAG, "返回");
                            String s = VideoConfig.instance.makeJson();
                            out.write(s.getBytes(), 0, s.getBytes().length);
                            out.flush();
                        }else if(cmd.equals("applyconfig"))
                        {
                            if(jsonObject.has("mac") )
                            {
                                String req_mac = jsonObject.getString("mac");
                                if( req_mac.equals( VideoConfig.instance.my_mac) == false)
                                {
                                    Log.e(TAG,"MAC不是本机。不响应应用配置命令");
                                    if(socket!= null){socket.close(); socket = null;}
                                    break;
                                }
                            }

                            boolean apply_ret = VideoConfig.instance.ApplyConfig(aa, socket);
                            /*if(apply_ret)
                            {
                                String s = "{\"result\":\"ok\"}";
                                out.write(s.getBytes(), 0, s.getBytes().length);
                                out.flush();
                            } else {
                                String s = "{\"result\":\"failed\"}";
                                out.write(s.getBytes(), 0, s.getBytes().length);
                                out.flush();
                            }*/
                        }
                        else if(cmd.equals("update"))
                        {
                            //todo update命令的时候 执行更新
                            // cmd=="update"  url  versionCode
                            //String url = jsonObject.getString("url");
                            // int versionCode= jsonObject.getInt("versionCode");

                            if( jsonObject.has("url") == false )
                            {
                                String s = "{\"result\":\"failed\"}";
                                out.write(s.getBytes(), 0, s.getBytes().length);
                                out.flush();
                            }
                            else {
                                String s = "{\"result\":\"ok\"}";
                                out.write(s.getBytes(), 0, s.getBytes().length);
                                out.flush();
                            }

                            Message message1 = Message.obtain();
                            message1.what = CameraPublishActivity.MessageType.msgOnUpdate.ordinal();//软件自动更新
                            message1.obj = aa;

                            if( handler  != null)
                                handler.sendMessage(message1);

                            //{"cmd":"update", "url":"https://ssfasdfasdf", "versionCode":2}
                        }
                    } catch (Exception e) {
                       // e.printStackTrace();
                        try{
                            if( socket != null){socket.close(); socket = null;}
                        }catch (IOException aa)
                        {
                            break;
                        }
                        break;
                    }
                }
            }

            Log.e(TAG, "接收线程退出.");
        }
    }

    public void sendMsg(byte[] msg) {
        synchronized(this)
        {
            if(socket == null)
            {
                Log.e(TAG, "发送失败socket是空");
                return;
            }

            try {
                if (socket != null && socket.isConnected()) {
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(msg);
                    outputStream.flush();
                }
                else {
                        Log.e(TAG,  "发送失败socket没有连接");
                    }
            } catch (IOException e) {
               // FireReconnect();
            }
        }
    }
}
