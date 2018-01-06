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
public class SockAPP {

    private static final String TAG = "SockAPP";

    public String hostName = "192.168.4.1";
    public int port = 80;
    private Socket socket = null;
    private Handler handler;

    Thread thSocket  = null;
    Thread thHearbeatTimer  = null;
    Thread thRecconet = null;

    private boolean ShouldStopNow = false;

    public SockAPP() {
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

        String sss = bytesToHexString(heart_beat_msg);
        Log.e(TAG, "触发心跳创建 " + sss);
        thHearbeatTimer = new Thread(new Runnable() {
            @Override
            public void run() {
                while (ShouldStopNow == false) {
                    if(ShouldStopNow == true)
                    {
                        Log.e(TAG, "ShouldStopNow = true 所以断开心跳");
                        break;
                    }

                    Log.e(TAG, "心跳");
                    sendMsg(heart_beat_msg);

                    Message me1 = Message.obtain();//心跳消息
                    me1.what = 11;
                    if (handler != null) handler.sendMessage(me1);

                    if(ShouldStopNow == true)
                    {
                        Log.e("SendThread", "ShouldStopNow = true 所以断开心跳");
                        break;
                    }

                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "InterruptedException 所以断开心跳");
                        break;
                    }
                }

                Log.e(TAG, "心跳停止.这是有意的吗?");
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
                            Log.e(TAG, "正在重连.");
                            thSocket = new Thread(new ReceiveWatchDog());
                            thSocket.start();
                        }

                        Log.e(TAG,"停止重连");

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

        String readBuffer = "";
        boolean showlog = false;

        protected void onDataReceived(byte[] buffer, int size) {
            if(showlog) Log.e("222**", String.valueOf(buffer) + " ##### " + ComPort.bytes2HexString(buffer, size) + " *** " + readBuffer);
            readBuffer = readBuffer + ComPort.bytes2HexString(buffer, size);

            //开头可能就不正确
            if (readBuffer.contains("FE")) {
                readBuffer = readBuffer.substring(readBuffer.indexOf("FE"));
            } else {
                readBuffer = "";
                if(showlog) Log.e("~~~~","开头可能就不正确 readBuffer = kong ");
            }

            //指令 至少是9位 包长度在第 7位
            while (readBuffer.length() > 9 * 2) {
                String slen = readBuffer.substring(12, 14);
                int len = Integer.parseInt(slen, 16);

                //包长度最大50
                if (len > 50)
                {
                    //包长度出错 应该是数据干扰
                    if(showlog) Log.e("~~~","包长度出错");
                    //丢弃这条指令
                    readBuffer = readBuffer.substring(0,2);
                    if (readBuffer.contains("FE")) {
                        readBuffer = readBuffer.substring(readBuffer.indexOf("FE"));
                    } else
                    {
                        readBuffer = "";
                        if(showlog) Log.e("~~~~","包长度出错 readBuffer = kong ");
                    }
                    continue;
                }

                if (readBuffer.length()>= len * 2) {
                    String sBegin = readBuffer.substring(0, 2);
                    if(showlog) Log.e("~~", "sBegin ******" + sBegin);
                    if (sBegin.equals("FE")) {
                        //开头正确
                        String msgContent = readBuffer.substring(0, len * 2);
                        if(showlog) Log.e("开头正确", msgContent);
                        //校验指令
                        if (ComPort.check_com_data_string(msgContent, len * 2)) {
                           Log.e(TAG, "收到:"+ msgContent);
                            readBuffer = readBuffer.substring(len * 2);
                            //指令正确
                            Message message = Message.obtain();
                            message.what = 10;
                            message.arg1 = len;
                            message.obj = ComPort.hexStringToBytes(msgContent);;
                            if (handler != null) handler.sendMessage(message);

                        } else {
                            //指令不正确
                            if(showlog) Log.e("指令不正确", msgContent + "***" + readBuffer);
                            readBuffer = readBuffer.substring(2);
                            if (readBuffer.contains("FE")) {
                                readBuffer = readBuffer.substring(readBuffer.indexOf("FE"));
                            } else {
                                readBuffer = "";
                                if(showlog) Log.e("~~~~","指令不正确 不包含FE readBuffer = kong ");
                            }
                        }
                    } else
                    {
                        //开头不正确
                        if(showlog) Log.e("开头不正确", readBuffer);
                        if (readBuffer.contains("FE")) {
                            readBuffer = readBuffer.substring(readBuffer.indexOf("FE"));
                        } else {
                            readBuffer = "";
                            if(showlog) Log.e("~~~~","开头不正确 目前不包含FE readBuffer = kong ");
                        }
                    }
                }
                else
                {
                    //等下一次接
                    if(showlog) Log.e("不够数", "等待" + readBuffer);
                    break;
                }
            }
        }


        @Override
        public void run() {
            try {
                Log.e(TAG, "try connect====IP:" + hostName + "Port:" + Integer.toString(port));

                ShouldStopNow = false;
                InetAddress addr = InetAddress.getByName(hostName);
                String domainName = addr.getHostName();//获得主机名
                String ServerIP = addr.getHostAddress();//获得IP地址

                socket = new Socket(ServerIP, port);
                socket.setKeepAlive(true);

                Log.e(TAG, "连接成功开始心跳");

                Message message1 = Message.obtain();//将IP地址保存给娃娃机
                message1.what = 0;
                message1.obj = ServerIP;
                if (handler != null) handler.sendMessage(message1);

                FireHeadbeat();
                StopReconnect();

                while (socket != null && socket.isConnected() && ShouldStopNow == false) {
                    InputStream reader = socket.getInputStream();

                    byte data_buff[] = new byte[1024];
                    int recv_len = reader.read( data_buff,0,1024);
                    if(recv_len > 0)
                    {
                        onDataReceived(data_buff, recv_len);
                    }
                    else
                        {
                            Log.e(TAG, "收到数据<=0.断开");
                            break;
                        }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            if (ShouldStopNow == false) {
                FireReconnect();

            } else {
                Log.e(TAG, "用户终止发送线程.");
            }
        }
    }

    public void sendMsg(byte[] msg) {
        synchronized(this)
        {
            if(socket == null)
            {
                Log.e(TAG, "socket是空,不发送");
                return;
            }

            try {
                if (socket != null && socket.isConnected()) {
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(msg);
                    outputStream.flush();
                }
                else {
                        Log.e(TAG, "socket没有连接.不发送");
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
