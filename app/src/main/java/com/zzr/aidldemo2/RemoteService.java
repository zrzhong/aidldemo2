package com.zzr.aidldemo2;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class RemoteService extends Service {
    private static final String TAG = "MainActivity";
    private CopyOnWriteArrayList<Book> books = new CopyOnWriteArrayList<>();
    private AtomicBoolean isServiceDestoryed = new AtomicBoolean();
    private RemoteCallbackList<IOnNewBookArrivedListener> mListenerList = new RemoteCallbackList<>();
    private Binder myBinder = new IBookManager.Stub() {

        @Override
        public List<Book> getBookList() throws RemoteException {
            Log.i(TAG, "getBookList: ");
            return books;
        }

        @Override
        public void addBook(Book book) throws RemoteException {
            Log.i(TAG, "addBook: ");
            books.add(book);
        }

        @Override
        public void registerListener(IOnNewBookArrivedListener listener) throws RemoteException {
            mListenerList.register(listener);
            int size = mListenerList.beginBroadcast();
            Log.i(TAG, "registerListener: size: " + size);
            mListenerList.finishBroadcast();
//            if (!mListenerList.contains(listener)) {
//                mListenerList.add(listener);
//            } else {
//                Log.i(TAG, "already exists");
//            }
        }

        @Override
        public void unRegisterListener(IOnNewBookArrivedListener listener) throws RemoteException {
            mListenerList.unregister(listener);
            int size = mListenerList.beginBroadcast();
            Log.i("MainActivity", "unRegisterListener: size: " + size);
            mListenerList.finishBroadcast();
//            if (mListenerList.contains(listener)) {
//                mListenerList.remove(listener);
//                Log.i(TAG, "unregister succeed");
//            } else {
//                Log.i(TAG, "no found,con not unregister");
//            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        //权限验证
        int check = checkCallingOrSelfPermission("com.zzr.aidldemo2.ACCESS_BOOK_SERVICE");
        if (check == PackageManager.PERMISSION_DENIED){
            return null;
        }
        return myBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: service");
        books.add(new Book(0, "android"));
        books.add(new Book(1, "ios "));
        new Thread(new ServiceWorker()).start();
    }

    private class ServiceWorker implements Runnable {

        @Override
        public void run() {
            while (!isServiceDestoryed.get()) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int bookId = books.size() + 1;
                if (bookId == 8) {
                    isServiceDestoryed.set(true);
                }
                Book newBook = new Book(bookId, "new Book#" + bookId);
                try {
                    onNewBookArrived(newBook);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void onNewBookArrived(Book newBook) throws RemoteException {
        books.add(newBook);
        Log.i(TAG, "onNewBookArrived,notify listeners");
        final int N = mListenerList.beginBroadcast();
        for (int i = 0; i < N; i++) {
            IOnNewBookArrivedListener listener = mListenerList.getBroadcastItem(i);
            if (listener != null) {
                listener.onNewBookArrived(newBook);
            }
        }
        mListenerList.finishBroadcast();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind: service");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: service");
        super.onDestroy();
    }
}
