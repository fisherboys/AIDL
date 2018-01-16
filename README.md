# AIDL
AIDL实现进程间通信
本文主要实现一个基本的远程AIDL调用，可参考android源码中的硬件访问服务实现方式。

首先大概总结一下与Service的通信方式有很多种：

1. 通过BroadCastReceiver：这种方式是最简单的，只能用来交换简单的数据；
2. 通过Messager：这种方式是通过一个传递一个Messager给对方，通过这个它来发送Message对象。这种方式只能单向传递数据。可以是Service到Activity，也可是是从Activity发送数据给Service。一个Messager不能同时双向发送。
3. 通过IBinder来实现远程调用（IPC）：这种方式是Android的最大特色之一，让你调用远程Service的接口，就像调用本地对象一样，实现非常灵活，写起来也相对复杂。

本文最重点谈一下怎么使用AIDL实现Service端和Client端的双向通信（或者叫“调用”）。

### 首先定义一个AIDL接口如下：

```
// IDownloadManagerService.aidl
package com.yu.aidltemplate;


interface IDownloadManagerService {
    /*
     * args: non
     * return: true if success
     * desc: start download
     */
    boolean startDownload();    
}
```

然后clean下工程，就会在build目录下生成相应的java文件IDownloadManagerService.java（类似于在源码目录执行mmm命令，在out目录下产生相应文件）。我们需要在相应Service中实现IDownloadManagerService.java中的方法。

### Service的实现如下：

```
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
```

在DownloadManagerService中实现一个IDownloadManagerService.Stub接口的Binder，并且在onBind\(\)中返回此Binder给客户端，客户端中即可通过此对象访问其中定义的方法startDownload\(\)。在AndroidManifest.xml中对DownloadManagerService进行申明（AS已经自动生成）:

```
<service
    android:name=".DownloadManagerService"
    android:enabled="true"
    android:exported="true">
</service>
```

### Client的调用方法如下：

```
private IDownloadManagerService mDownloadManagerService = null;
                ...
mDownloadManagerService = IDownloadManagerService.Stub.asInterface(service);//获取服务
                ...
mDownloadManagerService.startDownload();//客户端调用服务端方法
```

我们希望能够实现，当下载完成的时候，能够通知客户端。

实现的方法是，客户端注册一个回调到Service中，当Service完成下载的时候，就调用此回调。因为普通interface对象不能通过AIDL注册到Service中，我们需要定义一个AIDL接口，如下：

```
// IDownloadCallback.aidl
package com.yu.aidltemplate;

interface IDownloadCallback {
    /*
     * args: non
     * return: non
     * desc: 下载完成回调
     */
    void onDownloaded();
}
```

同时，在IDownloadManagerService.aidl中添加两个方法如下：

```
// IDownloadManagerService.aidl
package com.yu.aidltemplate;

// 注意这里需要手动import
import com.yu.aidltemplate.IDownloadCallback;

interface IDownloadManagerService {
    ...

    void addDownloadListener(IDownloadCallback cb);
    void delDownloadListener(IDownloadCallback cb);
}
```

这里需要注意的是，需要import com.yu.aidltemplate.IDownloadCallback;

DownloadManagerService.java的实现：

```
//DownloadManagerService.java
package com.yu.aidltemplate;

import ...

public class DownloadManagerService extends Service {
    private static final String TAG = "DownloadManagerService";

    private RemoteCallbackList<IDownloadCallback> mCallbacks = new RemoteCallbackList<>();

    private final IDownloadManagerService.Stub mDownloadManager = new IDownloadManagerService.Stub() {
        ...

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
    ...
    @Override
    public void onDestroy() {
        super.onDestroy();
        //Service销毁的时候，取消掉所有的回调
        mCallbacks.kill();
    }
}
```

这里的RemoteCallbackList帮我们自动处理了Link-To-Death的问题：假设，在实现Service的实现中，用一个List来保存注册进来的IDownloadCallback实例。如果客户端意外退出的话，需要从List列表中删掉对应的实例。否则不仅浪费资源，而且在回调的时候，会出现DeadObjectException。

在Service中用mCallbacks来保存回调列表，在注册和反注册IDownloadCallback回调的时候，只要调用mCallbacks.register\(cb\);和mCallbacks.unregister\(cb\);即可。然后，在DownloadThread函数中调用了notifyDownloaded\(\)函数，它的实现：

```
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
```

这里我们使用一个循环，获取每个callback，然后调用onDownload\(\)。循环开始前，使用mCallbacks.beginBroadcast\(\);返回回调对象的个数。循环结束的时候，调用mCallbacks.finishBroadcast\(\);来宣告完成。

另外，在Service销毁的时候，需要清除掉mCallbacks中的所有对象，如下：

```
@Override
    public void onDestroy() {
        super.onDestroy();
        //Service销毁的时候，取消掉所有的回调
        mCallbacks.kill();
    }
```

### 客户端：

客户端使用IDownloadCallback的方法，只要实现IDownloadCallback.Stub即可，如：

```
IDownloadCallback mCallback = new IDownloadCallback.Stub() {
        @Override
        public void onDownloaded() throws RemoteException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ClientActivity.this, "下载完成显示回调", Toast.LENGTH_SHORT).show();
                }
            });
        }
    };
```

至此，如果下载完成了，客户端就能自动受到回调了。
