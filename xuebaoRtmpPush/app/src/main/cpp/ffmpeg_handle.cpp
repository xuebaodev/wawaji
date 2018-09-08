//
// Created by eric on 2017/11/1.
//
#include <jni.h>
#include <string>
#include<android/log.h>
#include <exception>

//定义日志宏变量
#define logw(content)   __android_log_write(ANDROID_LOG_WARN,"eric",content)
#define loge(content)   __android_log_write(ANDROID_LOG_ERROR,"eric",content)
#define logd(content)   __android_log_write(ANDROID_LOG_DEBUG,"eric",content)
#define ALOG(level, TAG, ...)    ((void)__android_log_vprint(level, TAG, __VA_ARGS__))
#define SYS_LOG_TAG "eric"

extern "C" {
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
//引入时间
#include "libavutil/time.h"
#include "libavutil/imgutils.h"
#include "libavutil/log.h"
}

#include <iostream>

using namespace std;

jobject pushCallback = NULL;
jclass cls = NULL;
jmethodID mid = NULL;

int callback(JNIEnv *env, int64_t pts, int64_t dts, int64_t duration, long long index) {
//    logw("=================")
    if (pushCallback == NULL) {
        return -3;
    }
    if (cls == NULL) {
        return -1;
    }
    if (mid == NULL) {
        return -2;
    }
    env->CallVoidMethod(pushCallback, mid, (jlong) pts, (jlong) dts, (jlong) duration,
                        (jlong) index);
    return 0;
}

int avError(int errNum) {
    char buf[1024];
    //获取错误信息
    av_strerror(errNum, buf, sizeof(buf));
    loge(string().append("发生异常：").append(buf).c_str());
    return -1;
}

//获取FFmpeg相关信息
extern "C"
JNIEXPORT jstring JNICALL
Java_com_deerlive_usbcamera_ffmpeg_FFmpegHandle_getAvcodecConfiguration(JNIEnv *env,
                                                                  jobject instance) {
    char info[10000] = {0};
    sprintf(info, "%s\n", avcodec_configuration());
    return env->NewStringUTF(info);
}

/**
 * 设置回到对象
 */
extern "C"
JNIEXPORT jint JNICALL
Java_com_deerlive_usbcamera_ffmpeg_FFmpegHandle_setCallback(JNIEnv *env, jobject instance,
                                                      jobject pushCallback1) {
    //转换为全局变量
    pushCallback = env->NewGlobalRef(pushCallback1);
    if (pushCallback == NULL) {
        return -3;
    }
    cls = env->GetObjectClass(pushCallback);
    if (cls == NULL) {
        return -1;
    }
    mid = env->GetMethodID(cls, "videoCallback", "(JJJJ)V");
    if (mid == NULL) {
        return -2;
    }
    env->CallVoidMethod(pushCallback, mid, (jlong) 0, (jlong) 0, (jlong) 0, (jlong) 0);
    return 0;
}

//=======================================================================
//
// 摄像头采集数据并输出（文件/RTMP推流）
//
//=======================================================================

static void syslog_print(void *ptr, int level, const char *fmt, va_list vl)
{
    switch(level) {
        case AV_LOG_DEBUG:
            ALOG(ANDROID_LOG_VERBOSE, SYS_LOG_TAG, fmt, vl);
            break;
        case AV_LOG_VERBOSE:
            ALOG(ANDROID_LOG_DEBUG, SYS_LOG_TAG, fmt, vl);
            break;
        case AV_LOG_INFO:
            ALOG(ANDROID_LOG_INFO, SYS_LOG_TAG, fmt, vl);
            break;
        case AV_LOG_WARNING:
            ALOG(ANDROID_LOG_WARN, SYS_LOG_TAG, fmt, vl);
            break;
        case AV_LOG_ERROR:
            ALOG(ANDROID_LOG_ERROR, SYS_LOG_TAG, fmt, vl);
            break;
    }
}

