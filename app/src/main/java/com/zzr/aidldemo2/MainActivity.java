package com.zzr.aidldemo2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private IBookManager bookManager;
    private static final int MESSAGE_NEW_BOOK_ARRIVERD = 1;
    private IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            //binder死亡会回调该方法
            if (bookManager == null) {
                return;
            }
            bookManager.asBinder().unlinkToDeath(deathRecipient, 0);
            bookManager = null;
            //重新绑定远程服务
            Intent intent = new Intent(MainActivity.this, RemoteService.class);
            bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "onServiceConnected: 连接成功");
            if (service == null) {
                Log.i(TAG, "没有权限");
                return;
            }
            bookManager = IBookManager.Stub.asInterface(service);
            try {
                //设置死亡代理
                service.linkToDeath(deathRecipient, 0);
                List<Book> bookList = bookManager.getBookList();
                Log.i(TAG, "book size: " + bookList.size() + ",list type: " + bookList.getClass().getCanonicalName());
                Book book = new Book(4, "python");
                bookManager.addBook(book);
                Log.i(TAG, "book size2: " + bookManager.getBookList().size());
                //向服务端注册
                bookManager.registerListener(listener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "onServiceDisconnected: 连接失败");
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MESSAGE_NEW_BOOK_ARRIVERD:
                    Log.i(TAG, "receive new book: " + msg.obj);
                default:
                    super.handleMessage(msg);
            }
        }
    };
    private IOnNewBookArrivedListener listener = new IOnNewBookArrivedListener.Stub() {
        @Override
        public void onNewBookArrived(Book newBook) throws RemoteException {
            //收到服务端的通知 切换到主线程
            mHandler.obtainMessage(MESSAGE_NEW_BOOK_ARRIVERD, newBook).sendToTarget();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //绑定服务
        Intent intent = new Intent(this, RemoteService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        if (bookManager != null && bookManager.asBinder().isBinderAlive()) {
            try {
                bookManager.unRegisterListener(listener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        unbindService(serviceConnection);
        super.onDestroy();
    }
}
