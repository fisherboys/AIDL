// IDownloadManagerService.aidl
package com.yu.aidltemplate;

// 注意这里需要手动import
import com.yu.aidltemplate.IDownloadCallback;

interface IDownloadManagerService {
    /*
     * args: non
     * return: true if success
     * desc: start download
     */
    boolean startDownload();
    /*
     * args: callback
     * return: non
     * desc: register downloaded callback
     */
    void addDownloadListener(IDownloadCallback cb);
    /*
     * args: callback
     * return: non
     * desc: unregister downloaded callback
     */
    void delDownloadListener(IDownloadCallback cb);
    /*
     * args:
     * return: non
     * desc:add app to download list
     */
    void addAppToList(IBinder token, String name);
    /*
     * args:
     * return: non
     * desc: remove app from download list
     */
    void delAppFromList(IBinder token);
    /*
     * args: non
     * return: app list
     * desc: get apps in download list
     */
    List<String> getAppList();

}