int yuv_width;
int yuv_height;
int y_length;
int uv_length;
int width = 480;
int height = 640;
int fps = 25;
jlong gbitrate = 500000;//500K
int64_t startTime = 0;

// * 初始化
extern "C"
JNIEXPORT void JNICALL
Java_com_deerlive_usbcamera_ffmpeg_FFmpegHandle_SetConfig(JNIEnv *env, jobject instance,
                                                                 jint jw, jint jh, jint jfps, jint jbitrate) {
    width = jw;
    height = jh;
   // fps = jfps;
    gbitrate = jbitrate *1000;
}

 //简单修改2路之地一路的变量
AVFormatContext *ofmt_ctx1;
AVStream *video_st1;
AVCodecContext *pCodecCtx1;
AVPacket enc_pkt1;
AVFrame *pFrameYUV1;
int count1 = 0;

extern "C"
JNIEXPORT jint JNICALL
Java_com_deerlive_usbcamera_ffmpeg_FFmpegHandle_initVideo1(JNIEnv *env, jobject instance,
                                                    jstring url_) {
    const char *out_path = env->GetStringUTFChars(url_, 0);
    logd(out_path);

    AVCodec *pCodec;

    //计算yuv数据的长度
    yuv_width = width;
    yuv_height = height;
    y_length = width * height;
    uv_length = width * height / 4;

    av_register_all();
    avformat_network_init();
    av_log_set_level(AV_LOG_DEBUG);
    av_log_set_callback(syslog_print);
    //output initialize
    avformat_alloc_output_context2(&ofmt_ctx1, NULL, "mpegts", out_path);
    //output encoder initialize
    pCodec = avcodec_find_encoder(AV_CODEC_ID_MPEG1VIDEO);
    if (!pCodec) {
        loge("Can not find encoder!\n");
        return -1;
    }
    pCodecCtx1 = avcodec_alloc_context3(pCodec);
    //编码器的ID号，这里为264编码器，可以根据video_st里的codecID 参数赋值
    pCodecCtx1->codec_id = pCodec->id;
    //像素的格式，也就是说采用什么样的色彩空间来表明一个像素点
    pCodecCtx1->pix_fmt = AV_PIX_FMT_YUV420P;
    //编码器编码的数据类型
    pCodecCtx1->codec_type = AVMEDIA_TYPE_VIDEO;
    //编码目标的视频帧大小，以像素为单位
    pCodecCtx1->width = width;
    pCodecCtx1->height = height;
    pCodecCtx1->framerate = (AVRational) {fps, 1};
//    pCodecCtx1->time_base.den = fps;
//    pCodecCtx1->time_base.num = 1;
    //帧率的基本单位，我们用分数来表示，
    pCodecCtx1->time_base = (AVRational) {1,fps};
    //目标的码率，即采样的码率；显然，采样码率越大，视频大小越大
    pCodecCtx1->bit_rate = gbitrate * 2;     //gbitrate;//600000
    //固定允许的码率误差，数值越大，视频越小
    pCodecCtx1->bit_rate_tolerance = 0;

//    pCodecCtx1->keyint_min = 5;
//    pCodecCtx1->gop_size = 25;
    pCodecCtx1->max_b_frames = 0;

//    pCodecCtx1->has_b_frames = 0;

//    pCodecCtx1->qcompress = 0;
//    pCodecCtx1->me_range = 64;
//    pCodecCtx1->thread_count = 2;

//    pCodecCtx1->max_b_frames = 2;
//    pCodecCtx1->flags2 = 1;
//    pCodecCtx1->qcompress = 0;
//    pCodecCtx1->me_range = 16;
//    pCodecCtx1->thread_count = 2;

   pCodecCtx1->rc_min_rate = gbitrate * 1.5 ;     //gbitrate;
    pCodecCtx1->rc_max_rate = gbitrate * 2;     //gbitrate;


  //  pCodecCtx1->rc_buffer_size = gbitrate;
   //  pCodecCtx1->rc_initial_buffer_occupancy = gbitrate * 3 / 4;

   // pCodecCtx1->bit_rate_tolerance

   // pCodecCtx1->rc_buffer_aggressivity = (float)1.0;
//    pCodecCtx1->rc_initial_cplx = 0.5;



    //pCodecCtx1->gop_size = 300;
    //Some formats want stream headers to be separate.
   // if (ofmt_ctx1->oformat->flags & AVFMT_GLOBALHEADER)
    //    pCodecCtx1->flags |= CODEC_FLAG_GLOBAL_HEADER;

    //H264 codec param
 //   pCodecCtx->me_range = 16;
    //pCodecCtx->max_qdiff = 4;
//    pCodecCtx1->qcompress = 0.6;
    //最大和最小量化系数
//    pCodecCtx1->qmin = 10;
//    pCodecCtx1->qmax = 51;
    //Optional Param
    //两个非B帧之间允许出现多少个B帧数
    //设置0表示不使用B帧
    //b 帧越多，图片越小
//    pCodecCtx1->max_b_frames = 0;
    // Set H264 preset and tune
    AVDictionary *param = 0;
    //H.264
    if (pCodecCtx1->codec_id == AV_CODEC_ID_H264) {
//        av_dict_set(&param, "preset", "slow", 0);

        // * 这个非常重要，如果不设置延时非常的大
        // * ultrafast,superfast, veryfast, faster, fast, medium
       //  * slow, slower, veryslow, placebo.　这是x264编码速度的选项

       av_dict_set(&param, "preset", "superfast", 0);
        //av_dict_set(&param, "preset", "medium", 0);
        av_dict_set(&param, "tune", "zerolatency", 0);

     //   av_dict_set(&param, "profile", "main", 0);

//        av_dict_set(&param, "tune", "fastdecode", 0);
    }
    int ret = avcodec_open2(pCodecCtx1, pCodec, &param);
    if (ret < 0) {
        loge("Failed to open encoder!\n");
        return -1;
    }

    //Add a new stream to output,should be called by the user before avformat_write_header() for muxing
    video_st1 = avformat_new_stream(ofmt_ctx1, pCodec);
    if (video_st1 == NULL) {
        avcodec_close(pCodecCtx1);
        pCodecCtx1= NULL;
        return -1;
    }
    video_st1->time_base.num = 1;
    video_st1->time_base.den = fps;
//    video_st->codec = pCodecCtx;
    //video_st->codecpar->codec_tag = 0;
    avcodec_parameters_from_context(video_st1->codecpar, pCodecCtx1);

    //Open output URL,set before avformat_write_header() for muxing
    if (avio_open(&ofmt_ctx1->pb, out_path, AVIO_FLAG_READ_WRITE) < 0) {
        loge("Failed to open output file!\n");

        if (video_st1)
            avcodec_close(video_st1->codec);

        if (ofmt_ctx1) {
            avio_close(ofmt_ctx1->pb);
            avformat_free_context(ofmt_ctx1);
            ofmt_ctx1 = NULL;
        }

        avcodec_close(pCodecCtx1);
        pCodecCtx1= NULL;
        return -1;
    }

    //Write File Header
    avformat_write_header(ofmt_ctx1, NULL);

    startTime = av_gettime();
    return 0;
}


