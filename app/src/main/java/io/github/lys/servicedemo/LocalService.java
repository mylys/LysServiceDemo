package io.github.lys.servicedemo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by growth on 3/29/16.
 */
public class LocalService extends Service {
    private NotificationManager mNM;
    Notification notification = new Notification();
    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int notification_id = R.string.local_service_started_nfid;
    private int count = 1;
    public static final int MAX_Count = 100;
    private boolean quit;
    MediaPlayer myPlayer;


    //这里内部类
    private final IBinder mBinder = new LocalBinder();
    //定义OnBinder方法所返回的对象 Service 允许客户端通过该Ibinder对象来访问Service内部的数据
    public class LocalBinder extends Binder {
        LocalService getService() {
            return LocalService.this;
        }

        public int getCount() {
            /**
             * 获取Service的运行状态：count
             */
            return count;
        }
    }



    /**
     * 模拟下载任务，每秒钟更新一次
     */
    public void startDownLoad(){
        new Thread(new Runnable() {

            @Override
            public void run() {
                while(count < MAX_Count){
                    count += 5;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        }).start();
    }




    @Override
    public void onCreate() {
        Log.d(getClass().getSimpleName(),"LocalService start onCreate() ");
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        myPlayer = MediaPlayer.create(this, R.raw.loveme);
        myPlayer.setLooping(false); // Set looping
        super.onCreate();
        //startDownLoad();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //startForeground(110, notification);                     // 开始前台服务
        myPlayer.start();

        showNotification();
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        return START_NOT_STICKY;
    }

    /**
     * 被关闭之前回调该方法
     */
    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(notification_id);
        Log.d(getClass().getSimpleName(), "onDestroy()");
        stopForeground(true);// 停止前台服务--参数：表示是否移除之前的通知
        // Tell the user we stopped.
        Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
        this.quit = true;
        myPlayer.stop();


    }

    @Override
    public IBinder onBind(Intent intent) {
        //返回IBinder对象
        //这里可以让Service访问到Activity传来的数据
        Log.d(getClass().getSimpleName(), "onBind()");
        String data = intent.getStringExtra("activity");
        Log.e(getClass().getSimpleName(),"data:"+ data);
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e("", "当Service被断开连时，会回调该方法");
        return true;
    }




    /**
     * Show a notification while this service is running.
     * Display a notification about us starting.  We put an icon in the status bar.
     */
    /*private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string. local_service_started_nfid);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.local_service_label))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();

        // Send the notification.
        mNM.notify( notification_id, notification);
    }*/
    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // 在API11之后构建Notification的方式
        Intent nfIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        notification = new Notification.Builder(this.getApplicationContext())      // 获取构建好的Notification
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),
                        R.mipmap.ic_launcher))                     // 设置下拉列表中的图标(大图标)
                .setSmallIcon(R.mipmap.ic_launcher)                // 设置状态栏内的小图标
                .setTicker(getText(R.string.local_service_started_nfid))// the status text
                .setWhen(System.currentTimeMillis())               // 设置该通知发生的时间
                .setContentTitle(getText(R.string.local_service_label))                // 设置下拉列表里的标题
                .setContentText(getText(R.string.local_service_started_nfid))                      // 设置上下文内容
                .setContentIntent(contentIntent)                   // 设置PendingIntent The intent to send when the entry is clicked
                .build();
        notification.defaults = Notification.DEFAULT_SOUND;        //设置为默认的声音
        mNM.notify(notification_id, notification);                    //shown in the status barf
    }



}