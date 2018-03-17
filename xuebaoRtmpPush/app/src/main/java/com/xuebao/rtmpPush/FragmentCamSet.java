package com.xuebao.rtmpPush;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import android.os.Handler;

import android_serialport_api.ComPort;
import socks.VideoConfig;

/**
 *
 * Created by lijuan on 2016/8/23.
 */
public class FragmentCamSet extends Fragment {
    SeekBar sbStaturation = null;
    SeekBar sbContrast = null;
    SeekBar sbBrightness = null;

    public int uiStaturation  = 0;
    //对比度
    public int uiContrast = 0;
    //明度
    public int uiBrightness =0;



    public Handler mHandler = null;
    public static FragmentCamSet newInstance(int index, Handler h){
        Bundle bundle = new Bundle();
        bundle.putInt("index", 'A' + index);
        FragmentCamSet fragment = new FragmentCamSet();
        fragment.setArguments(bundle);
        fragment.mHandler = h;
        return fragment;
    }

    public  FragmentCamSet(){
    }

    public void ConfigToUI()
    {
        uiStaturation  = VideoConfig.instance.staturation ;
        uiContrast = VideoConfig.instance.contrast;
        uiBrightness = VideoConfig.instance.brightness ;


        if(sbStaturation != null) sbStaturation.setProgress( uiStaturation );
        if(sbStaturation != null) sbContrast.setProgress( uiContrast );
        if(sbStaturation != null) sbBrightness.setProgress( uiBrightness + 255);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camset, null);

        //发送给主线程处理
        Button btnApplayCam = (Button)  view.findViewById(R.id.button_applyCam);
        btnApplayCam.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v) {

                VideoConfig.instance.usingCustomConfig = true;
                VideoConfig.instance.staturation = uiStaturation  ;

                VideoConfig.instance.contrast = uiContrast ;

                VideoConfig.instance.brightness = uiBrightness ;

                Message message = Message.obtain();
                message.what = CameraPublishActivity.MessageType.msgApplyCamparam.ordinal();

                if (mHandler != null) mHandler.sendMessage(message);
            }
        });

        Button btnRestoreCam = (Button)  view.findViewById(R.id.button_restoreCam);
        btnRestoreCam.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v) {

                VideoConfig.instance.usingCustomConfig = false;
                uiStaturation = VideoConfig.instance.defaultStaturation;
                uiContrast = VideoConfig.instance.defaultContrast;
                uiBrightness = VideoConfig.instance.defaultBrightness;

                VideoConfig.instance.staturation = uiStaturation  ;
                VideoConfig.instance.contrast = uiContrast ;
                VideoConfig.instance.brightness = uiBrightness ;

                Message message = Message.obtain();
                message.what = CameraPublishActivity.MessageType.msgRestoreCamparam.ordinal();

                if (mHandler != null) mHandler.sendMessage(message);
            }
        });

        //饱和度
        sbStaturation = view.findViewById(R.id.sbStaturation);
        //对比度
        sbContrast = view.findViewById(R.id.sbContrast);
        //亮度
        sbBrightness = view.findViewById(R.id.sbBrightness);

        sbStaturation.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //拖动条停止拖动的时候调用
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //description.setText("拖动停止");
                uiStaturation = seekBar.getProgress();
            }

            //拖动条开始拖动的时候调用
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //description.setText("开始拖动");
            }

            //拖动条进度改变的时候调用
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                //description.setText("当前进度："+progress+"%");
            }
        });

        sbContrast.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //拖动条停止拖动的时候调用
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //description.setText("拖动停止");
                uiContrast = seekBar.getProgress();
            }

            //拖动条开始拖动的时候调用
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //description.setText("开始拖动");
            }

            //拖动条进度改变的时候调用
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                //description.setText("当前进度："+progress+"%");
            }
        });

        sbBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //拖动条停止拖动的时候调用
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //description.setText("拖动停止");
                uiBrightness = seekBar.getProgress()-255;
            }

            //拖动条开始拖动的时候调用
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //description.setText("开始拖动");
            }

            //拖动条进度改变的时候调用
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                //description.setText("当前进度："+progress+"%");
            }
        });

        ConfigToUI();

        return view;
    }
}
