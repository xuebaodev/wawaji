/*
 * smart_player_sdk.cs
 * smart_player_sdk
 * 
 * Github: https://github.com/daniulive/SmarterStreaming
 * 
 * Created by DaniuLive on 2017/04/19.
 * Copyright © 2014~2017 DaniuLive. All rights reserved.
 */

using System;
using System.Runtime.InteropServices;

namespace NT
{
    public class NTSmartPlayerSDK
    {
		/* 
         * Init
         * 
         * flag目前传0，后面扩展用
         * pReserve传NULL,扩展用
         * 
         * 成功返回 NT_ERC_OK
		 */
        [DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_Init(UInt32 flag, IntPtr pReserve);
		
		/*
         * Uninit
         * 
         * 这个是最后一个调用的接口
         * 
         * 成功返回 NT_ERC_OK
		 */  
        [DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_UnInit();

        /*
         * Open
         * 
         * flag目前传0，后面扩展用
         * pReserve传NULL,扩展用
         * hwnd 绘制画面用的窗口
         * 
         * 获取Handle
         * 
         * 成功返回 NT_ERC_OK
         */
        [DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_Open(out IntPtr pHandle, IntPtr hwnd, UInt32 flag, IntPtr pReserve);
		
		/*
         * Close
         * 
         * 调用这个接口之后handle失效
         * 
         * 成功返回 NT_ERC_OK
		 */
		[DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_Close(IntPtr handle);
		
        
		/*
         * 设置事件回调，如果想监听事件的话，建议调用Open成功后，就调用这个接口
         */
        [DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_SetEventCallBack(IntPtr handle, IntPtr call_back_data, SP_SDKEventCallBack call_back);

		/*
         * 设置视频大小回调接口
		 */
		[DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_SetVideoSizeCallBack(IntPtr handle, IntPtr callbackdata, SP_SDKVideoSizeCallBack call_back);
		
		/*
         * 设置视频回调, 吐视频数据出来
         * frame_format: 只能是NT_SP_E_VIDEO_FRAME_FORMAT_RGB32, NT_SP_E_VIDEO_FRAME_FROMAT_I420
		 */
        [DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_SetVideoFrameCallBack(IntPtr handle, Int32 frame_format,
            IntPtr callbackdata, SP_SDKVideoFrameCallBack call_back);

       /*
		* 设置绘制视频帧时，视频帧时间戳回调
		* 注意如果当前播放流是纯音频，那么将不会回调，这个仅在有视频的情况下才有效
		*/
        [DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_SetRenderVideoFrameTimestampCallBack(IntPtr handle,
            IntPtr callbackdata, SP_SDKRenderVideoFrameTimestampCallBack call_back);

        /*
         * 设置音频PCM帧回调, 吐PCM数据出来，目前每帧大小是10ms.
		 */
        [DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_SetAudioPCMFrameCallBack(IntPtr handle,
            IntPtr call_back_data, SP_SDKAudioPCMFrameCallBack call_back);

		/*
         * 开始播放,传URL进去
		 */
		[DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_Start(IntPtr handle, [MarshalAs(UnmanagedType.LPStr)]String url,
            IntPtr callbackdata, SP_SDKStartPlayCallBack call_back);
		
		/*
         * 停止播放
		 */
		[DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_Stop(IntPtr handle);
		
        //新接口++

        /*
		 * 设置URL
		 * 成功返回NT_ERC_OK
		 */
        [DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_SetURL(IntPtr handle, [MarshalAs(UnmanagedType.LPStr)]String url);

		/*
         * handle: 播放句柄
         * hwnd: 这个要传入真正用来绘制的窗口句柄
         * is_support: 如果支持的话 *is_support 为1， 不支持的话为0
         * 接口调用成功返回NT_ERC_OK
		 */
        [DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_IsSupportD3DRender(IntPtr handle, IntPtr hwnd, ref Int32 is_support);

		/*
         * 设置绘制窗口句柄，如果在调用Open时设置过，那这个接口可以不调用
         * 如果在调用Open时设置为NULL，那么这里可以设置一个绘制窗口句柄给播放器
         * 成功返回NT_ERC_OK
		 */
        [DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_SetRenderWindow(IntPtr handle, IntPtr hwnd);

		/*
		 * 设置是否播放出声音，这个和静音接口是有区别的
		 * 这个接口的主要目的是为了用户设置了外部PCM回调接口后，又不想让SDK播放出声音时使用
		 * is_output_auido_device: 1: 表示允许输出到音频设备，默认是1， 0：表示不允许输出. 其他值接口返回失败
		 * 成功返回NT_ERC_OK
		 */
        [DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_SetIsOutputAudioDevice(IntPtr handle, Int32 is_output_auido_device);

        /*
		 * 开始播放, 注意NT_SP_StartPlay和NT_SP_Start不能混用，要么使用NT_SP_StartPlay, 要么使用NT_SP_Start.
		 * NT_SP_Start和NT_SP_Stop是老接口，不推荐用。请使用NT_SP_StartPlay和NT_SP_StopPlay新接口
		 */
        [DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_StartPlay(IntPtr handle);

		/*
		 * 停止播放
		 */
        [DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_StopPlay(IntPtr handle);

		/*
		 * 设置本地录像目录, 必须是英文目录，否则会失败
		 */
        [DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_SetRecorderDirectory(IntPtr handle, [MarshalAs(UnmanagedType.LPStr)] String dir);

		/*
		 * 设置单个录像文件最大大小, 当超过这个值的时候，将切割成第二个文件
		 * size: 单位是KB(1024Byte), 当前范围是 [5MB-800MB], 超出将被设置到范围内
		 */
        [DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_SetRecorderFileMaxSize(IntPtr handle, UInt32 size);

		/*
		 * 设置录像文件名生成规则
		 */
        [DllImport(@"SmartPlayerSDK.dll", EntryPoint = "NT_SP_SetRecorderFileNameRuler", CallingConvention = CallingConvention.StdCall)]
        public static extern UInt32 NT_SP_SetRecorderFileNameRuler(IntPtr handle, ref NT_SP_RecorderFileNameRuler ruler);

		/*
		 * 设置录像回调接口
		 */
        [DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_SetRecorderCallBack(IntPtr handle,
            IntPtr call_back_data, SP_SDKRecorderCallBack call_back);

		/*
		 * 启动录像
		 */
        [DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_StartRecorder(IntPtr handle);

		/*
		 * 停止录像
		 */
        [DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_StopRecorder(IntPtr handle);

		/*
         * 绘制窗口大小改变时，必须调用
		 */
		[DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_OnWindowSize(IntPtr handle, Int32 cx, Int32 cy);
		
		/*
         * 万能接口, 设置参数， 大多数问题， 这些接口都能解决
		 */
		[DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_SetParam(IntPtr handle, UInt32 id, IntPtr pData);
		
		/*
         * 万能接口, 得到参数， 大多数问题，这些接口都能解决
		 */
		[DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_GetParam(IntPtr handle, UInt32 id, IntPtr pData);	

		/*
         * 设置buffer,最小0ms
		 */
		[DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_SetBuffer(IntPtr handle, Int32 buffer);	
		
		/*
         * 静音接口，1为静音，0为不静音
		 */
		[DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_SetMute(IntPtr handle, Int32 is_mute);

		/*
         * 设置RTSP TCP 模式, 1为TCP, 0为UDP, 仅RTSP有效
		 */
		[DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_SetRTSPTcpMode(IntPtr handle, Int32 isUsingTCP);

		/*
         * 设置秒开, 1为秒开, 0为不秒开
		 */
		[DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_SetFastStartup(IntPtr handle, Int32 isFastStartup);

        /*
         * 设置下载速度上报, 默认不上报下载速度
         * is_report: 上报开关, 1: 表上报. 0: 表示不上报. 其他值无效.
         * report_interval： 上报时间间隔（上报频率），单位是秒，最小值是1秒1次. 如果小于1且设置了上报，将调用失败
         * 注意：如果设置上报的话，请设置SetEventCallBack, 然后在回调函数里面处理这个事件.
         * 上报事件是：NT_SP_E_EVENT_ID_DOWNLOAD_SPEED
         * 这个接口必须在StartXXX之前调用
         * 成功返回NT_ERC_OK
		 */
        [DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_SetReportDownloadSpeed(IntPtr handle, Int32 is_report, Int32 report_interval);

		/*
         * 主动获取下载速度
         * speed： 返回下载速度，单位是Byte/s
         * （注意：这个接口必须在startXXX之后调用，否则会失败）
         * 成功返回NT_ERC_OK
		 */
        [DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_GetDownloadSpeed(IntPtr handle, ref Int32 speed);

        /*
		 * 捕获图片
		 * file_name_utf8: 文件名称，utf8编码
		 * call_back_data: 回调时用户自定义数据
		 * call_back: 回调函数，用来通知用户截图已经完成或者失败
		 * 成功返回 NT_ERC_OK
		 * 只有在播放时调用才可能成功，其他情况下调用，返回错误.
		 * 因为生成PNG文件比较耗时，一般需要几百毫秒,为防止CPU过高，SDK会限制截图请求数量,当超过一定数量时，
		 * 调用这个接口会返回NT_ERC_SP_TOO_MANY_CAPTURE_IMAGE_REQUESTS. 这种情况下, 请延时一段时间，等SDK处理掉一些请求后，再尝试.
		 */
        [DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_CaptureImage(IntPtr handle, [MarshalAs(UnmanagedType.LPStr)] String file_name_utf8,
            IntPtr call_back_data, SP_SDKCaptureImageCallBack call_back);

        /*
		 * 使用GDI绘制RGB32数据
		 * 32位的rgb格式, r, g, b各占8, 另外一个字节保留, 内存字节格式为: bb gg rr xx, 主要是和windows位图匹配, 在小端模式下，按DWORD类型操作，最高位是xx, 依次是rr, gg, bb
		 * 为了保持和windows位图兼容，步长(image_stride)必须是width_*4
		 * handle: 播放器句柄
		 * hdc: 绘制dc
		 * x_dst: 绘制面左上角x坐标
		 * y_dst: 绘制面左上角y坐标
		 * dst_width: 要绘制的宽度
		 * dst_height： 要绘制的高度
		 * x_src: 源图像x位置
		 * y_src: 原图像y位置
		 * rgb32_data: rgb32数据，格式参见前面的注释说明
		 * rgb32_data_size: 数据大小
		 * image_width： 图像实际宽度
		 * image_height： 图像实际高度
		 * image_stride： 图像步长
		 */
        [DllImport(@"SmartPlayerSDK.dll")]
        public static extern UInt32 NT_SP_GDIDrawRGB32(IntPtr handle, IntPtr hdc,
            Int32 x_dst, Int32 y_dst,
			Int32 dst_width, Int32 dst_height,
			Int32 x_src, Int32 y_src,
			Int32 src_width, Int32 src_height,
			IntPtr rgb32_data, UInt32 rgb32_data_size,
			Int32 image_width, Int32 image_height,
			Int32 image_stride);
	}
}