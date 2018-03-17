using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;

using System.Net.Sockets;
using System.Net;


using Newtonsoft.Json;
using System.Collections.ObjectModel;

using System.Runtime.InteropServices;
using NT;
using System.Windows.Interop;
using System.Windows.Forms;
using System.Drawing;
using MessageBox = System.Windows.MessageBox;
using System.IO;
using System.ComponentModel;

namespace WpfApp1
{
    /// <summary>
    /// MainWindow.xaml 的交互逻辑
    /// </summary>
    public partial class MainWindow : Window
    {

        ObservableCollection<AndroidClient> androidClientList = new ObservableCollection<AndroidClient>();

        //Dictionary<string, Socket> ipToSocket = new Dictionary<string, Socket>();

        AndroidClient selectAndroidClient = new AndroidClient();
        string selectIP;
        Socket selectSocket;
        //bool isRefound = false;

        //string playUrl;
        int playNowNum = 1;
        int playUrlMode = 0;
        string playUrlNow_1;
        string playUrlNow_2;

        bool isFinding = false;

        //进行更新UI的委托
        public delegate void UpdateUIDelegate(AndroidClient ableAndroidClient);

        public delegate void UpdateFindButton(bool isable);

        List<Thread> threadList = new List<Thread>();

        string localIp = "192.168.1.1";
        //Console.WriteLine(localIp);
        string ipPre = "192.168.1.1";


        private GridViewColumnHeader listViewSortCol = null;
        private SortAdorner listViewSortAdorner = null;







        //[] arEvents = new AutoResetEvent[256]; // 同步对象

        public MainWindow()
        {
            InitializeComponent();

        //    this.MainWindow_Closing;
            this.Closing += MainWindow_Closing;

            InitDaniu();


            savaApplyButton.IsEnabled = false;
            btn_play.IsEnabled = false;
            swithCamera.IsEnabled = false;
            // playModeRadio_ali.IsChecked = true;
            playModeRadio_same.IsChecked = true;
            RadioButton_Checked(null, null);

            localIp = GetLocalIP();
            ipPre = localIp.Substring(0, localIp.LastIndexOf('.') + 1);
        }

        //private void MainWindow_Closing1(object sender, System.ComponentModel.CancelEventArgs e)
        //{
        //    throw new NotImplementedException();
        //}

