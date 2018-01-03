package socks;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.locks.Lock;

import android_serialport_api.ComPort;

//连接应用服务器的类。包含接收 心跳 和重连
public class SendThread {

    public String hostName = "192.168.4.1";
    public int port = 80;
    private Socket socket = null;
    private Handler handler;

    Thread thSocket  = null;
    Thread thHearbeatTimer  = null;
    Thread thRecconet = null;

    private boolean ShouldStopNow = false;

    public SendThread() {
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

    private int ReadDataUnti(byte[] datas, int expect_len, InputStream is) {
        int readCount = 0; // 已经成功读取的字节的个数

        while (readCount < expect_len ) {
            try{
                int recv_len = is.read(datas, readCount, expect_len - readCount);
                if (recv_len <= 0) {
                    Log.e("is.read返回<=0" , "返回值"+ recv_len);
                    return -1;
                } else
                    readCount += recv_len;
            }catch (IOException e)
            {
                Log.e("ReadDataUnti接收错误", "");
                return -1;
            }
        }

        return readCount;
    }

    void FireHeadbeat()
    {
        if( thHearbeatTimer != null)
        {
            thHearbeatTimer.interrupt();
            thHearbeatTimer = null;
        }

        final byte heart_beat_msg[] = new byte[21];
        heart_beat_msg[0] = (byte) 0xfe;
        heart_beat_msg[1] = (byte) (0);
        heart_beat_msg[2] = (byte) (0);
        heart_beat_msg[3] = (byte) ~heart_beat_msg[0];
        heart_beat_msg[4] = (byte) ~heart_beat_msg[1];
        heart_beat_msg[5] = (byte) ~heart_beat_msg[2];
        heart_beat_msg[6] = (byte) (heart_beat_msg.length);
        heart_beat_msg[7] = (byte) 0x35;

        String strMAC = VideoConfig.instance.getMac();
        System.arraycopy(strMAC.getBytes(), 0, heart_beat_msg, 8, strMAC.getBytes().length);

        int total_c = 0;
        for (int i = 6; i < heart_beat_msg.length - 1; i++) {
            total_c += (heart_beat_msg[i] & 0xff);
        }
        heart_beat_msg[heart_beat_msg.length - 1] = (byte) (total_c % 100);

        thHearbeatTimer = new Thread(new Runnable() {
            @Override
            public void run() {
                while(ShouldStopNow == false)
                {
                    if(ShouldStopNow == true)
                        break;

                    Log.e("my headt", "breat");
                    sendMsg(heart_beat_msg);

                    if(ShouldStopNow == true)
                        break;

                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                Log.e("SendSock", "heartbeat stop");
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
                            Log.e("SendThread", "正在重连.");
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
                Log.e("SendThread.run.try", "==============IP:" + hostName + "Port:" + Integer.toString(port));

                ShouldStopNow = false;
                InetAddress addr = InetAddress.getByName(hostName);
                String domainName = addr.getHostName();//获得主机名
                String ServerIP = addr.getHostAddress();//获得IP地址

                socket = new Socket(ServerIP, port);
                socket.setKeepAlive(true);

                Log.e("应用服务器", "连接成功开始心跳");

                Message message1 = Message.obtain();//将IP地址保存给娃娃机
                message1.what = 0;
                message1.obj = ServerIP;
                if (handler != null) handler.sendMessage(message1);

                FireHeadbeat();
                StopReconnect();

                while (socket != null && socket.isConnected() && ShouldStopNow == false) {
                    InputStream reader = socket.getInputStream();
                    byte[] bHead = new byte[7];
                    int count = ReadDataUnti(bHead, 7, reader);
                    if (count != 7)
                    {
                        Log.e("SendThread","读头失败。断开连接.");
                        break;
                    }

                    int data_length = bHead[6] & 0xff;//byte2Int(bHead, 6, 7);
                    //Log.e("======socket head recv", "data len" + data_length);

                    byte datas[] = new byte[data_length - 7];
                    int data_recved_len = ReadDataUnti(datas, data_length - 7, reader);
                    if (data_recved_len != data_length - 7) {
                        Log.e("SendThread","读数据失败。断开连接.");
                        break;
                    }

                    byte total_data[] = new byte[data_length];
                    System.arraycopy(bHead, 0, total_data, 0, 7);
                    System.arraycopy(datas, 0, total_data, 7, data_length - 7);

                    Message message = Message.obtain();
                    message.what = 10;
                    message.arg1 = data_length;
                    message.obj = total_data;
                    if (handler != null) handler.sendMessage(message);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            if (ShouldStopNow == false) {
                FireReconnect();

            } else {
                Log.e("SendThread", "用户终止发送线程.");
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
