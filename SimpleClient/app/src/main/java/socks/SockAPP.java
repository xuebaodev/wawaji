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

        String readBuffer = "";
        boolean showlog = false;

        protected void onDataReceived(byte[] buffer, int size) {
            if (showlog)
                Log.e("222**", String.valueOf(buffer) + " ##### " + bytes2HexString(buffer, size) + " *** " + readBuffer);
            readBuffer = readBuffer + bytes2HexString(buffer, size);

            //开头可能就不正确
            if (readBuffer.contains("FE")) {
                readBuffer = readBuffer.substring(readBuffer.indexOf("FE"));
            } else {
                readBuffer = "";
                if (showlog) Log.e("~~~~", "开头可能就不正确 readBuffer = kong ");
            }

            //指令 至少是9位 包长度在第 7位
            while (readBuffer.length() > 9 * 2) {
                String slen = readBuffer.substring(12, 14);
                int len = Integer.parseInt(slen, 16);

                //包长度最大50
                if (len > 50) {
                    //包长度出错 应该是数据干扰
                    if (showlog) Log.e("~~~", "包长度出错");
                    //丢弃这条指令
                    readBuffer = readBuffer.substring(0, 2);
                    if (readBuffer.contains("FE")) {
                        readBuffer = readBuffer.substring(readBuffer.indexOf("FE"));
                    } else {
                        readBuffer = "";
                        if (showlog) Log.e("~~~~", "包长度出错 readBuffer = kong ");
                    }
                    continue;
                }

                if (readBuffer.length() >= len * 2) {
                    String sBegin = readBuffer.substring(0, 2);
                    if (showlog) Log.e("~~", "sBegin ******" + sBegin);
                    if (sBegin.equals("FE")) {
                        //开头正确
                        String msgContent = readBuffer.substring(0, len * 2);
                        if (showlog) Log.e("开头正确", msgContent);
                        //校验指令
                        if (check_com_data_string(msgContent, len * 2)) {
                            Log.e(TAG, "收到:" + msgContent);
                            readBuffer = readBuffer.substring(len * 2);
                            //指令正确
                            Message message = Message.obtain();
                            message.what = 10;
                            message.arg1 = len;
                            message.obj = hexStringToBytes(msgContent);
                            ;
                            if (handler != null) handler.sendMessage(message);

                        } else {
                            //指令不正确
                            if (showlog) Log.e("指令不正确", msgContent + "***" + readBuffer);
                            readBuffer = readBuffer.substring(2);
                            if (readBuffer.contains("FE")) {
                                readBuffer = readBuffer.substring(readBuffer.indexOf("FE"));
                            } else {
                                readBuffer = "";
                                if (showlog) Log.e("~~~~", "指令不正确 不包含FE readBuffer = kong ");
                            }
                        }
                    } else {
                        //开头不正确
                        if (showlog) Log.e("开头不正确", readBuffer);
                        if (readBuffer.contains("FE")) {
                            readBuffer = readBuffer.substring(readBuffer.indexOf("FE"));
                        } else {
                            readBuffer = "";
                            if (showlog) Log.e("~~~~", "开头不正确 目前不包含FE readBuffer = kong ");
                        }
                    }
                } else {
                    //等下一次接
                    if (showlog) Log.e("不够数", "等待" + readBuffer);
                    break;
                }
            }
        }

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

                        byte data_buff[] = new byte[1024];
                        int recv_len = reader.read(data_buff, 0, 1024);
                        if (recv_len > 0) {
                            Message message = Message.obtain();
                            message.what = 10;
                            message.arg1 = recv_len;
                            message.obj = data_buff;//hexStringToBytes(msgContent);
                            if (handler != null) handler.sendMessage(message);
                            //onDataReceived(data_buff, recv_len);
                        } else {
                            Log.e(TAG, "收到数据<=0.断开");
                            if( socket != null)
                            { socket.close();
                            socket = null;}
                            break;
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
        Log.e("Socket Sendount", ss);
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

    public static String bytes2HexString(byte[] b, int len) {
        String ret = "";
        for (int i = 0; i < len; i++) {
            String hex = Integer.toHexString(b[ i ] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            ret += hex.toUpperCase();
        }
        return ret;
    }

    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));

        }
        return d;
    }
    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public static boolean check_com_data_string(String data, int len) {
        if (len < 12) return false;
        int check_total = 0;
        //check sum
        for (int i=6 * 2 ;i < len - 2;i= i+2)
        {
            check_total += Integer.parseInt(data.substring(i,i+2),16);
        }
        if (check_total % 100 != Integer.parseInt(data.substring(len -2 ,len),16))
            return false;

        if (Integer.parseInt(data.substring(0,2),16) + Integer.parseInt(data.substring(6,8),16) != 255)
            return false;

        if (Integer.parseInt(data.substring(2,4),16) + Integer.parseInt(data.substring(8,10),16) != 255)
            return false;

        if (Integer.parseInt(data.substring(4,6),16) + Integer.parseInt(data.substring(10,12),16) != 255)
            return false;

        return true;
    }
}