// * H264编码并输出

extern "C"
JNIEXPORT jint JNICALL
Java_com_deerlive_usbcamera_ffmpeg_FFmpegHandle_onFrameCallback1(JNIEnv *env, jobject instance,
                                                          jbyteArray buffer_) {
//    startTime = av_gettime();
    jbyte *in = env->GetByteArrayElements(buffer_, NULL);

    if( pCodecCtx1 == NULL || ofmt_ctx1== NULL)//视频地址错误。初始化不成功。则不做后面的逻辑了。省的浪费,也会崩溃
        return -1;

    int ret = 0;
//初始化一个帧的数据结构，用于编码用
    //指定AV_PIX_FMT_YUV420P这种格式的
    pFrameYUV1 = av_frame_alloc();
    uint8_t *out_buffer = (uint8_t *) av_malloc(av_image_get_buffer_size(pCodecCtx1->pix_fmt, width, height, 1));
    av_image_fill_arrays(pFrameYUV1->data, pFrameYUV1->linesize, out_buffer, pCodecCtx1->pix_fmt, width, height, 1);

    //安卓摄像头数据为NV21格式，此处将其转换为YUV420P格式
    ////N21   0~width * height是Y分量，  width*height~ width*height*3/2是VU交替存储
    //复制Y分量的数据
    memcpy(pFrameYUV1->data[0], in, y_length); //Y

    for (int i = 0; i < uv_length; i++) {
        //将v数据存到第三个平面
        *(pFrameYUV1->data[2] + i) = *(in + y_length + i * 2);
        //将U数据存到第二个平面
        *(pFrameYUV1->data[1] + i) = *(in + y_length + i * 2 + 1);
    }

    pFrameYUV1->format = pCodecCtx1->pix_fmt;
    pFrameYUV1->width = yuv_width;
    pFrameYUV1->height = yuv_height;
    pFrameYUV1->pts = ++count1;
    //pFrameYUV->pts = (1.0 / 30) * 90 * count;                     ////////////////////////////
    //例如对于H.264来说。1个AVPacket的data通常对应一个NAL
    //初始化AVPacket
    enc_pkt1.data = NULL;
    enc_pkt1.size = 0;
    av_init_packet(&enc_pkt1);
//    __android_log_print(ANDROID_LOG_WARN, "eric", "编码前时间:%lld",
//                        (long long) ((av_gettime() - startTime) / 1000));
    //开始编码YUV数据
    ret = avcodec_send_frame(pCodecCtx1, pFrameYUV1);
    if (ret != 0) {
        loge("avcodec_send_frame error");
        return ret;
    }

    //获取编码后的数据
    ret = avcodec_receive_packet(pCodecCtx1, &enc_pkt1);
    av_frame_free(&pFrameYUV1);
    av_free(out_buffer);
//    __android_log_print(ANDROID_LOG_WARN, "eric", "编码时间:%lld",
//                        (long long) ((av_gettime() - startTime) / 1000));
    //是否编码前的YUV数据
    if (ret != 0 || enc_pkt1.size <= 0) {
        loge("avcodec_receive_packet error");
        avError(ret);
        return -2;
    }
    enc_pkt1.stream_index = video_st1->index;

    AVRational time_base = ofmt_ctx1->streams[0]->time_base;

    AVRational r_frame_rate1 = pCodecCtx1->framerate;

    AVRational time_base_q = {1, AV_TIME_BASE};
    int64_t calc_duration = (double)(AV_TIME_BASE) * (1 / av_q2d(r_frame_rate1));

    enc_pkt1.pts = av_rescale_q(count1 * calc_duration, time_base_q, time_base);
    enc_pkt1.dts = enc_pkt1.pts;
    enc_pkt1.duration = av_rescale_q(calc_duration, time_base_q, time_base);
    enc_pkt1.pos = -1;
    //loge("before send.");
    ret = av_interleaved_write_frame(ofmt_ctx1, &enc_pkt1);
   // loge("after send.");
    if (ret != 0) {
        loge("av_interleaved_write_frame failed");
        env->ReleaseByteArrayElements(buffer_, in, 0);
        return ret;
    }else{
        // count++;
        env->ReleaseByteArrayElements(buffer_, in, 0);
        return 0;
    }
}


