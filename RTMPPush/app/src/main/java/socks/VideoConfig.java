package socks;


import android.content.SharedPreferences;
import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Size;

import com.daniulive.smartpublisher.CameraPublishActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.Socket;
import java.util.List;


//此类封装读取或保存配置
public class VideoConfig
{
	public static VideoConfig instance;

    public Handler msgHandler = null;

    //上次保存的正确配置。每次应用之前先保存当前的值
    public int last_resolution_index = 2;
    public int last_video_width = 640;
    public int last_video_height = 360;

    //分辨率
    int resolution_index = 0;
    public void SetResolutionIndex(int newIndex)
    {
        last_resolution_index = resolution_index;
        resolution_index = newIndex;
    }
    public int GetResolutionIndex(){ return resolution_index; }

    int videoWidth = 640;
    public void SetVideoWidth(int w)
    {
        last_video_width =  videoWidth;
        videoWidth = w;
    }
    public int GetVideoWidth(){return videoWidth;}

    int videoHeight = 360;
    public void SetVideoHeight(int h)
    {
        last_video_height = videoHeight;
        videoHeight = h;
    }
    public int GetVideoHeight(){return videoHeight;}

    public void RestoreLastVideoSizeAndIndex()
    {
        resolution_index = last_resolution_index;
        videoWidth = last_video_width;
        videoHeight = last_video_height;
    }

    public boolean is_hardware_encoder = false;//硬编码 软编码

    //硬编码的码率
    public int hwEncoderKpbs  = 0;//码率

    //硬编码-帧率
    int encodeFPS = 20;
    public void SetFPS(int nFPS)
    {
        if(encodeFPS<=0 || encodeFPS >= 60 )
            encodeFPS = 20;
        else
            encodeFPS = nFPS;
    }

    public int GetFPS()
    {
        return  encodeFPS;
    }

    /* video软编码profile设置
 * 1: baseline profile
 * 2: main profile
 * 3: high profile
 * */
    public int sw_video_encoder_profile = 1;	//default with baseline profile

    //软编码编码速度
    public int sw_video_encoder_speed = 2;

    //录像
    public boolean is_need_local_recorder = false;		// 是否需要本地录像
    public String recDirFront = "/sdcard/daniulive/recfront";	//本地录像的路径
    public String recDirBack  = "/sdcard/daniulive/recback";

    //摄像头的配置
    private static final int PORTRAIT = 1;	//竖屏
    private static final int LANDSCAPE = 2;	//横屏
    public int currentOrigentation = PORTRAIT;//摄像头竖着放

    //推流地址1 2
    public String my_mac;
    public String url1 = "";
    public String url2 = "";

    public int appVersion;//本app的版本号。用于升级时比较大小

    //本机IP 应用服务器IP 端口
    public boolean using_dhcp = true;
    public String hostIP = "";//本机IP
    public String gateIP ="";//网关IP
    public String maskIP="";//子网掩码

    public String destHost = "";//应用服务器地址

    int destPort = 0;//应用服务器地址
    public void SetAppPort(int np)
    {
        if(np<=0 || np >= 65535)
            destPort = 0;
        else
            destPort = np;
    }

    public int GetAppPort()
    {
        return destPort;
    }

    public String configHost;//配置服务器

    int configPort;//地址
    public void SetConfigPort(int np)
    {
        if(np<=0 || np >= 65535)
            configPort = 0;
        else
            configPort = np;
    }

    public int GetConfigPort()
    {
        return configPort;
    }

    public String wifiSSID;//wifi名称
    public String wifiPassword;//wifi密码

    public String machine_name;//给人看的 方便记住这台机器
    public String userID;////这台娃娃机所属的用户。 ----即 哪个老板买了它。挂到名下方便管理

