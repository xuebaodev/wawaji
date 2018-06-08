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
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.util.Log;

public class SerialPort {

	private static final String TAG = "SerialPort";

	/*
	 * Do not remove or rename the field mFd: it is used by native method close();
	 */
	private FileDescriptor mFd;
	private FileInputStream mFileInputStream;
	private FileOutputStream mFileOutputStream;

	private  boolean isCheckOK = true;
	public SerialPort(File device, int baudrate, int flags) throws SecurityException, IOException {

		/* Check access permission */
		if (!device.canRead() || !device.canWrite()) {
			try {

				/* Missing read/write permission, trying to chmod the file */
				Process su;
				su = Runtime.getRuntime().exec("/system/bin/su");
				String cmd = "chmod 666 " + device.getAbsolutePath() + "\n"
						+ "exit\n";
				su.getOutputStream().write(cmd.getBytes());
				isCheckOK = true;
				if ((su.waitFor() != 0) || !device.canRead()
						|| !device.canWrite()) {
					isCheckOK = false;
					throw new SecurityException();
				}
			} catch (Exception e) {
				e.printStackTrace();
				isCheckOK = false;
				throw new SecurityException();

			}
		}

		if( isCheckOK )
		{
			try
			{
				mFd = open(device.getAbsolutePath(), baudrate, flags,5);//2018.2.1 听说串口要开关再开才不会出现bug
				if (mFd == null) {
					Log.e(TAG, "native open returns null");
					throw new IOException();
				}
				mFileInputStream = new FileInputStream(mFd);
				mFileOutputStream = new FileOutputStream(mFd);
			}catch (IOException iio)
			{
				Log.e(TAG, "串口打开引发异常");
				iio.printStackTrace();
			}
		}
		else
			Log.e(TAG, "串口打开失败");
	}

	// Getters and setters
	public InputStream getInputStream() {
		return mFileInputStream;
	}

	public OutputStream getOutputStream() {
		return mFileOutputStream;
	}

	// JNI
	private native static FileDescriptor open(String path, int baudrate, int flags,int min_byte);
	public native void close();
	static {
		System.loadLibrary("serial_port");
	}
}