        private void Button_Click(object sender, RoutedEventArgs e)
        {
            if (isFinding)
                return;
            findButton.Content = "正在查找...";
            findButton.IsEnabled = false;
            isFinding = true;
            
            //if (threadList.Count > 0)
            //{
            //    isRefound = true;
            //}
            killAllFindThread();
            resetClinet();



            new Thread(() =>
            {
                for (int i = 0; i <= 255; i++)
                {
                    string para = ipPre + i.ToString();
                    //CallWithTimeout(findConnect, 3);
                    try
                    {
                        Thread t1 = new Thread(new ParameterizedThreadStart(findConnect));
                        t1.IsBackground = true;
                        threadList.Add(t1);
                        t1.Start(para);
                        t1.Priority = ThreadPriority.Normal;
                    }
                    catch (ThreadAbortException ex)
                    {
                        //不作处理          
                    }
                    catch (Exception ex)
                    {
                        //处理
                    }
                }
            }).Start();


            //if (!isRefound)
            //{
             //   killAllFindThread();
                listView.ItemsSource = androidClientList;
                Console.WriteLine("!!!!*******************$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
            //}
            //else
            //    isRefound = false;


            new Thread(() =>
            {
                TimeSpan ts = new TimeSpan(0, 0, 1);
                for (int i = 15; i > 0; i--)
                {                    
                    Thread.Sleep(ts);
                }

                UpdateFindButton updateFindButton = new UpdateFindButton(UpdateFindButtonUI);
                this.Dispatcher.Invoke(updateFindButton, true);

                

            }).Start();
            
            // Thread.Sleep(200);
            // findButton.Content = "查找设备";
            // findButton.IsEnabled = true;
        }



        public void killAllFindThread()
        {
            foreach (var t in threadList)
            {
                if (t.IsAlive)
                {
                    try
                    {
                        t.Abort();//Note this isn't guaranteed to stop the thread.
                                  // //Console.WriteLine("*********&&&&&&&&&&& " + t.ToString() + "  is abort");
                    }
                    catch (ThreadAbortException ex)
                    {
                        //不作处理          
                    }
                    catch (Exception ex)
                    {
                        //处理
                        
                    }
                }
            }
            threadList.Clear();
        }




        /// <summary>  
        /// 获取当前使用的IP  
        /// </summary>  
        /// <returns></returns>  
        public static string GetLocalIP()
        {
            string result = RunApp("route", "print", true);
            Match m = Regex.Match(result, @"0.0.0.0\s+0.0.0.0\s+(\d+.\d+.\d+.\d+)\s+(\d+.\d+.\d+.\d+)");
            if (m.Success)
            {
                return m.Groups[2].Value;
            }
            else
            {
                try
                {
                    System.Net.Sockets.TcpClient c = new System.Net.Sockets.TcpClient();
                    c.Connect("www.baidu.com", 80);
                    string ip = ((System.Net.IPEndPoint)c.Client.LocalEndPoint).Address.ToString();
                    c.Close();
                    return ip;
                }
                catch (Exception)
                {
                    return null;
                }
            }
        }

        /// <summary>  
        /// 获取本机主DNS  
        /// </summary>  
        /// <returns></returns>  
        public static string GetPrimaryDNS()
        {
            string result = RunApp("nslookup", "", true);
            Match m = Regex.Match(result, @"\d+\.\d+\.\d+\.\d+");
            if (m.Success)
            {
                return m.Value;
            }
            else
            {
                return null;
            }
        }

        /// <summary>  
        /// 运行一个控制台程序并返回其输出参数。  
        /// </summary>  
        /// <param name="filename">程序名</param>  
        /// <param name="arguments">输入参数</param>  
        /// <returns></returns>  
        public static string RunApp(string filename, string arguments, bool recordLog)
        {
            try
            {
                if (recordLog)
                {
                    Trace.WriteLine(filename + " " + arguments);
                }
                Process proc = new Process();
                proc.StartInfo.FileName = filename;
                proc.StartInfo.CreateNoWindow = true;
                proc.StartInfo.Arguments = arguments;
                proc.StartInfo.RedirectStandardOutput = true;
                proc.StartInfo.UseShellExecute = false;
                proc.Start();

                using (System.IO.StreamReader sr = new System.IO.StreamReader(proc.StandardOutput.BaseStream, Encoding.Default))
                {

                    Thread.Sleep(100);           //貌似调用系统的nslookup还未返回数据或者数据未编码完成，程序就已经跳过直接执行  
                                                 //txt = sr.ReadToEnd()了，导致返回的数据为空，故睡眠令硬件反应  
                    if (!proc.HasExited)         //在无参数调用nslookup后，可以继续输入命令继续操作，如果进程未停止就直接执行  
                    {                            //txt = sr.ReadToEnd()程序就在等待输入，而且又无法输入，直接掐住无法继续运行  
                        proc.Kill();
                    }
                    string txt = sr.ReadToEnd();
                    sr.Close();
                    if (recordLog)
                        Trace.WriteLine(txt);
                    return txt;
                }
            }
            catch (Exception ex)
            {
                Trace.WriteLine(ex);
                return ex.Message;
            }
        }


        void findConnectWithTimeOut()
        {

        }


        public void findConnect(object data)
        {
            string serverIp = data as string;

           

            //Console.WriteLine(serverIp + " connectting...");

            //设定服务器IP地址  
            IPAddress ip = IPAddress.Parse(serverIp);


            Socket clientSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
            clientSocket.SendTimeout = 10000;
            clientSocket.ReceiveTimeout = 10000;
            try
            {
                try
                {
                    clientSocket.Connect(new IPEndPoint(ip, 7777)); //配置服务器IP与端口  
                    //Console.WriteLine("连接服务器成功");
                }
                catch (ThreadAbortException ex)
                {
                    //不作处理         
                    
                }
                catch (Exception ex)
                {
                    //处理
                    return;
                }
            }
            catch
            {
                return;
            }

            try
            {

                byte[] result = new byte[1024];

                string sendMessage = "{\"cmd\":\"getlist\"}\r\n";
                Console.WriteLine("sendMessage: " + sendMessage);
                try
                {
                    clientSocket.Send(Encoding.UTF8.GetBytes(sendMessage));
                    Console.WriteLine("send data OK");
                }
                catch
                {
                    clientSocket.Shutdown(SocketShutdown.Both);
                    clientSocket.Close();
                    Console.WriteLine("send data wrong");
                    return;
                }

                //通过clientSocket接收数据  
                int receiveLength = clientSocket.Receive(result);

                string getMessage = Encoding.UTF8.GetString(result, 0, receiveLength);

                //Console.WriteLine(getMessage + "***" + getMessage.Length);

                AndroidClient nowAndroidClient = null;
                if ((getMessage != null) && (!getMessage.Equals("")))
                {                   
                    nowAndroidClient = JsonConvert.DeserializeObject<AndroidClient>(getMessage);
                    nowAndroidClient.ip = serverIp;
                    try
                    {                
                        UpdateUIDelegate updateUIDelegate = new UpdateUIDelegate(UpdateUI);
                   
                        //通过调用委托
                        this.listView.Dispatcher.Invoke(updateUIDelegate, nowAndroidClient);                    
                    }
                    catch (Exception ex)
                    {
                        //Console.WriteLine(ex.Message);                     
                    }
                }               
                else
                {            
                    return;
                }   

                clientSocket.Disconnect(true);
                clientSocket.Close();
            }
            catch
            {

            }
        }


        private void UpdateFindButtonUI(bool isable)
        {
            findButton.IsEnabled = true;
            findButton.Content = "查找设备";
            isFinding = false;            
        }


        private void UpdateUI(AndroidClient ableAndroidClient)
        {
            androidClientList.Add(ableAndroidClient);
            numText.Text = androidClientList.Count.ToString();
            //对Listview绑定的数据源进行更新         
        }


        public void selectOneSocket()
        {
            if (selectSocket != null)
            {
                try
                {
                    selectSocket.Disconnect(false);

                }
                catch (Exception e)
                {
                    //Console.WriteLine(e);
                }
                finally
                {
                    selectSocket.Close();
                    selectSocket = null;
                }
            }

            try
            {
                byte[] result = new byte[1024];
                //Console.WriteLine("#####  " + selectIP);
                IPAddress ip = IPAddress.Parse(selectIP);
                selectSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
                try
                {
                    selectSocket.Connect(new IPEndPoint(ip, 7777)); //配置服务器IP与端口  
                    //Console.WriteLine("连接服务器成功");
                }
                catch
                {
                    //Console.WriteLine("连接服务器失败，请按回车键退出！");
                    return;
                }

                string sendMessage = "{\"cmd\":\"getlist\"}\r\n";
                try
                {
                    selectSocket.Send(Encoding.UTF8.GetBytes(sendMessage));
                    //Console.WriteLine("send data OK");
                }
                catch
                {
                    selectSocket.Shutdown(SocketShutdown.Both);
                    selectSocket.Close();
                    //Console.WriteLine("send data wrong");
                    return;
                }


                //通过selectSocket接收数据  
                int receiveLength = selectSocket.Receive(result);

                string getMessage = Encoding.UTF8.GetString(result, 0, receiveLength);

                //Console.WriteLine(getMessage + "***" + getMessage.Length);

                if ((getMessage == null) || (getMessage.Equals("")))              
                    return;              


                sendMessage = "{\"cmd\":\"getconfig\"}\r\n";
                try
                {
                    selectSocket.Send(Encoding.UTF8.GetBytes(sendMessage));
                    //Console.WriteLine("send data OK");
                }
                catch
                {
                    selectSocket.Shutdown(SocketShutdown.Both);
                    selectSocket.Close();
                    //Console.WriteLine("send data wrong");
                    return;
                }




                receiveLength = selectSocket.Receive(result);

                getMessage = Encoding.UTF8.GetString(result, 0, receiveLength);

                Console.WriteLine(getMessage);

                selectAndroidClient = JsonConvert.DeserializeObject<AndroidClient>(getMessage);
                //Console.WriteLine("~~~~  " + androidClientList.Count + "!!! " + getMessage);
                //Console.WriteLine("#####$$$$$$$$ " + selectAndroidClient.autoResolutionIndex);

            }
            catch
            {

            }

        }





        private void lv_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            LogLabel.Content = "";
            UpdateLogLabel.Content = "";
            UpdateApkButton.IsEnabled = true;
            selectAndroidClient = null;
            selectAndroidClient = listView.SelectedItem as AndroidClient;
            if (selectAndroidClient != null && selectAndroidClient is AndroidClient)
            {
                //MessageBox.Show(JsonConvert.SerializeObject(selectAndroidClient));
                selectIP = selectAndroidClient.ip;
                //Console.WriteLine("****** " + selectIP);
                selectOneSocket();
                updateUIData();
                savaApplyButton.IsEnabled = true;
                btn_play.IsEnabled = true;
                swithCamera.IsEnabled = true;
            }
            else
            {
                selectAndroidClient = new AndroidClient();
                savaApplyButton.IsEnabled = false;
                btn_play.IsEnabled = false;
                swithCamera.IsEnabled = false;

                    if (is_playing_)
                    {
                        //NT_SP_Stop是老接口，如使用，请配合NT_SP_Start同步使用
                        //NTSmartPlayerSDK.NT_SP_Stop(player_handle_);
                        NTSmartPlayerSDK.NT_SP_StopPlay(player_handle_);
                        is_playing_ = false;
                        pictureBox1.Invalidate();   //清空最后一帧数据，如不加，默认保留最后一帧画面
                        btn_play.Content = "播放";
                }
                
            }


        }

