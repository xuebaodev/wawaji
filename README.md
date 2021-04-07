**雪暴网络娃娃机**配套工程、配置工具源码及周边支持配置工具及文档.
This github provide xuebao claw machine streaming app[sourcecode include], tools[sourcecode include], server demo[sourcecode include], client demo[sourcecode include], and documents.

雪暴提供:
* 网络娃娃机整机或单独的控制板。
* 安卓板。摄像头(可选，但自行选购的摄像头引起的闪退或工作不正常，不予负责。请先自行测试是否能用)
* 双摄推流apk负责实现视频推流和协议转发控制娃娃机。支持RTMP视频，H5视频(即mpegts流，用于网页播放)

We supply:
* a claw machine use for network playing or only circuit board.
* an android board ,cameras[option, but if you deside to use your own cameras, make sure they can work by xuebaoRtmpPush.apk]
* streaming application use for streaming videos from cameras. Support RTMP,MPEGTS（for html）

您需要
* 自己开发一个应用服务器来将控制命令下发到娃娃机来实现娃娃机的天车爪子左右前后及下抓。以及支付,房间，玩家信息，库存，邮寄等功能。
* 架设一个流媒体服务器。如果是RTMP视频，可以是nginx,SRS，或直接阿里云腾讯云流媒体服务.如果是H5视频，则自行修改jsmpeg的简单服务器或使用七牛提供的服务。这个服务器用来接收安卓板上面摄像头的视频并分发给进入房间的用户.
* 自己开发一个玩家客户端，包括并不限于app，或者网页版,微信小程序形式(小程序只能播放mpegts格式的视频)。

You need to:
* develop a server use to handle the claw machine message and translate the operation command from player to claw machine.
* run a streaming server to accept the video from cameras and distribute to player client when they enter a room to watch this claw machine. We suggest use SRS or Nginx to do that. Or ,you can buy this service from some network provider directly.
* develop a player client, which can be APP or html web client.


产品特性
* RTMP低延迟。RTMP视频模式下，配合** <a href="https://github.com/daniulive/SmarterStreaming" target="_blank">大牛直播SDK播放端</a>**，将可以使视频延迟保证在一秒以内。
* H5视频天然低延迟（400ms左右）。可使用于网页客户端，微信小程序。缺点是带宽占用较高.
* 配置简单易用.只需要配置应用服务器地址，流媒体推流地址，娃娃机就可以正确联网并推流视频。通过下发给定格式的指令即可支持控制左右前后移动控制指令。
* 例子简单容易上手.通过我们提供的简单客户端（SimpleClient）和服务器(SimpleServer)，您可以快速的了解如何操作雪暴网络娃娃机以及如何开发您的应用服务器。定制您自己的网络娃娃机项目。
* 娃娃机管理和升级简单方便。配套xuebaoRtmpPush公网，局域网，本机，升级，安装，管理的一系列工具软件,该管理软件所使用的协议已经在wiki放出，且该工具的源码都已开放，以便您开发自己的管理软件。

product features
* low delay RTMP video with ** <a href="https://github.com/daniulive/SmarterStreaming" target="_blank">daniu SDK player in android ,ios</a>**
* low delay MPEGTS video ,but much more bandwidth will be needed.
* easy to use. in the streaming app, you need to config only streaming url, app server ip and port and that's all.
* we suply a simple clinet ,server demo , and sourcecode is also proviced . you can run a demo without 10 min.
* eay to manage. we develop some tools to make it easy to upgrade the app, to config the steaming url and so on.And ofcourse sourcecode is also included.


或您也可以只使用雪暴提供的硬件，上面运行其他家的app。
**本安卓板硬件和固件都已支持双路摄像头推流，因此它也支持各种互动直播技术，如腾讯互动直播，阿里连麦，即构，anyRCT的私有视频传输协议。** 
**基于本安卓版的 anyRTC 应用案例http://wawaji.anyrtc.cc/**  
**基于本安卓版的 即构 webH5 应用案例 http://wwj.zego.im/**
**以上方案已完整的对接好我们的整套硬件，提供直接可用的整套产品。包含推流端、服务器端、播放端、操作客户端**

Or you can olny buy our machine ,but not running xuebaoRtmpPush.apk.
** Two Cameras streaming are well supported. So, it fits any common type of livevideo like this company 腾讯互动直播，阿里连麦，即构，anyRCT,and any other private streaming protocol.(but i guess you should immplement yourself.)**
** andyRTC impplement sample http://wawaji.anyrtc.cc/**


