package com.daniulive.smartplayer;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.util.Log;
import android.os.*;
import android.os.Handler;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;


import socks.SockAPP;

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
public class MainActivity extends Activity {
    private final String TAG = "MainActivity";
    public static SockAPP sendThread;
    private final int PERMISSION_REQUEST = 0xa00;
    private View mDeviceScr;
    private TableLayout mDeviceView;//显示设备列表

    SharedPreferences share ;
    public static String playbackUrl = "rtmp://";
    public static String playbackUrl2 = "rtmp://";
    public static String ServerHost = "192.168.0.110";//your application server ip.
    public static int ServerPort = 1090;//your application server port. your control command is send to this server .and the server will translate to the doll machine

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        //finish();
        if(sendThread != null) {sendThread.StopNow(); sendThread = null;}
        System.exit(0);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        sendThread.SetHandler(handler);
    }

    public void onClickSetting(View v2) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle("configuration");
        View view1 = LayoutInflater.from(MainActivity.this).inflate(R.layout.layout_setting, null);
        builder.setView(view1);

        final EditText serverip = (EditText)view1.findViewById(R.id.serverip);
        final EditText serverport = (EditText)view1.findViewById(R.id.serverport);
        final EditText video_url1 = (EditText)view1.findViewById(R.id.video_url1);
        final EditText video_url2= (EditText)view1.findViewById(R.id.video_url2);

        serverip.setText(ServerHost);
        serverport.setText(Integer.toString(ServerPort));

        video_url1.setText(playbackUrl);
        video_url2.setText(playbackUrl2);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {

                SharedPreferences.Editor editor = share.edit();
                ServerHost = serverip.getText().toString().trim();
                ServerPort = Integer.parseInt( serverport.getText().toString().trim());
                playbackUrl = video_url1.getText().toString().trim();
                playbackUrl2 = video_url2.getText().toString().trim();

                editor.putString("ServerHost", ServerHost);
                editor.putInt("ServerPort", ServerPort);
                editor.putString("playbackUrl", playbackUrl);
                editor.putString("playbackUrl2", playbackUrl2);

                if(sendThread != null)
                {
                    sendThread.ApplyNewServer(ServerHost, ServerPort);
                }


                editor.commit();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {

            }
        });
        builder.show();
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //透明状态栏
            //getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            //透明导航栏
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
                getWindow().setNavigationBarColor(Color.BLACK);
            }

            //getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }

        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.INTERNET,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.MODIFY_AUDIO_SETTINGS}, PERMISSION_REQUEST);
        }

        share = this.getSharedPreferences("share", Context.MODE_PRIVATE);

        ServerPort = share.getInt("ServerPort", 7771);
        ServerHost = share.getString("ServerHost", "192.168.0.116");
        playbackUrl = share.getString("playbackUrl", "rtmp://");//rtmp video1 url
        playbackUrl2 = share.getString("playbackUrl2", "rtmp://");//rtmp video2 url

        mDeviceScr = findViewById(R.id.device_scr);
        mDeviceView = (TableLayout) findViewById(R.id.device_list);

        sendThread = new SockAPP();
        sendThread.StartWokring( handler, ServerHost, ServerPort);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case PERMISSION_REQUEST:
                if (grantResults != null && permissions != null) {
                    for (int i = 0; i < grantResults.length; i++) {
                        Log.d(TAG, "grantResults[" + i + "]:" + grantResults[i]);
                        Log.d(TAG, "permissions[" + i + "]:" + permissions[i]);
                    }
                }

                break;
        }
    }

    Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            switch ( msg.what) {
                case 0: {
                    Log.e("===MainActivity==", "Connect succ.");
                    Toast.makeText(getApplicationContext(), "connect ok", Toast.LENGTH_SHORT).show();
                    //获取场地设备列表
                    String jsoncmd="{\"cmd\":\"req_roomlist\"}";
                    byte msg_content[] = new byte[3+jsoncmd.length()];
                    msg_content[0]= (byte)0xda;
                    msg_content[1] = (byte)(jsoncmd.length()/256);
                    msg_content[2] = (byte)(jsoncmd.length()%256);
                    System.arraycopy(jsoncmd.getBytes(),0,msg_content,3,jsoncmd.getBytes().length);
                    if(sendThread != null) {
                        sendThread.SendOut( msg_content );
                    }
                }
                break;
                case 10://获取场地的设备列表
                {
                    int msg_len = msg.arg1;
                    byte test_data[] = (byte[]) (msg.obj);
                    String jsonString = new String(test_data, 0, msg_len );//从第八位开始，最后一位不要。
                    try {
                        JSONObject jsRet = new JSONObject(jsonString);
                        String strCmd = jsRet.getString("cmd");
                        JSONArray A05Result =null;
                        if(strCmd.equals("reply_roomlist"))
                        {
                            A05Result = jsRet.getJSONArray("rooms");
                        }
                        Log.e(TAG, "room count" + A05Result.length());
                        if( A05Result.length() ==0 )  Toast.makeText(getApplicationContext(), "room list is empty.", Toast.LENGTH_SHORT).show();
                        TableRow cur_row = null;
                        //动态生成界面控件
                        TableLayout v_room = (TableLayout) findViewById(R.id.device_list);
                        v_room.removeAllViews();

                        mDeviceScr.setVisibility(View.VISIBLE);
                        // deviceids

                        for (int i = 0; i < A05Result.length(); i++) {
                            String mac = A05Result.getString(i);

                            //生成界面控件
                            if (i % 2 == 0) {
                                //动态生成界面控
                                // TableLayout v_room = (TableLayout) findViewById(R.id.room_list);
                                final TableRow new_row = new TableRow(getApplicationContext());
                                cur_row = new_row;
                                new_row.setId(i % 2);
                                new_row.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                                v_room.addView(new_row);
                            }

                            LayoutInflater inflater3 = LayoutInflater.from(getApplicationContext());
                            View samview = inflater3.inflate(R.layout.layout_room, null);

                            RelativeLayout rel = (RelativeLayout) samview.findViewById(R.id.layout_rel);
                            TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);
                            params.weight = 1.0f;
                            params.width = 0;
                            params.height = ((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, getResources().getDisplayMetrics()));
                            rel.setLayoutParams(params);

                            TextView tii = samview.findViewById( R.id.txtRoomTitle);
                            tii.setText( mac );

                            ImageView imgBtn = (ImageView) samview.findViewById(R.id.imgRoom);
                            imgBtn.setTag(mac);
                            imgBtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    String amac = (String) v.getTag();
                                    //发送进设备命令-并切换界面
                                    String jsoncmd = String.format("{\"cmd\":\"enter_room\",\"mac\":\"%s\"}", amac);
                                    byte msg_content[] = new byte[3+jsoncmd.length()];
                                    msg_content[0]= (byte)0xda;
                                    msg_content[1] = (byte)(jsoncmd.length()/256);
                                    msg_content[2] = (byte)(jsoncmd.length()%256);
                                    System.arraycopy(jsoncmd.getBytes(),0,msg_content,3,jsoncmd.getBytes().length);
                                    if(sendThread != null) {
                                        sendThread.SendOut( msg_content );
                                    }

                                    if( playbackUrl.contains("rtmp://119"))
                                        playbackUrl = "rtmp://119.29.226.242:1935/hls/" + amac + "_1";

                                    if(playbackUrl2.contains("rtmp://119"))
                                        playbackUrl2 = "rtmp://119.29.226.242:1935/hls/" + amac + "_2";

                                    Intent intent = new Intent(MainActivity.this, SmartPlayer.class);
                                    intent.putExtra("mac", amac);
                                    //intent.putExtra("subRoomID", subRoomID);
                                    //intent.putExtra("devID", devID);
                                    //intent.putExtra("userName", DataModel.sDataModel.mPlayer.strID);
                                    startActivity(intent);
                                }
                            });

                            cur_row.addView(samview);
                        }

                        if (A05Result.length() % 2 != 0) {
                            TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT, 1.0f);
                            params.width = 0;
                            params.weight = 1.0f;
                            RelativeLayout emptyView = new RelativeLayout(getApplicationContext());
                            emptyView.setLayoutParams(params);
                            cur_row.addView(emptyView);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
            super.handleMessage(msg);
        }
    };
}
