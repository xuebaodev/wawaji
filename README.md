**雪暴网络娃娃机安卓版**配套测试工程源码及配置工具

雪暴网络科技是一家提供网络娃娃机实体机器并提供使用该机器开发网络娃娃机app相关技术支持的公司。
我们以卖机器为主，附带提供如何开发，部署的一些简单技术支持。方便您购买机器后，能够知道如何开发并运行自己的网络娃娃机项目。

本项目是雪暴网络娃娃机安卓版使用说明及例子源码。包含简单应用服务器，简单操作客户端，娃娃机操作命令文档，并提供全套的配置工具及最新版的安卓推流端。

<img src="http://chuantu.biz/t6/184/1514017000x-1566638157.png" alt="雪暴网络娃娃机安卓结构示意图" />

1.首先购买雪暴网络娃娃机，内含安卓版推流操纵端。视频摄像头可自行购买。具体型号及要求可咨询我公司。
然后根据要求安装好摄像头并摆好位置。连接网线。这时，如果网线已连接到公网，则该娃娃机已正常工作。

2.配置娃娃机参数。可使用本工程自带的局域网，公网配置软件，或安卓版连接鼠标显示器自行配置推流地址，娃娃机名称 ，视频参数等属性。

3.如何可以开始玩？参照本使用说明，架设好服务器，并将prebuilt里面的对应apk或exe正确运行即可快速看到效果。您也可以使用附带的源码定制自己的网络娃娃机。

4.建议使用者先运行一个局域网版本的快速测试。因为公网的服务器是需要申请，审核的。本文不包含公网流媒体服务器的架设教程。

以下将详细说明雪暴公司网络娃娃机局域网快速搭建及测试运行。及源码如何使用和二次开发所需要点。

**我想要快速看到效果！如何快速的实现网络娃娃机的整套流程搭建？**
<br>局域网仅为了快速看效果需要。部署公网将需要您的技术人员自行购买公网服务安装部署应用服务器及流媒体服务器。
<br>但其原理等同局域网。只是各个客户端的连接ip地址改成该服务器所在的IP地址或域名即可。
<br>**当快速搭建局域网流程时，务必确保手机客户端、服务器、娃娃机都在一个网段，以便于他们能够互相访问。**

首先，您必须有一台雪暴公司的安卓版网络娃娃机，并开机上电。

其次，将prebuilt文件夹中的simpleClient-C.apk或 simpleClient-JAVA.apk安装到您的手机上。这取决于你的技术人员是否会编译运行c或java程序。
<br>确保您手机的系统是安卓5.0以上。更低版本不保证运行成功。--或者，您可以从simpleClient工程中自行编译出一个apk出来。请将该手机通过wifi连入娃娃机所在的一个局域网。

然后，您必须有一台电脑与娃娃机共用一个局域网(只是为了快速看效果)。运行c版本的服务器将需要一个linux系统。java版本的可以无所谓。
linux：
g++ -std=c++11 -pthread ./SimpleServer.c -o SimpleServer

run
./SimpleServer

java:则直接用eclipse 打开工程SimpleServer 点运行即可。

这时候，应用服务器就已经准备就绪。请您的技术人员自行通过常规手段(ipconfig ifconfig)获取到此服务器所在的电脑的IP地址。然后，设定网络娃娃机所连接的应用服务器IP地址端口设定为7770。 
<br>如何设定？前面已说了，你可以接鼠标或显示器到安卓版上直接输入。也可以通过prebuild里的局域网配置工具
<br>这时候，看到应用服务器有接收到网络娃娃机心跳，说明娃娃机已准备就绪。

正常工作如图
<img src="http://chuantu.biz/t6/184/1514019686x-1566638157.png" alt="服务器运行示意图" />



