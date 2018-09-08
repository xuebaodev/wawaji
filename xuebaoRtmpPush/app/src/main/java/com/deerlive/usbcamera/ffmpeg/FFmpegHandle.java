package com.deerlive.usbcamera.ffmpeg;

import android.content.Context;

/**
 * Author : eric
 * CreateDate : 2017/11/1  15:32
 * Email : ericli_wang@163.com
 * Version : 2.0
 * Desc :
 * Modified :
 */

public class FFmpegHandle {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("avutil");
        System.loadLibrary("swresample");
        System.loadLibrary("avcodec");
        System.loadLibrary("avformat");
        System.loadLibrary("swscale");
        System.loadLibrary("avfilter");
        System.loadLibrary("avdevice");
        System.loadLibrary("postproc");
        System.loadLibrary("ffmpeg-handle");
    }

    public native void SetConfig(int jw, int jh, int jfps, int jbitrate);

    public native int initVideo1(String url);

    public native int onFrameCallback1(byte[] buffer);

    public native int close1();

    public native int initVideo2(String url);

    public native int onFrameCallback2(byte[] buffer);

    public native int close2();
}