//释放资源

extern "C"
JNIEXPORT jint JNICALL
Java_com_deerlive_usbcamera_ffmpeg_FFmpegHandle_close1(JNIEnv *env, jobject instance) {
    loge("close1  1");
    if (video_st1)
        avcodec_close(video_st1->codec);

    loge("close1  2");
    if(pFrameYUV1) {
        av_frame_free(&pFrameYUV1);
        pFrameYUV1 = NULL;
    }

    loge("close1 3");
    if (ofmt_ctx1) {
        loge("close1 3.5");
        avio_close(ofmt_ctx1->pb);
        loge("close1 3.6");
        avformat_free_context(ofmt_ctx1);
        loge("close1 3.7");
        ofmt_ctx1 = NULL;
    }

    loge("close1 4");
    avcodec_close(pCodecCtx1);
    pCodecCtx1= NULL;

    loge("close1 5");
    return 0;
}


//==============2========================
//简单修改2路之地2路的变量
AVFormatContext *ofmt_ctx2;
AVStream *video_st2;
AVCodecContext *pCodecCtx2;
AVPacket enc_pkt2;
AVFrame *pFrameYUV2;
int count2 = 0;

extern "C"
JNIEXPORT jint JNICALL
Java_com_deerlive_usbcamera_ffmpeg_FFmpegHandle_initVideo2(JNIEnv *env, jobject instance,
                                                           jstring url_) {
    const char *out_path = env->GetStringUTFChars(url_, 0);
    logd(out_path);

    AVCodec *pCodec;

    //计算yuv数据的长度
    yuv_width = width;
    yuv_height = height;
    y_length = width * height;
    uv_length = width * height / 4;

    av_register_all();
    avformat_network_init();
    av_log_set_level(AV_LOG_DEBUG);
    av_log_set_callback(syslog_print);
    //output initialize
    avformat_alloc_output_context2(&ofmt_ctx2, NULL, "mpegts", out_path);
    //output encoder initialize
    pCodec = avcodec_find_encoder(AV_CODEC_ID_MPEG1VIDEO);
    if (!pCodec) {
        loge("Can not find encoder!\n");
        return -1;
    }
    pCodecCtx2 = avcodec_alloc_context3(pCodec);
    //编码器的ID号，这里为264编码器，可以根据video_st里的codecID 参数赋值
    pCodecCtx2->codec_id = pCodec->id;
    //像素的格式，也就是说采用什么样的色彩空间来表明一个像素点
    pCodecCtx2->pix_fmt = AV_PIX_FMT_YUV420P;
    //编码器编码的数据类型
    pCodecCtx2->codec_type = AVMEDIA_TYPE_VIDEO;
    //编码目标的视频帧大小，以像素为单位
    pCodecCtx2->width = width;
    pCodecCtx2->height = height;
    pCodecCtx2->framerate = (AVRational) {fps, 1};
    //帧率的基本单位，我们用分数来表示，
    pCodecCtx2->time_base = (AVRational) {1,fps};
    //目标的码率，即采样的码率；显然，采样码率越大，视频大小越大
    pCodecCtx2->bit_rate = gbitrate * 2 ;     //gbitrate;
    //固定允许的码率误差，数值越大，视频越小
    pCodecCtx2->bit_rate_tolerance = 0;

//    pCodecCtx2->keyint_min = 5;
//    pCodecCtx2->gop_size = 25;
    pCodecCtx2->max_b_frames = 0;
//
//    pCodecCtx2->qcompress = 0;
//    pCodecCtx2->me_range = 64;
//    pCodecCtx2->thread_count = 2;

    pCodecCtx2->rc_min_rate = gbitrate * 1.5;     //gbitrate;
    pCodecCtx2->rc_max_rate = gbitrate * 2  ;     //gbitrate;

   // pCodecCtx2->rc_buffer_size = gbitrate;
   // pCodecCtx2->rc_initial_buffer_occupancy = gbitrate * 3 / 4;


    //pCodecCtx2->gop_size = 300;
    //Some formats want stream headers to be separate.
    //if (ofmt_ctx2->oformat->flags & AVFMT_GLOBALHEADER)
     //   pCodecCtx2->flags |= CODEC_FLAG_GLOBAL_HEADER;

    //H264 codec param
//    pCodecCtx->me_range = 16;
//    pCodecCtx->max_qdiff = 4;
//    pCodecCtx2->qcompress = 0.6;
    //最大和最小量化系数
//    pCodecCtx2->qmin = 10;
//    pCodecCtx2->qmax = 51;
    //Optional Param
    //两个非B帧之间允许出现多少个B帧数
    //设置0表示不使用B帧
    //b 帧越多，图片越小
//    pCodecCtx2->max_b_frames = 1;
 //   pCodecCtx2->has_b_frames = 0;
//    pCodecCtx2->refs = 6;
    // Set H264 preset and tune
    AVDictionary *param = 0;
    //H.264
    if (pCodecCtx2->codec_id == AV_CODEC_ID_H264) {
//        av_dict_set(&param, "preset", "slow", 0);

        // * 这个非常重要，如果不设置延时非常的大
        // * ultrafast,superfast, veryfast, faster, fast, medium
        //  * slow, slower, veryslow, placebo.　这是x264编码速度的选项

        av_dict_set(&param, "preset", "superfast", 0);
        av_dict_set(&param, "tune", "zerolatency", 0);
    }
    int ret = avcodec_open2(pCodecCtx2, pCodec, &param);
    if (ret < 0) {
        loge("Failed to open encoder!\n");
        return -1;
    }

    //Add a new stream to output,should be called by the user before avformat_write_header() for muxing
    video_st2 = avformat_new_stream(ofmt_ctx2, pCodec);
    if (video_st2 == NULL) {
        avcodec_close(pCodecCtx2);
        pCodecCtx2= NULL;

        return -1;
    }
    video_st2->time_base.num = 1;
    video_st2->time_base.den = fps;
//    video_st->codec = pCodecCtx;
    //video_st->codecpar->codec_tag = 0;
    avcodec_parameters_from_context(video_st2->codecpar, pCodecCtx2);

    //Open output URL,set before avformat_write_header() for muxing
    if (avio_open(&ofmt_ctx2->pb, out_path, AVIO_FLAG_READ_WRITE) < 0) {
        loge("Failed to open output file!\n");
        if (video_st2)
            avcodec_close(video_st2->codec);

        if (ofmt_ctx2) {
            avio_close(ofmt_ctx2->pb);
            avformat_free_context(ofmt_ctx2);
            ofmt_ctx2 = NULL;
        }

        avcodec_close(pCodecCtx2);
        pCodecCtx2= NULL;
        return -1;
    }

    //Write File Header
    avformat_write_header(ofmt_ctx2, NULL);

    startTime = av_gettime();
    return 0;
}


