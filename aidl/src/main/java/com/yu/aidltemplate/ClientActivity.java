package com.yu.aidltemplate;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class ClientActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "ClientActivity";

    private Handler mHandler = new Handler();

    private IDownloadManagerService mDownloadManagerService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        //Button
        findViewById(R.id.btn_bind).setOnClickListener(this);
        findViewById(R.id.btn_unbind).setOnClickListener(this);
        findViewById(R.id.btn_start_download).setOnClickListener(this);
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
}