        private void updateUIData()
        {
            //更新界面上的展示数据
            nameInput.Text = selectAndroidClient.name;

            if (selectAndroidClient.autoResolutionIndex == -1)
            {
                resolutionM.IsChecked = true;
                widthInput.Text = selectAndroidClient.width.ToString();
                heightInput.Text = selectAndroidClient.height.ToString();
            }
            else
            {
                ////Console.WriteLine("%%%%%%%%%%%" + selectAndroidClient.autoResolutionIndex);
                resolutionInput.SelectedIndex = selectAndroidClient.autoResolutionIndex;
                ////Console.WriteLine("%%%%%%%%%%%____   " + resolutionInput.SelectedIndex);
                resolutionA.IsChecked = true;
            }

            pushUrl_1.Text = selectAndroidClient.pushUrlFront;
            pushUrl_2.Text = selectAndroidClient.pushUrlBack;

            DHCPInput.IsChecked = selectAndroidClient.dhcp;
            IPShow.Content = selectAndroidClient.ip;
            macShow.Text = selectAndroidClient.mac;
            pwdShow.Text = selectAndroidClient.userID;

            operationServerInput.Text = selectAndroidClient.operateServer;

            if (selectAndroidClient.operatePort.ToString() != null)
                operationPortInput.Text = selectAndroidClient.operatePort.ToString();
            else
                operationPortInput.Text = "";

            configServerInput.Text = selectAndroidClient.configServer;

            if (selectAndroidClient.configPort.ToString() != null)
                configPortInput.Text = selectAndroidClient.configPort.ToString();
            else
                configPortInput.Text = "";

            hwEncodeInput.IsChecked = selectAndroidClient.encodeHW;
            encodeQualityInput.SelectedIndex = selectAndroidClient.encodeQuality - 1;

            if (selectAndroidClient.encodeNum.ToString() != null)
                encodeNumInput.Text = selectAndroidClient.encodeNum.ToString();
            else
                encodeNumInput.Text = "";

            if (selectAndroidClient.fps.ToString() != null)
                fpsInput.Text = selectAndroidClient.fps.ToString();
            else
                fpsInput.Text = "";

            recordInput.IsChecked = selectAndroidClient.record;

            if (selectAndroidClient.appVersion.ToString() != null)
                Versionlabel.Content = selectAndroidClient.appVersion.ToString();
            else
                Versionlabel.Content = "";


            enableConfigServerCheckBox.IsChecked = selectAndroidClient.enableConfigServer;

            switchToOneCheckBox.IsChecked = selectAndroidClient.swtichToOne;

            containAudioCheckBox.IsChecked = selectAndroidClient.containAudio;

            staturationSlider.Value = selectAndroidClient.staturation;

            contrastSlider.Value = selectAndroidClient.contrast;

            brightnessSlider.Value = selectAndroidClient.brightness;

            usingCustomConfigCheck.IsChecked = selectAndroidClient.usingCustomConfig;

        }




        //DHCP
        private void CheckBox_Checked(object sender, RoutedEventArgs e)
        {
            selectAndroidClient.dhcp = DHCPInput.IsChecked.Value;
        }

        //机器名
        private void TextBox_TextChanged(object sender, TextChangedEventArgs e)
        {
            selectAndroidClient.name = nameInput.Text;
        }

        //推流地址1
        private void TextBox_TextChanged_1(object sender, TextChangedEventArgs e)
        {
            selectAndroidClient.pushUrlFront = pushUrl_1.Text;
        }

        //推流地址2
        private void TextBox_TextChanged_2(object sender, TextChangedEventArgs e)
        {
            selectAndroidClient.pushUrlBack = pushUrl_2.Text;
        }

        //操作服务器域名
        private void TextBox_TextChanged_3(object sender, TextChangedEventArgs e)
        {
            selectAndroidClient.operateServer = operationServerInput.Text;
        }

        //操作服务器端口 预览
        private void TextBox_TextChanged_4(object sender, TextCompositionEventArgs e)
        {
            Regex re = new Regex("[^0-9.-]+");
            e.Handled = re.IsMatch(e.Text);
        }

        //参数服务器域名
        private void TextBox_TextChanged_5(object sender, TextChangedEventArgs e)
        {
            selectAndroidClient.configServer = configServerInput.Text;
        }

        //参数服务器端口 预览
        private void TextBox_TextChanged_6(object sender, TextCompositionEventArgs e)
        {
            Regex re = new Regex("[^0-9.-]+");
            e.Handled = re.IsMatch(e.Text);
        }

        //硬编码
        private void CheckBox_Checked_1(object sender, RoutedEventArgs e)
        {
            //Console.WriteLine("!!!!  " + hwEncodeInput.IsChecked.Value);

            selectAndroidClient.encodeHW = hwEncodeInput.IsChecked.Value;
        }


        //画质质量
        private void ComboBox_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            selectAndroidClient.encodeQuality = encodeQualityInput.SelectedIndex + 1;
        }

        //关键帧间隔 预览
        private void TextBox_TextChanged_7(object sender, TextCompositionEventArgs e)
        {
            Regex re = new Regex("[^0-9.-]+");
            e.Handled = re.IsMatch(e.Text);

        }

        //帧率 预览
        private void TextBox_TextChanged_8(object sender, TextCompositionEventArgs e)
        {
            Regex re = new Regex("[^0-9.-]+");
            e.Handled = re.IsMatch(e.Text);
        }

        //录像
        private void CheckBox_Checked_2(object sender, RoutedEventArgs e)
        {
            selectAndroidClient.record = recordInput.IsChecked.Value;
        }

