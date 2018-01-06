package update;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class SilentInstall {
    private Context mContext;
    private DownloadManager downloadManager;
    private long mTaskId;
    private static final String APK_NAME = "push.apk";
    private static final String APK_FULL_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/"+APK_NAME;

    public SilentInstall(Context context) {
        mContext = context;

        //注册广播接收者，监听下载状态
        mContext.registerReceiver(receiver,
               new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    public void startUpdate(String downloadUrl) {

       File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), APK_NAME);
        if (file.exists()) {
            Boolean bool = file.delete();
            Toast.makeText(mContext, "delete state: " + String.valueOf(bool), Toast.LENGTH_LONG).show();
        }

        //创建下载任务,downloadUrl就是下载链接
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        //指定下载路径和下载文件名
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, APK_NAME);
        //获取下载管理器
        downloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        //将下载任务加入下载队列，否则不会进行下载
        mTaskId = downloadManager.enqueue(request);

        //在通知栏中显示，默认就是显示的
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setVisibleInDownloadsUi(true);

        //加入下载队列后会给该任务返回一个long型的id，
        //通过该id可以取消任务，重启任务等等，看上面源码中框起来的方法
        // downloadManager.enqueue(request);
    }

    //广播接受者，接收下载状态
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkDownloadStatus();//检查下载状态
        }
    };

    //检查下载状态
    private void checkDownloadStatus() {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(mTaskId);//筛选下载任务，传入任务ID，可变参数
        Cursor c = downloadManager.query(query);
        if (c.moveToFirst()) {
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            switch (status) {
                case DownloadManager.STATUS_PAUSED:
                    log(">>>下载暂停");
                case DownloadManager.STATUS_PENDING:
                    log(">>>下载延迟");
                case DownloadManager.STATUS_RUNNING:
                    log(">>>正在下载");
                    break;
                case DownloadManager.STATUS_SUCCESSFUL:
                    log(">>>下载完成");
                    //下载完成安装APK
                    onSilentInstall(APK_FULL_PATH);
                    break;
                case DownloadManager.STATUS_FAILED:
                    log(">>>下载失败");
                    break;
            }
        }
    }

    private void onSilentInstall(String path) {
        Intent intent = new Intent();
        intent.setAction("ACTION_UPDATE_START");
        intent.putExtra("path", path);
        mContext.sendBroadcast(intent, null);

        Log.e("broastcat","Path" + path);
        Toast.makeText(mContext, "download finish!", Toast.LENGTH_LONG).show();
    }

    private void log(String str) {
        Log.v("TAG", str);
    }
}