    public void LoadConfig(Context context, Handler hh)
	{
        msgHandler = hh;

        SharedPreferences share = context.getSharedPreferences("pushConfig", Context.MODE_PRIVATE);

        resolution_index = share.getInt("resolution_index", 1);
        if(resolution_index != -1)
        {
            switch (resolution_index) {
                case 0: {
                    VideoConfig.instance.videoWidth = 960;
                    VideoConfig.instance.videoHeight = 720;
                }
                break;
                case 1:
                    VideoConfig.instance.videoWidth = 640;
                    VideoConfig.instance.videoHeight = 480;
                    break;
                case 2:
                    VideoConfig.instance.videoWidth = 640;
                    VideoConfig.instance.videoHeight = 360;
                    break;
                case 3:
                    VideoConfig.instance.videoWidth = 352;
                    VideoConfig.instance.videoHeight = 288;
                    break;
                case 4:
                    VideoConfig.instance.videoWidth = 320;
                    VideoConfig.instance.videoHeight = 240;
                    break;

                default:
                    VideoConfig.instance.videoWidth = 640;
                    VideoConfig.instance.videoHeight = 360;
            }
        }
        else
            {
                videoWidth = share.getInt("videoWidth", 640);
                videoHeight = share.getInt("videoHight", 360);
            }

        last_resolution_index = share.getInt("last_resolution_index", 2);
        last_video_width = share.getInt("last_video_width", 640);
        last_video_height = share.getInt("last_video_height", 360);

        is_hardware_encoder  = share.getBoolean("is_hardware_encoder", false);
        hwEncoderKpbs = share.getInt("hwEncoderKpbs", 560);
        encodeFPS = share.getInt("encodeFPS", 20);

        sw_video_encoder_profile = share.getInt("sw_video_encoder_profile", 1);
        sw_video_encoder_speed = share.getInt("sw_video_encoder_speed", 2);

        is_need_local_recorder = share.getBoolean("is_need_local_recorder", false);
        recDirFront = share.getString("recDirFront", "/sdcard/daniulive/recfront");
        recDirBack = share.getString("recDirBack", "/sdcard/daniulive/recback");

        currentOrigentation = share.getInt("currentOrigentation", 1);

        //rtmp://119.29.226.242:1935/hls/229031AA7875_1

        my_mac = getMac();
        url1 = share.getString("url1", "rtmp://videoServerNameOrIP:videoServerPort/catalog/pushID");
        url2 = share.getString("url2", "rtmp://videoServerNameOrIP:videoServerPort/catalog/pushID");

        using_dhcp = share.getBoolean("using_dhcp", true);
        my_mac  = getMac();

        wifiSSID = share.getString("wifiSSID", "");
        wifiPassword = share.getString("wifiPassword", "");

        hostIP = share.getString("hostIP", "");
        if(hostIP.equals(""))
        {
            hostIP = CameraPublishActivity.getLocalIpAddress();
        }

        gateIP = share.getString("gateIP", "192.168.0.0");
        maskIP = share.getString("maskIP", "192.168.0.0");

        destHost = share.getString("destHost", "");
        destPort = share.getInt("destPort", 0);

        configHost = share.getString("configHost", "192.168.0.116");
        configPort = share.getInt("configPort", 7776);

        userID = share.getString("userID", "xuebao");

        machine_name = share.getString("machine_name", "可爱小白兔");

        appVersion = APKVersionCodeUtils.getVersionCode(context);
	}