        //宽 预览
        private void TextBox_TextChanged_9(object sender, TextCompositionEventArgs e)
        {
            Regex re = new Regex("[^0-9.-]+");
            e.Handled = re.IsMatch(e.Text);

        }


        //高 预览
        private void TextBox_TextChanged_10(object sender, TextCompositionEventArgs e)
        {
            Regex re = new Regex("[^0-9.-]+");
            e.Handled = re.IsMatch(e.Text);
        }


        //保存并应用
        private void Button_Click_1(object sender, RoutedEventArgs e)
        {
            LogLabel.Content = "";

            if (selectIP == null)
            {
                savaApplyButton.IsEnabled = false;
                return;
            }

            if (selectIP.Equals(""))
            {
                savaApplyButton.IsEnabled = false;
                return;
            }

            if (!selectSocket.Connected)
            {
                savaApplyButton.IsEnabled = false;
                return;
            }


            try
            {

                selectAndroidClient.cmd = "applyconfig";

                string jsonAndroidClient = JsonConvert.SerializeObject(selectAndroidClient);


                savaApplyButton.IsEnabled = false;

                selectSocket.Send(Encoding.UTF8.GetBytes(jsonAndroidClient));
                //Console.WriteLine("jsonAndroidClient  " + jsonAndroidClient);


                byte[] result = new byte[1024];
                //通过clientSocket接收数据  
                int receiveLength = selectSocket.Receive(result);

                string getMessage = Encoding.UTF8.GetString(result, 0, receiveLength);

                //Console.WriteLine(getMessage + "***" + getMessage.Length);
                if (getMessage.Contains("ok"))
                {
                    //Console.WriteLine("success");
                    LogLabel.Content = selectIP + " 应用成功";
                }
                if (getMessage.Contains("fail"))
                {
                    //Console.WriteLine("failed");
                    LogLabel.Content = selectIP + " 应用失败";
                }

                savaApplyButton.IsEnabled = true;
            }
            catch
            {

            }
        }



        private void Window_Closing(object sender, System.ComponentModel.CancelEventArgs e)
        {
            //Console.WriteLine("is closing");
            resetClinet();
        }

        public void resetClinet()
        {
            listView.ItemsSource = null;
            // //Console.WriteLine("*********&&&&&&&&&&&&&&&&&&&&&$$$$$$$$$$%%%%%%%%%%%%%%%%%%%%%%%%");
            for (int i = 0; i < androidClientList.Count; i++)
            {
                string _ip = androidClientList[0].ip;

                androidClientList.RemoveAt(0);
            }

            androidClientList.Clear();
            //ipToSocket.Clear();

        }

        //预设分辨率
        private void resolutionA_Checked(object sender, RoutedEventArgs e)
        {
            selectAndroidClient.autoResolutionIndex = resolutionInput.SelectedIndex;
        }

        //自定义分辨率
        private void resolutionM_Checked(object sender, RoutedEventArgs e)
        {
            selectAndroidClient.autoResolutionIndex = -1;
        }

