/*
 * CameraPublishActivity.java
 * CameraPublishActivity
 * 
 * Github: https://github.com/daniulive/SmarterStreaming
 * 
 * Created by DaniuLive on 2015/09/20.
 * Copyright © 2014~2016 DaniuLive. All rights reserved.
 */

package com.xuebao.rtmpPush;

import com.daniulive.smartpublisher.RecorderManager;
import com.daniulive.smartpublisher.SmartPublisherJni.WATERMARK;
import com.daniulive.smartpublisher.SmartPublisherJniV2;
import com.eventhandle.NTSmartEventCallbackV2;
import com.eventhandle.NTSmartEventID;
//import com.voiceengine.NTAudioRecord;	//for audio capture..
import com.voiceengine.NTAudioRecordV2;
import com.voiceengine.NTAudioRecordV2Callback;
import com.voiceengine.NTAudioUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.hardware.Camera.AutoFocusCallback;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.Toast;
import android.widget.VideoView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import android_serialport_api.SerialPort;
import android_serialport_api.ComPort;
import socks.*;
import update.SilentInstall;

@SuppressWarnings("deprecation")
public class CameraPublishActivity extends Activity
{
	private static String TAG = "SmartPublisher";
	
	//NTAudioRecord audioRecord_ = null;	//for audio capture
	
	NTAudioRecordV2 audioRecord_ = null;
	
	NTAudioRecordV2Callback audioRecordCallback_ = null;

	private long publisherHandleBack = 0;
	
	private long publisherHandleFront = 0;
	
	private SmartPublisherJniV2 libPublisher = null;

	/* 推流分辨率选择
	 * 0: 640*480
	 * 1: 320*240
	 * 2: 176*144
	 * 3: 1280*720
	 * */
	private Spinner resolutionSelector;
	
	/* video软编码profile设置
     * 1: baseline profile
     * 2: main profile
     * 3: high profile
	 * */
	private Spinner swVideoEncoderProfileSelector;

	private Spinner recorderSelector;

	private Button  btnRecoderMgr;

	private Spinner swVideoEncoderSpeedSelector;

	private Button	btnHWencoder;
	
	private Button btnStartPush;
	private Button btnStartRecorder;
	
	private SurfaceView mSurfaceViewFront = null;  
    private SurfaceHolder mSurfaceHolderFront = null;  
    
    private SurfaceView mSurfaceViewBack = null;
    private SurfaceHolder mSurfaceHolderBack = null;  
    
    private Camera mCameraFront = null;  
	private AutoFocusCallback myAutoFocusCallbackFront = null;
	
	private Camera mCameraBack = null;
	private AutoFocusCallback myAutoFocusCallbackBack = null;
	
	private boolean mPreviewRunningFront = false;
	private boolean mPreviewRunningBack = false;

	private boolean isPushing   = false;
	private boolean isRecording = false;

	private String txt = "当前状态";
		
	private static final int FRONT = 1;		//前置摄像头标记
	private static final int BACK = 2;		//后置摄像头标记

	private int curFrontCameraIndex = -1;
	private int curBackCameraIndex  = -1;

    public static ComPort mComPort;
    public SockAPP sendThread;//应用服务器
	public SockConfig confiThread;//配置服务器
	MyTCServer lis_server = null;

	private WifiManager wifiManager;
	WifiAutoConnectManager wifiauto;
    private Context myContext;

	enum PushState{UNKNOWN, OK, FAILED, CLOSE};
	PushState pst_front = PushState.UNKNOWN;
	PushState pst_back = PushState.UNKNOWN;

	static {  
		System.loadLibrary("SmartPublisher");
	}

