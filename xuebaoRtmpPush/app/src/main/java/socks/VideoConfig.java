package socks;


import android.content.SharedPreferences;
import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Size;

import com.xuebao.rtmpPush.CameraPublishActivity;

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

    public int appVersion = 20180225;//本app的版本号。用于描述本版本是哪个版本。//不用APKversion是因为不方便回退版本 所以gradle里面的versionCode已经被弃用--modify at 20180202


    //=================changelog
    //20180225
    //远程配置逻辑修改:

    //修改：
    // 原先连接应用服务器成功，会将应用服务器IP保存到娃娃机，现在改成：远程配置功能变更时，则保存。

    //增加：
    //增加是否启用配置服务器的checkbox,当不启用配置服务器时，保存到娃娃机的就是空 和 0
    //增加协议：0x89 用于检测到推流故障时，通知服务器，本安卓板即将重启。

    //20180204
    //0x3c命令现在改成了配置服务器的地址。
    //获取到IP以后立刻检查娃娃机是否就绪，并给他设置参数。如果已就绪，则开始连接应用服务器
    //透传所有的0x34
    //一些遗漏的逻辑修修补补

    //20180203
    //switch case里面的魔数现在更改成枚举的方式以使程序更具可读性和扩展性。--需经过全面测试查看是否遗漏,或者改错。
    //重启系统前 会优雅的关掉应用服务器的连接防止它一脸懵逼的记录各种超时错误日志。当然你不处理关闭的socket我也没办法。
    //逻辑变更为等待IP后开始配置线程，开始检查时间。

   /*20180202
    健壮性修改和bug修复措施。

    修改：
    appversion现在不再读取gradle配置的应用版本。而是采取版本发行日期的版本号为准。因为采取应用版本时，不方便紧急情况下回退旧版本。
    打开串口函数：现在会打开，关闭，再打开。以便解决安卓板串口有时候无法正常收发数据的bug。
    摄像头的预览现在会在系统获取时间后几秒内才会开始。以便解决安卓板因为时间获取功能导致摄像头预览卡顿引发推流失败的bug。
    预览开始后，5秒钟循环检查是否预览已丢失(当摄像头失去连接时会产生这种状况)，如丢失，屏蔽开局指令。查询娃娃机状态，如空闲直接重启。否则等待玩家玩完本局，重启。

    增加：
    增加协议FE 00 00 01 FF FF 09 88 2D .安卓板收到此命令时，会重启安卓系统。
    程序现在会在开始时，检查娃娃机是否就绪。因为偶尔会有娃娃机自检比安卓板慢的情况。如就绪，才会开始连接应用并心跳。否则间隔1秒去发送0X34等待娃娃机就绪。
    这种情况是为了防止应用连接服务器后，娃娃机其实还未就绪时，导致玩家第一时间进入此机器后无法开局的问题。*/

    //分辨率
    int resolution_index = 0;
    public void SetResolutionIndex(int newIndex)
    {
        resolution_index = newIndex;
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
    }
    public int GetResolutionIndex(){ return resolution_index; }

    int videoWidth = 640;
    public void SetVideoWidth(int w)
    {
        videoWidth = w;
    }
    public int GetVideoWidth(){return videoWidth;}

    int videoHeight = 360;
    public void SetVideoHeight(int h)
    {
        videoHeight = h;
    }
    public int GetVideoHeight(){return videoHeight;}

    public void RestoreLastVideoSizeAndIndex(Context context)
    {
        SharedPreferences share = context.getSharedPreferences("pushConfig", Context.MODE_PRIVATE);
        resolution_index = share.getInt("resolution_index", 1);
        if (resolution_index != -1)
            SetResolutionIndex(resolution_index);
        else {
            videoWidth = share.getInt("videoWidth", 640);
            videoHeight = share.getInt("videoHight", 480);
        }
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
    public boolean enableConfigServer = false;//使用启用配置服务器

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

    public boolean videoPushState_1 = false;
    public boolean videoPushState_2 = false;
    public void LoadConfig(Context context, Handler hh)
	{
        msgHandler = hh;

        SharedPreferences share = context.getSharedPreferences("pushConfig", Context.MODE_PRIVATE);

        resolution_index = share.getInt("resolution_index", 1);
        if (resolution_index != -1)
            SetResolutionIndex(resolution_index);
        else {
            videoWidth = share.getInt("videoWidth", 640);
            videoHeight = share.getInt("videoHight", 480);
        }

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

        configHost = share.getString("configHost", "");
        configPort = share.getInt("configPort", 0);
        enableConfigServer = share.getBoolean("enableConfigServer", false);

        userID = share.getString("userID", "xuebao");

        machine_name = share.getString("machine_name", "可爱小白兔");
	}

    public void SaveConfig(Context context)
	{
        SharedPreferences share = context.getSharedPreferences("pushConfig", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = share.edit();

        editor.putInt("resolution_index", resolution_index);
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
        editor.putInt("videoWidth", videoWidth);
        editor.putInt("videoHight", videoHeight);
    }

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
        editor.putBoolean("enableConfigServer", enableConfigServer);

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

            inf.put("enableConfigServer", enableConfigServer);
            inf.put("configServer", configHost);
            inf.put("configPort", configPort);

            inf.put("wifiSSID", wifiSSID);
            inf.put("wifiPassword", wifiPassword);

            inf.put("dhcp", using_dhcp);
            inf.put("mac", my_mac);
            inf.put("userID", userID);

            inf.put("videoPushState_1", videoPushState_1);
            inf.put("videoPushState_2", videoPushState_2);

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
            if(resolution_index == -1)
            {
                if(jsonOBJ.has("width"))  SetVideoWidth( jsonOBJ.getInt("width") );
                if(jsonOBJ.has("height"))  SetVideoHeight(jsonOBJ.getInt("height"));
            }

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

            if(jsonOBJ.has("enableConfigServer")) enableConfigServer = jsonOBJ.getBoolean("enableConfigServer");
            if(jsonOBJ.has("configServer")) configHost = jsonOBJ.getString("configServer");
            if(jsonOBJ.has("configPort"))  SetConfigPort(jsonOBJ.getInt("configPort"));

            if(jsonOBJ.has("wifiSSID")) wifiSSID = jsonOBJ.getString("wifiSSID");
            if(jsonOBJ.has("wifiPassword")) wifiPassword = jsonOBJ.getString("wifiPassword");

            if(jsonOBJ.has("userID")) userID = jsonOBJ.getString("userID");

            if(jsonOBJ.has("dhcp")) using_dhcp = jsonOBJ.getBoolean("dhcp");

            Message message = Message.obtain();
            message.what = CameraPublishActivity.MessageType.msgConfigData.ordinal();
            message.obj = ss;
            if(msgHandler  != null) msgHandler.sendMessage(message);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}