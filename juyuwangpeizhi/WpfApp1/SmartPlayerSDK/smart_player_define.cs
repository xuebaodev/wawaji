/*
 * smart_player_define.cs
 * smart_player_define
 * 
 * Github: https://github.com/daniulive/SmarterStreaming
 * 
 * Created by DaniuLive on 2015/07/16.
 * Copyright © 2014~2017 DaniuLive. All rights reserved.
 */

using System;
using System.Runtime.InteropServices;

namespace NT
{
    public class NTSmartPlayerDefine
    {			
		/*错误码*/
		public enum SP_E_ERROR_CODE : uint
		{
			NT_ERC_SP_HWND_IS_NULL = NTBaseCodeDefine.NT_ERC_SMART_PLAYER_SDK | 0x1, // 窗口句柄是空
			NT_ERC_SP_HWND_INVALID = NTBaseCodeDefine.NT_ERC_SMART_PLAYER_SDK | 0x2, // 窗口句柄无效
            NT_ERC_SP_TOO_MANY_CAPTURE_IMAGE_REQUESTS = NTBaseCodeDefine.NT_ERC_SMART_PLAYER_SDK | 0x3, // 太多的截图请求
            NT_ERC_SP_WINDOW_REGION_INVALID = NTBaseCodeDefine.NT_ERC_SMART_PLAYER_SDK | 0x4, // 窗口区域无效，可能窗口宽或者高小于1
            NT_ERC_SP_DIR_NOT_EXIST = NTBaseCodeDefine.NT_ERC_SMART_PLAYER_SDK | 0x5, // 目录不存在
		}

		/*设置参数ID, 这个目前这么写，SmartPlayerSDK 已经划分范围*/
		public enum SP_E_PARAM_ID : uint
		{
			SP_PARAM_ID_BASE = NTBaseCodeDefine.NT_PARAM_ID_SMART_PLAYER_SDK,
		}

        /*事件ID*/
        public enum NT_SP_E_EVENT_ID : uint
        {
            NT_SP_E_EVENT_ID_BASE = NTBaseCodeDefine.NT_EVENT_ID_SMART_PLAYER_SDK,

	        NT_SP_E_EVENT_ID_CONNECTING			= NT_SP_E_EVENT_ID_BASE | 0x2,	/*连接中*/
	        NT_SP_E_EVENT_ID_CONNECTION_FAILED	= NT_SP_E_EVENT_ID_BASE | 0x3,	/*连接失败*/
	        NT_SP_E_EVENT_ID_CONNECTED			= NT_SP_E_EVENT_ID_BASE | 0x4,	/*已连接*/
	        NT_SP_E_EVENT_ID_DISCONNECTED		= NT_SP_E_EVENT_ID_BASE | 0x5,	/*断开连接*/

	        /* 接下来请从0x81开始*/
	        NT_SP_E_EVENT_ID_START_BUFFERING = NT_SP_E_EVENT_ID_BASE | 0x81, /*开始缓冲*/
	        NT_SP_E_EVENT_ID_BUFFERING		 = NT_SP_E_EVENT_ID_BASE | 0x82, /*缓冲中, param1 表示百分比进度*/
	        NT_SP_E_EVENT_ID_STOP_BUFFERING  = NT_SP_E_EVENT_ID_BASE | 0x83, /*停止缓冲*/

	        NT_SP_E_EVENT_ID_DOWNLOAD_SPEED  = NT_SP_E_EVENT_ID_BASE | 0x91, /*下载速度， param1表示下载速度，单位是(Byte/s)*/
        }


        /*定义视频帧图像格式*/
        public enum NT_SP_E_VIDEO_FRAME_FORMAT : uint
        {
            NT_SP_E_VIDEO_FRAME_FORMAT_RGB32 = 1, // 32位的rgb格式, r, g, b各占8, 另外一个字节保留, 内存字节格式为: bb gg rr xx, 主要是和windows位图匹配, 在小端模式下，按DWORD类型操作，最高位是xx, 依次是rr, gg, bb
            NT_SP_E_VIDEO_FRAME_FORMAT_ARGB = 2, // 32位的argb格式，内存字节格式是: bb gg rr aa 这种类型，和windows位图匹配
            NT_SP_E_VIDEO_FRAME_FROMAT_I420 = 3, // YUV420格式, 三个分量保存在三个面上
        }
    }

