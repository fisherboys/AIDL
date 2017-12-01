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
}
