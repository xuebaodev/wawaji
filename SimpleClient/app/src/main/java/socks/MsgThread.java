package socks;

import java.util.LinkedList;
import java.util.Queue;
import android.util.Log;
/**
 * Author:      JerryChow
 * Date:        2017/5/31 15:12
 * QQ:          384114651
 * Email:       zhoumricecream@163.com
 * Description:
 */
public class MsgThread extends Thread {

    // 发送消息的队列
    private Queue<byte[]> sendMsgQuene = new LinkedList<byte[]>();
    private SockAPP sd;

    private boolean shouldStopNow = false;

    public MsgThread(SockAPP sd) {
        this.sd = sd;
        shouldStopNow = false;
    }

    public synchronized void putMsg(byte[] msg) {
        sendMsgQuene.offer(msg);

        // 唤醒线程
        if (sendMsgQuene.size() != 0)
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
                // 当队列里的消息发送完毕后，线程等待
                while (sendMsgQuene.size() > 0) {
                    byte[] msg = sendMsgQuene.poll();
                    if (sd != null)
                    {
                        sd.sendMsg(msg);
                    }
                }
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Log.e("MsgThread", "send thread exit.");
        }
    }
}
