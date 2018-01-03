package socks;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

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

    public String hostName = "192.168.4.1";
    public int port = 80;
    private Socket socket = null;
    private Handler handler;

    Thread thSocket  = null;
    Thread thHearbeatTimer  = null;
    Thread thRecconet = null;

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

        if(thRecconet != null)
        {
            thRecconet.interrupt();
            thRecconet = null;
        }
    }

    public boolean IsReady()
    {
        if(socket != null && socket.isConnected()) return true;
        return false;
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

                        Log.e("SockConfig", "heartbeat.");

                        String strHeadBeat = String.format("{\"userID\":\"%s\",\"mac\":\"%s\",\"cmd\":\"heartbeat\",\"name\":\"%s\"}",
                                VideoConfig.instance.userID, VideoConfig.instance.my_mac,VideoConfig.instance.machine_name);

                        sendMsg(strHeadBeat.getBytes());

                        if(ShouldStopNow == true)
                            break;

                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }

                Log.e("SockConfig", "heartbeat stop");
            }
        });
        thHearbeatTimer.start();
    }

    public void StartWokring(Handler handler, String strHost, int dport)//connect and recv
    {
        this.handler = handler;
        hostName = strHost;
        port = dport;

        //先停掉heatBeat 和reconnet
        StopNow();

        thSocket = new Thread(new ReceiveWatchDog());
        thSocket.start();
    }

    void FireReconnect()
    {
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

        if( thRecconet == null )
        {
            thRecconet = new Thread(new Runnable() {
                @Override
                public void run() {

                        while(ShouldStopNow == false)
                        {
                            try {
                             Thread.sleep(3000);
                            } catch (InterruptedException e) {
                                break;
                            }
                            if(ShouldStopNow == true)
                                break;

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
                            Log.e("SockConfig", "正在重连.");
                            thSocket = new Thread(new ReceiveWatchDog());
                            thSocket.start();
                        }
                    Log.e("Reconnect","stop");
                }
            });
            thRecconet.start();
        }
    }

    void StopReconnect()
    {
        if(thRecconet != null)
        {
            thRecconet.interrupt();
            thRecconet = null;
        }
    }

    class ReceiveWatchDog implements Runnable {
        @Override
        public void run() {

            try {
                Log.e("SockConfig", "==============IP:"+ hostName +"Port:" +Integer.toString(port));

                ShouldStopNow = false;
                InetAddress addr = InetAddress.getByName(hostName);
                String domainName = addr.getHostName();//获得主机名
                String ServerIP = addr.getHostAddress();//获得IP地址

                socket = new Socket(ServerIP, port);
                socket.setKeepAlive(true);

                Log.e("配置服务器", "连接成功开始心跳");

                FireHeadbeat();
                StopReconnect();

                {
                    Message message = Message.obtain();
                    message.what = 3;
                    if(handler  != null) handler.sendMessage(message);
                }

                while(socket != null && socket.isConnected() && ShouldStopNow == false){
                    try{
                        DataInputStream input = new DataInputStream(socket.getInputStream());
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                        byte jj[] = new byte[2048];
                        int r_len = input.read(jj);
                        if (r_len <= 0)
                        {
                            Log.e("配置服务器", "收到数据小鱼等于0.断开");
                            break;
                        }

                        String aa = new String(jj,0,r_len, "UTF-8");

                        if( aa.indexOf('\u0000') != -1 )
                            aa = aa.substring(0,aa.indexOf('\u0000'));

                        Log.e("config str", aa);
                        JSONObject jsonObject = new JSONObject(aa);
                        if(jsonObject.has("cmd") == false)
                        {
                            Log.e("没有CMD","关闭此连接");
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
                                    Log.e("MAC错误","不响应获取配置命令");
                                    break;
                                }
                            }

                            Log.e("收到请求配置命令", "返回");
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
                                    Log.e("MAC错误","不响应应用配置命令");
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
                            message1.what = 110;//软件自动更新
                            message1.obj = aa;

                            if( handler  != null)
                                handler.sendMessage(message1);

                            //{"cmd":"update", "url":"https://ssfasdfasdf", "versionCode":2}
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
            catch (IOException e) {
               // e.printStackTrace();
            }

            if (ShouldStopNow == false) {
                FireReconnect();
            } else {
                Log.e("SockConfig", "用户终止发送线程.");
            }
        }
    }

    public void sendMsg(byte[] msg) {
        synchronized(this)
        {
            if(socket == null)
            {
                Log.e("发送失败", "socket是空");
                return;
            }

            try {
                if (socket != null && socket.isConnected()) {
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(msg);
                    outputStream.flush();
                }
                else {
                        Log.e("发送失败", "socket没有连接");
                    }
            } catch (IOException e) {
               // FireReconnect();
            }
        }
    }

    public static final String bytesToHexString(byte[] buffer)
    {
        StringBuffer sb = new StringBuffer(buffer.length);
        String temp;

        for (int i = 0; i < buffer.length; ++i)
        {
            temp = Integer.toHexString(0xff&buffer[i]);
            if (temp.length() < 2)
                sb.append(0);

            sb.append(temp);
        }

        return sb.toString();
    }
}
