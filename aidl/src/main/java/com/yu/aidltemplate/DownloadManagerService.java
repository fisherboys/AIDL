//DownloadManagerService.java
package com.yu.aidltemplate;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DownloadManagerService extends Service {
    private static final String TAG = "DownloadManagerService";

    private RemoteCallbackList<IDownloadCallback> mCallbacks = new RemoteCallbackList<>();
    private List<App> mApps = new ArrayList<>();

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

        @Override
        public void addAppToList(IBinder token, String name) throws RemoteException {
            int idx = findApp(token);
            if (idx >= 0) {
                Log.d(TAG, "addAppToList: already added");
                return;
            }

            App app = new App(token, name);
            //注册客户端死掉的通知
            token.linkToDeath(app, 0);

            mApps.add(app);
        }

        @Override
        public void delAppFromList(IBinder token) throws RemoteException {
            int idx = findApp(token);
            if (idx < 0) {
                Log.d(TAG, "delAppFromList: already deteled");
                return;
            }

            App app = mApps.get(idx);
            mApps.remove(app);

            //取消注册
            app.mToken.unlinkToDeath(app, 0);
        }

        @Override
        public List<String> getAppList() throws RemoteException {
            ArrayList<String> names = new ArrayList<>();
            for (App app : mApps) {
                names.add(app.mName);
            }
            return names;
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

    private final class App implements IBinder.DeathRecipient {
        public final IBinder mToken;
        public final String mName;

        public App(IBinder token, String name) {
            mToken = token;
            mName = name;
        }

        @Override
        public void binderDied() {
            //客户端死掉，执行此回调
            int index = mApps.indexOf(this);
            if (index < 0) {
                return;
            }
            Log.d(TAG, "binderDied: app died:  " + mName);
            mApps.remove(this);
        }
    }

    //通过IBinder查找app
    private int findApp(IBinder token) {
        for (int i = 0; i < mApps.size(); i++) {
            if (mApps.get(i).mToken == token) {
                return i;
            }
        }
        return -1;
    }
}
