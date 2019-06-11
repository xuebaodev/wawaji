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

import com.eventhandle.NTSmartEventCallbackV2;
import com.eventhandle.NTSmartEventID;
import com.eventhandle.SmartEventCallback;
import com.videoengine.*;

import org.json.JSONArray;
import org.json.JSONObject;


public class SmartPlayer extends Activity {

	public static String switchURL = "rtmp://your video stream url";

    private SurfaceView sSurfaceView = null;   
	
	private long playerHandle = 0;
	
	private static final int PORTRAIT = 1;
	private static final int LANDSCAPE = 2;
	private static final String TAG = "SmartPlayer";
	
	private SmartPlayerJniV2 libPlayer = null;
	
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

	class EventHande implements NTSmartEventCallbackV2
	{
		public void onNTSmartEventCallbackV2(long handle, int id, long param1,
											 long param2, String param3, String param4, Object param5) {
			switch (id) {
				case NTSmartEventID.EVENT_DANIULIVE_ERC_PLAYER_STARTED:
					Log.i(TAG, "start.");
					break;
				case NTSmartEventID.EVENT_DANIULIVE_ERC_PLAYER_CONNECTING:
					Log.i(TAG, "connecting...");
					break;
				case NTSmartEventID.EVENT_DANIULIVE_ERC_PLAYER_CONNECTION_FAILED:
					Log.i(TAG, "connect failed.");
					break;
				case NTSmartEventID.EVENT_DANIULIVE_ERC_PLAYER_CONNECTED:
					Log.i(TAG, "connect ok.");
					break;
				case NTSmartEventID.EVENT_DANIULIVE_ERC_PLAYER_DISCONNECTED:
					Log.i(TAG, "connect lose.");
					break;
				case NTSmartEventID.EVENT_DANIULIVE_ERC_PLAYER_STOP:
					Log.i(TAG, "close.");
					break;
				case NTSmartEventID.EVENT_DANIULIVE_ERC_PLAYER_RESOLUTION_INFO:
					Log.i(TAG, "resolution: width: " + param1 + ", height: " + param2);
					break;
				case NTSmartEventID.EVENT_DANIULIVE_ERC_PLAYER_NO_MEDIADATA_RECEIVED:
					Log.i(TAG, "not recv media data， maybe url wrong.");
					break;
				case NTSmartEventID.EVENT_DANIULIVE_ERC_PLAYER_SWITCH_URL:
					Log.i(TAG, "swtich URL");
					break;
				case NTSmartEventID.EVENT_DANIULIVE_ERC_PLAYER_CAPTURE_IMAGE:
					Log.i(TAG, "snapshot: " + param1 + " path：" + param3);

					if(param1 == 0)
					{
						Log.i(TAG, "snapshot ok.");
					}
					else
					{
						Log.i(TAG, "snapshot failed.");
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
			Log.i(TAG, "Create render failed.");
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
			Log.i(TAG, "onConfigurationChanged, with LANDSCAPE.");

			inflateLayout(LinearLayout.HORIZONTAL);

			currentOrigentation = LANDSCAPE;
		}
		else
		{
			Log.i(TAG, "onConfigurationChanged, with PORTRAIT.");

			inflateLayout(LinearLayout.VERTICAL);

			currentOrigentation = PORTRAIT;
		}

		if(!isPlaybackViewStarted)
			return;

		//libPlayer.SmartPlayerSetOrientation(playerHandle, currentOrigentation);

		Log.i(TAG, "Run out of onConfigurationChanged--");
	}

	/* //新版数据命令
DA
数据长度2位
json

{"cmd":"req_roomlist"}
{"cmd":"enter_room","mac":"XXXX"}
{"cmd":"exit_room"}
{"cmd":"start_game"}
{"cmd":"operation","type":0}
*/

	@Override
	protected  void onDestroy()
	{
		Log.i(TAG, "Run into activity destory++");

		if(playerHandle!=0)
		{
			libPlayer.SmartPlayerStopPlay(playerHandle);
			libPlayer.SmartPlayerClose(playerHandle);
			playerHandle = 0;
		}

		String jsoncmd="{\"cmd\":\"exit_room\"}";
		byte msg_content[] = new byte[3+jsoncmd.length()];
		msg_content[0]= (byte)0xda;
		msg_content[1] = (byte)(jsoncmd.length()/256);
		msg_content[2] = (byte)(jsoncmd.length()%256);
		System.arraycopy(jsoncmd.getBytes(),0,msg_content,3,jsoncmd.getBytes().length);
		if(MainActivity.sendThread != null) {
			MainActivity.sendThread.SendOut( msg_content );
		}

		super.onDestroy();
		finish();
		//System.exit(0);
	}


	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		Log.i(TAG, "Run into OnCreate++");

		libPlayer = new SmartPlayerJniV2();

		myContext = this.getApplicationContext();

		boolean bViewCreated = CreateView();

		setContentView(R.layout.smartplayer_activity);

		if (bViewCreated) {
			inflateLayout(LinearLayout.VERTICAL);
		}

		Log.i(TAG, "Start playback stream++");

		playerHandle = libPlayer.SmartPlayerOpen(myContext);

		if(playerHandle == 0)
		{
			Log.e(TAG, "surfaceHandle with nil..");
			//return;
		}

		libPlayer.SetSmartPlayerEventCallbackV2(playerHandle, new EventHande());
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
			//return;
		}

