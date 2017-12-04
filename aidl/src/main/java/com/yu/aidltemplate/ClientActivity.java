package com.yu.aidltemplate;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;

public class ClientActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "ClientActivity";

    private Handler mHandler = new Handler();

    private IDownloadManagerService mDownloadManagerService = null;
    private boolean mIsAdd = false;

    private IBinder mToken = new Binder();

    private Button btn_toggle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        //Button
        findViewById(R.id.btn_bind).setOnClickListener(this);
        findViewById(R.id.btn_unbind).setOnClickListener(this);
        findViewById(R.id.btn_start_download).setOnClickListener(this);
        findViewById(R.id.btn_getlist).setOnClickListener(this);
        findViewById(R.id.btn_kill_activity).setOnClickListener(this);

        btn_toggle = (Button) findViewById(R.id.btn_toggle);
        btn_toggle.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_bind:
                //绑定服务
                attemptToBindService();
                break;
            case R.id.btn_unbind:
                //解绑服务
                attemptToUnbindService();
                break;
            case R.id.btn_start_download:
                //开始下载
                startDownload();
                break;
            case R.id.btn_toggle:
                //添加或删除app
                toggleAdd();
                break;
            case R.id.btn_getlist:
                //列出下载列表中的app
                listApp();
                break;
            case R.id.btn_kill_activity:
                //finish();
                break;
            default:
                break;
        }
    }

    /*
     * args: non
     * return: non
     * desc: 绑定服务
     */
    private void attemptToBindService(){
        if (mDownloadManagerService != null) {
            Toast.makeText(this, "Download Service already binded, no need to bind again", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, DownloadManagerService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }

    /*
     * args: non
     * return: non
     * desc: 解绑服务
     */
    private void attemptToUnbindService() {
        if (mDownloadManagerService != null) {
            Toast.makeText(this, "Service unbind success", Toast.LENGTH_SHORT).show();
            unbindService(mServiceConnection);
        } else {
            Toast.makeText(this, "Service already unbind, no need to unbind again", Toast.LENGTH_SHORT).show();
        }
    }

    /*
     * args: non
     * return: non
     * desc: 客户端调用服务端的函数开始下载
     */
    private void startDownload() {
        if (mDownloadManagerService != null) {
            try {
                boolean isStart = mDownloadManagerService.startDownload();//客户端调用服务端方法
                Toast.makeText(this, "Does Start Download?  " + isStart, Toast.LENGTH_SHORT).show();
                mDownloadManagerService.addDownloadListener(mCallback);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Download Service is not binded yet", Toast.LENGTH_SHORT).show();
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "service connected");
            try {
                service.linkToDeath(mDeathRecipient, 0);//绑定死亡代理
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            Toast.makeText(ClientActivity.this, "Download Service connected", Toast.LENGTH_SHORT).show();
            mDownloadManagerService = IDownloadManagerService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            Toast.makeText(ClientActivity.this, "Download Service disconnected", Toast.LENGTH_SHORT).show();
            mDownloadManagerService = null;
        }
    };



    IDownloadCallback mCallback = new IDownloadCallback.Stub() {
        @Override
        public void onDownloaded() throws RemoteException {
            Log.d(TAG, "onDownloaded: 下载完成显示回调");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ClientActivity.this, "下载完成显示回调", Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    //绑定死亡代理，如果服务端意外退出，可以重新绑定服务
    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            if (mDownloadManagerService == null) {
                return;
            }
            mDownloadManagerService.asBinder().unlinkToDeath(mDeathRecipient, 0);
            mDownloadManagerService = null;
            attemptToBindService();
        }
    };

    /*
     * args: non
     * return: true if ready
     * desc: 判断服务是否就绪
     */
    private boolean isServiceReady() {
        if (mDownloadManagerService != null) {
            return true;
        } else {
            Toast.makeText(this, "Service is not available yet!", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    //添加或删除app
    private void toggleAdd() {
        if (!isServiceReady()) {
            return;
        }
        try {
            if (!mIsAdd) {
                String name = "App1";
                mDownloadManagerService.addAppToList(mToken, name);
                btn_toggle.setText(R.string.btn_delete);
                mIsAdd = true;
            } else {
                mDownloadManagerService.delAppFromList(mToken);
                btn_toggle.setText(R.string.btn_add);
                mIsAdd = false;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    //
    private void listApp() {
        if (!isServiceReady()) {
            return;
        }
        try {
            List<String> mApps = mDownloadManagerService.getAppList();
            Log.d(TAG, "listApp: " + mApps);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
