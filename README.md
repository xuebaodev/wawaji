**雪暴网络娃娃机安卓版**配套测试工程源码及配置工具    <a href="https://github.com/xuebaodev/wawaji/wiki" target="_blank">**点击这里查看本项目的WIKI文档**</a>

雪暴网络科技是一家提供网络娃娃机实体机器并提供使用该机器开发网络娃娃机app相关技术支持的公司。
我们以卖机器为主，附带提供如何开发，部署的一些简单技术支持。方便您购买机器后，能够知道如何开发并运行自己的网络娃娃机项目。


使用雪暴娃娃机的安卓版套装，您可以方便的开发支持双路摄像头推流的安卓软件,配合娃娃机实现视频和操作的反馈。
<br>或:
<br>
1.通过我们提供的RTMPPush.apk快速拥有一个完整的支持双路视频RTMP推流并使娃娃机联网的解决方案。配合** <a href="https://github.com/daniulive/SmarterStreaming" target="_blank">大牛直播SDK播放端</a>**，将可以使视频延迟保证在一秒左右。
* 只需要配置相应的视频服务器地址，应用服务器地址，流媒体服务器地址，娃娃机就可以正确联网并推流视频。
* 通过我们提供的简单客户端（SimpleClient）和服务器(SimpleServer)，您可以快速的了解如何操作雪暴网络娃娃机以及如何开发您的应用服务器。定制您自己的网络娃娃机项目。
* 配套RTMPPush公网，局域网，本机，升级，安装，管理的一系列工具软件,该管理软件所使用的协议稍后将在wiki放出，以便您开发自己的管理软件。
* 此双路推流程序RTMPPush由大牛直播提供三个月的试用[从2018.1.1开始计算]，如果要商用，请向大牛直播申请授权。然后您将自己负责授权版本的代码更新。

2.**本安卓板硬件和固件都已支持双路摄像头推流，因此它也支持各种互动直播技术，如腾讯互动直播，阿里连麦，即构，anyRCT的私有视频传输协议。** 
<br> **基于本安卓版的 anyRTC 应用案例http://wawaji.anyrtc.cc/**  
<br> **基于本安卓版的 即构 webH5 应用案例 http://wwj.zego.im/**
**以上方案已完整的对接好我们的整套硬件，提供直接可用的整套产品。包含推流端、服务器端、播放端、操作客户端**
<br>您也可以选择自行开发您的推流程序和私有协议，此时您也可以购买我们的产品，此时:
<br>在您的apk中，您将需要
* 自己采集摄像头的数据，并区分绑定前后摄像头，然后将数据编码并发送给您的(流媒体，或其他协议)服务器。
* 根据网络娃娃机主板发来的命令处理数据并转发给应用服务器。该文档对应[**网络娃娃机主板对接协议.docx**].更详细的在wiki页面提供。


您还将需要
* 自己开发配套的协议来标识并管理您的娃娃机。

***

本项目是雪暴网络娃娃机安卓版使用说明及例子源码。包含RTMP推流端程序及配套的配置工具软件，简单应用服务器示例源码，简单操作客户端示例源码，娃娃机协议文档。

<img src="http://chuantu.biz/t6/195/1514962766x-1404793057.png" alt="雪暴网络娃娃机安卓结构示意图" />


以下是雪暴网络娃娃机安卓版部署流程:

1.首先购买雪暴网络娃娃机安卓版套装。然后根据要求安装好摄像头并摆好位置。连接网线，开机上电。

2.使用prebuild中的RTMPPush文件夹中的安装脚本将RTMPPush.apk安装到安卓板上,或者您自己从源码编译安装，这时，如果网线已连接到路由器，则该娃娃机已正常工作。
<br>--此APK提供标准RTMP双路视频推流，及处理娃娃机命令等操作。如选择自己的私有协议，则此步骤必须跳过。

3.运行SimpleServer程序-即开启应用服务器，记录好服务器此时的IP。--如果您已熟知娃娃机的协议，可以忽略本例子代码自行开发您的应用服务器。

```
linux：
g++ -std=c++11 -pthread ./SimpleServer.c -o SimpleServer

run
./SimpleServer
```

<br>java:则直接用eclipse 打开工程SimpleServer 点运行即可。

4.配置娃娃机参数。可使用本工程自带的局域网，公网配置软件，或安卓版连接鼠标显示器自行配置应用服务器地址、推流视频质量，及推流地址，娃娃机名称等属性。
(使用SimpleServer做服务器时，应用服务器端口设置为7770, 配置服务器端口设置为7776)

