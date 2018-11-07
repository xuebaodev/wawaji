package socks;


import android.content.SharedPreferences;
import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Size;

import com.xuebao.rtmpPush.CameraPublishActivity;
import com.xuebao.rtmpPush.R;

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

    public int appVersion = 20181102;//本app的版本号。用于描述本版本是哪个版本。//不用APKversion是因为不方便回退版本 所以gradle里面的versionCode已经被弃用--modify at 20180202

    //=================changelog
    //20181102 在不使用声音的情况下，不执行CheckInitAudioRecorder.实测某些时候会导致闪退。autofocus添加了try。--未测试
    //20181026 修正一个bug。收到服务器的0x35现在不会再发往串口了。
    //20180911 增加对0x17取mac请求的支持。修改H5推流的rotateYUV90函数，降低gc的频率
    //         两个推流地址一样时或都是空时，不推。
    //20180908 废弃A0推流状态错误报告。统一为0x89. 更新工程编辑器到google最新版3.1.4. 修正H5推流时，插了第二路摄像头，但是不设置url导致的闪退。
    //20180906 修正H5拔掉后置摄像头以后，不停重推的问题。
    //20180905 修改后置摄像头预览失败时的逻辑，不再重启。而是无视
    //20180904 添加了gpio某个脚置高置低的支持。0x93时根据推流状态决定亮灭。初始是亮。
    //20180823 修改了ffmpeg库里面某个av_inter——frame死循环不返回的问题,重编译并使用自己产生的ffmpeg库。 注释ffmpeg_handle关于码率控制方面的一些代码。
    //20180529 将所有逻辑放到程序启动后3秒再做。看看启动闪退的问题能不能解决。废弃了0x3c.usb读文件配置wifi功能现在加了个代码开关。并且默认不启用。SerialPort.java 打开串口的部分 添加了一些健壮性的检查
    //20180528 增加一个详细日志的界面
    //20180517 0x93逻辑修正。使逻辑更严谨。
    //20180516 增加命令0x93 可以在机器长时空闲或无玩家观看时，服务器发送0x93过来使推流停止或降低码率。降低带宽占用。

    //20180514 增加了socket读超时处理，当20秒内收不到任何数据时，发送0x92给服务器，如果3秒内服务器不返回0x92，则socket视为断网(或网线被拔)。尝试重连。

    //20180510 退出时，局域网的7777监听线程现在可以正常退出了。

    //21080503 onDestroy时 现在会关掉串口对象，避免覆盖安装有可能引起串口打开失败的问题。

    //21080429 修改底层serial_port.so的代码，改善串口接收延迟导致的响应延迟。 修改数据收发逻辑，优先透传，主线程按需处理，提高响应速度(此处延迟主要因为sendmessage调度产生)

    //20180427 增加一个开关，release版本减少输出 log.i log.e。在程序退出时 删掉了动态绑定的u盘插入事件receiver 消除警告。

    //20180426 增加码率界面参数 修改一个帧率不起作用的问题。

    //20180421 修改并发布，当接收到串口的心跳0x35为空时，发送mac和本机ip给串口,最多重试三次。

    //20180420 修改串口接收处理函数的一个bug。该bug会导致收到非法的fe包时，清掉所有收到的数据。而设计的逻辑原本是：清掉该fe，寻找下一个fe

    //20180419 收到串口数据时，未处理前，打印到屏幕上。帮助调试有时候串口无反应的情况。

    //20180412
    //不再主动心跳。透传心跳消息

    //20180317
    /*增加 摄像头的调色功能。需要特定的安卓板以及rom支持。
    * */


    //20180313
    /*
    新增功能:
    1.录像。 当u盘存在 且可用空间大于500MB时，会在游戏开始时录像，游戏结束后停止录像。
    2.推流的视频现在可以选择是否包含声音。
    3.当两个摄像头存在时，可以选择只推一路，收到切换命令后，将另一个摄像头的流推往之前的地址。节省一半的带宽。
    新增[命令]:
        0x90 收到时，如果是只推一路模式，则会尝试切到另一个摄像头。
    切流后，安卓版会返回 0x91 index 。其中 index 为1时，表示当前正在推第一路摄像头的数据。为2时，表示正在推第二路摄像头的数据 具体的字段见wiki*/

    //20180306
    //修复串口接收及socket接收的数据长度刚好为9时，不触发通知的bug。原因如下。原先是> 而实际应为>=
    //指令 至少是9位 包长度在第 7位
    //       while (readBuffer.length() >= 9 * 2) {

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

   public boolean pushH5 = false;//false 使用RTMP。 true 使用MPEG 2018.0.25
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

    //编码的码率
    public int encoderKpbs  = 0;//码率.以K为单位

    //编码-帧率
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

    public boolean containAudio = false;//包含声音

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

    public boolean swtichToOne = false;//2018.3.8 增加只推一路，然后根据收到的命令切换摄像头的模式
    public int     curPushWay = 1;//1 前路 2 后路

    public boolean videoPushState_1 = false;
    public boolean videoPushState_2 = false;

    public boolean usingCustomConfig = false;
    //饱和度
    public int staturation  = 0;
    //对比度
    public int contrast = 0;
    //明度
    public int brightness =0;

    //存储默认的值用于恢复默认值
    public int defaultStaturation = 0;
    public int defaultContrast = 0;
    public int defaultBrightness = 0;

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

        pushH5 = share.getBoolean("pushH5", false);

        encodeFPS = share.getInt("encodeFPS", 20);
        encoderKpbs = share.getInt("encoderKpbs", 560);

        sw_video_encoder_profile = share.getInt("sw_video_encoder_profile", 1);
        sw_video_encoder_speed = share.getInt("sw_video_encoder_speed", 2);

        is_need_local_recorder = share.getBoolean("is_need_local_recorder", false);

        currentOrigentation = share.getInt("currentOrigentation", 1);

        //rtmp://119.29.226.242:1935/hls/229031AA7875_1

        my_mac = getMac();
        url1 = share.getString("url1", "");
        url2 = share.getString("url2", "");

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

        swtichToOne = share.getBoolean("swtichToOne", false);

        containAudio = share.getBoolean("containAudio", containAudio);

        //add 2018.03.16
        usingCustomConfig = share.getBoolean("usingCustomConfig", false);
        staturation = share.getInt("staturation", 0);
        contrast = share.getInt("contrast", 0);
        brightness = share.getInt("brightness", 0);

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
        editor.putBoolean("pushH5", pushH5);

        editor.putInt("encoderKpbs", encoderKpbs);
        editor.putInt("encodeFPS", encodeFPS);

        editor.putInt("sw_video_encoder_profile", sw_video_encoder_profile);
        editor.putInt("sw_video_encoder_speed", sw_video_encoder_speed);

        editor.putBoolean("is_need_local_recorder", is_need_local_recorder);

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

        editor.putBoolean("swtichToOne", swtichToOne);
        editor.putBoolean("containAudio", containAudio);

        editor.putBoolean("usingCustomConfig", usingCustomConfig);
        editor.putInt("staturation", staturation);
        editor.putInt("contrast", contrast);
        editor.putInt("brightness", brightness);

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
            inf.put("pushH5", pushH5);
            inf.put("encodeQuality", sw_video_encoder_profile);
            inf.put("encodeNum", sw_video_encoder_speed);
            inf.put("fps", encodeFPS);
            inf.put("record", is_need_local_recorder);
            inf.put("pushUrlFront", url1);
            inf.put("pushUrlBack", url2);
            inf.put("appVersion", appVersion);

            inf.put("encoderKpbs", encoderKpbs);

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

            inf.put("swtichToOne", swtichToOne);

            inf.put("videoPushState_1", videoPushState_1);
            inf.put("videoPushState_2", videoPushState_2);
            inf.put("containAudio", containAudio);

            inf.put("usingCustomConfig", usingCustomConfig);

            inf.put("staturation", staturation);
            inf.put("contrast", contrast);
            inf.put("brightness", brightness);

            inf.put("defaultStaturation", defaultStaturation);
            inf.put("defaultContrast", defaultContrast);
            inf.put("defaultBrightness", defaultBrightness);

            return inf.toString();
        }catch (JSONException e)
        {
            e.printStackTrace();
            return "{\"cmd\":\"makejson\",\"result\":-1}";
        }
    }

    public boolean ApplyConfig(String jsonString, Socket ss)
    {
        try {
            JSONObject jsonOBJ = new JSONObject(jsonString);

            if(jsonOBJ.has("name"))
            machine_name =  jsonOBJ.getString("name");

            Log.e("asdfsadf", machine_name);
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

            if(jsonOBJ.has("pushH5")) pushH5 = jsonOBJ.getBoolean("pushH5");

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

            if( jsonOBJ.has("swtichToOne")) swtichToOne = jsonOBJ.getBoolean("swtichToOne");

            if(jsonOBJ.has("dhcp")) using_dhcp = jsonOBJ.getBoolean("dhcp");
            if(jsonOBJ.has("containAudio")) containAudio = jsonOBJ.getBoolean("containAudio");

            if(jsonOBJ.has("encoderKpbs")) encoderKpbs = jsonOBJ.getInt("encoderKpbs");

            if(jsonOBJ.has("usingCustomConfig")) usingCustomConfig = jsonOBJ.getBoolean("usingCustomConfig");
            if( usingCustomConfig == false)
            {
                staturation = defaultStaturation;
                contrast = defaultContrast;
                brightness = defaultBrightness;
            }else
                {
                    if(jsonOBJ.has("staturation")) staturation = jsonOBJ.getInt("staturation");
                    if(jsonOBJ.has("contrast")) contrast = jsonOBJ.getInt("contrast");
                    if(jsonOBJ.has("brightness")) brightness = jsonOBJ.getInt("brightness");
                }

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