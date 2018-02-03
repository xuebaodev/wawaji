/*
 * SmartPlayer.java
 * SmartPlayer
 * 
 * Github: https://github.com/daniulive/SmarterStreaming
 * WebSite: http://www.daniulive.com
 *
 * Created by DaniuLive on 2015/09/26.
 * Copyright © 2014~2017 DaniuLive. All rights reserved.
 */

package com.daniulive.smartplayer;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;  
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
  
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.eventhandle.SmartEventCallback;
import com.videoengine.*;

import org.json.JSONArray;
import org.json.JSONObject;


public class SmartPlayer extends Activity {

	public static String switchURL = "rtmp://your video stream server url";

    private SurfaceView sSurfaceView = null;   
	
	private long playerHandle = 0;
	
	private static final int PORTRAIT = 1;
	private static final int LANDSCAPE = 2;
	private static final String TAG = "SmartPlayer";
	
	private SmartPlayerJni libPlayer = null;
	
	private int currentOrigentation = PORTRAIT;
	private boolean isPlaybackViewStarted = false;

	private boolean isHardwareDecoder = true;
	private int playBuffer = 0;
	private boolean isFastStartup = true;
	private boolean switchUrlFlag = false;

	//Button btnPopInputText;
	private Vibrator vibrator;
    
    private Context myContext;

	boolean b_start_new_game = false;

	static {  
		System.loadLibrary("SmartPlayer");
	}