5.此时，如果在应用服务器看到娃娃机有心跳连接。则表示娃娃机已准备就绪。

正常工作如图
<img src="http://chuantu.biz/t6/184/1514019686x-1566638157.png" alt="服务器运行示意图" />

6.将prebuild文件夹下的simpleClient-C.apk或simpleClinet-JAVA.apk安装到您的手机上(安卓5.0以上)。 这取决于之前您安装的SimpleServer版本。--或者，您可以从simpleClient工程中自行编译出一个apk出来。请将该手机通过wifi连入娃娃机所在的一个局域网。
<br>然后点开配置按钮(齿轮状图标)，输入应用服务器IP，端口默认为7771 。如您已架设流媒体服务器，请输入之前娃娃机配好的推流地址。点击OK，您就可以开始玩网络抓娃娃了。
<br>下图是simpleClinet-JAVA.apk的简单界面示意图。 C版本的有所不同。但都必须要配置好参数。

<img src="http://chuantu.biz/t6/184/1514020688x-1404793385.png" alt="客户端运行示意图" />
<br>
<img src="http://chuantu.biz/t6/184/1514020706x-1404793385.png" alt="客户端运行示意图2" />

友情提示:建议使用者先运行一个局域网版本的快速测试。因为公网的服务器是需要申请，审核的。部署公网将需要您的技术人员自行购买公网服务安装部署应用服务器及流媒体服务器。
<br>**当快速搭建局域网流程时，务必确保手机客户端、服务器、娃娃机都在一个网段，以便于他们能够互相访问。**
<br>**局域网仅为了快速看效果需要。部署到公网时只是各个客户端的连接ip地址改成该服务器所在的IP地址或域名即可。**

常见问题：
娃娃机是动了，为什么看不到视频？
<br>因为您的娃娃机需要配置推流地址。 您需要您的技术人员部署一个流媒体服务器，并设置推流参数为您的流媒体服务器所需要的格式。simpleClinet也需要设置相应的视频推流地址。


工程文件说明

* [**RTMPPush**] 基于大牛SDK双路RTMP摄像头推流及娃娃机指令交互安卓板源码。 此源码包含的RTMP双路推流授权有效期仅为3个月。如果决定采用大牛双路推流，请自行联系授权。

* [**SimpleClient**] 简单操作客户端-安卓版的源码。
	* 您可以通过此源码更好的熟悉娃娃机的操作命令。
	* 或直接在此基础上开发出您特有的网络娃娃机app。
	* 本客户端视频播放使用了** <a href="https://github.com/daniulive/SmarterStreaming" target="_blank">大牛直播SDK</a>**  **如要商用请自己联系，我们不负责版权问题**

* [**SimpleServer**]简单应用服务器java源码。
	* 包含了应用服务器基本的工作流程。比如列举房间列表，玩家进出，开始玩的命令中转到网络娃娃机的基本流程。
	* 您可以自行二次开发实现排队预约，支付等等功能。
	* 应用服务器 （默认端口  对于安卓板xuebaoPush.apk是7770，对于玩家手机客户端simpleClient.apk是7771）
	* 配置服务器 （默认端口 对于安卓板xuebaoPush.apk是7776，对于公网配置工具是7778）
	* RTMPPush.apk启动后，会自动注册到配置服务器，外网配置工具可通过配置服务器配置安卓板参数
	* 目前没做用户认证，因此多个用户同时修改一个娃娃机的配置时，只有最后一个配置者的命令会生效。

* [**prebuild**] 包含了最新的安卓推流程序, 局域网配置工具 公网配置工具 预生成的安卓简单操作客户端.
	* 安卓版电脑有线部署工具，电脑有线直连安装脚本。
	* 雪暴安卓板局域网批量部署工具--局域网内批量安装xuebaoPush.apk工具
	* 雪暴安卓推流应用局域网配置工具--搜索局域网内所有机器，并配置参数或更新推流程序。
	* 雪暴安卓推流应用外网配置工具--配合服务器端的协议，将可以实现远程配置参数和执行推流程序的更新。
	* simpleClient-C.apk 配合c版本服务运行的客户端
	* simpleClient-JAVA.apk 配合java版本服务运行的客户端

* [**安卓板与娃娃机指令交互说明**] 当您决定不使用RTMPPush或者使用第三方私有协议实现推流，文件夹包含了对接娃娃机主板的核心代码。方便您自己实现推流程序

