本程序供免费使用，用于测试工业主机推流mpegts。使雪暴网络娃娃机类app能够支持H5 微信 app视频播放。
由于尚在开发完善阶段，因此使用此产品，即视为默认接受有可能出现的各种bug,如发现bug，请及时沟通反馈，我们会测试确认，并视情况决定予是否以修复。请及时查看如下地址，以便获取最新的程序。
项目更新
https://github.com/xuebaodev/wawaji/tree/master/xuebaoH5push

升级安装：
请使用部署工具进行app的更新。该工具位于https://github.com/xuebaodev/wawaji/tree/master/prebuild。
请把xuebaoH5push.zip解压出来的文件夹覆盖掉部署工具底下的xuebaoH5push

或windows命令行输入
<br>pscp -sftp -pw csx -r xuebaoH5push  csx@192.168.1.136:/home/csx/Desktop 将文件拷贝到linux上面。
<br>然后进入xuebaoH5push文件夹，打开终端定位到此目录 输入java pusherApp
<br>pscp在压缩包里面有带。如果要用此方式执行覆盖安装，当然要把pscp复制出来再执行了！不然xuebaoH5push里面的pscp去哪里找xuebaoH5push这个文件夹。
<br>192.168.1.136此处的IP要变成linux真实机器的IP。ip怎么看？输入ifconfig.

H5播放端和服务器端原型请参考这里.我们只是处理了推流端。
https://github.com/phoboslab/jsmpeg


配置说明简要<br>
由于出厂选择内置推流端时，本app已设置为自启动。因此参照安卓板配置工具(现名雪暴通用推流应用局域网配置工具)配置安卓的步骤，给它配置推流地址，操作服务器等等参数即可。要注意的是此处是http格式的推流地址 而非rtmp://开头的格式。
<br>目前局域网配置工具没有提供播放h5的功能，所以播放还请你在浏览器中打开。<br>
配置详情请看
[安卓板的部署配置使用](https://github.com/xuebaodev/wawaji/wiki/%E5%AE%89%E5%8D%93%E7%89%88%E5%A8%83%E5%A8%83%E6%9C%BA%E5%AE%89%E8%A3%85%E9%83%A8%E7%BD%B2%E6%8C%87%E5%8D%97)


补充说明：
如果不选择内置推流端，则怎么设置程序自启动等等一些安装上的问题,以及由此带来的各种功能不正常的问题，您需要自行研究解决。
(建议购买我们硬件的客户，选择内置，因为除了程序，系统层次也需要一些优化)


Change Log:
* 20180612 换串口库。修正每个三分钟都会收到多余的00字节的问题。请慎用JSerial这个库。
* 20180609 初始提交。-已通过长时稳定性测试。绝大部分与安卓版功能都已实现。录像功能未实现。