	/* Generate basic layout */
	private void inflateLayout(int orientation) {
		RelativeLayout v_room = (RelativeLayout)findViewById(R.id.grid_video_view_container1);
		v_room.addView(sSurfaceView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
	}

	public void onClickClose(View view) {
		libPlayer.SmartPlayerClose(playerHandle);
		playerHandle = 0;
		finish();
	}

	class EventHande implements SmartEventCallback
	{
		@Override
		public void onCallback(int code, long param1, long param2, String param3, String param4, Object param5){
			switch (code) {
				case EVENTID.EVENT_DANIULIVE_ERC_PLAYER_STARTED:
					Log.i(TAG, "开始。。");
					break;
				case EVENTID.EVENT_DANIULIVE_ERC_PLAYER_CONNECTING:
					Log.i(TAG, "连接中。。");
					break;
				case EVENTID.EVENT_DANIULIVE_ERC_PLAYER_CONNECTION_FAILED:
					Log.i(TAG, "连接失败。。");
					break;
				case EVENTID.EVENT_DANIULIVE_ERC_PLAYER_CONNECTED:
					Log.i(TAG, "连接成功。。");
					break;
				case EVENTID.EVENT_DANIULIVE_ERC_PLAYER_DISCONNECTED:
					Log.i(TAG, "连接断开。。");
					break;
				case EVENTID.EVENT_DANIULIVE_ERC_PLAYER_STOP:
					Log.i(TAG, "关闭。。");
					break;
				case EVENTID.EVENT_DANIULIVE_ERC_PLAYER_RESOLUTION_INFO:
					Log.i(TAG, "分辨率信息: width: " + param1 + ", height: " + param2);
					break;
				case EVENTID.EVENT_DANIULIVE_ERC_PLAYER_NO_MEDIADATA_RECEIVED:
					Log.i(TAG, "收不到媒体数据，可能是url错误。。");
					break;
				case EVENTID.EVENT_DANIULIVE_ERC_PLAYER_SWITCH_URL:
					Log.i(TAG, "切换播放URL。。");
					break;
				case EVENTID.EVENT_DANIULIVE_ERC_PLAYER_CAPTURE_IMAGE:
					Log.i(TAG, "快照: " + param1 + " 路径：" + param3);

					if(param1 == 0)
					{
						Log.i(TAG, "截取快照成功。.");
					}
					else
					{
						Log.i(TAG, "截取快照失败。.");
					}
					break;
			}
		}
	}

	/* Create rendering */
	private boolean CreateView() {

		if(sSurfaceView == null)
		{
        	 /*
             *  useOpenGLES2:
             *  If with true: Check if system supports openGLES, if supported, it will choose openGLES.
             *  If with false: it will set with default surfaceView;
             */
			sSurfaceView = NTRenderer.CreateRenderer(this, true);
		}

		if(sSurfaceView == null)
		{
			Log.i(TAG, "Create render failed..");
			return false;
		}

		return true;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		Log.i(TAG, "Run into onConfigurationChanged++");

		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
		{
			Log.i(TAG, "onConfigurationChanged, with LANDSCAPE。。");

			inflateLayout(LinearLayout.HORIZONTAL);

			currentOrigentation = LANDSCAPE;
		}
		else
		{
			Log.i(TAG, "onConfigurationChanged, with PORTRAIT。。");

			inflateLayout(LinearLayout.VERTICAL);

			currentOrigentation = PORTRAIT;
		}

		if(!isPlaybackViewStarted)
			return;

		//libPlayer.SmartPlayerSetOrientation(playerHandle, currentOrigentation);

		Log.i(TAG, "Run out of onConfigurationChanged--");
	}

	@Override
	protected  void onDestroy()
	{
		Log.i(TAG, "Run into activity destory++");

		if(playerHandle!=0)
		{
			libPlayer.SmartPlayerClose(playerHandle);
			playerHandle = 0;
		}

		byte com_cmd[]= user_uart_sendcom(3);
		if(MainActivity.sendThread != null)
		{
			MainActivity.sendThread.SendOut( com_cmd );
		}

		super.onDestroy();
		finish();
		//System.exit(0);
	}


	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		Log.i(TAG, "Run into OnCreate++");

		libPlayer = new SmartPlayerJni();

		myContext = this.getApplicationContext();

		boolean bViewCreated = CreateView();

		setContentView(R.layout.smartplayer_activity);

		if (bViewCreated) {
			inflateLayout(LinearLayout.VERTICAL);
		}

		Log.i(TAG, "Start playback stream++");

		playerHandle = libPlayer.SmartPlayerInit(myContext);

		if(playerHandle == 0)
		{
			Log.e(TAG, "surfaceHandle with nil..");
			return;
		}

		libPlayer.SetSmartPlayerEventCallback(playerHandle, new EventHande());
		libPlayer.SmartPlayerSetSurface(playerHandle, sSurfaceView); 	//if set the second param with null, it means it will playback audio only..
		libPlayer.SmartPlayerSetAudioOutputType(playerHandle, 0);
		libPlayer.SmartPlayerSetBuffer(playerHandle, playBuffer);
		libPlayer.SmartPlayerSetLowLatencyMode(playerHandle, 1);//low delay mode
		libPlayer.SmartPlayerSetFastStartup(playerHandle, isFastStartup?1:0);
		libPlayer.SmartPlayerSaveImageFlag(playerHandle, 0);
		libPlayer.SmartPlayerSetMute(playerHandle, 1);

		if( isHardwareDecoder )
		{
			Log.i(TAG, "check isHardwareDecoder: " + isHardwareDecoder);

			int hwChecking = libPlayer.SetSmartPlayerVideoHWDecoder(playerHandle, isHardwareDecoder?1:0);

			Log.i(TAG, "[daniulive] hwChecking: " + hwChecking);
		}


		if(MainActivity.playbackUrl == null){
			Log.e(TAG, "playback URL with NULL...");
			return;
		}

		int iPlaybackRet = libPlayer.SmartPlayerStartPlayback(playerHandle, MainActivity.playbackUrl);

		if(iPlaybackRet != 0)
		{
			Log.e(TAG, "StartPlayback strem failed..");
			return;
		}

		Log.e(TAG, "StartPlayback strem " + MainActivity.playbackUrl);

		ButtonListener b = new ButtonListener();
		ImageButton mUpButton = (ImageButton)findViewById(R.id.btn_up);
		mUpButton.setOnClickListener(b);
		mUpButton.setOnTouchListener(b);

		ImageButton mDownButton = (ImageButton)findViewById(R.id.btn_down);
		mDownButton.setOnClickListener(b);
		mDownButton.setOnTouchListener(b);

		ImageButton mLeftButton = (ImageButton)findViewById(R.id.btn_left);
		mLeftButton.setOnClickListener(b);
		mLeftButton.setOnTouchListener(b);

		ImageButton mRightButton = (ImageButton)findViewById(R.id.btn_right);
		mRightButton.setOnClickListener(b);
		mRightButton.setOnTouchListener(b);

		ImageButton mEnterButton = (ImageButton)findViewById(R.id.btn_enter);
		mEnterButton.setOnClickListener(b);
		mEnterButton.setOnTouchListener(b);

		ImageButton mKaijuButton = (ImageButton)findViewById(R.id.btn_kaiju);
		mKaijuButton.setOnClickListener(b);
		mKaijuButton.setOnTouchListener(b);

		vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

		ImageButton btnChangeCam = (ImageButton)findViewById(R.id.btn_changeCam);
		btnChangeCam.bringToFront();
		btnChangeCam.setOnClickListener(b);
		btnChangeCam.setOnTouchListener(b);

		switchURL = MainActivity.playbackUrl;
		MainActivity.sendThread.SetHandler(handler);
	}

	//construct the send packet
	int g_packget_id = 0;
	byte[] user_uart_sendcom(int... params) {
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

		g_packget_id++;
		return send_buf;
	}