    public void SaveConfig(Context context)
	{
        SharedPreferences share = context.getSharedPreferences("pushConfig", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = share.edit();

        editor.putInt("resolution_index", resolution_index);
        editor.putInt("videoWidth", videoWidth);
        editor.putInt("videoHight", videoHeight);

        editor.putInt("last_resolution_index", last_resolution_index);
        editor.putInt("last_video_width", last_video_width);
        editor.putInt("last_video_height", last_video_height);

        editor.putBoolean("is_hardware_encoder", is_hardware_encoder);
        editor.putInt("hwEncoderKpbs", hwEncoderKpbs);
        editor.putInt("encodeFPS", encodeFPS);

        editor.putInt("sw_video_encoder_profile", sw_video_encoder_profile);
        editor.putInt("sw_video_encoder_speed", sw_video_encoder_speed);

        editor.putBoolean("is_need_local_recorder", is_need_local_recorder);
        editor.putString("recDirFront", recDirFront);
        editor.putString("recDirBack", recDirBack);

        editor.putInt("currentOrigentation", currentOrigentation);

        editor.putString("url1", url1);
        editor.putString("url2", url2);

        editor.putString("userID", userID);

        editor.putBoolean("using_dhcp", using_dhcp);
        if(using_dhcp == false)
        {
            editor.putString("hostIP", hostIP);
            editor.putString("gateIP", gateIP);
            editor.putString("maskIP", maskIP);
        }

        editor.putString("wifiSSID", wifiSSID);
        editor.putString("wifiPassword", wifiPassword);

        editor.putString("destHost", destHost);
        editor.putInt("destPort", destPort);

        editor.putString("configHost", configHost);
        editor.putInt("configPort", configPort);

        editor.putString("machine_name", machine_name);
        editor.commit();
	}

    public String getMac() {
        String macSerial = null;
        String str = "";
        try {
            Process pp = Runtime.getRuntime().exec("cat /sys/class/net/eth0/address ");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);

            for (; null != str; ) {
                str = input.readLine();
                if (str != null) {
                    macSerial = str.trim();// 去空格
                    break;
                }
            }
        } catch (IOException ex) {
            // 赋予默认值
            ex.printStackTrace();

            return "getmacfailed.";
        }

        if (macSerial != null && macSerial.length() > 0)
            macSerial = macSerial.replaceAll(":", "");
        else {
            return "emptymac";
        }

        macSerial = macSerial.toUpperCase();
        return macSerial;
    }
/*
 public string operateServer { get; set; }
        public int operatePort { get; set; }
        public string configServer { get; set; }
        public int configPort { get; set; }
*/
    public String makeJson()
    {
        try {
            JSONObject inf = new JSONObject();
            inf.put("cmd", "configreply");
            inf.put("name", machine_name);
            inf.put("autoResolutionIndex", resolution_index);
            inf.put("width", videoWidth);
            inf.put("height", videoHeight);
            inf.put("encodeHW", is_hardware_encoder);
            inf.put("encodeQuality", sw_video_encoder_profile);
            inf.put("encodeNum", sw_video_encoder_speed);
            inf.put("fps", encodeFPS);
            inf.put("record", is_need_local_recorder);
            inf.put("pushUrlFront", url1);
            inf.put("pushUrlBack", url2);
            inf.put("appVersion", appVersion);

            inf.put("ip", hostIP);
            inf.put("operateServer", destHost);
            inf.put("operatePort", destPort);

            inf.put("configServer", configHost);
            inf.put("configPort", configPort);

            inf.put("wifiSSID", wifiSSID);
            inf.put("wifiPassword", wifiPassword);

            inf.put("dhcp", using_dhcp);
            inf.put("mac", my_mac);
            inf.put("userID", userID);

            return inf.toString();
        }catch (JSONException e)
        {
            e.printStackTrace();
            return "{\"cmd\":\"GameEndData\",\"result\":\"-4\"}";
        }
    }

    public boolean ApplyConfig(String jsonString, Socket ss)
    {
        try {
            JSONObject jsonOBJ = new JSONObject(jsonString);

            if(jsonOBJ.has("name"))
            machine_name =  jsonOBJ.getString("name");

            if(jsonOBJ.has("autoResolutionIndex")) SetResolutionIndex( jsonOBJ.getInt("autoResolutionIndex"));
            if(jsonOBJ.has("width"))  SetVideoWidth( jsonOBJ.getInt("width") );
            if(jsonOBJ.has("height"))  SetVideoHeight(jsonOBJ.getInt("height"));
            if(jsonOBJ.has("encodeHW")) is_hardware_encoder = jsonOBJ.getBoolean("encodeHW");
            if(jsonOBJ.has("encodeQuality")) sw_video_encoder_profile = jsonOBJ.getInt("encodeQuality");
            if(jsonOBJ.has("encodeNum")) sw_video_encoder_speed = jsonOBJ.getInt("encodeNum");
            if(jsonOBJ.has("fps"))  SetFPS(jsonOBJ.getInt("fps")) ;

            if(jsonOBJ.has("record")) is_need_local_recorder = jsonOBJ.getBoolean("record");
            if(jsonOBJ.has("pushUrlFront")) url1 = jsonOBJ.getString("pushUrlFront");
            if(jsonOBJ.has("pushUrlBack")) url2 = jsonOBJ.getString("pushUrlBack");
            if(jsonOBJ.has("ip")) hostIP = jsonOBJ.getString("ip");

            if(jsonOBJ.has("operateServer")) destHost = jsonOBJ.getString("operateServer");
            if(jsonOBJ.has("operatePort")) SetAppPort(jsonOBJ.getInt("operatePort") );

            if(jsonOBJ.has("configServer")) configHost = jsonOBJ.getString("configServer");
            if(jsonOBJ.has("configPort"))  SetConfigPort(jsonOBJ.getInt("configPort"));

            if(jsonOBJ.has("wifiSSID")) wifiSSID = jsonOBJ.getString("wifiSSID");
            if(jsonOBJ.has("wifiPassword")) wifiPassword = jsonOBJ.getString("wifiPassword");

            if(jsonOBJ.has("userID")) userID = jsonOBJ.getString("userID");

            if(jsonOBJ.has("dhcp")) using_dhcp = jsonOBJ.getBoolean("dhcp");

            Message message = Message.obtain();
            message.what = 104;//更新配置到UI
            message.obj = ss;
            if(msgHandler  != null) msgHandler.sendMessage(message);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}