    /*如果三项都是0的话，将不能启动录像*/
    [StructLayoutAttribute(LayoutKind.Sequential)]
    public struct NT_SP_RecorderFileNameRuler
    {
        public UInt32 type_;                                          // 这个值目前默认是0，将来扩展用
        [MarshalAs(UnmanagedType.LPStr)] public String file_name_prefix_;  // 设置一个录像文件名前缀, 例如:daniulive
        public Int32 append_date_;                                    // 如果是1的话，将在文件名上加日期, 例如:daniulive-2017-01-17
        public Int32 append_time_;                                    // 如果是1的话，将增加时间，例如:daniulive-2017-01-17-17-10-36
    }

    /*定义视频帧结构.*/
    [StructLayoutAttribute(LayoutKind.Sequential)]
    public struct NT_SP_VideoFrame
    {
        public Int32 format_;  // 图像格式, 请参考NT_SP_E_VIDEO_FRAME_FORMAT
	    public Int32 width_;   // 图像宽
	    public Int32 height_;  // 图像高

        public Int64 timestamp_; // 时间戳, 一般是0,不使用, 以ms为单位的

	    //具体的图像数据, argb和rgb32只用第一个, I420用前三个
	    public IntPtr plane0_;
	    public IntPtr plane1_;
	    public IntPtr plane2_;
	    public IntPtr plane3_;

	    // 每一个平面的每一行的字节数，对于argb和rgb32，为了保持和windows位图兼容，必须是width_*4
	    // 对于I420, stride0_ 是y的步长, stride1_ 是u的步长, stride2_ 是v的步长,
	    public Int32 stride0_;
	    public Int32 stride1_;
	    public Int32 stride2_;
	    public Int32 stride3_;
    }

    /*
     *拉流吐视频数据时，一些相关的数据
     */
    [StructLayoutAttribute(LayoutKind.Sequential)]
    public struct NT_SP_PullStreamVideoDataInfo
    {
        public Int32 is_key_frame_; /* 1:表示关键帧, 0：表示非关键帧 */
        public UInt64 timestamp_;	/* 单位是毫秒 */
	    public Int32 width_;	/* 一般是0 */
	    public Int32 height_; /* 一般也是0 */
	    public IntPtr parameter_info_; /* 一般是NULL */
	    public UInt32 parameter_info_size_; /* 一般是0 */
	    public UInt64 reserve_; /* 保留  */
    }

    /*
     *拉流吐音频数据时，一些相关的数据
     */
    [StructLayoutAttribute(LayoutKind.Sequential)]
    public struct NT_SP_PullStreamAuidoDataInfo
    {
        public Int32 is_key_frame_; /* 1:表示关键帧, 0：表示非关键帧 */
        public UInt64 timestamp_;	/* 单位是毫秒 */
	    public Int32 sample_rate_;	/* 一般是0 */
	    public Int32 channel_; /* 一般是0 */
	    public IntPtr parameter_info_; /* 如果是AAC的话，这个是有值的, 其他编码一般忽略 */
	    public UInt32 parameter_info_size_; /*如果是AAC的话，这个是有值的, 其他编码一般忽略 */
	    public UInt64 reserve_; /* 保留  */
    }

    /*
     * 当播放器得到时候视频大小后，会回调
     */
    [UnmanagedFunctionPointerAttribute(CallingConvention.StdCall)]
    public delegate void SP_SDKVideoSizeCallBack(IntPtr handle, IntPtr user_data,
        Int32 width, Int32 height);

    /*
     * 视频图像回调
     * status:目前不用，默认是0，将来可能会用
     * frame: NT_SP_VideoFrame* 
     */
    [UnmanagedFunctionPointerAttribute(CallingConvention.StdCall)]
    public delegate void SP_SDKVideoFrameCallBack(IntPtr handle, IntPtr user_data,
        UInt32 status, IntPtr frame);

