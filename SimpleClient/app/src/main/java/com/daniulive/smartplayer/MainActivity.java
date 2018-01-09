package com.daniulive.smartplayer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import socks.MsgThread;
import socks.SendThread;


class RoomInfo
{
    String mac;
    String vurl1;
    String vurl2;
}

public class MainActivity extends Activity {

    public static SendThread sendThread;

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

                if(sendThread != null) {sendThread.StopNow(); sendThread = null;}
                try {
                    InetAddress addr = InetAddress.getByName(ServerHost);
                    String ServerIP = addr.getHostAddress();
                    sendThread = new SendThread(handler, ServerIP, ServerPort);//dip
                    sendThread.start();

                } catch (UnknownHostException e) {
                    e.printStackTrace();
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
            getWindow().setNavigationBarColor(Color.BLACK);
            //getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }

        setContentView(R.layout.activity_main);

        share = this.getSharedPreferences("share", Context.MODE_PRIVATE);

        ServerPort = share.getInt("ServerPort", 7771);
        ServerHost = share.getString("ServerHost", "192.168.0.116");
        playbackUrl = share.getString("playbackUrl", "rtmp://");//rtmp video1 url
        playbackUrl2 = share.getString("playbackUrl2", "rtmp://");//rtmp video2 url

        mDeviceScr = findViewById(R.id.device_scr);
        mDeviceView = (TableLayout) findViewById(R.id.device_list);

        if (sendThread != null) {
            sendThread.StopNow();
            sendThread = null;
        }
        try {
            InetAddress addr = InetAddress.getByName(ServerHost);
            String ServerIP = addr.getHostAddress();
            sendThread = new SendThread(handler, ServerIP, ServerPort);//dip
            sendThread.start();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    //断开已有连接。重新连接到服务器
    void ServerStopAndReconnect()
    {
        if (sendThread != null) {
            sendThread.StopNow();
            sendThread = null;
        }

        try {
            InetAddress addr = InetAddress.getByName(ServerHost);
            String ServerIP = addr.getHostAddress();
            sendThread = new SendThread(handler, ServerIP, ServerPort);//dip
            sendThread.start();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    Timer reconnect_timer = null;
    Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            switch ( msg.what) {
                case 0: {
                    Log.e("===MainActivity==", "Connect succ.");
                    Toast.makeText(getApplicationContext(), "连接成功", Toast.LENGTH_SHORT).show();
                    //获取场地设备列表

                    byte msg_content[] = new byte[9];
                    msg_content[0] = (byte) 0xfe;
                    msg_content[1] = (byte) (0);
                    msg_content[2] = (byte) (0);
                    msg_content[3] = (byte) ~msg_content[0];
                    msg_content[4] = (byte) ~msg_content[1];
                    msg_content[5] = (byte) ~msg_content[2];
                    msg_content[6] = (byte) (msg_content.length);
                    msg_content[7] = (byte) 0x1;

                    int total_c = 0;
                    for (int i = 6; i < msg_content.length - 1; i++) {
                        total_c += (msg_content[i] & 0xff);
                    }
                    msg_content[msg_content.length - 1] = (byte) (total_c % 100);

                    if(sendThread != null)
                    {
                        sendThread.sendMsg( msg_content );
                    }
                }
                break;
                case 10://获取场地的设备列表
                {
                    int msg_len = msg.arg1;
                    byte test_data[] = (byte[]) (msg.obj);
                    int cmd = (test_data[7] & 0xff);
                    String jsonString = new String(test_data, 9, test_data.length - 9);
                    try {
                        JSONArray A05Result = new JSONArray(jsonString);

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
                                //动态生成界面控件
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
                                    byte msg_content[] = new byte[9 + amac.getBytes().length];
                                    msg_content[0] = (byte) 0xfe;
                                    msg_content[1] = (byte) (0);
                                    msg_content[2] = (byte) (0);
                                    msg_content[3] = (byte) ~msg_content[0];
                                    msg_content[4] = (byte) ~msg_content[1];
                                    msg_content[5] = (byte) ~msg_content[2];
                                    msg_content[6] = (byte) (msg_content.length);
                                    msg_content[7] = (byte) 0x2;

                                    System.arraycopy(amac.getBytes(),0,msg_content,8,amac.getBytes().length);

                                    int total_c = 0;
                                    for (int i = 6; i < msg_content.length - 1; i++) {
                                        total_c += (msg_content[i] & 0xff);
                                    }
                                    msg_content[msg_content.length - 1] = (byte) (total_c % 100);

                                    if(sendThread != null)
                                    {
                                        sendThread.sendMsg( msg_content );
                                    }

                                    if( playbackUrl.equals("rtmp://") )
                                        playbackUrl = "rtmp://119.29.226.242:1935/hls/" + amac + "_1";

                                    if(playbackUrl2.equals("rtmp://"))
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
                case 20: {
                    if( reconnect_timer == null)
                    {
                        if (sendThread != null) {
                            sendThread.StopNow();

                            sendThread = null;
                        }

                        reconnect_timer = new Timer();
                        reconnect_timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                reconnect_timer = null;
                                handler.sendEmptyMessage(21);
                                Log.e("case20", "socket连接已断开。正在重连");
                            }
                        }, 3000);
                    }
                }
                break;
                case 21:
                {
                    ServerStopAndReconnect();
                }
                break;

            }

            super.handleMessage(msg);
        }
    };

}