	BroadcastReceiver mSdcardReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {

			String path = intent.getData().getPath();

			if(intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)){
				Toast.makeText(context, "path1111:"+intent.getData().getPath(), Toast.LENGTH_SHORT).show();
				Message message = Message.obtain();
				message.what = 190;
				message.obj = path;
				if(mHandler  != null) mHandler.sendMessage(message);

			}else if(intent.getAction().equals(Intent.ACTION_MEDIA_REMOVED)){
				Log.e("123", "remove ACTION_MEDIA_REMOVED" + path);
			}else if(intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)){
				Log.e("123", "remove ACTION_MEDIA_REMOVED" + path);
			}
		}
	};

	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); 	//屏幕常亮
        setContentView(R.layout.activity_main);
		myContext = this.getApplicationContext();

		wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
		wifiauto = new WifiAutoConnectManager(wifiManager);
		//check psw if not exit

		pst_front = PushState.UNKNOWN;
		pst_back = PushState.UNKNOWN;

		//接受U盘挂载事件
		IntentFilter filter = null;
		filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);   //接受外媒挂载过滤器
		filter.addAction(Intent.ACTION_MEDIA_REMOVED);   //接受外媒挂载过滤器
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);   //接受外媒挂载过滤器
		filter.addDataScheme("file");

		registerReceiver(mSdcardReceiver, filter,"android.permission.READ_EXTERNAL_STORAGE",null);

		VideoConfig.instance = new VideoConfig();
		VideoConfig.instance.LoadConfig(this, mHandler);

		//串口对象
		if (mComPort == null) {
			mComPort = new ComPort(mHandler);
		}
		mComPort.Start();

		initUI();

		UpdateConfigToUI();

		if(getLocalIpAddress().equals(""))//网卡尚未就绪IP地址没有获取
		{
			mHandler.sendEmptyMessage(1);
		}
		else
		{
			mHandler.sendEmptyMessage(2);
		}
	}

	@Override
	protected  void onDestroy()
	{
		Log.i(TAG, "activity destory!");

		if(confiThread != null)
		{
			Log.e("app退出", "配置线程终止");
			confiThread.StopNow(); confiThread = null;
		}

		if(sendThread != null)
		{
			Log.e("app退出", "应用线程终止");
			sendThread.StopNow(); sendThread = null;
		}

		if( lis_server != null)
		{
			Log.e("app退出", "监听线程终止");
			lis_server.StopNow(); lis_server = null;
		}

		if ( isPushing || isRecording )
		{
			if( audioRecord_ != null )
			{
				Log.i(TAG, "surfaceDestroyed, call StopRecording..");

				//audioRecord_.StopRecording();
				//audioRecord_ = null;

				audioRecord_.Stop();

				if ( audioRecordCallback_ != null )
				{
					audioRecord_.RemoveCallback(audioRecordCallback_);
					audioRecordCallback_ = null;
				}

				audioRecord_ = null;
			}

			stopPush();
			stopRecorder();

			isPushing = false;
			isRecording = false;

			if ( publisherHandleFront != 0 )
			{
				if ( libPublisher != null )
				{
					libPublisher.SmartPublisherClose(publisherHandleFront);
					publisherHandleFront = 0;
				}
			}

			if ( publisherHandleBack != 0 )
			{
				if ( libPublisher != null )
				{
					libPublisher.SmartPublisherClose(publisherHandleBack);
					publisherHandleBack = 0;
				}
			}
		}

		super.onDestroy();
		finish();
		System.exit(0);
	}

	void initUI()
	{
		TextView tviapptitle = findViewById(R.id.id_app_title);
		tviapptitle.setText( APKVersionCodeUtils.getVerName(this) + APKVersionCodeUtils.getVersionCode(this) );

		//DHCP checkbox的逻辑
		CheckBox cbDHCP = findViewById(R.id.checkBoxAutoGet);
		cbDHCP.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton button, boolean isChecked) {
				if (isChecked) {
					findViewById(R.id.my_ip_addr).setEnabled(false);
					findViewById(R.id.my_netgate_tip).setVisibility(View.INVISIBLE);
					findViewById(R.id.my_gate_addr).setVisibility(View.INVISIBLE);
					findViewById(R.id.my_netmask_tip).setVisibility(View.INVISIBLE);
					findViewById(R.id.my_netmask_addr).setVisibility(View.INVISIBLE);
					VideoConfig.instance.using_dhcp = true;
					EditText eti_my_ip_addr= findViewById(R.id.my_ip_addr);
					eti_my_ip_addr.setText( getLocalIpAddress() );

				}else {
					findViewById(R.id.my_ip_addr).setEnabled(true);
					findViewById(R.id.my_netgate_tip).setVisibility(View.VISIBLE);
					findViewById(R.id.my_gate_addr).setVisibility(View.VISIBLE);
					findViewById(R.id.my_netmask_tip).setVisibility(View.VISIBLE);
					findViewById(R.id.my_netmask_addr).setVisibility(View.VISIBLE);
					VideoConfig.instance.using_dhcp = false;
				}
			}
		} );

		//是否使用预设视频配置
		CheckBox cbPrefernce = findViewById( R.id.checkUsePrefence);
		cbPrefernce.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton button, boolean isChecked) {
				if (isChecked) {
					findViewById(R.id.resolutionSelctor).setVisibility(View.VISIBLE);
					findViewById(R.id.custum_w_tip).setVisibility(View.INVISIBLE);
					findViewById(R.id.custum_wideo_w).setVisibility(View.INVISIBLE);
					findViewById(R.id.custum_h_tip).setVisibility(View.INVISIBLE);
					findViewById(R.id.custum_wideo_h).setVisibility(View.INVISIBLE);

				}else {
					findViewById(R.id.resolutionSelctor).setVisibility(View.INVISIBLE);
					findViewById(R.id.custum_w_tip).setVisibility(View.VISIBLE);
					findViewById(R.id.custum_wideo_w).setVisibility(View.VISIBLE);
					findViewById(R.id.custum_h_tip).setVisibility(View.VISIBLE);
					findViewById(R.id.custum_wideo_h).setVisibility(View.VISIBLE);
				}
			}
		} );

		//分辨率配置
		resolutionSelector = (Spinner)findViewById(R.id.resolutionSelctor);
		final String []resolutionSel = new String[]{"960*720", "640*480","640*360", "352*288","320*240"};
		ArrayAdapter<String> adapterResolution = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, resolutionSel);
		adapterResolution.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		resolutionSelector.setAdapter(adapterResolution);
		resolutionSelector.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if( isPushing || isRecording)
				{
					return;
				}

				SwitchResolution(position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		//软编码配置
		swVideoEncoderProfileSelector = (Spinner)findViewById(R.id.swVideoEncoderProfileSelector);
		final String []profileSel = new String[]{"BaseLineProfile", "MainProfile", "HighProfile"};
		ArrayAdapter<String> adapterProfile = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, profileSel);
		adapterProfile.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		swVideoEncoderProfileSelector.setAdapter(adapterProfile);
		swVideoEncoderProfileSelector.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if( isPushing || isRecording)
				{
					return;
				}

				VideoConfig.instance.sw_video_encoder_profile = position + 1;
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});

		//软编码关键帧数
		swVideoEncoderSpeedSelector = (Spinner)findViewById(R.id.sw_video_encoder_speed_selctor);
		final String [] video_encoder_speed_Sel = new String[]{"6", "5", "4", "3", "2", "1"};
		ArrayAdapter<String> adapterVideoEncoderSpeed = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, video_encoder_speed_Sel);
		adapterVideoEncoderSpeed.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		swVideoEncoderSpeedSelector.setAdapter(adapterVideoEncoderSpeed);
		swVideoEncoderSpeedSelector.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				VideoConfig.instance.sw_video_encoder_speed = 6 - position;
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});


		//Recorder related settings
		recorderSelector = (Spinner)findViewById(R.id.recoder_selctor);
		final String []recoderSel = new String[]{"本地不录像", "本地录像"};
		ArrayAdapter<String> adapterRecoder = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, recoderSel);
		adapterRecoder.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		recorderSelector.setAdapter(adapterRecoder);
		recorderSelector.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if ( 1 == position )
				{
					VideoConfig.instance.is_need_local_recorder = true;
				}
				else
				{
					VideoConfig.instance.is_need_local_recorder = false;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		btnRecoderMgr = (Button)findViewById(R.id.button_recoder_manage);
		btnRecoderMgr.setOnClickListener(new ButtonRecorderMangerListener());

		btnStartRecorder = (Button)findViewById(R.id.button_start_recorder);
		btnStartRecorder.setOnClickListener(new ButtonStartRecorderListener());
		//end

		btnHWencoder = (Button)findViewById(R.id.button_hwencoder);
		btnHWencoder.setOnClickListener(new ButtonHardwareEncoderListener());

		mSurfaceViewFront = (SurfaceView) this.findViewById(R.id.surface_front);
		mSurfaceHolderFront = mSurfaceViewFront.getHolder();
		mSurfaceHolderFront.addCallback(new NT_SP_SurfaceHolderCallback(FRONT));
		mSurfaceHolderFront.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		//自动聚焦变量回调
		myAutoFocusCallbackFront = new AutoFocusCallback()
		{
			public void onAutoFocus(boolean success, Camera camera) {
				if(success)//success表示对焦成功
				{
					Log.i(TAG, "Front onAutoFocus succeed...");
				}
				else
				{
					Log.i(TAG, "Front onAutoFocus failed...");
				}
			}
		};

		mSurfaceViewBack = (SurfaceView) this.findViewById(R.id.surface_back);
		mSurfaceHolderBack = mSurfaceViewBack.getHolder();
		mSurfaceHolderBack.addCallback(new NT_SP_SurfaceHolderCallback(BACK));
		mSurfaceHolderBack.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		//自动聚焦变量回调
		myAutoFocusCallbackBack = new AutoFocusCallback()
		{
			public void onAutoFocus(boolean success, Camera camera) {
				if(success)//success表示对焦成功
				{
					Log.i(TAG, "Back onAutoFocus succeed...");
				}
				else
				{
					Log.i(TAG, "Back onAutoFocus failed...");
				}
			}
		};

		TextView timac = findViewById(R.id.id_mac);
		timac.setText("MAC: "+ VideoConfig.instance.getMac());

		btnStartPush = (Button)findViewById(R.id.button_start_push);
		btnStartPush.setOnClickListener(new ButtonStartPushListener());
	}

	byte[] strIPtob(String sip)
	{
		String [] ipb = sip.split("\\.");
		byte[]b = new byte[4];
		b[0]=(byte)Integer.parseInt(ipb[0]);
		b[1]=(byte)Integer.parseInt(ipb[1]);
		b[2]=(byte)Integer.parseInt(ipb[2]);
		b[3]=(byte)Integer.parseInt(ipb[3]);
		return b;
	}

	int g_packget_id = 0;
	void send_com_data(int... params) {
		byte send_buf[] = new byte[8+params.length];
		send_buf[0] = (byte) 0xfe;
		send_buf[1] = (byte) (g_packget_id);
		send_buf[2] = (byte) (g_packget_id >> 8);
		send_buf[3] = (byte) ~send_buf[0];
		send_buf[4] = (byte) ~send_buf[1];
		send_buf[5] = (byte) ~send_buf[2];
		send_buf[6] = (byte) (8+params.length);
		for (int i = 0; i < params.length; i++) {
			send_buf[7+i] = (byte)(params[i]);
		}

		int sum = 0;
		for (int i = 6; i < (8+params.length - 1); i++) {
			sum += (send_buf[i]&0xff);
		}

		send_buf[8+params.length-1] = (byte)(sum % 100);
		mComPort.SendData(send_buf, send_buf.length);
		g_packget_id++;
	}

	void SaveAppHostInfoToCom(String AIP)
	{
		String [] ipb = AIP.split("\\.");
		byte[]b = new byte[6];
		b[0]=(byte)Integer.parseInt(ipb[0]);
		b[1]=(byte)Integer.parseInt(ipb[1]);
		b[2]=(byte)Integer.parseInt(ipb[2]);
		b[3]=(byte)Integer.parseInt(ipb[3]);

		b[4] =(byte)( VideoConfig.instance.GetAppPort()/256 );
		b[5] = (byte)( VideoConfig.instance.GetAppPort()%256 );

		send_com_data(0x40, b[0],  b[1], b[2],  b[3],  b[4],  b[5]);
	}

	void ComParamSet(boolean includeMAC, boolean includedIP, boolean includeduserID) {

		//给娃娃机发送本机的MAC
		if(includeMAC)
		{
			byte msg_content[] = new byte[21];
			msg_content[0] = (byte) 0xfe;
			msg_content[1] = (byte) (0);
			msg_content[2] = (byte) (0);
			msg_content[3] = (byte) ~msg_content[0];
			msg_content[4] = (byte) ~msg_content[1];
			msg_content[5] = (byte) ~msg_content[2];
			msg_content[6] = (byte) (msg_content.length);
			msg_content[7] = (byte) 0x3f;
			String strMAC = VideoConfig.instance.getMac();
			System.arraycopy(strMAC.getBytes(), 0, msg_content, 8, strMAC.getBytes().length);
			int total_c = 0;
			for (int i = 6; i < msg_content.length - 1; i++) {
				total_c += (msg_content[i] & 0xff);
			}
			msg_content[msg_content.length - 1] = (byte) (total_c % 100);
			mComPort.SendData(msg_content, msg_content.length);
			String sss = SockAPP.bytesToHexString( msg_content );
			outputInfo("MaC发往串口"+ sss);
		}

		//ip
		if(includedIP)
		{
			byte msg_content[] = new byte[13];
			msg_content[0] = (byte) 0xfe;
			msg_content[1] = (byte) (0);
			msg_content[2] = (byte) (0);
			msg_content[3] = (byte) ~msg_content[0];
			msg_content[4] = (byte) ~msg_content[1];
			msg_content[5] = (byte) ~msg_content[2];
			msg_content[6] = (byte) (msg_content.length);
			msg_content[7] = (byte) 0x39;

			if(VideoConfig.instance.hostIP.equals(""))
				VideoConfig.instance.hostIP = getLocalIpAddress();

			byte bip[] = strIPtob(VideoConfig.instance.hostIP );
			System.arraycopy(bip, 0, msg_content, 8, bip.length);

			int total_c = 0;
			for (int i = 6; i < msg_content.length - 1; i++) {
				total_c += (msg_content[i] & 0xff);
			}
			msg_content[msg_content.length - 1] = (byte) (total_c % 100);
			mComPort.SendData(msg_content, msg_content.length);
		}

		//userid
		if(includeduserID)
		{
			byte msg_content[] = new byte[25];
			msg_content[0] = (byte) 0xfe;
			msg_content[1] = (byte) (0);
			msg_content[2] = (byte) (0);
			msg_content[3] = (byte) ~msg_content[0];
			msg_content[4] = (byte) ~msg_content[1];
			msg_content[5] = (byte) ~msg_content[2];
			msg_content[6] = (byte) (msg_content.length);
			msg_content[7] = (byte) 0x3a;

			int psw_len = VideoConfig.instance.userID.length() >16?16:VideoConfig.instance.userID.length();
			System.arraycopy(VideoConfig.instance.userID.getBytes(), 0, msg_content, 8, psw_len);

			int total_c = 0;
			for (int i = 6; i < msg_content.length - 1; i++) {
				total_c += (msg_content[i] & 0xff);
			}
			msg_content[msg_content.length - 1] = (byte) (total_c % 100);
			mComPort.SendData(msg_content, msg_content.length);
		}
	}

    //断开已有连接。重新连接到服务器
    void ServerStopAndReconnect()
	{
		if( VideoConfig.instance.destHost.equals("") || VideoConfig.instance.GetAppPort()==0)
		{
			outputInfo("应用服务器未配置。不连.");
			Toast.makeText(getApplicationContext(), "应用服务器未配置。不连.", Toast.LENGTH_SHORT).show();
			Log.e("MainAAAAA", "应用服务器未配置。不连.");
			return;
		}

		if( sendThread != null)
		{
			if(sendThread.hostName.equals(VideoConfig.instance.destHost) && sendThread.port == VideoConfig.instance.GetAppPort())
				return;
		}

		if (sendThread != null) {
			Log.e("应用服务器配置变", "重新连接");
			sendThread.StopNow();
			sendThread = null;
		}

        sendThread = new SockAPP();
		sendThread.StartWokring(mHandler, VideoConfig.instance.destHost, VideoConfig.instance.GetAppPort());
	}

	//断开已有连接。重新连接到服务器
	void ConfigServerStopAndReconnect()
	{
		Log.e("ConfigSerReconnect", "ConfigServerStopAndReconnect");
		if( VideoConfig.instance.configHost.equals("") || VideoConfig.instance.GetConfigPort()==0)
		{
			outputInfo("配置服务器未配置。不连.");
			Toast.makeText(getApplicationContext(), "配置服务器未配置。不连.", Toast.LENGTH_SHORT).show();
			Log.e("MainAAAAA", "配置服务器未配置。不连.");
			return;
		}

		if( confiThread != null)
		{
			if(confiThread.hostName .equals(VideoConfig.instance.configHost) && confiThread.port == VideoConfig.instance.GetConfigPort())
				return;
		}

		if (confiThread != null) {
			Log.e("配置服务器配置变", "重新连接");
			confiThread.StopNow();
			confiThread = null;
		}

		confiThread = new SockConfig();
		confiThread.StartWokring(mHandler,  VideoConfig.instance.configHost, VideoConfig.instance.GetConfigPort());
	}

	public static String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						String ipAddress = inetAddress.getHostAddress().toString();
						if(!ipAddress.contains("::"))
							return inetAddress.getHostAddress().toString();
					}else
						continue;
				}
			}
		} catch (SocketException ex) {
			Log.e("adsf", ex.toString());
		}
		return "";
	}

	//更新配置到UI
	void UpdateConfigToUI()
	{
		//本机名称
		EditText eti_myname= findViewById(R.id.id_my_name1);
		eti_myname.setText( VideoConfig.instance.machine_name );

		//是否自动获取IP
		if(VideoConfig.instance.using_dhcp == false)
		{
			CheckBox cbDHCP = findViewById(R.id.checkBoxAutoGet);
			cbDHCP.setChecked( false );

			//设置本机为静态IP 并且设置IP地址
			findViewById(R.id.my_ip_addr).setEnabled(true);
			EditText eti = (EditText)findViewById(R.id.my_ip_addr);
			eti.setText( VideoConfig.instance.hostIP);

			findViewById(R.id.my_netgate_tip).setVisibility(View.VISIBLE);
			findViewById(R.id.my_gate_addr).setVisibility(View.VISIBLE);
			eti = (EditText)findViewById(R.id.my_gate_addr);
			eti.setText( VideoConfig.instance.gateIP);

			findViewById(R.id.my_netmask_tip).setVisibility(View.VISIBLE);
			findViewById(R.id.my_netmask_addr).setVisibility(View.VISIBLE);
			eti = (EditText)findViewById(R.id.my_netmask_addr);
			eti.setText(VideoConfig.instance.maskIP);

		}else {
			CheckBox cbDHCP = findViewById(R.id.checkBoxAutoGet);
			cbDHCP.setChecked( true );

			//设置为动态获取IP
			findViewById(R.id.my_ip_addr).setEnabled(false);
			EditText eti_my_ip_addr= findViewById(R.id.my_ip_addr);
			VideoConfig.instance.hostIP = getLocalIpAddress();
			eti_my_ip_addr.setText( VideoConfig.instance.hostIP );

			findViewById(R.id.my_netgate_tip).setVisibility(View.INVISIBLE);
			findViewById(R.id.my_gate_addr).setVisibility(View.INVISIBLE);
			findViewById(R.id.my_netmask_tip).setVisibility(View.INVISIBLE);
			findViewById(R.id.my_netmask_addr).setVisibility(View.INVISIBLE);
		}

		//是否使用预设分辨率
		CheckBox cbPrefernce = findViewById( R.id.checkUsePrefence);
		if(VideoConfig.instance.GetResolutionIndex() != -1)
		{
			cbPrefernce.setChecked( true );
			findViewById(R.id.resolutionSelctor).setVisibility(View.VISIBLE);
			findViewById(R.id.custum_w_tip).setVisibility(View.INVISIBLE);
			findViewById(R.id.custum_wideo_w).setVisibility(View.INVISIBLE);
			findViewById(R.id.custum_h_tip).setVisibility(View.INVISIBLE);
			findViewById(R.id.custum_wideo_h).setVisibility(View.INVISIBLE);
			resolutionSelector.setSelection( VideoConfig.instance.GetResolutionIndex() );
		}
		else
		{
			cbPrefernce.setChecked(false);
			findViewById(R.id.resolutionSelctor).setVisibility(View.INVISIBLE);
			findViewById(R.id.custum_w_tip).setVisibility(View.VISIBLE);
			findViewById(R.id.custum_wideo_w).setVisibility(View.VISIBLE);
			EditText eti = (EditText)findViewById(R.id.custum_wideo_w);
			eti.setText( Integer.toString( VideoConfig.instance.GetVideoWidth() ) );

			findViewById(R.id.custum_h_tip).setVisibility(View.VISIBLE);
			findViewById(R.id.custum_wideo_h).setVisibility(View.VISIBLE);
			eti = (EditText)findViewById(R.id.custum_wideo_h);
			eti.setText( Integer.toString( VideoConfig.instance.GetVideoHeight() ) );
		}

		//软硬编码按钮
		if ( VideoConfig.instance.is_hardware_encoder )
		{
			btnHWencoder.setText("当前硬编码");
			//显示软编码选项
			findViewById(R.id.swVideoEncoderProfileSelector).setVisibility(View.INVISIBLE);
			findViewById(R.id.speed_tip).setVisibility(View.INVISIBLE);
			findViewById(R.id.sw_video_encoder_speed_selctor).setVisibility(View.INVISIBLE);
		}
		else {
			btnHWencoder.setText("当前软编码");
			//显示软编码选项
			findViewById(R.id.swVideoEncoderProfileSelector).setVisibility(View.VISIBLE);
			swVideoEncoderProfileSelector.setSelection(VideoConfig.instance.sw_video_encoder_profile -1);

			findViewById(R.id.speed_tip).setVisibility(View.VISIBLE);
			findViewById(R.id.sw_video_encoder_speed_selctor).setVisibility(View.VISIBLE);
			swVideoEncoderSpeedSelector.setSelection( 6 - VideoConfig.instance.sw_video_encoder_speed);
		}

		//帧率
		EditText eti = findViewById(R.id.push_rate);
		eti.setText(Integer.toString( VideoConfig.instance.GetFPS() ));

		//推流地址1
		EditText eti_url1 = findViewById(R.id.cam1_url_edit);
		eti_url1.setText( VideoConfig.instance.url1 );

		//推流地址2
		EditText eti_url2 = findViewById(R.id.cam2_url_edit);
		eti_url2.setText( VideoConfig.instance.url2 );

		//应用服务器IP
		EditText eti_serverip= findViewById(R.id.server_ip);
		eti_serverip.setText( VideoConfig.instance.destHost );

		//应用服务器端口
		EditText eti_server_port= findViewById(R.id.server_port);
		eti_server_port.setText( Integer.toString(VideoConfig.instance.GetAppPort() ));

		//配置服务器ip
		EditText eti_configserverip= findViewById(R.id.config_server_ip);
		eti_configserverip.setText( VideoConfig.instance.configHost );

		//配置服务器端口
		EditText eti_configserver_port= findViewById(R.id.config_server_port);
		eti_configserver_port.setText( Integer.toString(VideoConfig.instance.GetConfigPort()));

		//录像选项
		if(VideoConfig.instance.is_need_local_recorder == false)
			recorderSelector.setSelection( 0 );
		else
			recorderSelector.setSelection( 1 );

		EditText eti_userID = findViewById(R.id.id_userid);
		eti_userID.setText( VideoConfig.instance.userID);
	}

	boolean SaveConfigFromUI()
	{
		//保存--本机名称
		EditText eti_myname= findViewById(R.id.id_my_name1);
		VideoConfig.instance.machine_name = eti_myname.getText().toString().trim();

		//dhcp
		CheckBox cbDHCP = findViewById(R.id.checkBoxAutoGet);
		if(cbDHCP.isChecked())
		{
			VideoConfig.instance.using_dhcp = true;
			EditText eti_my_ip_addr= findViewById(R.id.my_ip_addr);
			VideoConfig.instance.hostIP = getLocalIpAddress();
			eti_my_ip_addr.setText( VideoConfig.instance.hostIP );
		} else{
			VideoConfig.instance.using_dhcp = false;

			EditText eti_my_ip_addr= findViewById(R.id.my_ip_addr);
			VideoConfig.instance.hostIP = eti_my_ip_addr.getText().toString().trim();

			ComParamSet(false,true,false);

			EditText eti_my_gate =  findViewById(R.id.my_gate_addr);
			VideoConfig.instance.gateIP = eti_my_gate.getText().toString().trim();

			EditText eti_my_mask = findViewById(R.id.my_netmask_addr);
			VideoConfig.instance.maskIP = eti_my_mask.getText().toString().trim();
		}

		//推流分辨率 查看是否使用预设
		//是否使用预设分辨率
		CheckBox cbPrefernce = findViewById( R.id.checkUsePrefence);
		if(cbPrefernce.isChecked() )
		{
			VideoConfig.instance.SetResolutionIndex( resolutionSelector.getSelectedItemPosition() );
		}
		else//不使用
		{
			VideoConfig.instance.SetResolutionIndex(-1);

			EditText eti_my_video_w =  findViewById(R.id.custum_wideo_w);
			String ss = eti_my_video_w.getText().toString().trim();
			VideoConfig.instance.SetVideoWidth( Integer.parseInt( eti_my_video_w.getText().toString().trim() ) );

			EditText eti_my_video_h =  findViewById(R.id.custum_wideo_h);
			VideoConfig.instance.SetVideoHeight( Integer.parseInt( eti_my_video_h.getText().toString().trim() ) );
		}

		//is_hardware_encoder已经实时更改了
		if(VideoConfig.instance.is_hardware_encoder)
		{

		}
		else//软编码
			{
				//软编码配置
				VideoConfig.instance.sw_video_encoder_profile = swVideoEncoderProfileSelector.getSelectedItemPosition()+1;

				//软编码关键帧数
				VideoConfig.instance.sw_video_encoder_speed = 6- swVideoEncoderSpeedSelector.getSelectedItemPosition();
			}

		//帧率
		EditText eti = findViewById(R.id.push_rate);
		String strFPS = eti.getText().toString().trim();
		VideoConfig.instance.SetFPS( Integer.parseInt( strFPS ) );

		//保存--推流地址
		EditText cam_front_url = (EditText) findViewById(R.id.cam1_url_edit);
		EditText cam_back_url = (EditText) findViewById(R.id.cam2_url_edit);

		VideoConfig.instance.url1 = cam_front_url.getText().toString().trim();
		VideoConfig.instance.url2 = cam_back_url.getText().toString().trim();

		EditText eti_serverip= findViewById(R.id.server_ip);
		VideoConfig.instance.destHost = eti_serverip.getText( ).toString().trim();

		EditText eti_server_port= findViewById(R.id.server_port);
		String strPort = eti_server_port.getText().toString().trim();
		VideoConfig.instance.SetAppPort( Integer.parseInt( strPort ) );

		EditText eti_config_serverip= findViewById(R.id.config_server_ip);
		VideoConfig.instance.configHost = eti_config_serverip.getText( ).toString().trim();

		EditText eti_config_server_port= findViewById(R.id.config_server_port);
		String streti_config_serveripPort = eti_config_server_port.getText().toString().trim();
		VideoConfig.instance.SetConfigPort( Integer.parseInt( streti_config_serveripPort ) );

		EditText eti_userID = findViewById(R.id.id_userid);
		VideoConfig.instance.userID = eti_userID.getText().toString().trim();
		ComParamSet(false,false,true);

		boolean is_applyok = true;
		if(VideoConfig.instance.GetResolutionIndex() == -1)//查看自定义的配置是否合法 不合法不推。
		{
			Camera front_camera = GetCameraObj(FRONT);
			if ( front_camera != null && mSurfaceHolderFront != null )
			{
				front_camera.stopPreview();
				is_applyok = initCamera(FRONT, mSurfaceHolderFront);
			}

			Camera back_camera = GetCameraObj(BACK);
			if ( back_camera != null && mSurfaceHolderBack != null )
			{
				back_camera.stopPreview();
				if( is_applyok )
					is_applyok = initCamera(BACK, mSurfaceHolderBack);
			}

			//当两个摄像头都不存在 并且不是预设分辨率时 视为失败。不要问为什么。和小林商量的时候确定的
			if( front_camera == null && back_camera == null && VideoConfig.instance.GetResolutionIndex() == -1)
				is_applyok = false;

			if( is_applyok == false)
			{
				RestoreConfigAndUpdateVideoUI();
				Toast.makeText(getApplicationContext(), "无效的视频配置，已恢复原状!", Toast.LENGTH_SHORT).show();
				Log.e("asdfasdf", "无效的视频配置，已恢复原状");
				//	return false;
			}
		}

		return true;
	}


    private void outputInfo(String strTxt) {
        TextView et = (TextView) findViewById(R.id.txtlog);
        String str_conten = et.getText().toString();
        if (et.getLineCount() >= 20)
            et.setText(strTxt);
        else {
            str_conten += "\r\n";
            str_conten += strTxt;
            et.setText(str_conten);
        }
    }

    public Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
				case 1://空循环检查IP地址是否已准备就绪就绪 才能开始推
					{
						if(getLocalIpAddress().equals(""))
						{
							mHandler.sendEmptyMessageDelayed(1, 5000);
							Toast.makeText(getApplicationContext(), "网络未就绪。等待", Toast.LENGTH_SHORT).show();
						}
						else
						{
							mHandler.sendEmptyMessageDelayed( 2, 2000);
						}
					}
					break;
				case 2:
					{
						//本地无目标服务器地址配置 先跟串口要
						if( VideoConfig.instance.destHost.equals("") || VideoConfig.instance.GetAppPort()==0)
						{
							send_com_data(0x3c);//跟串口要IP和端口 要到以后 如果合法 它会自己开始连接并心跳
						}else
							ServerStopAndReconnect();

						//连接配置服务器
						ConfigServerStopAndReconnect();

						mHandler.sendEmptyMessageDelayed(3, 2000);
					}
					break;
				case 3:
				{
					UpdateConfigToUI();//有可能拿到了新的应用服务器端口地址。所以更新UI

					libPublisher = new SmartPublisherJniV2();

					ComParamSet(true, true, true);//给串口发 MAC 本机IP 密码

					//监听本地局域网端口
					lis_server = new MyTCServer();
					lis_server.init();

					//开始推流
					UIClickStartPush();
				}
				break;
				case 0://应用服务器连接成功--将地址保存到串口
					{
						String strAIP = (String)msg.obj;
						Log.e("应用IP是:" , "ip:" + strAIP);
						SaveAppHostInfoToCom( strAIP );
						//Log.e("CameraPublishActivity", "应用服务器连接成功开始心跳");
						//mHandler.sendEmptyMessageDelayed(5, 10000);//开始心跳
					}
					break;
                case 10: {
                    //收到socket过来的消息
					int msg_len = msg.arg1;
					byte test_data[] = (byte[]) (msg.obj);

					if( (test_data[7] & 0xff) != 0x35)
					{
						String sock_data = ComPort.bytes2HexString(test_data, msg_len);
						outputInfo("收到网络数据:" + sock_data + "发往串口");
						//检查如果不是配置IP之类的东西 就往串口发

						//往串口发
						mComPort.SendData(test_data, msg_len);
					}
                }
                break;
				case 11:
					{
						//心跳调试
						outputInfo("发送心跳消息");
					}
					break;
				case 104:
					{
						//收到配置口过来的数据
						outputInfo("应用更改.");
						UpdateConfigToUI();
						UIClickStopPush();

						ConfigServerStopAndReconnect();
						ServerStopAndReconnect();

						boolean is_applyok = true;
						Camera front_camera = GetCameraObj(FRONT);
						if ( front_camera != null && mSurfaceHolderFront != null )
						{
							front_camera.stopPreview();
							is_applyok = initCamera(FRONT, mSurfaceHolderFront);
						}

						Camera back_camera = GetCameraObj(BACK);
						if ( back_camera != null && mSurfaceHolderBack != null )
						{
							back_camera.stopPreview();
							if( is_applyok )
								is_applyok = initCamera(BACK, mSurfaceHolderBack);
						}

						//当两个摄像头都不存在 并且是预设分辨率时 视为失败。不要问为什么。和小林商量的时候确定的
						if( front_camera == null && back_camera == null && VideoConfig.instance.GetResolutionIndex() == -1)
							is_applyok = false;

						Socket ssa = (Socket)msg.obj;
						if( is_applyok == false )
						{
							RestoreConfigAndUpdateVideoUI();
							Toast.makeText(getApplicationContext(), "无效的视频配置，已恢复原状!", Toast.LENGTH_SHORT).show();
							UIClickStartPush();

							try
							{
								if (ssa != null && ssa.isConnected()) {
									String s = "{\"result\":\"failed\"}";//将结果发回发送端
									OutputStream outputStream = ssa.getOutputStream();
									outputStream.write(s.getBytes(), 0, s.getBytes().length);
									outputStream.flush();
								}
							}catch (IOException e) {
								e.printStackTrace();
								Log.e("返回结果","失败");
							}
						}else{
								try
								{
									if (ssa != null && ssa.isConnected()) {
										String s = "{\"result\":\"ok\"}";//将结果发回发送端
										OutputStream outputStream = ssa.getOutputStream();
										outputStream.write(s.getBytes(), 0, s.getBytes().length);
										outputStream.flush();
									}
								}catch (IOException e)
								{
									e.printStackTrace();
									Log.e("返回结果","失败");
								}
								UIClickStartPush();
							}
					}
					break;
				case 110://收到更新命令
				{
					String jsonStr = (String) (msg.obj);
					try {
						JSONObject jsonObject = new JSONObject(jsonStr);
						String url = jsonObject.getString("url");

						int versionCode =0;
						if( jsonObject.has("versionCode"))
							versionCode= jsonObject.getInt("versionCode");

						Log.e("收到更新命令", "url" + url + " 当前版本:" + APKVersionCodeUtils.getVersionCode(getApplicationContext()));
						SilentInstall upobj = new SilentInstall(getApplicationContext());
						upobj.startUpdate( url );
					}
					catch (JSONException jse)
					{
						jse.printStackTrace();
					}
				}
				break;
                case 188: {//串口过来的消息
                    byte test_data[] = (byte[]) (msg.obj);
                    int data_len = msg.arg1;

					//处理从串口过来的消息。如果是心跳 不管。如果不是 转发给服务器
					String com_data = ComPort.bytes2HexString(test_data, data_len);
					//if( check_com_data( test_data, data_len ) == false )
					{
					//	Log.e("串口收到", "串口数据错误" + com_data);
					}

					outputInfo("com recv len:" + data_len + " data" + com_data);

					if(test_data[7] == (byte) 0x35)
					{
						//wawaji is alive
					}
					else if(test_data[7] == (byte) 0x42 )//串口过来的应用服务器IP地址和端口
					{
						//只要不是空 就重新开启sendThread
						int a = test_data[8] & 0xff;
						int b = test_data[9] & 0xff;
						int c = test_data[10] & 0xff;
						int d = test_data[11] & 0xff;

						int e = test_data[12] & 0xff;
						int f = test_data[13] & 0xff;
						int nPort =e*256 + f;

						String s_ip = String.format("%d.%d.%d.%d", a, b, c, d);
						outputInfo("收到串口应用IP地址" + s_ip + "端口" + nPort);
						Log.e("收到串口应用IP地址", s_ip + "端口" + nPort );

						if( s_ip.equals("0.0.0.0") == false && nPort != 0)
						{
							VideoConfig.instance.destHost = s_ip;
							VideoConfig.instance.SetAppPort(  nPort );

							//应用服务器IP
							EditText eti_serverip= findViewById(R.id.server_ip);
							eti_serverip.setText( VideoConfig.instance.destHost );

							//应用服务器端口
							EditText eti_server_port= findViewById(R.id.server_port);
							eti_server_port.setText( Integer.toString(VideoConfig.instance.GetAppPort() ));

							ServerStopAndReconnect();

							VideoConfig.instance.SaveConfig(getApplicationContext());
						} else {
								outputInfo("该地址非法。不连接。");
							}
					}
					else {
						if (sendThread != null) {
							sendThread.sendMsg(test_data);
							outputInfo("sending to server");
						}
					}
                    break;
                }

				case 190://插U盘
					{
						String UPath = (String) (msg.obj);
						// 获取sd卡的对应的存储目录
						//获取指定文件对应的输入流
						try {
							FileInputStream fis = new FileInputStream(UPath + "/config.txt");
							//将指定输入流包装成 BufferedReader
							BufferedReader br = new BufferedReader(new InputStreamReader(fis,"GBK"));

							StringBuilder sb = new StringBuilder("");
							String line = null;
							//循环读取文件内容
							while((line=br.readLine())!=null){
								sb.append(line);
							}

							//关闭资源
							br.close();
							Log.e("file content", sb.toString());

							try {
								JSONObject jsonOBJ = new JSONObject(sb.toString());
								if( jsonOBJ.has("wifiSSID"))
								{
									String wifiSSID = jsonOBJ.getString("wifiSSID");

									String wifiPassword = "";
									if(jsonOBJ.has("wifiPassword"))
										wifiPassword = jsonOBJ.getString("wifiPassword");

									//启用wifi
									if (!wifiManager.isWifiEnabled())
										wifiManager.setWifiEnabled(true);

									//连接特定的wifi
									int ntype = wifiPassword.equals("")?1:3;
									WifiAutoConnectManager.WifiCipherType ntr =  wifiPassword.equals("")?
											WifiAutoConnectManager.WifiCipherType.WIFICIPHER_NOPASS: WifiAutoConnectManager.WifiCipherType.WIFICIPHER_WPA;

                                    Log.e("连接wifi", "ssid" + wifiSSID + " pwd " + wifiPassword + "type" + ntype);
									//WifiUtil.createWifiInfo(wifiSSID, wifiPassword, ntype, wifiManager);

									wifiauto.connect(wifiSSID, wifiPassword, ntr);
								}

								//todo 解析并应用json
								boolean apply_ret = VideoConfig.instance.ApplyConfig(sb.toString(), null);

							} catch (Exception e) {
								e.printStackTrace();
								Log.e("u盘配置文件错误", "Json file Error." );
							}

						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					break;
            }
        }
    };



    void SwitchResolution(int position)
    {
		//分辨率配置
		//"960*720", "640*480","640*360", "352*288","320*240"
    	Log.i(TAG, "Current Resolution position: " + position);

		VideoConfig.instance.SetResolutionIndex( position );

		switch (position) {
			case 0: {
				VideoConfig.instance.SetVideoWidth( 960 );
				VideoConfig.instance.SetVideoHeight( 720 );
			}
			break;
			case 1:
				VideoConfig.instance.SetVideoWidth( 640 );
				VideoConfig.instance.SetVideoHeight( 480 );
				break;
			case 2:
				VideoConfig.instance.SetVideoWidth( 640 );
				VideoConfig.instance.SetVideoHeight( 360 );
				break;
			case 3:
				VideoConfig.instance.SetVideoWidth( 352 );
				VideoConfig.instance.SetVideoHeight( 288 );
				break;
			case 4:
				VideoConfig.instance.SetVideoWidth( 320 );
				VideoConfig.instance.SetVideoHeight( 240 );
				break;
			case 5:
				{
					VideoConfig.instance.SetVideoWidth(555);
					VideoConfig.instance.SetVideoHeight(555);
				}
				break;
			default:
				VideoConfig.instance.SetVideoWidth( 640 );
				VideoConfig.instance.SetVideoHeight( 360 );
		}

		boolean is_applyok = true;
		Camera front_camera = GetCameraObj(FRONT);
    	if ( front_camera != null && mSurfaceHolderFront != null )
    	{
    		front_camera.stopPreview();
			is_applyok  = initCamera(FRONT, mSurfaceHolderFront);
    	}
    	
    	Camera back_camera = GetCameraObj(BACK);
    	if ( back_camera != null && mSurfaceHolderBack != null )
    	{
    		back_camera.stopPreview();
			if( is_applyok != false)
				is_applyok = initCamera(BACK, mSurfaceHolderBack);
    	}

		//当两个摄像头都不存在 并且是预设分辨率时 视为失败。不要问为什么。和小林商量的时候确定的
		if( front_camera == null && back_camera == null && VideoConfig.instance.GetResolutionIndex() == -1)
			is_applyok = false;

		if( is_applyok == false )
		{
			Toast.makeText(getApplicationContext(), "错误的配置,回退到正确的配置", Toast.LENGTH_SHORT).show();

			RestoreConfigAndUpdateVideoUI();
		}
    }

    void  RestoreConfigAndUpdateVideoUI()
	{
		VideoConfig.instance.RestoreLastVideoSizeAndIndex(this);

		if( VideoConfig.instance.GetResolutionIndex() != -1 )
		{
            CheckBox cbPrefernce = findViewById( R.id.checkUsePrefence);
            cbPrefernce.setChecked(true);

			findViewById(R.id.resolutionSelctor).setVisibility(View.VISIBLE);
			findViewById(R.id.custum_w_tip).setVisibility(View.INVISIBLE);
			findViewById(R.id.custum_wideo_w).setVisibility(View.INVISIBLE);
			findViewById(R.id.custum_h_tip).setVisibility(View.INVISIBLE);
			findViewById(R.id.custum_wideo_h).setVisibility(View.INVISIBLE);
			resolutionSelector.setSelection( VideoConfig.instance.GetResolutionIndex() );
		}
		else {
				CheckBox cbPrefernce = findViewById( R.id.checkUsePrefence);
				cbPrefernce.setChecked(false);
				findViewById(R.id.resolutionSelctor).setVisibility(View.INVISIBLE);
				findViewById(R.id.custum_w_tip).setVisibility(View.VISIBLE);
				findViewById(R.id.custum_wideo_w).setVisibility(View.VISIBLE);
				EditText eti = (EditText)findViewById(R.id.custum_wideo_w);
				eti.setText( Integer.toString( VideoConfig.instance.GetVideoWidth() ) );

				findViewById(R.id.custum_h_tip).setVisibility(View.VISIBLE);
				findViewById(R.id.custum_wideo_h).setVisibility(View.VISIBLE);
				eti = (EditText)findViewById(R.id.custum_wideo_h);
				eti.setText( Integer.toString( VideoConfig.instance.GetVideoHeight() ) );
			}
	}

    //Configure recorder related function.
    
    void ConfigRecorderFuntion(String rec, long handle, boolean isNeedLocalRecorder)
    {
    	if ( libPublisher != null )
    	{
    		if ( isNeedLocalRecorder )
    		{
    			if ( rec != null && !rec.isEmpty() )
        		{
        			int ret = libPublisher.SmartPublisherCreateFileDirectory(rec);
            		if ( 0 == ret )
            		{
            			if ( 0 != libPublisher.SmartPublisherSetRecorderDirectory(handle, rec) )
            			{
            				Log.e(TAG, "Set recoder dir failed , path:" + rec);
            				return;
            			}
            			
            			if ( 0 != libPublisher.SmartPublisherSetRecorder(handle, 1) )
            			{
            				Log.e(TAG, "SmartPublisherSetRecoder failed.");
            				return;
            			}
            			
            			if ( 0 != libPublisher.SmartPublisherSetRecorderFileMaxSize(handle, 200) )
            			{
            				Log.e(TAG, "SmartPublisherSetRecoderFileMaxSize failed.");
            				return;
            			}
            		
            		}
            		else
            		{
            			Log.e(TAG, "Create recoder dir failed, path:" + rec);
            		}
        		}
    		}
    		else
    		{
    			if ( 0 != libPublisher.SmartPublisherSetRecorder(handle, 0) )
    			{
    				Log.e(TAG, "SmartPublisherSetRecoder failed.");
    				return;
    			}
    		}
    	}
    }
    
    void ConfigRecorderFuntion(boolean isNeedLocalRecorder)
    {
    	ConfigRecorderFuntion(VideoConfig.instance.recDirFront, publisherHandleFront, isNeedLocalRecorder);
    	ConfigRecorderFuntion(VideoConfig.instance.recDirBack, publisherHandleBack, isNeedLocalRecorder);
    }
    
    class ButtonRecorderMangerListener implements OnClickListener
    {
    	public  void onClick(View v)
    	{
    		if ( mCameraFront != null )
    		{
    			mCameraFront.stopPreview();
    			mCameraFront.release();
    			mCameraFront = null;
    		}
    		
    		if ( mCameraBack != null )
    		{
    			mCameraBack.stopPreview();
    			mCameraBack.release();
    			mCameraBack = null;
    		}
    		
    	    Intent intent = new Intent();
            intent.setClass(CameraPublishActivity.this, RecorderManager.class);
            intent.putExtra("RecoderDir", VideoConfig.instance.recDirFront);
            startActivity(intent);
    	}
    }
    
    class ButtonHardwareEncoderListener  implements OnClickListener
    {
    	public void onClick(View v)
    	{
    		VideoConfig.instance.is_hardware_encoder = !VideoConfig.instance.is_hardware_encoder;
    		
    		if ( VideoConfig.instance.is_hardware_encoder )
    		{
				btnHWencoder.setText("当前硬编码");
				//显示软编码选项
				findViewById(R.id.swVideoEncoderProfileSelector).setVisibility(View.INVISIBLE);
				findViewById(R.id.speed_tip).setVisibility(View.INVISIBLE);
				findViewById(R.id.sw_video_encoder_speed_selctor).setVisibility(View.INVISIBLE);
			}
    		else {
					btnHWencoder.setText("当前软编码");
					//显示软编码选项
					findViewById(R.id.swVideoEncoderProfileSelector).setVisibility(View.VISIBLE);
					findViewById(R.id.speed_tip).setVisibility(View.VISIBLE);
					findViewById(R.id.sw_video_encoder_speed_selctor).setVisibility(View.VISIBLE);
				}

    	}
    }

    void NotifyStreamResult(int CameraType, PushState nowPS)//当推流状态变化时，通知服务器端
	{
		if( CameraType == 0)
		{
			if( nowPS == pst_front)
				return;

			pst_front = nowPS;
		}
		else if( CameraType == 1)
		{
			if( nowPS == pst_back)
				return;

			pst_back = nowPS;
		}

		byte msg_content[] = new byte[22];
		msg_content[0] = (byte) 0xfe;
		msg_content[1] = (byte) (0);
		msg_content[2] = (byte) (0);
		msg_content[3] = (byte) ~msg_content[0];
		msg_content[4] = (byte) ~msg_content[1];
		msg_content[5] = (byte) ~msg_content[2];
		msg_content[6] = (byte) (msg_content.length);
		msg_content[7] = (byte) 0xa0;

		System.arraycopy(VideoConfig.instance.getMac().getBytes(), 0, msg_content, 8, VideoConfig.instance.getMac().getBytes().length);

		if(  CameraType == 0 && nowPS == PushState.FAILED )
		{
			msg_content[20] = 0x00;//
		}
		else if( CameraType == 0 && nowPS == PushState.OK  )
		{
			msg_content[20] = 0x01;//
		}
		else if( CameraType == 0 && nowPS == PushState.CLOSE  )
		{
			msg_content[20] = 0x02;//
		}
		else if( CameraType == 1 && nowPS == PushState.FAILED  )
		{
			msg_content[20] = 0x10;//
		}
		else if( CameraType == 1 && nowPS == PushState.OK  )
		{
			msg_content[20] = 0x11;//
		}
		else if( CameraType == 1 && nowPS == PushState.CLOSE  )
		{
			msg_content[20] = 0x12;//
		}

		int total_c = 0;
		for (int i = 6; i < msg_content.length - 1; i++) {
			total_c += (msg_content[i] & 0xff);
		}
		msg_content[msg_content.length - 1] = (byte) (total_c % 100);

		if(sendThread != null) sendThread.sendMsg(  msg_content );
	}

    class EventHandeV2 implements NTSmartEventCallbackV2
    {
    	 @Override
    	 public void onNTSmartEventCallbackV2(long handle, int id, long param1, long param2, String param3, String param4, Object param5){
    		 
    		 Log.d(TAG, "EventHandeV2: handle=" + handle + " id:" + id);
    		 
    		 switch (id) { 
                 case NTSmartEventID.EVENT_DANIULIVE_ERC_PUBLISHER_STARTED:
                	 txt = "开始。。";
                     break;
                 case NTSmartEventID.EVENT_DANIULIVE_ERC_PUBLISHER_CONNECTING:
                	 txt = "连接中。。";
                     break;
                 case NTSmartEventID.EVENT_DANIULIVE_ERC_PUBLISHER_CONNECTION_FAILED:
                	 txt = "连接失败。。";
					 if( handle == publisherHandleFront)
					 {
						 VideoConfig.instance.videoPushState_1 = false;
						 NotifyStreamResult(0,PushState.FAILED);
						 TextView tvFr = findViewById(R.id.cam1_url_tip);
						 if(tvFr != null) tvFr.setTextColor(Color.rgb(255, 0, 0));
						 getWindow().getDecorView().postInvalidate();
					 }
					 else if(handle == publisherHandleBack)
					 {
						 NotifyStreamResult(1, PushState.FAILED);
						 VideoConfig.instance.videoPushState_2 = false;
						 TextView tvFr = findViewById(R.id.cam2_url_tip);
						 if(tvFr != null) tvFr.setTextColor(Color.rgb(255, 0, 0));
						 getWindow().getDecorView().postInvalidate();
					 }
                     break;
                 case NTSmartEventID.EVENT_DANIULIVE_ERC_PUBLISHER_CONNECTED:
                	 txt = "连接成功。。";
					 if( handle == publisherHandleFront)
					 {
						 NotifyStreamResult(0, PushState.OK);
						 VideoConfig.instance.videoPushState_1 = true;
						 TextView tvFr = findViewById(R.id.cam1_url_tip);
						 if(tvFr != null) tvFr.setTextColor(Color.rgb(0, 255, 0));
						 getWindow().getDecorView().postInvalidate();
					 }
					 else if(handle == publisherHandleBack)
					 {
						 NotifyStreamResult(1, PushState.OK);
						 VideoConfig.instance.videoPushState_2 = true;
						 TextView tvFr = findViewById(R.id.cam2_url_tip);
						 if(tvFr != null) tvFr.setTextColor(Color.rgb(0, 255, 0));
						 getWindow().getDecorView().postInvalidate();
					 }
                     break;
                 case NTSmartEventID.EVENT_DANIULIVE_ERC_PUBLISHER_DISCONNECTED:
                	 txt = "连接断开。。";
					 if( handle == publisherHandleFront)
					 {
						 VideoConfig.instance.videoPushState_1 = false;
						 NotifyStreamResult(0,PushState.FAILED);
						 TextView tvFr = findViewById(R.id.cam1_url_tip);
						 if(tvFr != null) tvFr.setTextColor(Color.rgb(255, 0, 0));
						 getWindow().getDecorView().postInvalidate();
					 }
					 else if(handle == publisherHandleBack)
					 {
						 NotifyStreamResult(1, PushState.FAILED);
						 VideoConfig.instance.videoPushState_2 = false;
						 TextView tvFr = findViewById(R.id.cam2_url_tip);
						 if(tvFr != null) tvFr.setTextColor(Color.rgb(255, 0, 0));
						 getWindow().getDecorView().postInvalidate();
					 }
                     break;
                 case NTSmartEventID.EVENT_DANIULIVE_ERC_PUBLISHER_STOP:
                	 txt =  "关闭。。";
					 if( handle == publisherHandleFront)
					 {
						 NotifyStreamResult(0, PushState.CLOSE);
						 VideoConfig.instance.videoPushState_1 = false;
						 TextView tvFr = findViewById(R.id.cam1_url_tip);
						 if(tvFr != null) tvFr.setTextColor(Color.rgb(255, 0, 0));
						 getWindow().getDecorView().postInvalidate();
					 }
					 else if(handle == publisherHandleBack)
					 {
						 NotifyStreamResult(1, PushState.CLOSE);
						 VideoConfig.instance.videoPushState_2 = false;
						 TextView tvFr = findViewById(R.id.cam2_url_tip);
						 if(tvFr != null) tvFr.setTextColor(Color.rgb(255, 0, 0));
						 getWindow().getDecorView().postInvalidate();
					 }
                     break;
                 case NTSmartEventID.EVENT_DANIULIVE_ERC_PUBLISHER_RECORDER_START_NEW_FILE:
                	 Log.i(TAG, "开始一个新的录像文件 : " + param3);
                	 txt = "开始一个新的录像文件。。";
                     break;
                 case NTSmartEventID.EVENT_DANIULIVE_ERC_PUBLISHER_ONE_RECORDER_FILE_FINISHED:
                	 Log.i(TAG, "已生成一个录像文件 : " + param3);
                	 txt = "已生成一个录像文件。。";
                     break;
                     
                 case NTSmartEventID.EVENT_DANIULIVE_ERC_PUBLISHER_SEND_DELAY:
                	 Log.i(TAG, "发送时延: " + param1 + " 帧数:" + param2);
                	 txt = "收到发送时延..";
                	 break;
                	 
                 case NTSmartEventID.EVENT_DANIULIVE_ERC_PUBLISHER_CAPTURE_IMAGE:
                	 Log.i(TAG, "快照: " + param1 + " 路径：" + param3);
                	 
                	 if(param1 == 0)
                	 {
                		 txt = "截取快照成功。."; 
                	 }
                	 else
                	 {
                		 txt = "截取快照失败。."; 
                	 }
                	 break;
             }
             
             String str = "当前回调状态：" + txt;

             Log.d(TAG, str);
         }
    }

    private void ConfigControlEnable(boolean isEnable)
    {
    	btnRecoderMgr.setEnabled(isEnable);
		btnHWencoder.setEnabled(isEnable);

		findViewById(R.id.id_my_name1).setEnabled(isEnable);
		findViewById(R.id.checkBoxAutoGet).setEnabled(isEnable);//dhcp
		if(isEnable)
		{
			if(VideoConfig.instance.using_dhcp == false)
			{
				findViewById(R.id.my_ip_addr).setEnabled(true);
			}else {
				findViewById(R.id.my_ip_addr).setEnabled(false);
			}
		}else
			{
				findViewById(R.id.my_ip_addr).setEnabled(false);
			}

		findViewById(R.id.my_gate_addr).setEnabled(isEnable);
		findViewById(R.id.my_netmask_addr).setEnabled(isEnable);

		findViewById(R.id.checkUsePrefence).setEnabled(isEnable);
		findViewById(R.id.resolutionSelctor).setEnabled(isEnable);
		findViewById(R.id.custum_wideo_w).setEnabled(isEnable);
		findViewById(R.id.custum_wideo_h).setEnabled(isEnable);

		findViewById(R.id.swVideoEncoderProfileSelector).setEnabled(isEnable);
		findViewById(R.id.sw_video_encoder_speed_selctor).setEnabled(isEnable);
		findViewById(R.id.push_rate).setEnabled(isEnable);
		findViewById(R.id.cam1_url_edit).setEnabled(isEnable);
		findViewById(R.id.cam2_url_edit).setEnabled(isEnable);

		findViewById(R.id.server_ip).setEnabled(isEnable);
		findViewById(R.id.server_port).setEnabled(isEnable);

		findViewById(R.id.config_server_ip).setEnabled(isEnable);
		findViewById(R.id.config_server_port).setEnabled(isEnable);

		findViewById(R.id.recoder_selctor).setEnabled(isEnable);
    }

    private void SetConfig(long handle)
    {
    	if ( libPublisher == null )
    		return;
    	
    	if ( handle == 0 )
    		return;

		//非镜像
		libPublisher.SmartPublisherSetMirror(handle, 0);

		//静音
		libPublisher.SmartPublisherSetMute(handle, 1);

		//设置码率
		if ( VideoConfig.instance.is_hardware_encoder )
		{
			int hwHWKbps = setHardwareEncoderKbps(VideoConfig.instance.GetVideoWidth(), VideoConfig.instance.GetVideoHeight());

			Log.i(TAG, "hwHWKbps: " + hwHWKbps);

			int isSupportHWEncoder = libPublisher.SetSmartPublisherVideoHWEncoder(handle, hwHWKbps);

			if (isSupportHWEncoder == 0) {
				Log.i(TAG, "Great, it supports hardware encoder!");
			}
		}

		//硬编码
		if ( VideoConfig.instance.is_hardware_encoder )
		{
			libPublisher.SmartPublisherSetFPS(handle, 20);
		}

		libPublisher.SetSmartPublisherEventCallbackV2(handle, new EventHandeV2());

		//音频-set AAC encoder
		libPublisher.SmartPublisherSetAudioCodecType(handle, 1);
		//音频-噪音抑制
		libPublisher.SmartPublisherSetNoiseSuppression(handle, 0);
		//音频编码
		libPublisher.SmartPublisherSetAGC(handle, 0);

		libPublisher.SmartPublisherSetSWVideoEncoderProfile(handle, VideoConfig.instance.sw_video_encoder_profile);
		libPublisher.SmartPublisherSetSWVideoEncoderSpeed(handle,  VideoConfig.instance.sw_video_encoder_speed);

		libPublisher.SmartPublisherSaveImageFlag(handle, 0);
		libPublisher.SmartPublisherSetClippingMode(handle, 0);
    }

    private void InitAndSetConfig()
    {
		//Camera front_cam = GetCameraObj(FRONT);
		//if(front_cam != null)
		{
			publisherHandleFront = libPublisher.SmartPublisherOpen(myContext, /*audio_opt*/0, /*video_opt*/1,
					VideoConfig.instance.GetVideoWidth(), VideoConfig.instance.GetVideoHeight());

			if ( publisherHandleFront != 0 )
			{
				SetConfig(publisherHandleFront);
				Log.e("前置摄像头", "ID"+publisherHandleFront);
			}
		}

		//Camera back_cam = GetCameraObj(BACK);
		//if( back_cam != null)
		{
			publisherHandleBack = libPublisher.SmartPublisherOpen(myContext, /*audio_opt*/0, /*video_opt*/1,
					VideoConfig.instance.GetVideoWidth(), VideoConfig.instance.GetVideoHeight());

			if ( publisherHandleBack != 0 )
			{
				SetConfig(publisherHandleBack);
				Log.e("后置摄像头", "ID"+publisherHandleBack);
			}
		}
    }

	void UIClickStartPush() {
		if (getLocalIpAddress().equals(""))
			return;

		if (libPublisher == null)
			return;

		outputInfo("开推.");

		Log.i(TAG, "onClick start push..");

		VideoConfig.instance.SaveConfig(this);

		isPushing = true;

		VideoConfig.instance.videoPushState_1 = false;
		VideoConfig.instance.videoPushState_2 = false;

		Camera front_cam = GetCameraObj(FRONT);
		if (front_cam != null)
		{
			if (!isRecording) {
				publisherHandleFront = libPublisher.SmartPublisherOpen(myContext, /*audio_opt*/0, /*video_opt*/1, VideoConfig.instance.GetVideoWidth(), VideoConfig.instance.GetVideoHeight());
				if (publisherHandleFront != 0) {
					SetConfig(publisherHandleFront);
					Log.e("前置摄像头", "ID" + publisherHandleFront);
				}
			}

			if (libPublisher.SmartPublisherSetURL(publisherHandleFront, VideoConfig.instance.url1) != 0) {
				Log.e(TAG, "Failed to set publish stream URL..");
				outputInfo("前置推流地址应用失败.");
			}

			int startRet = libPublisher.SmartPublisherStartPublisher(publisherHandleFront);
			if (startRet != 0) {
				isPushing = false;
				Log.e(TAG, "Failed to start push stream..");
				TextView tvFr = findViewById(R.id.cam1_url_tip);
				if(tvFr != null) tvFr.setTextColor(Color.rgb(255, 0, 0));
			}
			else
				{
					Log.e(TAG,VideoConfig.instance.url1 + "开始推流" );
				}
		}

		Camera back_cam = GetCameraObj(BACK);
		if (back_cam != null)
	 	{
			if (!isRecording) {
				publisherHandleBack = libPublisher.SmartPublisherOpen(myContext, /*audio_opt*/0, /*video_opt*/1,
						VideoConfig.instance.GetVideoWidth(), VideoConfig.instance.GetVideoHeight());

				if (publisherHandleBack != 0) {
					SetConfig(publisherHandleBack);
					Log.e("后置摄像头", "ID" + publisherHandleBack);
				}
			}

			if (libPublisher.SmartPublisherSetURL(publisherHandleBack, VideoConfig.instance.url2) != 0) {
				Log.e(TAG, "Failed to set publish stream URL..");
			}
			int startRet = libPublisher.SmartPublisherStartPublisher(publisherHandleBack);
			if (startRet != 0) {
				isPushing = false;
				Log.e(TAG, "Failed to start push stream back..");
				TextView tvFr = findViewById(R.id.cam2_url_tip);
				if(tvFr != null) tvFr.setTextColor(Color.rgb(255, 0, 0));
			}
		}

		if (!isRecording && isPushing == true ) {
			ConfigControlEnable(false);
			btnStartPush.setText(" 停止推送 ");
		}else if(isPushing == false) {
				ConfigControlEnable(true);
				btnStartPush.setText(" 推送");
				outputInfo("推送失败。检查推流URL,或摄像头是否已插好");
			}
	}

	void UIClickStopPush()
	{
		outputInfo("停推.");
		stopPush();

		if ( !isRecording )
		{
			ConfigControlEnable(true);
		}

		btnStartPush.setText(" 推送");
		isPushing = false;

		return;
	}
    
    class ButtonStartPushListener implements OnClickListener {
		public void onClick(View v) {
			if (isPushing) {
				UIClickStopPush();
				return;
			} else {

				boolean previewOK = SaveConfigFromUI();

				//检查是否需要重连服务器
				ServerStopAndReconnect();

				//检查是否需要连接配置服务器
				ConfigServerStopAndReconnect();

				if (previewOK) {
					UIClickStartPush();
				}
			}
		}
	};
    
    class ButtonStartRecorderListener implements OnClickListener
    {
        public void onClick(View v)
        {
        	if ( isRecording )
        	{
        		stopRecorder();
        		
        		if ( !isPushing )
        		{
        			ConfigControlEnable(true);
        		}
        		
        		btnStartRecorder.setText(" 录像");
        		isRecording = false;
        		return;
        	}
        	
        	
        	Log.i(TAG, "onClick start recorder..");   
        	
        	if( libPublisher == null )
        		return;
        	        	
        	isRecording = true;
        	
        	if ( !isPushing )
        	{
        		InitAndSetConfig();
        	}
        	
        	ConfigRecorderFuntion(true);
        	
            int startRet = libPublisher.SmartPublisherStartRecorder(publisherHandleFront);
            if( startRet != 0 )
            {
            	isRecording = false;
            	
            	Log.e(TAG, "Failed to start recorder.");
            	return;
            }

            startRet = libPublisher.SmartPublisherStartRecorder(publisherHandleBack);
            if( startRet != 0)
            {
                isPushing = false;
                     	
                Log.e(TAG, "Failed to start recorder stream.. back");
                return;
            }

			if ( !isPushing )
			{
				ConfigControlEnable(false);
			}

			btnStartRecorder.setText(" 停止录像");
        }
    };

    private void stopPush()
    {
    	if ( !isRecording )
    	{
    		if( audioRecord_ != null )
 	        {
 				Log.i(TAG, "stopPush, call audioRecord_.StopRecording.."); 
 				
 				audioRecord_.Stop();
 				
 				if ( audioRecordCallback_ != null )
 				{
 					audioRecord_.RemoveCallback(audioRecordCallback_);
 					audioRecordCallback_ = null;
 				}
 				
 				audioRecord_ = null;
 	        }
    	}
    	
    	 if ( libPublisher != null && publisherHandleFront != 0 )
    	 {
    		libPublisher.SmartPublisherStopPublisher(publisherHandleFront);
    	 }
    		 
    	 if ( !isRecording )
    	 {
    		 if ( publisherHandleFront != 0 )
    	 	 {
    	 		if ( libPublisher != null )
    	 	    {
    	 	    	libPublisher.SmartPublisherClose(publisherHandleFront);
    	 	    	publisherHandleFront = 0;
    	 	    }
    	 	}
    	 }
    	 
    	 
    	 if ( libPublisher != null && publisherHandleBack != 0 )
    	 {
    		libPublisher.SmartPublisherStopPublisher(publisherHandleBack);
    	 }
    	 
    	 if ( !isRecording )
    	 {
    		 if ( publisherHandleBack != 0 )
    	 	 {
    	 		if ( libPublisher != null )
    	 	    {
    	 	    	libPublisher.SmartPublisherClose(publisherHandleBack);
    	 	    	publisherHandleBack = 0;
    	 	    }
    	 	}
    	 }
    	 
    }
    
    private void stopRecorder()
    {
    	if ( !isPushing )
    	{
    		if( audioRecord_ != null )
 	        {
 				Log.i(TAG, "stopRecorder, call audioRecord_.StopRecording.."); 
 	        	
 				audioRecord_.Stop();
 				
 				if ( audioRecordCallback_ != null )
 				{
 					audioRecord_.RemoveCallback(audioRecordCallback_);
 					audioRecordCallback_ = null;
 				}
 				
 				audioRecord_ = null;
 	        }
    	}
    	
    	if ( libPublisher != null && publisherHandleFront != 0)
    	{
    	   libPublisher.SmartPublisherStopRecorder(publisherHandleFront);
    	}
    	
    	if ( !isPushing )
   	 	{
   		 	if ( publisherHandleFront != 0 )
   		 	{
   		 		if ( libPublisher != null )
   		 		{
   		 			libPublisher.SmartPublisherClose(publisherHandleFront);
   		 			publisherHandleFront = 0;
   		 		}
   		 	}
   	 	}

   	 if ( libPublisher != null && publisherHandleBack != 0)
   	 {
   		libPublisher.SmartPublisherStopRecorder(publisherHandleBack);
   	 }

   	 if ( !isPushing )
   	 {
   		 if ( publisherHandleBack != 0 )
   	 	 {
   	 		if ( libPublisher != null )
   	 	    {
   	 	    	libPublisher.SmartPublisherClose(publisherHandleBack);
   	 	    publisherHandleBack = 0;
   	 	    }
   	 	}
   	 }    	
    }


	private void SetCameraFPS(Camera.Parameters parameters)
	{
		if ( parameters == null )
			return;
		
		int[] findRange = null;
		
		int defFPS = 20*1000;
		
		List<int[]> fpsList = parameters.getSupportedPreviewFpsRange();
		if ( fpsList != null && fpsList.size() > 0 )
		{
			for ( int i = 0; i < fpsList.size(); ++i )
			{
				int[] range = fpsList.get(i);
				if ( range != null 
						&& Camera.Parameters.PREVIEW_FPS_MIN_INDEX <  range.length 
						 && Camera.Parameters.PREVIEW_FPS_MAX_INDEX < range.length )
				{
					Log.i(TAG, "Camera index:" + i + " support min fps:" + range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX]);
					
					Log.i(TAG, "Camera index:" + i + " support max fps:" + range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);	
						
					if ( findRange == null )
					{
						if ( defFPS <= range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX] )
						{
							findRange = range;
							
							Log.i(TAG, "Camera found appropriate fps, min fps:" + range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX]
									+ " ,max fps:" + range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
						}
					}
				}
			}
		}
		
		if ( findRange != null  )
		{
			parameters.setPreviewFpsRange(findRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX], findRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
		}
	}

	/*it will call when surfaceChanged*/
	private boolean initCamera(int camera_type, SurfaceHolder holder)
	{  
		Log.i(TAG, "initCa11mera..");
		
		Camera camera = GetCameraObj(camera_type);
		if ( camera == null )
		{
			Log.e(TAG, "initCa111mera camera is null, type=" + camera_type);
			//return false;
		}
		
		int cameraIndex = GetCameraIndex(camera_type);
		if ( -1 == cameraIndex )
		{
			Log.e(TAG, "initCam11era cameraIndex is -1, type=" + camera_type);
			//return false;
		}
		
		if ( FRONT == camera_type  && camera != null)
		{
			if ( mPreviewRunningFront )
			{
				camera.stopPreview();
			}
		}
		else if ( BACK == camera_type && camera != null )
		{
			if ( mPreviewRunningBack )
			{
				camera.stopPreview();
			}
		}
			
		Camera.Parameters parameters ;
		try {
			parameters = camera.getParameters();
			//List<Size> ss = parameters.getSupportedPictureSizes();
			//VideoConfig.instance.videoSizes = ss;

			//for(int i = 0; i< ss.size(); i++)
			//{
			//	Log.e("sadfasdf","camTYpe:" + camera_type + "w:" + ss.get(i).width + " h:" + ss.get(i).height);
			//}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

		parameters.setPreviewSize(VideoConfig.instance.GetVideoWidth(), VideoConfig.instance.GetVideoHeight());
		parameters.setPictureFormat(PixelFormat.JPEG); 
		parameters.setPreviewFormat(PixelFormat.YCbCr_420_SP); 
		
		SetCameraFPS(parameters);

		if(camera != null) camera.setDisplayOrientation (90);

		Log.e("Cmeraaaa", "apply w:" + VideoConfig.instance.GetVideoWidth() + "h "+ VideoConfig.instance.GetVideoHeight());
		try
		{
			if(camera != null) camera.setParameters(parameters);
		}
		catch (Exception ex)
		{
			Log.e("*******","Apply Camera Config failed.");
			return false;
		}

		int bufferSize = (((VideoConfig.instance.GetVideoWidth()|0xf)+1) * VideoConfig.instance.GetVideoHeight() * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat())) / 8;

		if(camera != null) camera.addCallbackBuffer(new byte[bufferSize]);

		if(camera != null) camera.setPreviewCallbackWithBuffer(new NT_SP_CameraPreviewCallback(camera_type));
        
		try
        {
			if(camera != null) camera.setPreviewDisplay(holder);
        } catch (Exception ex) {
        	// TODO Auto-generated catch block 
        	if(null != camera)
        	{  
        		camera.release();  
        		camera = null;
        		SetCameraObj(camera_type, null);
            }
        	ex.printStackTrace();

			return false;
        }  
        
		if ( camera != null )
		{
			camera.startPreview();  
		}
        
        if ( FRONT == camera_type  && camera!= null)
        {
        	camera.autoFocus(myAutoFocusCallbackFront);
        	mPreviewRunningFront = true;  	
        }
        else if ( BACK == camera_type && camera!= null)
        {
        	camera.autoFocus(myAutoFocusCallbackBack);
        	mPreviewRunningBack = true; 
        }

        return true;
	}
	
	int GetCameraIndex(int type)
	{
		if ( FRONT == type )
		{
			return curFrontCameraIndex;
		}
		else if ( BACK == type )
		{
			return curBackCameraIndex;
		}
		else
		{
			Log.i(TAG, "GetCameraIndex type error, type=" + type);
			return -1;
		}
	}
	
	Camera GetCameraObj(int type)
	{
		if ( FRONT == type )
		{
			return mCameraFront;
		}
		else if ( BACK == type )
		{
			return mCameraBack;
		}
		else
		{
			Log.i(TAG, "GetCameraObj type error, type=" + type);
			return null;
		}
	}
	
	void SetCameraObj(int type, Camera c)
	{
		if ( FRONT == type )
		{
			mCameraFront = c;
		}
		else if ( BACK == type )
		{
			mCameraBack = c;
		}
		else
		{
			Log.i(TAG, "SetCameraObj type error, type=" + type);
		}
	}
	
	class NT_SP_SurfaceHolderCallback implements Callback
	{
		private int type_ = 0;
		
		public NT_SP_SurfaceHolderCallback(int type)
		{
			type_ = type;
		}
		
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			
			Log.i(TAG, "surfaceCreated..type_=" + type_);
			
			if ( type_ != FRONT && type_ != BACK )
			{
				Log.e(TAG, "surfaceCreated type error, type=" + type_);
				return;
			}
			
			try {
				
				if ( type_ == FRONT )
				{
					int cammeraIndex = findFrontCamera();
					if( cammeraIndex == -1 )
		            {
		            	Log.e(TAG, "surfaceCreated, There is no front camera!!");
		            	return;
		            }  
				}
				else if ( type_ == BACK )
				{
					int cammeraIndex = findBackCamera();
					if ( -1 == cammeraIndex )
					{
						Log.e(TAG, "surfaceCreated, there is no back camera");
						
						return;
					}
				}
				
				if ( GetCameraObj(type_) == null )
				{
					Camera c = openCamera(type_); 
					SetCameraObj(type_, c);
				}
		        
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
		}
		
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			Log.e(TAG, "surfaceChanged..");
			
			if ( type_ != FRONT && type_ != BACK )
				return;
			
			initCamera(type_, holder);
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// TODO Auto-generated method stub
			Log.i(TAG, "Surface Destroyed"); 
		}
	}

	class NT_SP_CameraPreviewCallback implements PreviewCallback
	{
		private int type_ = 0;
		
		private int frameCount_ = 0;
		
		public NT_SP_CameraPreviewCallback(int type)
		{
			type_ = type;
		}
		
		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
			
			frameCount_++;
			if ( frameCount_ % 5000 == 0 )
			{
				//Log.i("OnPre", "gc+");
				System.gc();
				//Log.i("OnPre", "gc-");
			}
		
			if (data == null) {
				Parameters params = camera.getParameters();
				Size size = params.getPreviewSize();
				int bufferSize = (((size.width|0x1f)+1) * size.height * ImageFormat.getBitsPerPixel(params.getPreviewFormat())) / 8;
				camera.addCallbackBuffer(new byte[bufferSize]);
			} 
			else 
			{
				if(  isPushing || isRecording )
				{
					if ( FRONT == type_ && publisherHandleFront != 0 )
					{
						libPublisher.SmartPublisherOnCaptureVideoData(publisherHandleFront, data, data.length, BACK, VideoConfig.instance.currentOrigentation);
					}
					
					if ( BACK == type_ && publisherHandleBack != 0 )
					{
						libPublisher.SmartPublisherOnCaptureVideoData(publisherHandleBack, data, data.length, BACK, VideoConfig.instance.currentOrigentation);
					}
				}
				
				camera.addCallbackBuffer(data);
			}
		} 
	}
	
    @SuppressLint("NewApi")
    private Camera openCamera(int type){
        
    	int frontIndex =-1;
        int backIndex = -1;
        int cameraCount = Camera.getNumberOfCameras();
        Log.i(TAG, "cameraCount: " + cameraCount);
        
        CameraInfo info = new CameraInfo();
        for(int cameraIndex = 0; cameraIndex<cameraCount; cameraIndex++){
            Camera.getCameraInfo(cameraIndex, info);
            
            if(info.facing == CameraInfo.CAMERA_FACING_FRONT){
                frontIndex = cameraIndex;
            }else if(info.facing == CameraInfo.CAMERA_FACING_BACK){
                backIndex = cameraIndex;
            }
        }
        
        if( type == FRONT && frontIndex != -1) {
        	curFrontCameraIndex = frontIndex;
            return Camera.open(frontIndex);
        }else if(type == BACK && backIndex != -1) {
        	curBackCameraIndex = backIndex;
            return Camera.open(backIndex);
        }
        
        return null;
    }

	
	//Check if it has front camera
	private int findFrontCamera(){	
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras(); 
        
        for ( int camIdx = 0; camIdx < cameraCount;camIdx++ ) {
            Camera.getCameraInfo( camIdx, cameraInfo );
            if ( cameraInfo.facing ==Camera.CameraInfo.CAMERA_FACING_FRONT ) {
               return camIdx;
            }
        }
    	return -1;
    }
	
	//Check if it has back camera
    private int findBackCamera(){
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
              
        for ( int camIdx = 0; camIdx < cameraCount;camIdx++ ) {
            Camera.getCameraInfo( camIdx, cameraInfo );
            if ( cameraInfo.facing ==Camera.CameraInfo.CAMERA_FACING_BACK ) {
               return camIdx;
            }
        }
    	return -1;
    }
    
    private int setHardwareEncoderKbps(int width, int height)
    {
    	int hwEncoderKpbs = 560;
    	
    	switch(width) {
        case 176:
        	hwEncoderKpbs = 220;
            break;
        case 320:
        	hwEncoderKpbs = 380;
            break;
        case 640:
        	hwEncoderKpbs = 560;
            break;
        case 1280:
        	hwEncoderKpbs = 1200;
            break;
        default:
        	hwEncoderKpbs = 1000;
    	}
    	
    	return hwEncoderKpbs;
    }
    
	/**
     * 根据目录创建文件夹
     * @param context
     * @param cacheDir
     * @return
     */
    public static File getOwnCacheDirectory(Context context, String cacheDir) {
        File appCacheDir = null;
        //判断sd卡正常挂载并且拥有权限的时候创建文件
        if ( Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) && hasExternalStoragePermission(context)) {
            appCacheDir = new File(Environment.getExternalStorageDirectory(), cacheDir);
        	Log.i(TAG, "appCacheDir: " + appCacheDir);
        }
        if (appCacheDir == null || !appCacheDir.exists() && !appCacheDir.mkdirs()) {
            appCacheDir = context.getCacheDir();
        }
        return appCacheDir;
    }

    /**
     * 检查是否有权限
     * @param context
     * @return
     */
    private static boolean hasExternalStoragePermission(Context context) {
        int perm = context.checkCallingOrSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE");
        return perm == 0;
    }


   
}