您也可以选择自行开发您的推流程序和私有协议，您也可以购买我们的产品，此时:
在您的apk中，您将需要
* 自己采集摄像头的数据，并区分绑定前后摄像头，然后将数据编码并发送给您的(流媒体，或其他协议)服务器。
* 根据网络娃娃机主板发来的命令处理数据并转发给应用服务器。<a href="https://github.com/xuebaodev/wawaji/wiki" target="_blank">最新版文档</a>
* 自己开发配套的协议来标识并管理您的娃娃机。

When you deside to develop your special streaming app and protocal, you can also buy this porduct.When you do that ,you should:
* get the camera data and identify which is front camera which is back camera ,and encode then streaming to your own video server.
* hanle all the data from serial port(which is send from the claw machine ),and transport to you app server, fllowwing this document <a href="https://github.com/xuebaodev/wawaji/wiki" target="_blank">Lastes document</a>
* identify claw machines by yourself.

***
使用本套软硬件的合作公司产品示例.<br>

## <a href="https://github.com/changshenxikf/changshenxiDev" target="_blank">线上线下弹珠机H5版本</a> ##
体验地址:http://mmz.csxtech.com.cn/web_wawa_h5/

<img width="250" height="250" src="https://raw.githubusercontent.com/changshenxikf/changshenxiDev/master/photo/%E8%90%8C%E8%90%8C%E5%BC%B9%E7%90%83h5%E4%BA%8C%E7%BB%B4%E7%A0%81.png "/>

Success Project in commercial using.<br>
## <a href="https://github.com/changshenxikf/changshenxiDev" target="_blank">H5 playing claw machine</a> ##
playing url:http://mmz.csxtech.com.cn/web_wawa_h5/

***