		libPlayer.SmartPlayerSetUrl(playerHandle, MainActivity.playbackUrl);

		int iPlaybackRet = libPlayer.SmartPlayerStartPlay(playerHandle);

		if(iPlaybackRet != 0)
		{
			Log.e(TAG, "StartPlayback strem failed..");
			//return;
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

	public void OnClickReboot(View v)
    {
		String jsoncmd="{\"cmd\":\"reboot\"}";
		byte msg_content[] = new byte[3+jsoncmd.length()];
		msg_content[0]= (byte)0xda;
		msg_content[1] = (byte)(jsoncmd.length()/256);
		msg_content[2] = (byte)(jsoncmd.length()%256);
		System.arraycopy(jsoncmd.getBytes(),0,msg_content,3,jsoncmd.getBytes().length);
		if(MainActivity.sendThread != null) {
			MainActivity.sendThread.SendOut( msg_content );
		}
    }


	//安卓板特有协议。模拟游戏结束。以便可以在不接娃娃机时测试录像逻辑
	public void OnClickFakeEnd(View v)
	{

	}

	int abs = 0;
    public void OnClickSend93(View v)
    {
		String jsoncmd="";
		if( abs ==0) {abs =1;jsoncmd="{\"cmd\":\"stop_stream\"}";}
		else {abs = 0; jsoncmd="{\"cmd\":\"start_stream\"}";}

		byte msg_content[] = new byte[3+jsoncmd.length()];
		msg_content[0]= (byte)0xda;
		msg_content[1] = (byte)(jsoncmd.length()/256);
		msg_content[2] = (byte)(jsoncmd.length()%256);
		System.arraycopy(jsoncmd.getBytes(),0,msg_content,3,jsoncmd.getBytes().length);
		if(MainActivity.sendThread != null) {
			MainActivity.sendThread.SendOut( msg_content );
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
					String jsonString = new String(test_data, 0, msg_len );//从第八位开始，最后一位不要。
					Log.e("PlayerA",jsonString);
					try {
						JSONObject jsRet = new JSONObject(jsonString);
						String strCmd = jsRet.getString("cmd");
						if( strCmd.equals("start_game" ))
						{
							int start_ret = jsRet.getInt("ret");
							if( start_ret ==1)
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
						}
						else if(strCmd.equals("game_ret"))
						{
							int game_ret = jsRet.getInt("ret");
							if( game_ret ==1 )
							{
								Toast.makeText(getApplicationContext(), "You grasp the doll!", Toast.LENGTH_SHORT).show();
							}
							else if(game_ret == 0)
							{
								Toast.makeText(getApplicationContext(), "You lose", Toast.LENGTH_SHORT).show();
							}
							else//other result see in document
							{
								Toast.makeText(getApplicationContext(), "other code:" + game_ret, Toast.LENGTH_SHORT).show();
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
					}catch (Exception e) {
						e.printStackTrace();
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

					String jsoncmd="{\"cmd\":\"start_game\"}";
					byte msg_content[] = new byte[3+jsoncmd.length()];
					msg_content[0]= (byte)0xda;
					msg_content[1] = (byte)(jsoncmd.length()/256);
					msg_content[2] = (byte)(jsoncmd.length()%256);
					System.arraycopy(jsoncmd.getBytes(),0,msg_content,3,jsoncmd.getBytes().length);
					if(MainActivity.sendThread != null)
					{
						MainActivity.sendThread.SendOut( msg_content );
					}
				}
			}

			if(v.getId() == R.id.btn_enter)
			{
				//{"cmd":"operation","type":4}
				String jsoncmd = "{\"cmd\":\"operation\",\"type\":4}";
				byte msg_content[] = new byte[3+jsoncmd.length()];
				msg_content[0]= (byte)0xda;
				msg_content[1] = (byte)(jsoncmd.length()/256);
				msg_content[2] = (byte)(jsoncmd.length()%256);
				System.arraycopy(jsoncmd.getBytes(),0,msg_content,3,jsoncmd.getBytes().length);
				if(MainActivity.sendThread != null) {
					MainActivity.sendThread.SendOut( msg_content );
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

				if(v.getId() != R.id.btn_kaiju && v.getId() != R.id.btn_changeCam && (v.getId() != R.id.btn_enter))
				{
					String jsoncmd = "{\"cmd\":\"operation\",\"type\":5}";
					byte msg_content[] = new byte[3+jsoncmd.length()];
					msg_content[0]= (byte)0xda;
					msg_content[1] = (byte)(jsoncmd.length()/256);
					msg_content[2] = (byte)(jsoncmd.length()%256);
					System.arraycopy(jsoncmd.getBytes(),0,msg_content,3,jsoncmd.getBytes().length);
					if(MainActivity.sendThread != null) {
						MainActivity.sendThread.SendOut( msg_content );
					}
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
					String jsoncmd = String.format("{\"cmd\":\"operation\",\"type\":%d}", dir);
					byte msg_content[] = new byte[3+jsoncmd.length()];
					msg_content[0]= (byte)0xda;
					msg_content[1] = (byte)(jsoncmd.length()/256);
					msg_content[2] = (byte)(jsoncmd.length()%256);
					System.arraycopy(jsoncmd.getBytes(),0,msg_content,3,jsoncmd.getBytes().length);
					if(MainActivity.sendThread != null) {
						MainActivity.sendThread.SendOut( msg_content );
					}
				}
			}

			return false;
		}
	}

}