* [**SimpleServer.c**]	简单应用服务器c版本的源码。该源码配套客户端SimpleClient-C.apk使用

* [**网络娃娃机主板对接协议.docx**]	网络娃娃机相关的命令文档。 例如，按下左右移动操作是发什么命令？下抓是什么命令？报故障时是什么命令？方便您的程序员在不使用以上范例的时候，自行发送命令使娃娃机可以操作。

开放的源码工程可以免费使用，修改及二次开发。雪暴公司对此源码不提供技术支持(会有少许的bug更新修复)。商用则需要同大牛直播协商版权问题。
<br>如果在操作娃娃机移动下抓开局的基本命令对接上仍存在困难,欢迎直接前来要求技术指导.
<br>如果有潜在的bug也欢迎反馈。

安卓板刷机工具及固件（推荐使用4.4）
<br>链接：https://pan.baidu.com/s/1mi3ltKs 密码：wu39

其他资料总汇：
<br>链接：https://pan.baidu.com/s/1jIpHpMA 密码：5y3l


SRS流媒体服务器搭建指引
https://github.com/ossrs/srs/wiki/v1_CN_SampleRTMP

## 更多信息 ##

**<a href="http://www.xuebao-game.com/" target="_blank">广州雪暴电子科技官方网站:http://www.xuebao-game.com/</a>**

<br>**如感兴趣欢迎来厂洽谈合作了解详情。联系人：李生 13926265855**

微信连接
https://mp.weixin.qq.com/s/UlOTFWT5IfIxEI3h_PaRNQ


<br>
<br>
<img src="http://chuantu.biz/t6/184/1514020722x-1404793385.jpg" alt="二维码" />




=====================Code Explain============
<br>1.SimpleClient Main Code Location 
<br>SimpleClient\app\src\main\java\com\daniulive\smartplayer。from line 396
<br>socket send class is SendThread.java

<br>to build app)
<br>Android Studio 2.3 or higher
<br>Android SDK 25 is required.--this is decide by you.lower or higher is not care. change the build.gradle by yourself. 
<br>Gradle 3.3


<br>SimpleServer.c is a simple translate server running on linux like linuxmint and ubuntu.

<br>to build server SimpleServer.c)
g++ -std=c++11 -pthread ./SimpleServer.c -o SimpleServer

<br>run
<br>./SimpleServer


how to control the doll machine from the app?
<br>1.build the app. then run it in your android phone. Android 5.0 or higher.  

<br>2.build ther server and run it.(this server is running on Internet, otherwise your app mobile must be in the same network LAN with this server.)
<br>-----in terminal :ifconfig to get your server ip.

<br>3.open your app installed in step 1.Click the black tool icon. Input the server ip and port (default 7771)in the step 2.Press OK.

<br>4.configure the doll machine to connect to server port 7770, and the ip in step 
<br>2. When success, you will see the heart beat info from the doll machine.---this is call room.
<br>(How to configure the doll machine's ip?See other document or ask the tech support people to do this)

<br>5.you can start play by click green button.


<br>===================question========================
<br>1.Can't see heart beat from the doll machine?
<br>
<br>Make sure your doll machine's ip is connect to your server. That would happend when the ip is conflict, if your configure is absolutly right.(Make sure the LAN line is connect to the route too!)
<br>
<br>2.I can see the headbeat from the doll machine ,but ,when click start game ,nothing happend?
<br>
<br>Check the server output info.It will display something when your app is successed connect to the server.
When you see your operation data is print out in the server screen from the app, that app is ok.It may be doll machine's fault.
However ,when the doll is manage to connect to the sever ,you can aslo see the debug info from the server too.
<br>
<br>3.The video stream is black.
<br>
<br>Hmmm.....Make sure your camera stream is push to the stream server(on Internet or same LAN with app) and your app is input the right stream url.Require restart app after change the video url.
<br>(How to setup a stream server? https://github.com/ossrs/srs/wiki/v1_CN_SampleRTMP here is an opensource server Called SRS. Just google it!)
<br>
<br>4.OK,now everything works. but the video is in highly delay?
<br>
<br>Well, you should find quicker solution by yourself. We are using https://github.com/daniulive/SmarterStreaming for a test. 
<br>And if you like this, contract them by yourself.By the way ,Your app's name should be SmartPlayerSDKDemo when using their free version.