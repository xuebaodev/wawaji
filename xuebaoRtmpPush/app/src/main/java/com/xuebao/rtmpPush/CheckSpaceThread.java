package com.xuebao.rtmpPush;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.util.Log;
import android.widget.SimpleAdapter;

/**
 * Author:      JerryChow
 * Date:        2017/5/31 15:12
 * QQ:          384114651
 * Email:       zhoumricecream@163.com
 * Description:
 */
public class CheckSpaceThread extends Thread {

    // 发送消息的队列
    private Handler sd;

    private String sdPath;
    private  String videoDir = "/xuebaoRecFront";

    private boolean shouldStopNow = false;

    public CheckSpaceThread(Handler sd, String path) {
        this.sd = sd;
        sdPath = path;
        shouldStopNow = false;
    }

    public synchronized void Check(String path) {
		sdPath = path;
        notify();
    }

    public void StopNow()
    {
        shouldStopNow = true;
        interrupt();
    }

    @Override
    public void run() {
        super.run();
        synchronized (this) {

            while (shouldStopNow == false) {

                if( sdPath.equals("") == false)
                {
                    StatFs sf = new StatFs(sdPath);
                    long blockSize = sf.getBlockSize();
                    long blockCount = sf.getBlockCount();
                    long availCount = sf.getAvailableBlocks();

                    //Log.d(TAG, "block大小:"+ blockSize+",block数目:"+ blockCount+",总大小:"+blockSize*blockCount/1024+"KB");
                    if(CameraPublishActivity.DEBUG) Log.e("CheckSpaceThread", "valid block：:"+ availCount+",Free space:"+ (availCount*blockSize>>20)+"MB");

                    long leftSpace = (availCount*blockSize>>20); //MB
                    if ( leftSpace < 300) //300
                    {
                        DelAllRecFiles( sdPath  + videoDir );
                    }

                    if( sd != null)
                    {
                        Message me1 = Message.obtain();//通知界面更新
                        me1.what = CameraPublishActivity.MessageType.msgUpdateFreeSpace.ordinal();
                        if (sd != null) sd.sendMessage(me1);
                    }
                }

				//等待
                try {
                    wait();
                } catch (InterruptedException e) {
                //    e.printStackTrace();
                }
            }

            if(CameraPublishActivity.DEBUG) Log.e("CheckSpaceThread", "Check thread exit");
        }
    }

    private void DelAllRecFiles( String recDirPath )
    {
        if(CameraPublishActivity.DEBUG)  Log.i("CheckFileThread", "delete file...");

        if ( recDirPath == null )
        {
            if(CameraPublishActivity.DEBUG)  Log.i("CheckFileThread", "recDirPath is null");
            return;
        }

        if ( recDirPath.isEmpty() )
        {
            if(CameraPublishActivity.DEBUG)  Log.i("CheckFileThread", "recDirPath is empty");
            return;
        }

        File recDirFile = null;
        try
        {
            recDirFile = new File(recDirPath);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return;
        }

        if ( !recDirFile.exists() )
        {
            if(CameraPublishActivity.DEBUG) Log.e("Tag", "rec dir is not exist, path:" + recDirPath);
            return;
        }

        if ( !recDirFile.isDirectory() )
        {
            if(CameraPublishActivity.DEBUG)  Log.e("CheckFileThread", recDirPath + " is not dir");
            return;
        }

        File[] files = recDirFile.listFiles();
        if ( files == null )
        {
            return;
        }

        //排序文件名
        List fileList = Arrays.asList(files);
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (o1.isDirectory() && o2.isFile())
                    return -1;
                if (o1.isFile() && o2.isDirectory())
                    return 1;
                return o1.getName().compareTo(o2.getName());
            }
        });

        //int ioo = 0;
       // for (File file1 : files) {
        //    ioo ++;
        //   Log.e("文件名排序", "序号" +ioo+  file1.getName());
       // }

        int delCount = files.length > 200 ?200 : files.length -2;
        if(CameraPublishActivity.DEBUG) Log.e("file check thread", "deleting" +delCount +"files");

        if( delCount <= 0)
            return;

        try
        {
            for ( int i =0; i < delCount; ++i )
            {
                File recFile = files[i];
                if ( recFile == null )
                {
                    continue;
                }

                if ( !recFile.isFile() )
                {
                    continue;
                }

                if ( !recFile.exists() )
                {
                    continue;
                }

                String name = recFile.getName();
                if ( name == null )
                {
                    continue;
                }

                if ( name.isEmpty() )
                {
                    continue;
                }

                if ( name.endsWith(".mp4") )
                {
                    if ( recFile.delete()  )
                    {
                        if(CameraPublishActivity.DEBUG)  Log.e("CheckFileThread", "Delete file:" + name);
                    }
                    else
                    {
                        if(CameraPublishActivity.DEBUG) Log.e("CheckFileThread", "Delete file failed, " + name);
                    }
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