// * H264编码并输出

extern "C"
JNIEXPORT jint JNICALL
Java_com_deerlive_usbcamera_ffmpeg_FFmpegHandle_onFrameCallback2(JNIEnv *env, jobject instance,
                                                                 jbyteArray buffer_) {
//    startTime = av_gettime();
    jbyte *in = env->GetByteArrayElements(buffer_, NULL);

    if( pCodecCtx2 == NULL || ofmt_ctx2== NULL)//视频地址错误。初始化不成功。则不做后面的逻辑了。省的浪费,也会崩溃
        return -1;

    int ret = 0;
//初始化一个帧的数据结构，用于编码用
    //指定AV_PIX_FMT_YUV420P这种格式的
    pFrameYUV2 = av_frame_alloc();
    uint8_t *out_buffer = (uint8_t *) av_malloc(av_image_get_buffer_size(pCodecCtx2->pix_fmt, width, height, 1));
    av_image_fill_arrays(pFrameYUV2->data, pFrameYUV2->linesize, out_buffer, pCodecCtx2->pix_fmt, width, height, 1);

    //安卓摄像头数据为NV21格式，此处将其转换为YUV420P格式
    ////N21   0~width * height是Y分量，  width*height~ width*height*3/2是VU交替存储
    //复制Y分量的数据
    memcpy(pFrameYUV2->data[0], in, y_length); //Y

    for (int i = 0; i < uv_length; i++) {
        //将v数据存到第三个平面
        *(pFrameYUV2->data[2] + i) = *(in + y_length + i * 2);
        //将U数据存到第二个平面
        *(pFrameYUV2->data[1] + i) = *(in + y_length + i * 2 + 1);
    }

    pFrameYUV2->format = pCodecCtx2->pix_fmt;
    pFrameYUV2->width = yuv_width;
    pFrameYUV2->height = yuv_height;
    pFrameYUV2->pts = ++count2;
    //pFrameYUV->pts = (1.0 / 30) * 90 * count;                     ////////////////////////////
    //例如对于H.264来说。1个AVPacket的data通常对应一个NAL
    //初始化AVPacket
    enc_pkt2.data = NULL;
    enc_pkt2.size = 0;
    av_init_packet(&enc_pkt2);
//    __android_log_print(ANDROID_LOG_WARN, "eric", "编码前时间:%lld",
//                        (long long) ((av_gettime() - startTime) / 1000));
    //开始编码YUV数据
    ret = avcodec_send_frame(pCodecCtx2, pFrameYUV2);
    if (ret != 0) {
        loge("avcodec_send_frame error");
        return ret;
    }

    //获取编码后的数据
    ret = avcodec_receive_packet(pCodecCtx2, &enc_pkt2);
    av_frame_free(&pFrameYUV2);
    av_free(out_buffer);
//    __android_log_print(ANDROID_LOG_WARN, "eric", "编码时间:%lld",
//                        (long long) ((av_gettime() - startTime) / 1000));
    //是否编码前的YUV数据

    if (ret != 0 || enc_pkt2.size <= 0) {
        loge("avcodec_receive_packet error");
        avError(ret);
        return -2;
    }
    enc_pkt2.stream_index = video_st2->index;

    AVRational time_base = ofmt_ctx2->streams[0]->time_base;

    AVRational r_frame_rate1 = pCodecCtx2->framerate;

    AVRational time_base_q = {1, AV_TIME_BASE};

    int64_t calc_duration = (double)(AV_TIME_BASE) * (1 / av_q2d(r_frame_rate1));

    enc_pkt2.pts = av_rescale_q(count2 * calc_duration, time_base_q, time_base);
    enc_pkt2.dts = enc_pkt2.pts;
    enc_pkt2.duration = av_rescale_q(calc_duration, time_base_q, time_base);
    enc_pkt2.pos = -1;

    ret = av_interleaved_write_frame(ofmt_ctx2, &enc_pkt2);
    if (ret != 0) {
        loge("av_interleaved_write_frame failed");
        env->ReleaseByteArrayElements(buffer_, in, 0);
        return ret;
    }else
    {
        env->ReleaseByteArrayElements(buffer_, in, 0);
        return 0;
    }
}


//释放资源

extern "C"
JNIEXPORT jint JNICALL
Java_com_deerlive_usbcamera_ffmpeg_FFmpegHandle_close2(JNIEnv *env, jobject instance) {
    if (video_st2)
        avcodec_close(video_st2->codec);
    if(pFrameYUV2) {
        av_frame_free(&pFrameYUV2);
        pFrameYUV2 = NULL;
    }
    if (ofmt_ctx2) {
        avio_close(ofmt_ctx2->pb);
        avformat_free_context(ofmt_ctx2);
        ofmt_ctx2 = NULL;
    }
    avcodec_close(pCodecCtx2);
    pCodecCtx2= NULL;
    return 0;
}