    /*
     * 音频PCM数据回调, 目前每帧长度是10ms
     * status:目前不用，默认是0，将来可能会用
     * data: PCM 数据
     * size: 数据大小
     * sample_rate: 采样率
     * channel: 通道数
     * per_channel_sample_number: 每个通道的采样数
     */
     [UnmanagedFunctionPointerAttribute(CallingConvention.StdCall)]
    public delegate void SP_SDKAudioPCMFrameCallBack(IntPtr handle, IntPtr user_data,
        UInt32 status, IntPtr data, UInt32 size,
        Int32 sample_rate, Int32 channel, Int32 per_channel_sample_number);

    /*
     * 绘制视频时，视频帧时间戳回调, 这个用在一些特殊场景下，没有特殊需求的用户不需要关注
     * timestamp: 单位是毫秒
     * reserve1: 保留参数
     * reserve2: 保留参数
     */
    [UnmanagedFunctionPointerAttribute(CallingConvention.StdCall)]
     public delegate void SP_SDKRenderVideoFrameTimestampCallBack(IntPtr handle, IntPtr user_data,
        UInt64 timestamp, UInt64 reserve1, IntPtr reserve2);

    /*
     * 截屏回调
     * result: 如果截屏成功的话，result是NT_ERC_OK,其他错误
     */
     [UnmanagedFunctionPointerAttribute(CallingConvention.StdCall)]
    public delegate void SP_SDKCaptureImageCallBack(IntPtr handle, IntPtr user_data, UInt32 result, [MarshalAs(UnmanagedType.LPStr)] String file_name);

    /*
     * 录像回调
     * status: 1:表示开始写一个新录像文件. 2:表示已经写好一个录像文件
     * file_name: 实际录像文件名
     */
    [UnmanagedFunctionPointerAttribute(CallingConvention.StdCall)]
     public delegate void SP_SDKRecorderCallBack(IntPtr handle, IntPtr user_data, UInt32 status, [MarshalAs(UnmanagedType.LPStr)] String file_name);

    /*
     * 调用Start时传入, 回调接口
     */
    [UnmanagedFunctionPointerAttribute(CallingConvention.StdCall)]
    public delegate void SP_SDKStartPlayCallBack(IntPtr handle, IntPtr user_data, UInt16 result);

    /*
     * 拉流时，视频数据回调
     * video_codec_id: 请参考NT_MEDIA_CODEC_ID
     * data: 视频数据
     * size: 视频数据大小
     * info: 视频数据相关信息，请参考NT_SP_PullStreamVideoDataInfo
     * reserve: 保留参数
     */
    [UnmanagedFunctionPointerAttribute(CallingConvention.StdCall)]
    public delegate void SP_SDKPullStreamVideoDataCallBack(IntPtr handle, IntPtr user_data, 
        UInt32 video_codec_id, IntPtr data, UInt32 size,
        IntPtr info, IntPtr reserve);

    /* 
     * 拉流时，音频数据回调
     * auido_codec_id: 请参考NT_MEDIA_CODEC_ID
     * data: 音频数据
     * size: 音频数据大小
     * info: 音频数据相关信息，请参考NT_SP_PullStreamAuidoDataInfo
     * reserve: 保留参数
     */
     [UnmanagedFunctionPointerAttribute(CallingConvention.StdCall)]
     public delegate void SP_SDKPullStreamAudioDataCallBack(IntPtr handle, IntPtr user_data, 
        UInt32 auido_codec_id, IntPtr data, UInt32 size,
        IntPtr info, IntPtr reserve);

    /*
     * *播放器事件回调
     * event_id: 事件ID，请参考NT_SP_E_EVENT_ID
     * param1 到 param6, 值的意义和具体事件ID相关, 注意如果具体事件ID没有说明param1-param6的含义，那说明这个事件不带参数
     */
     [UnmanagedFunctionPointerAttribute(CallingConvention.StdCall)]
     public delegate void SP_SDKEventCallBack(IntPtr handle, IntPtr user_data, 
        UInt32 event_id, 
        Int64 param1, 
        Int64 param2, 
        UInt64 param3, 
        IntPtr param4,
        IntPtr param5,
        IntPtr param6);
}