        //选择了自定义选项
        private void resolutionInput_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            selectAndroidClient.autoResolutionIndex = resolutionInput.SelectedIndex;
        }

        //宽
        private void widthInput_TextChanged(object sender, TextChangedEventArgs e)
        {
            if (widthInput.Text.Length != 0)
                selectAndroidClient.width = int.Parse(widthInput.Text);
        }

        //操作服务器端口
        private void operationPortInput_TextChanged(object sender, TextChangedEventArgs e)
        {
            if (operationPortInput.Text.Length != 0)
                selectAndroidClient.operatePort = int.Parse(operationPortInput.Text);
        }

        //参数服务器端口
        private void configPortInput_TextChanged(object sender, TextChangedEventArgs e)
        {
            if (configPortInput.Text.Length != 0)
                selectAndroidClient.configPort = int.Parse(configPortInput.Text);
        }


        //启用配置服务器选项
        private void CheckBox_Checked_3(object sender, RoutedEventArgs e)
        {
            selectAndroidClient.enableConfigServer = enableConfigServerCheckBox.IsChecked.Value;
            if (selectAndroidClient.enableConfigServer)
            {
                configServerInput.IsEnabled = true;
                configPortInput.IsEnabled = true;
            }
            else
            {
                configServerInput.IsEnabled = false;
                configPortInput.IsEnabled = false;
            }
        }

        //关键帧间隔
        private void encodeNumInput_TextChanged(object sender, TextChangedEventArgs e)
        {
            if (encodeNumInput.Text.Length != 0)
                selectAndroidClient.encodeNum = int.Parse(encodeNumInput.Text);
        }

        //帧率
        private void fpsInput_TextChanged(object sender, TextChangedEventArgs e)
        {
            if (fpsInput.Text.Length != 0)
                selectAndroidClient.fps = int.Parse(fpsInput.Text);
            //Console.WriteLine(fpsInput.Text + " ######~~~~###### " + selectAndroidClient.fps);
        }

        //高
        private void heightInput_TextChanged(object sender, TextChangedEventArgs e)
        {
            if (heightInput.Text.Length != 0)
                selectAndroidClient.height = int.Parse(heightInput.Text);
        }

        //拉流地址同推流地址
        private void RadioButton_Checked(object sender, RoutedEventArgs e)
        {
            playUrlMode = 0;
            playUrlNow_1 = selectAndroidClient.pushUrlFront;
            playUrlNow_2 = selectAndroidClient.pushUrlBack;
        }

        //阿里拉流格式
        private void RadioButton_Checked_1(object sender, RoutedEventArgs e)
        {
            //可能会不带端口
            //rtmp://video-center.alivecdn.com:1935/XBWLWAWA/000002_1?vhost=push.klgw888.com
            //rtmp://push.klgw888.com:1935/XBWLWAWA/000002_1
            playUrlMode = 1;
            string url, servername;
            int servernameIndex, portIndex;
            if (selectIP == null)
                return;
            if (selectIP.Equals(""))
            {
                return;
            }

            url = selectAndroidClient.pushUrlFront;
            servernameIndex = url.IndexOf("?vhost=");
            portIndex = url.LastIndexOf(":");
            if ((servernameIndex == -1) || (portIndex == -1))
            {
                playUrl_1.Text = "出错了";
                
            }
            else
            {
                servername = url.Substring(servernameIndex + 7);
                if (portIndex == url.IndexOf(":"))
                {
                    portIndex = url.IndexOf("com") + 3;                    
                }
                playUrlNow_1 = "rtmp://" + servername + url.Substring(portIndex, servernameIndex - portIndex);               
                playUrl_1.Text = playUrlNow_1;
            }

            url = selectAndroidClient.pushUrlBack;
            servernameIndex = url.IndexOf("?vhost=");
            portIndex = url.LastIndexOf(":");
            if ((servernameIndex == -1) || (portIndex == -1))
            {
                playUrl_2.Text = "出错了";
                
            }
            else
            {
                servername = url.Substring(servernameIndex + 7);
                if (portIndex == url.IndexOf(":"))
                {
                    portIndex = url.IndexOf("com") + 3;
                }
                playUrlNow_2 = "rtmp://" + servername + url.Substring(portIndex, servernameIndex - portIndex);
                playUrl_2.Text = playUrlNow_2;
            }
        }

        //自定义拉流地址
        private void RadioButton_Checked_2(object sender, RoutedEventArgs e)
        {
            playUrlMode = 2;
            playUrlNow_1 = playUrl_1.Text;
            playUrlNow_2 = playUrl_2.Text;
        }

        //切换镜头
        private void Button_Click_2(object sender, RoutedEventArgs e)
        {
            if (playNowNum == 1)
            {
                playNowNum = 2;
            }
            else
            {
                playNowNum = 1;
            }

            playUrlNum.Content = playNowNum.ToString();

            btn_play_Click_1();
            btn_play_Click_1();
        }

        //播放
        private void btn_play_Click(object sender, RoutedEventArgs e)
        {
            if (playUrlMode == 0)
            {
                RadioButton_Checked(null, null);
            }
            if (playUrlMode == 1)
            {
                RadioButton_Checked_1(null, null);
            }
            if (playUrlMode == 2)
            {
                RadioButton_Checked_2(null, null);
            }

            Console.WriteLine(playUrlNow_1 + "  " + playUrlNow_2);
            btn_play_Click_1();
            //  
        }


        //排序 

        private void listViewColumnHeader_Click(object sender, RoutedEventArgs e)
        {
            GridViewColumnHeader column = (sender as GridViewColumnHeader);
            string sortBy = column.Tag.ToString();
            if (listViewSortCol != null)
            {
                AdornerLayer.GetAdornerLayer(listViewSortCol).Remove(listViewSortAdorner);
                listView.Items.SortDescriptions.Clear();
            }

            ListSortDirection newDir = ListSortDirection.Ascending;
            if (listViewSortCol == column && listViewSortAdorner.Direction == newDir)
                newDir = ListSortDirection.Descending;

            listViewSortCol = column;
            listViewSortAdorner = new SortAdorner(listViewSortCol, newDir);
            AdornerLayer.GetAdornerLayer(listViewSortCol).Add(listViewSortAdorner);
            listView.Items.SortDescriptions.Add(new SortDescription(sortBy, newDir));
        }









        /**************
         * 
         * 
         * 大牛播放的
         * 
         * 
         * ***********/


        [DllImport("kernel32.dll", EntryPoint = "CopyMemory")]
        static extern void CopyMemory(IntPtr Destination, IntPtr Source, uint Length);
        private IntPtr player_handle_;

        private Int32 width_ = 0;
        private Int32 height_ = 0;

        private bool is_sdk_init_ = false;
        private bool is_playing_ = false;
        private bool is_recording_ = false;

        private UInt32 connection_status_ = 0;
        private UInt32 buffer_status_ = 0;
        private Int32 buffer_percent_ = 0;
        private Int32 download_speed_ = -1;


        public String urlPath = "rtmp://live.hkstv.hk.lxdns.com/live/hks";


        SP_SDKVideoSizeCallBack video_size_call_back_;

        delegate void ResolutionNotifyCallback(Int32 width, Int32 height);
        ResolutionNotifyCallback resolution_notify_callback_;


        //video
        SP_SDKVideoFrameCallBack video_frame_call_back_;
        delegate void VideoFrameCallBack(UInt32 status, NT_SP_VideoFrame frame);
        VideoFrameCallBack set_video_frame_call_back_;

        //audio
        SP_SDKAudioPCMFrameCallBack audio_pcm_frame_call_back_;
        delegate void AudioPCMFrameCallBack(UInt32 status, IntPtr data, UInt32 size,
        Int32 sample_rate, Int32 channel, Int32 per_channel_sample_number);
        AudioPCMFrameCallBack set_audio_pcm_frame_call_back_;

        SP_SDKRenderVideoFrameTimestampCallBack video_frame_ts_callback_;
        delegate void SetRenderVideoFrameTimestampCallBack(UInt64 timestamp, UInt64 reserve1, IntPtr reserve2);
        SetRenderVideoFrameTimestampCallBack set_render_video_frame_timestamp_callback_;

        SP_SDKCaptureImageCallBack capture_image_call_back_;
        delegate void SetCaptureImageCallBack(UInt32 result, [MarshalAs(UnmanagedType.LPStr)] String file_name);
        SetCaptureImageCallBack set_capture_image_call_back_;

        SP_SDKRecorderCallBack record_call_back_;
        delegate void SetRecordCallBack(UInt32 status, [MarshalAs(UnmanagedType.LPStr)] String file_name);
        SetRecordCallBack set_record_call_back_;

        SP_SDKEventCallBack event_call_back_;
        delegate void SetEventCallBack(UInt32 event_id,
                Int64 param1,
                Int64 param2,
                UInt64 param3,
                IntPtr param4,
                IntPtr param5,
                IntPtr param6);
        SetEventCallBack set_event_call_back_;


        IntPtr handle;

        public void InitDaniu()
        {
            //Console.WriteLine("!!~~");
            handle = pictureBox1.Handle;

            //Console.WriteLine("!!~~112");

            is_sdk_init_ = false;
            is_playing_ = false;

            //Console.WriteLine("!!~~4444");

            set_video_frame_call_back_ = new VideoFrameCallBack(SDKVideoFrameCallBack);

            //Console.WriteLine("!!~~445656");

            set_audio_pcm_frame_call_back_ = new AudioPCMFrameCallBack(SDKAudioPCMFrameCallBack);

            //Console.WriteLine("!!~~45454");
            set_event_call_back_ = new SetEventCallBack(EventCallBack);

            //Console.WriteLine("!!~~767777");

            UInt32 isInited = NT.NTSmartPlayerSDK.NT_SP_Init(0, IntPtr.Zero);

            //Console.WriteLine("!!~~654643563");
            if (isInited != 0)
            {
                MessageBox.Show("调用NT_SP_Init失败..");
                return;
            }

            is_sdk_init_ = true;
            //Console.WriteLine("!!~~22");
            if (player_handle_ == IntPtr.Zero)
            {
                player_handle_ = new IntPtr();


                UInt32 ret_open = NTSmartPlayerSDK.NT_SP_Open(out player_handle_, handle, 0, IntPtr.Zero);

                if (ret_open != 0)
                {
                    player_handle_ = IntPtr.Zero;
                    MessageBox.Show("调用NT_SP_Open失败..");
                    return;
                }
            }
            //Console.WriteLine("!!~~33");
            event_call_back_ = new SP_SDKEventCallBack(SDKEventCallBack);

            NTSmartPlayerSDK.NT_SP_SetEventCallBack(player_handle_, handle, event_call_back_);
            //Console.WriteLine("!!~~44");
        }



        public void SDKVideoFrameCallBack(UInt32 status, NT_SP_VideoFrame frame)
        {
            //这里拿到回调frame，进行相关操作
            //....

            //release
            Marshal.FreeHGlobal(frame.plane0_);
        }

        public void SDKAudioPCMFrameCallBack(UInt32 status, IntPtr data, UInt32 size,
        Int32 sample_rate, Int32 channel, Int32 per_channel_sample_number)
        {
            //这里拿到回调的PCM frame，进行相关操作（如自己播放）
            //label_debug.Text = per_channel_sample_number.ToString();

            //release
            Marshal.FreeHGlobal(data);
        }

        public void SetVideoFrameCallBack(IntPtr handle, IntPtr userData, UInt32 status, IntPtr frame)
        {
            if (frame == IntPtr.Zero)
            {
                return;
            }

            NT_SP_VideoFrame video_frame = (NT_SP_VideoFrame)Marshal.PtrToStructure(frame, typeof(NT_SP_VideoFrame));

            NT_SP_VideoFrame pVideoFrame = new NT_SP_VideoFrame();

            pVideoFrame.format_ = video_frame.format_;
            pVideoFrame.width_ = video_frame.width_;
            pVideoFrame.height_ = video_frame.height_;

            pVideoFrame.timestamp_ = video_frame.timestamp_;
            pVideoFrame.stride0_ = video_frame.stride0_;
            pVideoFrame.stride1_ = video_frame.stride1_;
            pVideoFrame.stride2_ = video_frame.stride2_;
            pVideoFrame.stride3_ = video_frame.stride3_;

            Int32 argb_size = video_frame.stride0_ * video_frame.height_;

            pVideoFrame.plane0_ = Marshal.AllocHGlobal(argb_size);
            CopyMemory(pVideoFrame.plane0_, video_frame.plane0_, (UInt32)argb_size);


            set_video_frame_call_back_(status, pVideoFrame);

        }



        public void SDKEventCallBack(IntPtr handle, IntPtr user_data,
            UInt32 event_id,
            Int64 param1,
            Int64 param2,
            UInt64 param3,
            IntPtr param4,
            IntPtr param5,
            IntPtr param6)
        {

            set_event_call_back_(event_id, param1, param2, param3, param4, param5, param6);

        }

        private void EventCallBack(UInt32 event_id,
            Int64 param1,
            Int64 param2,
            UInt64 param3,
            IntPtr param4,
            IntPtr param5,
            IntPtr param6)
        {
            if (!is_playing_ && !is_recording_)
            {
                return;
            }

            if ((UInt32)NTSmartPlayerDefine.NT_SP_E_EVENT_ID.NT_SP_E_EVENT_ID_CONNECTING == event_id
                || (UInt32)NTSmartPlayerDefine.NT_SP_E_EVENT_ID.NT_SP_E_EVENT_ID_CONNECTION_FAILED == event_id
                || (UInt32)NTSmartPlayerDefine.NT_SP_E_EVENT_ID.NT_SP_E_EVENT_ID_CONNECTED == event_id
                || (UInt32)NTSmartPlayerDefine.NT_SP_E_EVENT_ID.NT_SP_E_EVENT_ID_DISCONNECTED == event_id)
            {
                connection_status_ = event_id;
            }

            if ((UInt32)NTSmartPlayerDefine.NT_SP_E_EVENT_ID.NT_SP_E_EVENT_ID_START_BUFFERING == event_id
                || (UInt32)NTSmartPlayerDefine.NT_SP_E_EVENT_ID.NT_SP_E_EVENT_ID_BUFFERING == event_id
                || (UInt32)NTSmartPlayerDefine.NT_SP_E_EVENT_ID.NT_SP_E_EVENT_ID_STOP_BUFFERING == event_id)
            {
                buffer_status_ = event_id;

                if ((UInt32)NTSmartPlayerDefine.NT_SP_E_EVENT_ID.NT_SP_E_EVENT_ID_BUFFERING == event_id)
                {
                    buffer_percent_ = (Int32)param1;
                }
            }

            if ((UInt32)NTSmartPlayerDefine.NT_SP_E_EVENT_ID.NT_SP_E_EVENT_ID_DOWNLOAD_SPEED == event_id)
            {
                download_speed_ = (Int32)param1;

            }

            String show_str = "";

            if (connection_status_ != 0)
            {
                show_str += "链接状态: ";

                if ((UInt32)NTSmartPlayerDefine.NT_SP_E_EVENT_ID.NT_SP_E_EVENT_ID_CONNECTING == connection_status_)
                {
                    show_str += "链接中";
                }
                else if ((UInt32)NTSmartPlayerDefine.NT_SP_E_EVENT_ID.NT_SP_E_EVENT_ID_CONNECTION_FAILED == connection_status_)
                {
                    show_str += "链接失败";
                }
                else if ((UInt32)NTSmartPlayerDefine.NT_SP_E_EVENT_ID.NT_SP_E_EVENT_ID_CONNECTED == connection_status_)
                {
                    show_str += "链接成功";
                }
                else if ((UInt32)NTSmartPlayerDefine.NT_SP_E_EVENT_ID.NT_SP_E_EVENT_ID_DISCONNECTED == connection_status_)
                {
                    show_str += "链接断开";
                }
            }

            if (download_speed_ != -1)
            {
                String ss = "  下载速度: " + (download_speed_ * 8 / 1000).ToString() + "kbps " + (download_speed_ / 1024).ToString() + "KB/s";
                // speedLabl.Content = (download_speed_ / 1024).ToString() + "KB/s";

                show_str += ss;
            }

            if (buffer_status_ != 0)
            {
                show_str += "  缓冲状态: ";

                if ((UInt32)NTSmartPlayerDefine.NT_SP_E_EVENT_ID.NT_SP_E_EVENT_ID_START_BUFFERING == buffer_status_)
                {
                    show_str += "开始缓冲";
                }
                else if ((UInt32)NTSmartPlayerDefine.NT_SP_E_EVENT_ID.NT_SP_E_EVENT_ID_BUFFERING == buffer_status_)
                {
                    String ss = "缓冲中 " + buffer_percent_.ToString() + "%";
                    show_str += ss;
                }
                else if ((UInt32)NTSmartPlayerDefine.NT_SP_E_EVENT_ID.NT_SP_E_EVENT_ID_STOP_BUFFERING == buffer_status_)
                {
                    show_str += "结束缓冲";
                }
            }
        }

        private void btn_play_Click_1()
        {
            if (playNowNum == 1)
            {
                urlPath = playUrlNow_1;
            }
            else
            {
                urlPath = playUrlNow_2;
            }

            playUrlNum.Content = playNowNum.ToString();

            if (btn_play.Content.Equals("播放"))
            {
                if (!is_recording_)
                {
                    if (!InitCommonSDKParam())
                    {
                        MessageBox.Show("设置参数错误!");
                        return;
                    }
                }
                //video frame callback (YUV/RGB)
                //format请参见 NT_SP_E_VIDEO_FRAME_FORMAT，如需回调YUV，请设置为 NT_SP_E_VIDEO_FRAME_FROMAT_I420
                video_frame_call_back_ = new SP_SDKVideoFrameCallBack(SetVideoFrameCallBack);
                NTSmartPlayerSDK.NT_SP_SetVideoFrameCallBack(player_handle_, (Int32)NT.NTSmartPlayerDefine.NT_SP_E_VIDEO_FRAME_FORMAT.NT_SP_E_VIDEO_FRAME_FORMAT_RGB32, handle, video_frame_call_back_);


                UInt32 ret_start = NTSmartPlayerSDK.NT_SP_StartPlay(player_handle_);

                if (ret_start != 0)
                {
                    MessageBox.Show("播放失败..");
                    return;
                }


                is_playing_ = true;

                btn_play.Content = "停止";
            }
            else
            {
                if (is_playing_)
                {
                    //NT_SP_Stop是老接口，如使用，请配合NT_SP_Start同步使用
                    //NTSmartPlayerSDK.NT_SP_Stop(player_handle_);
                    NTSmartPlayerSDK.NT_SP_StopPlay(player_handle_);
                    is_playing_ = false;
                    pictureBox1.Invalidate();   //清空最后一帧数据，如不加，默认保留最后一帧画面
                }

                btn_play.Content = "播放";
                //lable_cur_status_txt.Content = "";
            }

        }

        //NTSmartPlayerSDK.NT_SP_SetMute(player_handle_, 1);

        //NTSmartPlayerSDK.NT_SP_SetMute(player_handle_, 0);




        // private void SmartPlayerForm_FormClosing(object sender, FormClosingEventArgs e)
        private void MainWindow_Closing(object sender, System.ComponentModel.CancelEventArgs e)
        { 
            if (is_playing_)
            {
                NTSmartPlayerSDK.NT_SP_StopPlay(player_handle_);
                is_playing_ = false;
            }
                       

            if (player_handle_ != IntPtr.Zero)
            {
                NTSmartPlayerSDK.NT_SP_Close(player_handle_);
                player_handle_ = IntPtr.Zero;
            }

            if (is_sdk_init_)
            {
                NTSmartPlayerSDK.NT_SP_UnInit();
                is_sdk_init_ = false;
            }

            if (selectSocket != null)
            {
                try
                {
                    selectSocket.Disconnect(false);

                }
                catch (Exception ee)
                {
                    //Console.WriteLine(ee);
                }
                finally
                {
                    selectSocket.Close();
                    selectSocket = null;
                }
            }
        }


        private bool InitCommonSDKParam()
        {
            if (is_playing_ || is_recording_)
            {
                return true;
            }

            if (IntPtr.Zero == player_handle_)
                return false;

            String url = urlPath;

            if (String.IsNullOrEmpty(url))
            {
                return false;
            }

            Int32 buffer_time = 100;

            NTSmartPlayerSDK.NT_SP_SetBuffer(player_handle_, buffer_time);

            NTSmartPlayerSDK.NT_SP_SetRTSPTcpMode(player_handle_, 0);

            NTSmartPlayerSDK.NT_SP_SetMute(player_handle_, 1);

            NTSmartPlayerSDK.NT_SP_SetReportDownloadSpeed(player_handle_, 1, 1);

            NTSmartPlayerSDK.NT_SP_SetURL(player_handle_, url);

            return true;
        }


        //用户ID
        private void pwdShow_TextChanged(object sender, TextChangedEventArgs e)
        {
            selectAndroidClient.userID = pwdShow.Text;
        }


        //应用升级
        private void Button_Click_3(object sender, RoutedEventArgs e)
        {
            UpdateLogLabel.Content = "";

            if (selectIP == null)
            {
                UpdateApkButton.IsEnabled = false;
                return;
            }

            if (selectIP.Equals(""))
            {
                UpdateApkButton.IsEnabled = false;
                return;
            }

            if (!selectSocket.Connected)
            {
                UpdateApkButton.IsEnabled = false;
                return;
            }

            if ((apkUrlInput.Text == null) || (apkUrlInput.Text.Equals("")))
            {
                UpdateApkButton.IsEnabled = false;
                return;
            }

            try
            {
                UpdateApk updateApk = new UpdateApk();
                updateApk.cmd = "update";
                updateApk.url = apkUrlInput.Text;
                

                string jsonUpdateApk = JsonConvert.SerializeObject(updateApk);


                UpdateApkButton.IsEnabled = false;

                selectSocket.Send(Encoding.UTF8.GetBytes(jsonUpdateApk));            


                byte[] result = new byte[1024];
                //通过clientSocket接收数据  
                int receiveLength = selectSocket.Receive(result);

                string getMessage = Encoding.UTF8.GetString(result, 0, receiveLength);

                //Console.WriteLine(getMessage + "***" + getMessage.Length);
                if (getMessage.Contains("ok"))
                {
                    //Console.WriteLine("success");
                    UpdateLogLabel.Content = selectIP + " 发送成功";
                }
                if (getMessage.Contains("fail"))
                {
                    //Console.WriteLine("failed");
                    UpdateLogLabel.Content = selectIP + " 发送失败";
                }

                UpdateApkButton.IsEnabled = true;
            }
            catch
            {

            }
        }


        //切换至一路
        private void CheckBox_Checked_4(object sender, RoutedEventArgs e)
        {
            selectAndroidClient.swtichToOne = switchToOneCheckBox.IsChecked.Value;
        }

        private void containAudioCheckBox_Checked(object sender, RoutedEventArgs e)
        {
            selectAndroidClient.containAudio = containAudioCheckBox.IsChecked.Value;
        }

        private void staturationSlider_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            selectAndroidClient.staturation =(int) staturationSlider.Value;            
        }

        private void usingCustomConfigCheckBox_Checked(object sender, RoutedEventArgs e)
        {
            selectAndroidClient.usingCustomConfig = usingCustomConfigCheck.IsChecked.Value;
        }

        private void contrastSlider_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            selectAndroidClient.contrast = (int)contrastSlider.Value;
        }

        private void brightnessSlider_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            selectAndroidClient.brightness = (int)brightnessSlider.Value;
        }

















        //private void fpsInput_TextChanged(object sender, TextChangedEventArgs e)
        //{

        //}

        ////操作服务器端口
        //private void operationPortInput_TextChanged(object sender, TextChangedEventArgs e)
        //{
        //    selectAndroidClient.operatePort = int.Parse(operationPortInput.Text);
        //}

        //private void configPortInput_TextChanged(object sender, TextChangedEventArgs e)
        //{

        //}



        //void CallWithTimeout(Func<System.Object, System.Object> action, int timeoutMilliseconds)
        //{
        //    Thread threadToKill = null;
        //    Action wrappedAction = () =>
        //    {
        //        threadToKill = Thread.CurrentThread;
        //        action();
        //    };

        //    IAsyncResult result = wrappedAction.BeginInvoke(null, null);
        //    if (result.AsyncWaitHandle.WaitOne(timeoutMilliseconds))
        //    {
        //        wrappedAction.EndInvoke(result);
        //    }
        //    else
        //    {
        //        threadToKill.Abort();
        //        throw new TimeoutException();
        //    }
        //}



    }






    public class AndroidClient
    {
        public string cmd { get; set; }
        public string name { get; set; }
        public int autoResolutionIndex { get; set; }
        public int width { get; set; }
        public int height { get; set; }
        public bool encodeHW { get; set; }
        public int encodeQuality { get; set; }
        public int encodeNum { get; set; }
        public int fps { get; set; }
        public bool record { get; set; }
        public string pushUrlFront { get; set; }
        public string pushUrlBack { get; set; }
        public string ip { get; set; }
        public bool dhcp { get; set; }
        public string operateServer { get; set; }
        public int operatePort { get; set; }
        public string configServer { get; set; }
        public int configPort { get; set; }
        public string mac { get; set; }
        public string userID { set; get; }
        public int appVersion { set; get; }
        public bool enableConfigServer { set; get; }
        public bool swtichToOne { set; get; }
        public bool containAudio { set; get; }

        public bool usingCustomConfig { set; get; }
        public int staturation { set; get; }
        public int contrast { set; get; }
        public int brightness { set; get; }



        public AndroidClient(string _ip, string _name)
        {
            name = _name;
            ip = _ip;
        }

        public AndroidClient()
        {
        }
    }

    public class UpdateApk
    {
        public string cmd { get; set; }
        public string url { get; set; }
        public UpdateApk()
        {

        }
    }



    //AndroidClient androidClient = new AndroidClient();
    //string jsonAndroidClient = JsonConvert.SerializeObject(androidClient);
    //AndroidClient deserializedAndroidClient = JsonConvert.DeserializeObject<AndroidClient>(jsonAndroidClient);









    //class TimeOutSocket
    //{
    //    private  bool IsConnectionSuccessful = false;
    //    private  Exception socketexception;
    // //   private  ManualResetEvent TimeoutObject = new ManualResetEvent(false);

    //    //public static TcpClient Connect(IPEndPoint remoteEndPoint, int timeoutMSec)
    //    public string Connect(string serverip, int timeoutMSec, ManualResetEvent TimeoutObject)
    //    {      
    //        socketexception = null;
    //        int serverport = 7777;
    //        TcpClient tcpclient = new TcpClient();

    //        tcpclient.BeginConnect(serverip, serverport,
    //            new AsyncCallback(CallBackMethod), tcpclient);

    //        //Console.WriteLine("~~~  " + serverip);
    //        if (TimeoutObject.WaitOne(timeoutMSec))
    //        {
    //            if (IsConnectionSuccessful)
    //            {
    //                //Console.WriteLine(serverip + "is OK ***** ");
    //                return serverip;
    //            }
    //            else
    //            {
    //                //Console.WriteLine(serverip + "is not OK");
    //                //throw socketexception;

    //            }
    //        }
    //        else
    //        {
    //            tcpclient.Close();            
    //        }
    //        return null;
    //    }
    //    private  void CallBackMethod(IAsyncResult asyncresult)
    //    {
    //        try
    //        {
    //            IsConnectionSuccessful = false;
    //            TcpClient tcpclient = asyncresult.AsyncState as TcpClient;

    //            if (tcpclient.Client != null)
    //            {
    //                //Console.WriteLine("not null ~~~~");
    //                tcpclient.EndConnect(asyncresult);
    //                IsConnectionSuccessful = true;
    //            }
    //        }
    //        catch (Exception ex)
    //        {
    //            IsConnectionSuccessful = false;
    //            socketexception = ex;
    //        }
    //        finally
    //        {
    //           // TimeoutObject.Set();
    //        }
    //    }
    //}




    public class SortAdorner : Adorner
    {
        private static Geometry ascGeometry =
                Geometry.Parse("M 0 4 L 3.5 0 L 7 4 Z");

        private static Geometry descGeometry =
                Geometry.Parse("M 0 0 L 3.5 4 L 7 0 Z");

        public ListSortDirection Direction { get; private set; }

        public SortAdorner(UIElement element, ListSortDirection dir)
                : base(element)
        {
            this.Direction = dir;
        }

        protected override void OnRender(DrawingContext drawingContext)
        {
            base.OnRender(drawingContext);

            if (AdornedElement.RenderSize.Width < 20)
                return;

            TranslateTransform transform = new TranslateTransform
                    (
                            AdornedElement.RenderSize.Width - 15,
                            (AdornedElement.RenderSize.Height - 5) / 2
                    );
            drawingContext.PushTransform(transform);

            Geometry geometry = ascGeometry;
            if (this.Direction == ListSortDirection.Descending)
                geometry = descGeometry;
            drawingContext.DrawGeometry(System.Windows.Media.Brushes.Black, null, geometry);

            drawingContext.Pop();
        }
    }







}