打开你的手机刚安装的apk:SmartPlayerDemo。点击设定按钮，进入设定界面，连接应用服务器IP地址，输入应用服务器的IP，端口7771.点击OK。退出并重启app。
应该会看到一个娃娃机的列表。点击进去就可以开始操作了。
<img src="http://chuantu.biz/t6/184/1514019793x-1404793385.png" alt="客户端运行示意图" />
<br>
<img src="http://chuantu.biz/t6/184/1514019817x-1404793385.png" alt="客户端运行示意图2" />


linux版本的服务器及apk步骤有所不同。但都同样需要配置参数。

娃娃机是动了，为什么看不到视频？
<br>因为您的娃娃机需要配置推流地址。 您需要您的技术人员部署一个流媒体服务器，并设置推流参数为您的流媒体服务器所需要的格式。不管是公网还是局域网。如果是推流到局域网，则必须保证 娃娃机 手机 应用服务器 流媒体服务器都是一个局域网。


工程文件说明

- [x] [**prebuild**]	包含了局域网配置工具 公网配置工具 预生成的安卓简单操作客户端，并包含最新的安卓推流程序。

  simpleClient-C.apk 配合c版本服务运行的客户端

  simpleClient-JAVA.apk 配合java版本服务运行的客户端

  android-push.apk 最新版的安卓推流程序。

  android局域网调参工具

  android外网调参工具   


- [x] [**SimpleClient**] 简单操作客户端-安卓版的源码。您可以通过此源码更好的熟悉娃娃机的操作命令。
<br>或直接在此基础上开发出您特有的网络娃娃机app。
<br>本客户端视频播放使用了** <a href="https://github.com/daniulive/SmarterStreaming" target="_blank">大牛直播SDK</a>**  **如要商用请自己联系，我们不负责版权问题**

- [x] [**SimpleServer**]简单应用服务器java源码。
<br>包含了应用服务器基本的工作流程。比如列举房间列表，玩家进出，开始玩的命令中转到网络娃娃机的基本流程。
<br>您可以自行二次开发实现排队预约，支付等等功能。
<br>应用服务器 （默认端口  对于安卓板android-push.apk是7770，对于玩家手机客户端simpleClient.apk是7771）
<br>配置服务器 （默认端口 对于安卓板android-push.apk是7776，对于公网配置工具是7778）
<br>android-push.apk启动后，会自动注册到配置服务器，外网配置工具可通过配置服务器配置安卓板参数
<br>目前没做用户认证，因此多个用户同时修改一个娃娃机的配置时，只有最后一个配置者的命令会生效。


- [x] [**SimpleServer.c**]	简单应用服务器c版本的源码。该源码配套客户端SimpleClient-C.apk使用

- [x] [**document.docx**]	网络娃娃机相关的命令文档。 例如，按下左右移动操作是发什么命令？下抓是什么命令？报故障时是什么命令？方便您的程序员在不使用以上范例的时候，自行发送命令使娃娃机可以操作。

开放的源码工程可以免费使用，修改及二次开发。雪暴公司对此源码不提供技术支持。
<br>如果在操作娃娃机移动下抓开局的基本命令对接上仍存在困难,欢迎直接前来要求技术指导.
<br>如果有潜在的bug也欢迎反馈。

安卓板刷机工具及固件（推荐使用4.4）
<br>链接：https://pan.baidu.com/s/1mi3ltKs 密码：wu39

其他资料总汇：
<br>链接：https://pan.baidu.com/s/1jIpHpMA 密码：5y3l


SRS流媒体服务器搭建指引
https://github.com/ossrs/srs/wiki/v1_CN_SampleRTMP

## 更多信息 ##

<br>**更多详情请联系QQ：147497411 欢迎直接上门详询。**
<br>
<br>
<img src="http://chuantu.biz/t6/184/1514019817x-1404793385.png" alt="二维码" />




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

<br>3.open your app installed in step 1.Click the black tool icon. Input the server ip and port (default 1090)in the step 2.Press OK.

<br>4.configure the doll machine to connect to server port 1080, and the ip in step 2. When success, you will see the heart beat info from the doll machine.---this is call room.
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