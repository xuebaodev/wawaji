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


//连接应用服务器的类。包含接收 心跳 和重连
public class SockAPP {
    private static final String TAG = "SockAPP";

    public String hostName = "192.168.4.1";
    public int port = 80;
    private Socket socket = null;
    private Handler handler;

    Thread thSocket = null;

    MsgThread msgThread= null;

    private boolean ShouldStopNow = false;

    public SockAPP() {
    }

    public void SetHandler(Handler handler)
    {
        this.handler = handler;
    }

    public void StopNow() {
        ShouldStopNow = true;

        if (socket != null) {
            try {
                socket.close();
                socket = null;
            } catch (IOException e) {
            }
        }
        if (thSocket != null) {
            thSocket.interrupt();
            thSocket = null;
        }

        if( msgThread != null)
        {
            msgThread.StopNow();
            msgThread.interrupt();
            msgThread = null;
        }
    }

    public void StartWokring(Handler handler, String strHost, int dport)//connect and recv
    {
        this.handler = handler;
        hostName = strHost;
        port = dport;

        ShouldStopNow = false;

        thSocket = new Thread(new ReceiveWatchDog());
        thSocket.start();

        msgThread = new MsgThread(this);
        msgThread.start();
    }

    public void ApplyNewServer(String strHost, int dport) {
        if (hostName.equals(strHost) && port == dport)
            return;

        hostName = strHost;
        port = dport;

        try {
            if(socket != null)
            {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {

        }
    }

    class ReceiveWatchDog implements Runnable {

        @Override
        public void run() {
            while (ShouldStopNow == false) {
                if (hostName.equals("") || port == 0)//参数不对。空循环
                {
                    try {
                        Thread.sleep(1000);
                        continue;
                    } catch (InterruptedException ea) {
                        continue;
                    }
                } else {
                    try {
                        Log.e(TAG, "try connect====IP:" + hostName + "Port:" + Integer.toString(port));

                        ShouldStopNow = false;
                        InetAddress addr = InetAddress.getByName(hostName);
                        String domainName = addr.getHostName();//获得主机名
                        String ServerIP = addr.getHostAddress();//获得IP地址

                        socket = new Socket(ServerIP, port);
                        socket.setKeepAlive(true);

                        Message message1 = Message.obtain();//将IP地址保存给娃娃机
                        message1.what = 0;
                        message1.obj = ServerIP;
                        if (handler != null) handler.sendMessage(message1);
                    } catch (IOException e) {
                        Log.e(TAG, "Connect excetion..retry after 3s");
                        try {
                            Thread.sleep(1000);
                            continue;
                        } catch (InterruptedException ea) {
                            continue;
                        }
                    }
                }

                while (socket != null && socket.isConnected() && ShouldStopNow == false) {
                    try {
                        InputStream reader = socket.getInputStream();

                        byte head[] = new byte[3];
                        int recv_len = reader.read(head, 0, 3);
                        if( recv_len<=0)
                        {
                            Log.e(TAG, "收到数据<=0.断开");
                            if( socket != null) {
                                socket.close();
                                socket = null;
                            }
                            break;
                        }
                        int headInt = (head[0]&0xff);
                        if( headInt==0xda && recv_len ==3)
                        {
                            int data_len = (int)(head[1]&0xff)*256 + head[2]&0xff;
                            byte data_body[] = new byte[data_len];

                            recv_len = reader.read(data_body, 0, data_len);
                            if( recv_len < data_len )//继续接收
                            {
                                Log.e(TAG, "接收不完整 继续接收");
                                int total_recv_ren = recv_len;
                                int left_data_len = data_len - recv_len;
                                while(total_recv_ren <data_len)
                                {
                                    recv_len = reader.read(data_body, total_recv_ren, left_data_len);
                                    if( recv_len<=0)
                                    {
                                        Log.e(TAG, "收到数据<=0.断开");
                                        if( socket != null) {
                                            socket.close();
                                            socket = null;
                                        }
                                        break;
                                    }
                                    total_recv_ren+= recv_len;
                                    left_data_len -= recv_len;
                                }
                            }

                            Message message = Message.obtain();
                            message.what = 10;
                            message.arg1 = data_len;
                            message.obj = data_body;//hexStringToBytes(msgContent);
                            if (handler != null) handler.sendMessage(message);
                                //onDataReceived(data_buff, recv_len);
                        }
                    } catch (IOException ea) {
                       // ea.printStackTrace();
                        try {
                            if( socket != null){socket.close();
                            socket = null;}
                        } catch (IOException aa) {
                            break;
                        }
                    }
                }
            }

            Log.e(TAG, "接收线程退出.");
        }
    }

    public void SendOut(byte[] msg)
    {
        String ss = bytesToHexString(msg);
        //Log.e("Socket Sendount", ss);
        if( msgThread != null)
            msgThread.putMsg( msg );
    }

    void sendMsg(byte[] msg) {
            if (socket == null) {
                Log.e(TAG, "socket是空,不发送");
                return;
            }

            try {
                if (socket != null && socket.isConnected()) {
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(msg);
                    outputStream.flush();
                } else {
                    Log.e(TAG, "socket没有连接.不发送");
                }
            } catch (IOException e) {
                // FireReconnect();
            }
    }

    public static final String bytesToHexString(byte[] buffer) {
        StringBuffer sb = new StringBuffer(buffer.length);
        String temp;

        for (int i = 0; i < buffer.length; ++i) {
            temp = Integer.toHexString(0xff & buffer[i]);
            if (temp.length() < 2)
                sb.append(0);

            sb.append(temp);
        }

        return sb.toString();
    }
}
