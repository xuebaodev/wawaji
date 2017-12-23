package socks;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.locks.Lock;

/**
 * Author:      JerryChow
 * Date:        2017/5/31 15:22
 * QQ:          384114651
 * Email:       zhoumricecream@163.com
 * Description:
 */
public class SendThread extends Thread {
    private Socket socket = null;
    public int port = 80;
    public String hostName = "192.168.4.1";
    private Handler handler;

    private boolean ShouldStopNow = false;

    public SendThread(Handler handler, String strHost, int dport) {
        this.handler = handler;
        hostName = strHost;
        port = dport;
        ShouldStopNow = false;
    }

    public void SetHandler(Handler handler)
    {
        this.handler = handler;
    }

    public void StopNow()
    {
        ShouldStopNow = true;
        CloseSocket();
        interrupt();
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

    @Override
    public void run() {
        super.run();
       // while (ShouldStopNow == false)
        //{
            if (socket == null) {
                try {
                    Log.e("SendThread.run.try", "==============IP:"+ hostName +"Port:" +Integer.toString(port));

                    InetAddress addr = InetAddress.getByName(hostName);
                    String domainName = addr.getHostName();//获得主机名
                    String ServerIP = addr.getHostAddress();//获得IP地址

                    socket = new Socket(ServerIP, port);
                    socket.setKeepAlive(true);

                    Message message = Message.obtain();
                    message.what = 0;
                    if(handler  != null) handler.sendMessage(message);

                } catch (IOException e) {
                    e.printStackTrace();
                   // break;
                }
            }

            while(socket != null && socket.isConnected() && ShouldStopNow == false){
                try{
                    InputStream reader=socket.getInputStream();
                    byte[] bHead = new byte[7];
                    int count = ReadDataUnti(bHead, 7, reader);
                    if(count != 7 )
                    {
                        break;
                    }

                    int data_length =  bHead[6]&0xff;//byte2Int(bHead, 6, 7);
                    Log.e("======socket head recv", "data len" + data_length);

                    byte datas[] = new byte[data_length-7];
                    int data_recved_len = ReadDataUnti(datas, data_length-7, reader);
                    if( data_recved_len != data_length-7)
                    {
                        break;
                    }

                    byte total_data[] = new byte[data_length];
                    System.arraycopy(bHead,0,total_data,0,7);
                    System.arraycopy(datas,0,total_data,7,data_length-7);

                    Log.i("=====recv====", bytesToHexString(total_data));

                    Message message = Message.obtain();
                    message.what = 10;
                    message.arg1 = data_length;
                    message.obj = total_data;
                    if(handler  != null) handler.sendMessage(message);

                }catch(Exception e){
                    e.printStackTrace();
                    break;
                }
            }
      // }

        if(ShouldStopNow == false)
        {
            Message message = Message.obtain();
            message.what = 20;
            message.obj = "soket异常.三秒后重试";
            if(handler  != null) handler.sendMessage(message);
            CloseSocket();
        }
        else {
            Log.e("SendThread", "用户终止发送线程.");
            CloseSocket();
        }
    }

    void CloseSocket()
    {
        ShouldStopNow = true;

        try {
            if(socket != null)
            {
                Log.e("==============", "CloseSocket");
                socket.close();
                socket = null;
            }
        }catch (IOException e)
        {
            e.printStackTrace();
            Log.e("SendThread", "socket关闭时 引发异常");
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
                ShouldStopNow = true;
                Message message = Message.obtain();
                message.what = 20;
                message.obj = "socket发送引发异常";
                if(handler  != null) handler.sendMessage(message);
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