![雪暴网络娃娃机安卓结构示意图](https://raw.githubusercontent.com/xuebaodev/wawaji/master/raw/main_0.jpg)
![雪暴网络娃娃机安卓结构示意图](https://raw.githubusercontent.com/xuebaodev/wawaji/master/raw/all_en.jpg)

<a href="https://github.com/xuebaodev/wawaji/wiki" target="_blank">**点击这里查看最全面的指南Browse WIKI to get more detail**</a>

工程文件说明

* [**xuebaoRtmpPush**] 双路摄像头推流RTMP/mpegts及娃娃机指令交互安卓板源码。其中RTMP推流基于大牛SDK **此摄像头推流程序已获得大牛直播授权使用于本安卓板** ，mpegts(即俗称的H5视频)由七牛提供推流源码
	* this is the core application running on android board.it will streaming 2 cameras videos to the videos server(RTMP or MPEGTS),and translate the net command between server and claw machine.

* [**SimpleClient**] 简单操作客户端-安卓版的源码。
	* SimpleClinet is a simple app to watch video and play claw machine demo. you can understand how the flow works by the sourcecode.or develop your custom app through this source.this rtmp player is supply from ** <a href="https://github.com/daniulive/SmarterStreaming" target="_blank">daniu SDK</a>**  ** and maybe has expired. you should get the lasted free .so from their github to let this app play RTMP video normally.You can contract them if you need this player component.
	* 您可以通过此源码更好的熟悉娃娃机的操作命令。或直接在此基础上开发出您特有的网络娃娃机app。本客户端视频播放使用了** <a href="https://github.com/daniulive/SmarterStreaming" target="_blank">大牛直播SDK</a>**  **如要商用请自己联系，我们不负责版权问题**,您也可以根据自己的需求选择腾讯SDK或者ijkplayer来实现视频流播放。

* [**SimpleServer**]简单应用服务器java源码。
	* 兼容串口版和安卓版的娃娃机协议。
	* 包含了应用服务器基本的工作流程。比如列举房间列表，玩家进出，开始玩的命令中转到网络娃娃机的基本流程。
	* 支持多个玩家，多个娃娃机。但同一时间，一台娃娃机只能由一个玩家操控。
	* 您可以自行二次开发实现排队预约，支付等等功能。
	* 应用服务器 （默认端口:娃娃机请连接到7770 端口。玩家手机客户端SimpleClient.apk请连接到7771）
	* 配置服务器[可选] （默认端口 对于安卓板xuebaoRtmpPush是7776，对于公网配置工具是7778）--如果不用配置功能。这个相关代码都可以忽略。
	* this server is a simple server to handle claw machine message and player(not inlcude video).Something like player login ,start game, move ,claw down .You should config the claw machine to port 7770. and SimplePlayer to 7771.The config server is an option when you config them in the same LAN instead.

* [**xuebaoH5push**] 使用工业主机linux推流mpegts视频的java app，以便H5端也能够玩网络抓娃娃类的游戏。
	* Another java client running on linux to push the video stream in MPEGTS,and translate the message between server and claw machine.Just like xuebaoRtmpPush.apk running on android board.

* [**juyuwangbushu**] 局域网部署工具的源码。该工程编译出来的工具用于批量安装xuebaoRtmpPush.apk.因为我们出厂是不带这个apk的。需要你们自行安装。该工具在prebuild里面有直接编译好的版本。
	* The sourcecode of xuebaoRtmpPush.apk install tool using in same LAN.

* [**juyuwangpeizhi**] 局域网批量配置工具的源码。该工程编译出来的工具可以给xuebaoRtmpPush配置推流地址，推流质量，应用服务器地址等。该工具在prebuild里面有直接编译好的版本。
	* The sourcecode of xuebaoRtmpPush.apk config param tool using in same LAN.

* [**prebuild**] 包含了最新的安卓推流程序, 局域网配置工具 公网配置工具 预生成的安卓简单操作客户端.Prebuild Lasted Apks, tools.
	* PC Install Tool.电脑有线直连安装脚本。When using computer to install xuebaoRtmpPush.apk, you should use this.
	* SimpleClient.apk 手机玩家简单客户端.也可以自行从SimpleClient编译生成。Simple Player Client Demo. you can build it from SimpleClient by yourself.
	* xuebaoRtmpPush.apk 由源码编译出来的apk。您也可以自行从源码生成。 app running on android board. you can build it from xuebaoRtmpPush by yourslef.
	* APKInstallTool--局域网内批量安装xuebaoRtmpPush.apk工具. Install tool of xuebaoRtmpPush.apk. Using this when you need to upgreade your xuebaoRtmpPush.apk.
	* LANConfigTool--搜索局域网内所有机器，并配置参数或更新推流程序。Config tool for xuebaoRtmpPush in LAN. It will search all  machines in LAN and list out to config.
	* 雪暴安卓推流应用外网配置工具--配合服务器端的协议，将可以实现远程配置参数和执行推流程序的更新。Config tool by remote. When you are far away from the claw machine(not in the same place), you can config it's param by remote.However ,you must config the config-server in the first time. and ,a config-server is required. SimpleServer is also immplement a demo for that.


* [**安卓板与娃娃机指令交互说明**] 当您决定不使用xuebaoRtmpPush或者使用第三方私有协议实现推流，文件夹包含了对接娃娃机主板的核心代码。方便您自己实现推流程序.When you desdie to implement Streaming app by yourslef, this is a part sourcecode of serialport use to communicate with claw board.

* [**SimpleServer.c**]	简单应用服务器c版本的源码。仅支持一娃娃机一玩家的简单转发，主要是为了演示核心逻辑。deprecate server written in c.Not support for the lasted SimpleClient.apk.

* [**server.go**]	简单应用服务器go版本源码。支持websocket操作。A server to handle webclient player through Websocket and claw machine.

* [**claw machine document.docx**]	网络娃娃机相关的命令文档。 A document for the claw board command.But we are highly suggest you to watch WIKI.
<br><a href="https://github.com/xuebaodev/wawaji/wiki" target="_blank">**更加详细请看WIKI**</a>

以下是雪暴网络娃娃机安卓版部署流程: <br>
* 1) 先配好视频服务器。
* 2) 开启应用服务器.
* 3) 插好摄像头，接好娃娃机和安卓板的线路。通电。(如在测试阶段，可以插一个显示器到安卓上，查看具体情况)
* 4) 使用配置工具将娃娃机的推流地址和应用服务器地址配好。
* 5) 玩家登录到应用服务器，开始操作娃娃机。

Install Step:
* 1) start video streaming server。(RTMP:SRS,Nginx,etc. MPEGTS: websocket-relay.js...)
* 2) start apllication server。
* 3) plug cameras to android board and poweron.(if it's the first time you buy our machine ,we highly recommend you plug a display on android board to see the software running state.This will help you to slove many problem ,like: is it connected to network? is the camera work right? is the claw machine data ok? etc...watch the log rolling on the screen)
* 4) use the config tool to config streaming url and application server ip(Domain is also supported) and port.
* 5) use SimpleClient.apk or your app to connected to the application server to start play.

详细如下(this is the detail step)<br>
1.首先购买雪暴网络娃娃机安卓版套件。然后根据要求安装好摄像头并摆好位置。连接网线，开机上电。(可行性评估阶段也可以要求雪暴先行提供工厂测试机。我们会将机器连接到您的应用服务器上)

1.First of all ,you need to buy one claw machine.and plugin cameras and put it in the right place.Plugin netting twine.Power on.(if you to test feasibility, we also support a machine to connect to your application server. )

