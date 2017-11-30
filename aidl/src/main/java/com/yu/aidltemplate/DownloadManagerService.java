//DownloadManagerService.java
package com.yu.aidltemplate;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class DownloadManagerService extends Service {
    private static final String TAG = "DownloadManagerService";

    private final IDownloadManagerService.Stub mDownloadManager = new IDownloadManagerService.Stub() {
        @Override
        public boolean startDownload() throws RemoteException {
            new Thread(new DownloadThread()).start();
            return true;
        }
    };

    //模拟下载线程
    private class DownloadThread implements Runnable {
        @Override
        public void run() {
            Log.d(TAG, "run: DownloadThread");
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public DownloadManagerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mDownloadManager;
    }
}
