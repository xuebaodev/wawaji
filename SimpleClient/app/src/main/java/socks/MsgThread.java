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
    private static final String TAG = "MsgThread";
    // 发送消息的队列
    private Queue<byte[]> sendMsgQuene = new LinkedList<byte[]>();
    // 是否发送消息
    private boolean send = true;

    private SendThread sd;

    public MsgThread(SendThread sd) {
        this.sd = sd;
    }

    public synchronized void putMsg(byte[] msg) {
        sendMsgQuene.offer(msg);

        // 唤醒线程
        if (sendMsgQuene.size() != 0)
            notify();
    }

    public void StopNow()
    {
        send = false;
        interrupt();
    }

    @Override
    public void run() {
        super.run();
        synchronized (this) {
            while (send) {
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
                    break;
                }
            }
        }
    }

    public void setSend(boolean send) {
        this.send = send;
    }
}
