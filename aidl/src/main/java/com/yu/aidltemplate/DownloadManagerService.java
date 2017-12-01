//DownloadManagerService.java
package com.yu.aidltemplate;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

public class DownloadManagerService extends Service {
    private static final String TAG = "DownloadManagerService";

    private RemoteCallbackList<IDownloadCallback> mCallbacks = new RemoteCallbackList<>();

    private final IDownloadManagerService.Stub mDownloadManager = new IDownloadManagerService.Stub() {
        @Override
        public boolean startDownload() throws RemoteException {
            new Thread(new DownloadThread()).start();
            return true;
        }

        @Override
        public void addDownloadListener(IDownloadCallback cb) throws RemoteException {
            mCallbacks.register(cb);
        }

        @Override
        public void delDownloadListener(IDownloadCallback cb) throws RemoteException {
            mCallbacks.unregister(cb);
        }
    };

    //模拟下载线程
    private class DownloadThread implements Runnable {
        @Override
        public void run() {
            Log.d(TAG, "run: DownloadThread");
            try {
                Thread.sleep(5000);
                notifyDownloaded();//下载完成后通知回调
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //
    private void notifyDownloaded() throws RemoteException {
        final int len = mCallbacks.beginBroadcast();
        for (int i = 0; i < len; i++) {
            IDownloadCallback callback = mCallbacks.getBroadcastItem(i);
            if (callback != null) {
                try {
                    callback.onDownloaded();//通知回调
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        mCallbacks.finishBroadcast();
    }

    public DownloadManagerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mDownloadManager;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Service销毁的时候，取消掉所有的回调
        mCallbacks.kill();
    }
}
