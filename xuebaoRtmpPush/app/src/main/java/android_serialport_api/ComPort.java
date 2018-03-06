/*
 * Copyright 2009 Cedric Priscal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package android_serialport_api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

import android.R.string;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.xuebao.rtmpPush.CameraPublishActivity;

import android_serialport_api.SerialPort;

public  class ComPort {

	protected SerialPort mSerialPort;
	protected OutputStream mOutputStream;
	private InputStream mInputStream;
	private ReadThread mReadThread;
	private Handler mHandler= null;

	boolean showlog = false;

	private class ReadThread extends Thread {

		@Override
		public void run() {
			super.run();
			while(!isInterrupted()) {
				int size=0;
				try {
					byte[] buffer = new byte[128];
					for(int k=0;k<128;k++)
						buffer[k]=0;

					if (mInputStream == null)
					    return;
					size = mInputStream.read(buffer);

					if (size > 0) {

                        Log.i("com_recv", "size" + size);
						onDataReceived(buffer, size);
					}
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}
		}
	}

	public ComPort(Handler handler)
	{
		mHandler = handler;
	}

	public void Start() {
		
		try {
			if (mSerialPort == null) {

				mSerialPort = new SerialPort(new File("/dev/ttyS1"), 115200, 0);
			}
			mOutputStream = mSerialPort.getOutputStream();
			mInputStream = mSerialPort.getInputStream();

			/* Create a receiving thread */
			mReadThread = new ReadThread();
			mReadThread.start();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidParameterException e) {
			e.printStackTrace();
		}
	}

	//protected abstract void onDataReceived(final byte[] buffer, final int size);
	
	public void SendData(byte[] buffer, int size)
	{
		try {
			if (mOutputStream != null) {
				mOutputStream.write(buffer,0, size);
				mOutputStream.flush();
			} else {
				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
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

	String readBuffer = "";
	protected void onDataReceived(byte[] buffer, int size) {
		if(showlog) Log.e("222**", String.valueOf(buffer) + " ##### " + bytes2HexString(buffer, size) + " *** " + readBuffer);
		readBuffer = readBuffer + bytes2HexString(buffer, size);

		//开头可能就不正确
		if (readBuffer.contains("FE")) {
			readBuffer = readBuffer.substring(readBuffer.indexOf("FE"));
		} else {
			readBuffer = "";
			if(showlog) Log.e("~~~~","开头可能就不正确 readBuffer = kong ");
		}

		//指令 至少是9位 包长度在第 7位
		while (readBuffer.length() >= 9 * 2) {
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
					Log.e("开头正确com", msgContent);
					//校验指令
					if (check_com_data_string(msgContent, len * 2)) {
						Log.e("指令正确com", msgContent);
						readBuffer = readBuffer.substring(len * 2);
						//指令正确
						if (mOutputStream != null) {
							if (mHandler != null) {
								Message message = Message.obtain();
								message.what = CameraPublishActivity.MessageType.msgComData.ordinal();
								message.arg1 = len;
								//这里可以直接传 msgContent 看你们自己的接收端处理参数类型
								message.obj = hexStringToBytes(msgContent);
								mHandler.sendMessage(message);
								Log.e("发送指令", msgContent);


								Message msa = Message.obtain();
								msa.what = CameraPublishActivity.MessageType.msgDebugTxt.ordinal();
								msa.obj = "发出串口数据";
								mHandler.sendMessage(msa);
							}
						}
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
	

	public void Destroy() {
		if (mReadThread != null)
			mReadThread.interrupt();
		
		if (mSerialPort != null) {
			mSerialPort.close();
			mSerialPort = null;
		}
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