2.Run SimpleServer in Java version or server.go if you have develop your server, ignore this step,and start your server now.

* windows下安装java环境变量。直接用eclipse 打开工程SimpleServer 点运行即可。 Install java environment,Setup Path variable in system variable.Open eclipse and import project SimpleServer. then press run.
* linux请先安装配置好环境变量 如ubuntu 或linuxmint。先执行sudo vim /etc/profile 最后一行加上如下字段。--请以您自己下载的路径为准。本例只是给个说明
* if you are using linux ,you should install java environment and edit path environment.here is a sample of ubuntu with jdk1.8.0_162.(notice that you should write in your real path.i am extract at /opt/jdk1.8.0_162, but not you.)

```
JAVA_HOME=/opt/jdk1.8.0_162
CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar:
PATH=$PATH:$JAVA_HOME/bin
export PATH JAVA_HOME CLASSPATH

```
然后
<br>切换到SimpleServer/src 输入：javac SimpleApp.java. [cd SimpleServer/src. type javac SimpleApp.java]
<br>再输入:java SimpleApp [type java SimpleApp]
<br>停止服务器。请输入exit.[how to stop the server？ type exit in terminal console.]


3.配置娃娃机参数。可使用本工程自带的局域网，公网配置软件，或安卓版连接鼠标显示器自行配置应用服务器地址、推流视频质量，及推流地址，娃娃机名称等属性。
(使用SimpleServer做服务器时，应用服务器端口设置为7770, 配置服务器端口设置为7776)

3.use config tool to config xuebaoRtmpPush param,or you can plug mouse , keborad , display in android board to config(this is ridiculous when you have so many machines to config.) When using SimpleServer , please config app server to 7770. and simpleClinet.apk to 7771

此时，如果在应用服务器看到娃娃机有心跳连接。则表示娃娃机已准备就绪。
if you see the log print below picture,then the claw machine is ready.