	//check the data is valid or not
	boolean check_com_data(byte[] data, int len) {

		if (len < 6) return false;
		int check_total = 0;

		//check sum
		for (int i = 0; i < len; i++) {
			if ((i >= 6) && (i < len - 1))
				check_total += (data[i] & 0xff);
		}

		if (data[0] != (byte) (~data[3]&0xff) && data[1] != (byte) (~data[4]&0xff) && data[2] != (byte) (~data[5]&0xff))
			return false;

		if (check_total % 100 != data[len - 1]) {
			return false;
		}

		return true;
	}

	public void OnClickReboot(View v)
    {
        byte com_cmd[] = user_uart_sendcom(0x88);
        Log.e("==sending==", MainActivity.sendThread.bytesToHexString(com_cmd));
        if(MainActivity.sendThread != null)
        {
            MainActivity.sendThread.SendOut( com_cmd );
        }
    }

	Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case 10:
				{
					int msg_len = msg.arg1;
					byte test_data[] = (byte[]) (msg.obj);
					if (check_com_data(test_data, msg_len) == false)
					{
						Log.e("=====data recv===", "-----com check error-----");
						break;
					}
					int cmd = (test_data[7] & 0xff);
					Log.e("==onhandle==", Integer.toString(cmd));
					switch (cmd)
					{
						case 0x31://new game result notify from server.
						{
							//enable the operation button
							ImageButton mKaijuButton = (ImageButton)findViewById(R.id.btn_kaiju);
							mKaijuButton.setVisibility(View.INVISIBLE);

							ImageButton mUpButton = (ImageButton)findViewById(R.id.btn_up);
							mUpButton.setVisibility(View.VISIBLE);

							ImageButton mDownButton = (ImageButton)findViewById(R.id.btn_down);
							mDownButton.setVisibility(View.VISIBLE);

							ImageButton mLeftButton = (ImageButton)findViewById(R.id.btn_left);
							mLeftButton.setVisibility(View.VISIBLE);

							ImageButton mRightButton = (ImageButton)findViewById(R.id.btn_right);
							mRightButton.setVisibility(View.VISIBLE);

							ImageButton mEnterButton = (ImageButton)findViewById(R.id.btn_enter);
							mEnterButton.setVisibility(View.VISIBLE);
						}
						break;
						case 0x33://when the doll machine is reset to state available. it will notify the grasp result to playing player--and other people in the room.
						{
							int zhuawawaret = (test_data[8] & 0xff);
							if( zhuawawaret ==1 )
							{
								Toast.makeText(getApplicationContext(), "You grasp the doll!", Toast.LENGTH_SHORT).show();
							}
							else if(zhuawawaret == 0)
							{
								Toast.makeText(getApplicationContext(), "You lose", Toast.LENGTH_SHORT).show();
							}
							else//other result see in document
							{
								Toast.makeText(getApplicationContext(), "other code:" + zhuawawaret, Toast.LENGTH_SHORT).show();
							}

							//reset the ui to play next game
							ImageButton mKaijuButton = (ImageButton)findViewById(R.id.btn_kaiju);
							mKaijuButton.setVisibility(View.VISIBLE);

							ImageButton mUpButton = (ImageButton)findViewById(R.id.btn_up);
							mUpButton.setVisibility(View.INVISIBLE);

							ImageButton mDownButton = (ImageButton)findViewById(R.id.btn_down);
							mDownButton.setVisibility(View.INVISIBLE);

							ImageButton mLeftButton = (ImageButton)findViewById(R.id.btn_left);
							mLeftButton.setVisibility(View.INVISIBLE);

							ImageButton mRightButton = (ImageButton)findViewById(R.id.btn_right);
							mRightButton.setVisibility(View.INVISIBLE);

							ImageButton mEnterButton = (ImageButton)findViewById(R.id.btn_enter);
							mEnterButton.setVisibility(View.INVISIBLE);
							Log.e("=====NotifyGameEnd===", "-----------------");

						}
						break;
					}
				}
				break;
			}
			super.handleMessage(msg);
		}
	};

	Timer timer =null;
	int dir = -1;

	class ButtonListener implements View.OnClickListener, View.OnTouchListener{

		public void onClick(View v) {
			if(v.getId() == R.id.btn_up
					||v.getId() == R.id.btn_down
					||v.getId() == R.id.btn_left
					||v.getId() == R.id.btn_right
					||v.getId() == R.id.btn_kaiju
					||v.getId() == R.id.btn_enter){

				 //vibrator.vibrate(100);
			}

			if(v.getId() == R.id.btn_changeCam)
			{
				switchUrlFlag = !switchUrlFlag;
				if ( switchUrlFlag )
				{
					switchURL = MainActivity.playbackUrl2;	//switch to another stream
				}
				else
				{
					switchURL = MainActivity.playbackUrl;
				}

				if ( playerHandle != 0 )
				{
					libPlayer.SmartPlayerSwitchPlaybackUrl(playerHandle, switchURL);
				}
			}

			if(v.getId() == R.id.btn_kaiju)
			{
				b_start_new_game = true;
				if(b_start_new_game == true)
				{
					b_start_new_game = false;

					int cmd = 0x31;//start a new game
					int param_timeout = 60;//time out auto fire grasp. in second.
					int catch_result = 0;//client should send 0 always. the server need to decide this is 1 or 0
					int power_catch = 0;//0 forever
					int power_ontop = 0;//0 forever
					int power_move = 0;//0 forever
					int power_max = 0;//0 forever
					int hold_height = 0;//0 forever see detail in document
					byte com_cmd[] = user_uart_sendcom(0x31, param_timeout, catch_result, power_catch,power_ontop,power_move,power_max,hold_height);
					Log.e("==sending==", MainActivity.sendThread.bytesToHexString(com_cmd));
					if(MainActivity.sendThread != null)
					{
						MainActivity.sendThread.SendOut( com_cmd );
					}
				}
			}

			if(v.getId() == R.id.btn_enter)
			{
				byte com_cmd[] = user_uart_sendcom(0x32, 4, 0, 0);//下抓
				if(MainActivity.sendThread != null)
				{
					MainActivity.sendThread.SendOut( com_cmd );
				}
			}
		}

		public boolean onTouch(View v, MotionEvent event) {

			if(event.getAction() == MotionEvent.ACTION_UP){

				if(v.getId() == R.id.btn_changeCam)
				{
					((ImageButton)v).setImageDrawable(getResources().getDrawable(R.drawable.change_camera_normal));
				}
				else if(v.getId() == R.id.btn_kaiju)
				{
					((ImageButton)v).setImageDrawable(getResources().getDrawable(R.drawable.start_grasp_doll_normal));
				}
				else if(v.getId() == R.id.btn_enter)
				{
					((ImageButton)v).setImageDrawable(getResources().getDrawable(R.drawable.down_grasp_normal));
				}
				else if(v.getId() == R.id.btn_up)
				{
					((ImageButton)v).setImageDrawable(getResources().getDrawable(R.drawable.operation_up_normal));
				}
				else if(v.getId() == R.id.btn_down)
				{
					((ImageButton)v).setImageDrawable(getResources().getDrawable(R.drawable.operation_down_normal));
				}
				else if(v.getId() == R.id.btn_left)
				{
					((ImageButton)v).setImageDrawable(getResources().getDrawable(R.drawable.operation_left_normal));
				}
				else  if(v.getId() == R.id.btn_right)
				{
					((ImageButton)v).setImageDrawable(getResources().getDrawable(R.drawable.operation_right_normal));
				}

				int num2 = 0;
				int num3 = 0;
				byte com_cmd[] = user_uart_sendcom(0x32, 0x05, num2, num3);//release pressing button  send cmd 05
				if(MainActivity.sendThread != null)
				{
					MainActivity.sendThread.SendOut( com_cmd );
				}
			}
			if(event.getAction() == MotionEvent.ACTION_DOWN){

				// vibrator.vibrate(100);
				if(v.getId() == R.id.btn_changeCam)
				{
					((ImageButton)v).setImageDrawable(getResources().getDrawable(R.drawable.change_camera_press));
				}
				else if(v.getId() == R.id.btn_kaiju)
				{
					((ImageButton)v).setImageDrawable(getResources().getDrawable(R.drawable.start_grasp_doll_press));
				}
				else if(v.getId() == R.id.btn_enter)
				{
					((ImageButton)v).setImageDrawable(getResources().getDrawable(R.drawable.down_grasp_press));
				}
				else if(v.getId() == R.id.btn_up)
				{
					((ImageButton)v).setImageDrawable(getResources().getDrawable(R.drawable.operation_up_press));
					dir =1;
				}
				else if(v.getId() == R.id.btn_down)
				{
					((ImageButton)v).setImageDrawable(getResources().getDrawable(R.drawable.operation_down_press));
					dir = 0;
				}
				else if(v.getId() == R.id.btn_left)
				{
					((ImageButton)v).setImageDrawable(getResources().getDrawable(R.drawable.operation_left_press));
					dir = 2;
				}
				else  if(v.getId() == R.id.btn_right)
				{
					((ImageButton)v).setImageDrawable(getResources().getDrawable(R.drawable.operation_right_press));
					dir = 3;
				}

				if(v.getId() == R.id.btn_up
						||v.getId() == R.id.btn_down
						||v.getId() == R.id.btn_left
						||v.getId() == R.id.btn_right)
				{
					//press any operation button .send 0x32 once.
					int num2 = 136;
					int num3 = 19;
					byte com_cmd[] = user_uart_sendcom(0x32, dir, num2, num3);//constant move time 5000 in millisecond. don't change.
					if(MainActivity.sendThread != null)
					{
						MainActivity.sendThread.SendOut( com_cmd );
					}
				}
			}

			return false;
		}
	}

}