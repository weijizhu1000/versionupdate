

package com.foodsecurity.xupdate.proxy.impl;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.foodsecurity.xupdate.Xupdate;
import com.foodsecurity.xupdate.entity.UpdateEntity;
import com.foodsecurity.xupdate.proxy.IUpdateDownloader;
import com.foodsecurity.xupdate.service.DownloadService;
import com.foodsecurity.xupdate.service.OnFileDownloadListener;

/**
 * 默认版本更新下载器
 *
 * @author zhujianwei134
 * @since 2018/7/5 下午5:06
 */
public class DefaultUpdateDownloader implements IUpdateDownloader {

    private DownloadService.DownloadBinder mDownloadBinder;

    /**
     * 服务绑定连接
     */
    private ServiceConnection mServiceConnection;

    /**
     * 是否已绑定下载服务
     */
    private boolean mIsBound;

    @Override
    public void startDownload(final @NonNull UpdateEntity updateEntity, final @Nullable OnFileDownloadListener downloadListener) {
        DownloadService.bindService(mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mIsBound = true;
                startDownload((DownloadService.DownloadBinder) service, updateEntity, downloadListener);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mIsBound = false;
            }
        });
    }

    /**
     * 开始下载
     * @param binder
     * @param updateEntity
     * @param downloadListener
     */
    private void startDownload(DownloadService.DownloadBinder binder, @NonNull UpdateEntity updateEntity, @Nullable OnFileDownloadListener downloadListener) {
        mDownloadBinder = binder;
        mDownloadBinder.start(updateEntity, downloadListener);
    }

    @Override
    public void cancelDownload() {
        if (mDownloadBinder != null) {
            mDownloadBinder.stop("取消下载");
        }
        if (mIsBound && mServiceConnection != null) {
            Xupdate.getContext().unbindService(mServiceConnection);
            mIsBound = false;
        }
    }

    /**
     * 后台下载更新
     */
    @Override
    public void backgroundDownload() {
        if (mDownloadBinder != null) {
            mDownloadBinder.showNotification();
        }
    }
}