正常工作如图
![服务器运行示意图1](https://raw.githubusercontent.com/xuebaodev/wawaji/master/raw/server_0.png)

4.将prebuild文件夹下的SimpleClient.apk(安卓4.4以上)安装到您的手机上。 或者，您可以从SimpleClient工程中自行编译出一个apk出来。请将该手机通过wifi连入娃娃机所在的一个局域网。
4.install SimpleClient.apk to your android phone. Or you can build it from sourcecode. If you are testing in LAN, before you start apk, please make sure your phone and claw machine are in the same LAN.

<br>然后点开配置按钮(右上角齿轮状图标)，输入应用服务器IP，端口默认为7771 。如您已架设流媒体服务器，请输入之前娃娃机配好的推流地址。点击OK，您就可以开始玩网络抓娃娃了。
<br>In SimpleClient.apk .press config button in right top conner, enter the app server ip, and port 7771. if your video server is ready, you can input the view streamurl below. and press ok.Restart the app. When see a room, you can enter and free to play.

<br>下图是simpleClinet.apk的简单界面示意图。this is a picture about simpleClinet.apk.
<br>![客户端运行示意图](https://raw.githubusercontent.com/xuebaodev/wawaji/master/raw/client_0.png)![客户端运行示意图2](https://raw.githubusercontent.com/xuebaodev/wawaji/master/raw/client_1.png)

友情提示:建议使用者先运行一个局域网版本的快速测试。因为公网的服务器是需要申请，审核的。部署公网将需要您的技术人员自行购买公网服务安装部署应用服务器及流媒体服务器。
Note: You should run a quick test in LAN.Because the internet server need to buy.
<br>**当快速搭建局域网流程时，务必确保手机客户端、服务器、娃娃机都在一个网段，以便于他们能够互相访问。**
<br>**When running in LAN, MAKE SURE SimpleClient, SimpleServer,Video Server,  Claw Machine are in the same LAN.**
<br>**局域网仅为了快速看效果需要。部署到公网时只是各个客户端的连接ip地址改成该服务器所在的IP地址或域名即可。**
<br>**LAN test is for your quick to know what is needed. When Running on internet, simplely change the xuebaoRtmpPush's app server ip, streaming url and SimpleClient's server ip,and everything is done.**

常见问题：Common Question:
娃娃机是动了，为什么看不到视频？Claw machine can move when press button on SimpleClient, But why the video is blank?
<br>因为您的娃娃机需要配置推流地址。 您需要您的技术人员部署一个流媒体服务器，并设置推流参数为您的流媒体服务器所需要的格式。simpleClinet也需要设置相应的视频推流地址。
<br>Because your xuebaoRtmpPush's streaming url is not pushing video to your streaming server. Or your SimpleClient isn't input the right url to watch.
<br>And, properly, your free version of daniu play sdk is out of date.

版权声明.License<br>
<br>源码工程xuebaoRtmpPush则可以自由修改，但只限运行于雪暴公司提供的安卓板。**应用的名字不能改动，因为是和推流模块授权绑定的。**
<br>sourcecode is free to rewrite modify ,but only running on android board we sell.**Don't change the app name, because the RTMP push license is bind with name.**
此双路推流程序xuebaoRtmpPush由大牛直播授权使用于雪暴公司出品的安卓板。任何人不得将其使用于雪暴提供的安卓板以外的任何其他地方。否则一经发现，雪暴公司和<a href="https://github.com/daniulive/SmarterStreaming" target="_blank">大牛直播</a>将有权起诉其侵权。
<br>源码工程SimpleClient SimpleServer可以免费使用，修改及二次开发。雪暴公司对此源码不提供技术支持。
<br>SimpleClient SimpleServer is MIT.
<br>SimpleClient中的视频播放器由大牛直播sdk提供。如果要商用，请联系它给予正式版的授权。您也可以根据自身团队情况去选择其他播放器端。比如腾讯SDK，开源的IJKPLAYER等等支持解码RTMP视频流的sdk都可以。例子中的播放器授权可能会过期。请自行去大牛的github获取更新。
<br>the rtmp player moudle in SimpleClient is provide from daniu SDK.it will be unavailable when out of date,so you can choose other player, but ,for now ,it's much quicker than other player.If you have enought technique, you can optimize other opensource player to make the same effect.

<br>注意事项 Note:
<br>**指令不能太频繁。不建议一秒往娃娃机发送10个以上的指令。**
<br>**Don't send 10 command(not 10 byte.but 10 command is about 512 byte) to claw machine in one second.**

## 更多信息 more info ##
**<a href="http://www.xuebao-game.com/" target="_blank">Vist web site to know more:http://www.xuebao-game.com/</a>**
**<a href="http://www.xuebao-game.com/" target="_blank">广州雪暴电子科技官方网站:http://www.xuebao-game.com/</a>**

<br>**如感兴趣欢迎来厂洽谈合作了解详情。联系人：何生 13380085941**

微信连接
https://mp.weixin.qq.com/s/UlOTFWT5IfIxEI3h_PaRNQ

![二维码](https://raw.githubusercontent.com/xuebaodev/wawaji/master/raw/QRScan.jpg)

=====================How to build============
<br>Android Studio 3.1.4 or higher
<br>Android SDK 27 or higher. 
<br>Gradle 4.4

<br>===================common question========================
how to control the claw machine from the app?
<br>1.build the simpleClinet. then run it in your android phone. Android 5.0 or higher.  

<br>2.build ther server and run it.(this server is running on Internet, otherwise your app mobile must be in the same network LAN with this server.)
<br>-----in terminal :ifconfig to get your server ip.

<br>3.start your app installed in step 1.Click the black tool icon. Input the server ip and port (default 7771)in the step 2, and input streaming url.Press OK.

<br>4.configure the claw machine to connect to server port 7770, and the ip in step 
<br>2. When success, you will see the heart beat info from the claw machine.---this is call room.
<br>(How to configure the claw machine's ip?See other document or ask the tech support people to do this)

<br>5.you can start play by click green button.

<br>1.Can't see heartbeat from the claw machine?
<br>
<br>Make sure your claw machine's ip is connect to your server. That would happend when the ip is conflict, if your configure is absolutly right.(Make sure the LAN line is connect to the route too!)
<br>
<br>2.I can see the headbeat from the claw machine ,but ,when click start game ,nothing happend?
<br>
<br>Check the server output info.It will display something when your app is successed connect to the server.
When you see your operation data is print out in the server screen from the app, that app is ok.It may be claw machine's fault.
However ,when the claw is manage to connect to the sever ,you can aslo see the debug info from the server too.
<br>
<br>3.The video stream is black.
<br>
<br>Hmmm.....Make sure your camera stream is push to the stream server(on Internet or same LAN with app) and your app is input the right stream url.Require restart SimpleClient after change the video url.
<br>(How to setup a stream server? https://github.com/ossrs/srs/wiki/v1_CN_SampleRTMP here is an opensource server Called SRS. Just google it!)
<br>
<br>4.OK,now everything works. but the video is in highly delay?
<br>
<br>Well, you should find quicker solution by yourself. We are using https://github.com/daniulive/SmarterStreaming for a test. 
<br>And if you like this, contract them by yourself.By the way ,Your app's name should be SmartPlayerSDKDemo when using their free version.