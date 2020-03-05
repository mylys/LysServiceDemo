package io.github.lys.servicedemo;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;


//Service启动分为普通启动和绑定启动:startService()和bindService()
//服务的生命周期:onCreate→onStartCommand()/onBind→onDestroy
public class MainActivity extends AppCompatActivity {
    LocalService.LocalBinder localBinder;
    public Button btn;
    public Button btn1;
    public Button btn2;
    Intent intent;
    private int progress = 0;

    private LocalService mBoundService;
    private boolean mIsBound,mIsRemoteBound;

    private TextView tv_remote_callback,tv_get_count;

    /** Messenger for communicating with service. */
    Messenger mService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_remote_callback = (TextView) findViewById(R.id.tv_remote_callback);
        tv_get_count = (TextView) findViewById(R.id.tv_get_count);
        //tv_get_count.setText("值：" + localBinder.getCount());
    }

    /**
     * 监听进度，每秒钟获取调用MsgService的getProgress()方法来获取进度，更新UI
     */
    public void listenProgress(){
        new Thread(new Runnable() {

            @Override
            public void run() {
                while(progress < mBoundService.MAX_Count){
                    progress = localBinder.getCount();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.e(getClass().getSimpleName(),"progress="+ progress);
                }

            }
        }).start();
    }


    public void getServiceCount(View view) {
        mBoundService.startDownLoad();
        //监听进度
        listenProgress();

        tv_get_count.setText("值：" + localBinder.getCount());
    }

    public boolean isServiceWork(Context mContext, String serviceName) {
        boolean isWork = false;
        ActivityManager myAM = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> myList = myAM.getRunningServices(40);
        if (myList.size() <= 0) {
            return false;
        }
        for (int i = 0; i < myList.size(); i++) {
            String mName = myList.get(i).service.getClassName().toString();
            if (mName.equals(serviceName)) {
                isWork = true;
                break;
            }
        }
        return isWork;
    }

    public void startLocalService(View view) {
        intent = new Intent(this, LocalService.class);
        intent.putExtra("activity","AAAAAAAAAAAAAAAAA");
        startService(intent);
    }


    public void stopLocalService(View view) {
        stopService(new Intent(MainActivity.this, LocalService.class));
    }

    public void bindLocalService(View view) {
        //service ：该参数通过Intent指定要启动的Service
        //conn： 是一个ServiceConnection对象，用于监听访问者与Service之间的连接状态，；
        //fkags：指定绑定是是否自动创建Service，0（不创建） BIND_AUTO_CREATE(自动创建)；
        //IBinder对象相当于Service组件的内部钩子，关联到绑定的Service组件，其他程序组件绑定该Service时，Service就会把IBinder对象返回给其他程序组件
        bindService(new Intent(MainActivity.this,LocalService.class), mLocalServiceConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;

    }
    public void unbindLocalService(View view) {
        unbindLocalService();
    }

    void unbindLocalService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mLocalServiceConnection);
            mIsBound = false;
            Log.e(getLocalClassName(),"unbindLocalService");
        }
    }




    //定义一个ServicesCounnection对象
    private ServiceConnection mLocalServiceConnection = new ServiceConnection() {

        //当Activity与Service连接成功时回调该方法
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            localBinder = (LocalService.LocalBinder) service;
            mBoundService = ((LocalService.LocalBinder)service).getService();
            //获取Service的OnBind方法所返回的MyBinder对象
            LocalService.LocalBinder binder = (LocalService.LocalBinder) service;
            // Tell the user about this for our demo.
            Toast.makeText(MainActivity.this, R.string.local_service_connected,Toast.LENGTH_SHORT).show();
        }

        //断开时调用
        @Override
        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
            Toast.makeText(MainActivity.this, R.string.local_service_disconnected,Toast.LENGTH_SHORT).show();
        }
    };


    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RemoteMessengerService.MSG_SET_VALUE:
                    Log.e(getLocalClassName(), " MessengerService.MSG_SET_VALUE msg.arg1 " + msg.arg1);

                    tv_remote_callback.setText("Received from service: " + msg.arg1);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mRemoteConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = new Messenger(service);
            tv_remote_callback.setText("Attached.");

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null, RemoteMessengerService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);

                // Give it some value as an example.
                msg = Message.obtain(null, RemoteMessengerService.MSG_SET_VALUE, this.hashCode(), 0);
                Log.e(getLocalClassName(), " Remote Service.send(msg) msg=" + msg);

                mService.send(msg);

            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }

            // As part of the sample, tell the user what happened.
            Toast.makeText(MainActivity.this, R.string.remote_service_connected,
                    Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            tv_remote_callback.setText("Disconnected.");

            // As part of the sample, tell the user what happened.
            Toast.makeText(MainActivity.this, R.string.remote_service_disconnected,
                    Toast.LENGTH_SHORT).show();
        }
    };


    public void bindRemoteService(View view) {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        bindService(new Intent(MainActivity.this,
                RemoteMessengerService.class), mRemoteConnection, Context.BIND_AUTO_CREATE);
        mIsRemoteBound = true;
        tv_remote_callback.setText("Binding.");
    }

    public void unbindRemoteService(View view) {
        if (mIsRemoteBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null,
                            RemoteMessengerService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

            // Detach our existing connection.
            unbindService(mRemoteConnection);
            mIsRemoteBound = false;
            tv_remote_callback.setText("Unbinding.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindLocalService();
    }

}
