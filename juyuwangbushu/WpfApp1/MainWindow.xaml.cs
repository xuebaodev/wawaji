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
using System.Threading;

using Newtonsoft.Json;
using System.Collections.ObjectModel;
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


        bool isFinding = false;

        //进行更新UI的委托
        public delegate void UpdateUIDelegate(AndroidClient ableAndroidClient);

        public delegate void UpdateFindButton(bool isable);


        public delegate void UpdateLogLabel(string str);



        private GridViewColumnHeader listViewSortCol = null;
        private SortAdorner listViewSortAdorner = null;


        List<Thread> threadList = new List<Thread>();

        //[] arEvents = new AutoResetEvent[256]; // 同步对象

        public MainWindow()
        {
            InitializeComponent();
        }

        private void Button_Click(object sender, RoutedEventArgs e)
        {
            if (isFinding)
                return;
            findButton.Content = "正在查找...";
            findButton.IsEnabled = false;
            isFinding = true;

            LogLabel.Text = "正在查找...\n";

            killAllFindThread();
            resetClinet();

            string localIp = GetLocalIP();
            Console.WriteLine(localIp);
            string ipPre = localIp.Substring(0, localIp.LastIndexOf('.') + 1);

            new Thread(() =>
            {
                for (int i = 0; i <= 255; i++)
                {
                    string para = ipPre + i.ToString();
                    Thread t1 = new Thread(new ParameterizedThreadStart(findConnect));
                    t1.IsBackground = true;
                    threadList.Add(t1);
                    t1.Start(para);
                    t1.Priority = ThreadPriority.BelowNormal;
                    Thread.Sleep(50);
                    //findConnect(para);
                }
            }).Start();


            listView.ItemsSource = androidClientList;
            Console.WriteLine("!!!!*******************$$$$$$$$$$$$$$$$$$$$$$$$$$$$");

            new Thread(() =>
            {
                TimeSpan ts = new TimeSpan(0, 0, 1);
                for (int i = 23; i > 0; i--)
                {
                    Thread.Sleep(ts);
                }

                UpdateFindButton updateFindButton = new UpdateFindButton(UpdateFindButtonUI);
                this.Dispatcher.Invoke(updateFindButton, true);
            }).Start();

            installButton.IsEnabled = true;
            uninstallButton.IsEnabled = true;
            customButton.IsEnabled = true;
            LogViewer.ScrollToEnd();
        }


        private void UpdateFindButtonUI(bool isable)
        {
            findButton.IsEnabled = true;
            findButton.Content = "查找设备";
            isFinding = false;
            LogLabel.Text = LogLabel.Text + "查找结束\n";
            
        }

        private void UpdateLogLabelUI(string str)
        {
            LogLabel.Text = LogLabel.Text + str;
            LogViewer.ScrollToEnd();
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
                                  // Console.WriteLine("*********&&&&&&&&&&& " + t.ToString() + "  is abort");

                    }
                    catch
                    {

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
            try
            {
            //    Console.WriteLine("findConnect " + data.ToString());
                string serverIp = data as string;
                // new process对象  
                System.Diagnostics.Process p = new System.Diagnostics.Process();

                // 设置属性  
                p.StartInfo.UseShellExecute = false;
                p.StartInfo.CreateNoWindow = true;
                p.StartInfo.FileName = "tryToConnect.bat";
                //p.StartInfo.FileName = "adb";
                p.StartInfo.RedirectStandardError = true;
                p.StartInfo.RedirectStandardInput = true;
                p.StartInfo.RedirectStandardOutput = true;
                String command = string.Empty;

                p.StartInfo.Arguments = serverIp;

                // 开启process线程  
                p.Start();
                // 获取返回结果，这个是最简单的字符串的形式返回，现在试试以其他的形式去读取返回值的结果。  

                string str = string.Empty;
                StreamReader readerout = p.StandardOutput;
                string line = string.Empty;
                while (!readerout.EndOfStream)
                {
                    line = readerout.ReadLine();
                    //Console.WriteLine(line);  
                    //将得到的结果写入到excle中  
                    //excut_result.Add(line);
                    Console.WriteLine(serverIp + "  " + line);
                    if (line.Contains("connected"))
                    {
                        try
                        {
                            UpdateUIDelegate updateUIDelegate = new UpdateUIDelegate(UpdateUI);

                            //通过调用委托
                            this.listView.Dispatcher.Invoke(updateUIDelegate, new AndroidClient(serverIp));

                        }
                        catch (Exception ex)
                        {
                            Console.WriteLine(ex.Message);
                        }

                    }
                }
                p.WaitForExit(3000);
                p.Close();
            }
            catch
            {
                //File.WriteAllText(data.ToString(), "find something wrong");
                Console.WriteLine("find something wrong");
            }
        }



        private void UpdateUI(AndroidClient ableAndroidClient)
        {
            LogLabel.Text = LogLabel.Text + "find " + ableAndroidClient.ip + " \n";
            androidClientList.Add(ableAndroidClient);
            numText.Text = androidClientList.Count.ToString();
            //对Listview绑定的数据源进行更新
        }












        private void Window_Closing(object sender, System.ComponentModel.CancelEventArgs e)
        {
            Console.WriteLine("is closing");
            resetClinet();
        }

        public void resetClinet()
        {
            listView.ItemsSource = null;
            // Console.WriteLine("*********&&&&&&&&&&&&&&&&&&&&&$$$$$$$$$$%%%%%%%%%%%%%%%%%%%%%%%%");
            for (int i = 0; i < androidClientList.Count; i++)
            {
                string _ip = androidClientList[0].ip;

                androidClientList.RemoveAt(0);
            }

            androidClientList.Clear();
            //ipToSocket.Clear();

        }

        //安装
        private void Button_Click_2(object sender, RoutedEventArgs e)
        {
            installButton.IsEnabled = false;
            uninstallButton.IsEnabled = false;
            customButton.IsEnabled = false;
            LogLabel.Text = "";
            //foreach (AndroidClient _ac in androidClientList)
            foreach (AndroidClient item in listView.SelectedItems)
            {
               
                Thread t1 = new Thread(new ParameterizedThreadStart(doInstall));
                t1.IsBackground = true;
                // threadList.Add(t1);
                t1.Start(item.ip);
                t1.Priority = ThreadPriority.BelowNormal;
                Thread.Sleep(20);
            }
           // installButton.IsEnabled = true;
        }


        //安装后启动，最后重启系统
        public void doInstall(object data)
        {
            Console.WriteLine("doInstall " + data.ToString());
            string serverIp = data as string;
            // new process对象  
            System.Diagnostics.Process p = new System.Diagnostics.Process();

            // 设置属性  
            p.StartInfo.UseShellExecute = false;
            p.StartInfo.CreateNoWindow = true;
            p.StartInfo.FileName = "install_restart.bat";
            p.StartInfo.RedirectStandardError = true;
            p.StartInfo.RedirectStandardInput = true;
            p.StartInfo.RedirectStandardOutput = true;
            String command = string.Empty;


            p.StartInfo.Arguments = serverIp;

            // 开启process线程  
            p.Start();
            // 获取返回结果，这个是最简单的字符串的形式返回，现在试试以其他的形式去读取返回值的结果。  

            string str = string.Empty;
            StreamReader readerout = p.StandardOutput;
            string line = string.Empty;
            Boolean isSuccess = false;
            UpdateLogLabel ull = new UpdateLogLabel(UpdateLogLabelUI);
            while (!readerout.EndOfStream)
            {
                line = readerout.ReadLine();
                //Console.WriteLine(line);  
                //将得到的结果写入到excle中  
                //excut_result.Add(line);
                Console.WriteLine(serverIp + "  " + line);
                str = serverIp + " : " + line + "\n";
                this.Dispatcher.Invoke(ull, str);
                if (line.Contains("Success"))
                {
                    try
                    {
                        Console.WriteLine(serverIp + " install success");
                        isSuccess = true;
                    }
                    catch (Exception ex)
                    {
                        Console.WriteLine(ex.Message);
                    }
                }
            }
            if (isSuccess)
                str = serverIp + " : install Success\n";
            else
                str = serverIp + " : install Failed\n";
            
            this.Dispatcher.Invoke(ull, str);
            p.WaitForExit();
            p.Close();
        }

        //卸载
        private void Button_Click_3(object sender, RoutedEventArgs e)
        {           
            uninstallButton.IsEnabled = false;
            LogLabel.Text = "";
            //  foreach (AndroidClient _ac in androidClientList)
            foreach (AndroidClient item in listView.SelectedItems)
            {
               
                Thread t1 = new Thread(new ParameterizedThreadStart(doUninstall));
                t1.IsBackground = true;
                // threadList.Add(t1);
                t1.Start(item.ip);
                t1.Priority = ThreadPriority.BelowNormal;
                Thread.Sleep(20);
            }
            uninstallButton.IsEnabled = true;
        }


        //卸载
        public void doUninstall(object data)
        {
            Console.WriteLine("doUninstall " + data.ToString());
            string serverIp = data as string;
            // new process对象  
            System.Diagnostics.Process p = new System.Diagnostics.Process();

            // 设置属性  
            p.StartInfo.UseShellExecute = false;
            p.StartInfo.CreateNoWindow = true;
            p.StartInfo.FileName = "auto_uninstall.bat";
            p.StartInfo.RedirectStandardError = true;
            p.StartInfo.RedirectStandardInput = true;
            p.StartInfo.RedirectStandardOutput = true;
            String command = string.Empty;


            p.StartInfo.Arguments = serverIp;

            // 开启process线程  
            p.Start();
            // 获取返回结果，这个是最简单的字符串的形式返回，现在试试以其他的形式去读取返回值的结果。  

            string str = string.Empty;
            StreamReader readerout = p.StandardOutput;
            string line = string.Empty;
            Boolean isSuccess = false;
            UpdateLogLabel ull = new UpdateLogLabel(UpdateLogLabelUI);
            while (!readerout.EndOfStream)
            {
                line = readerout.ReadLine();
                //Console.WriteLine(line);  
                //将得到的结果写入到excle中  
                //excut_result.Add(line);
                Console.WriteLine(serverIp + "  " + line);
                str = serverIp + " : " + line + "\n";
                this.Dispatcher.Invoke(ull, str);
                if (line.Contains("Success"))
                {
                    try
                    {
                        Console.WriteLine(serverIp + " auto_uninstall success");
                        isSuccess = true;
                    }
                    catch (Exception ex)
                    {
                        Console.WriteLine(ex.Message);
                    }
                }
            }
            if (isSuccess)
                str = serverIp + " : uninstall Success\n";
            else
                str = serverIp + " : uninstall Failed\n";
            this.Dispatcher.Invoke(ull, str);

            p.WaitForExit();
            p.Close();
        }




        private void OnSelectAllChanged(object sender, RoutedEventArgs e)
        {
            if (selectAll.IsChecked.HasValue && selectAll.IsChecked.Value)
                listView.SelectAll();
            else
                listView.UnselectAll();
        }


        private void OnUncheckItem(object sender, RoutedEventArgs e)
        {
            selectAll.IsChecked = false;
        }

        private void Button_Click_4(object sender, RoutedEventArgs e)
        {
            
        }

        private void Button_Click_1(object sender, RoutedEventArgs e)
        {
            foreach (AndroidClient item in listView.SelectedItems)
            {
                Console.WriteLine(" ****&&&&****   " + item.ip);
            }
        }

        //自定义
        private void Button_Click_5(object sender, RoutedEventArgs e)
        {
            customButton.IsEnabled = false;
            LogLabel.Text = "";
            foreach (AndroidClient item in listView.SelectedItems)
            {

                Thread t1 = new Thread(new ParameterizedThreadStart(doCustom));
                t1.IsBackground = true;
                // threadList.Add(t1);
                t1.Start(item.ip);
                t1.Priority = ThreadPriority.BelowNormal;
                Thread.Sleep(20);
            }
            customButton.IsEnabled = true;
        }


        //自定义
        public void doCustom(object data)
        {
            Console.WriteLine("customize " + data.ToString());
            string serverIp = data as string;
            // new process对象  
            System.Diagnostics.Process p = new System.Diagnostics.Process();

            // 设置属性  
            p.StartInfo.UseShellExecute = false;
            p.StartInfo.CreateNoWindow = true;
            p.StartInfo.FileName = "customize.bat";
            p.StartInfo.RedirectStandardError = true;
            p.StartInfo.RedirectStandardInput = true;
            p.StartInfo.RedirectStandardOutput = true;
            String command = string.Empty;


            p.StartInfo.Arguments = serverIp;

            // 开启process线程  
            p.Start();
            // 获取返回结果，这个是最简单的字符串的形式返回，现在试试以其他的形式去读取返回值的结果。  

            string str = string.Empty;
            StreamReader readerout = p.StandardOutput;
            string line = string.Empty;
            Boolean isSuccess = false;
            UpdateLogLabel ull = new UpdateLogLabel(UpdateLogLabelUI);
            while (!readerout.EndOfStream)
            {
                line = readerout.ReadLine();
                //Console.WriteLine(line);  
                //将得到的结果写入到excle中  
                //excut_result.Add(line);
                Console.WriteLine(serverIp + "  " + line);
                str = serverIp + " : " + line + "\n";
                this.Dispatcher.Invoke(ull, str);
                if (line.Contains("Success"))
                {
                    try
                    {
                        Console.WriteLine(serverIp + " doCunstom success");
                        isSuccess = true;
                    }
                    catch (Exception ex)
                    {
                        Console.WriteLine(ex.Message);
                    }
                }
            }
            if (isSuccess)
                str = serverIp + " : doCunstom Success\n";
            else
                str = serverIp + " : doCunstom Failed\n";            
            this.Dispatcher.Invoke(ull, str);
            p.WaitForExit();
            p.Close();
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









    }







    public class AndroidClient
    {

        public string ip { get; set; }




        public AndroidClient(string _ip)
        {

            ip = _ip;
        }

        public AndroidClient()
        {
        }
    }

    //AndroidClient androidClient = new AndroidClient();
    //string jsonAndroidClient = JsonConvert.SerializeObject(androidClient);
    //AndroidClient deserializedAndroidClient = JsonConvert.DeserializeObject<AndroidClient>(jsonAndroidClient);



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
            drawingContext.DrawGeometry(Brushes.Black, null, geometry);

            drawingContext.Pop();
        }
    }




}
