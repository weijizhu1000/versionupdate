

package com.foodsecurity.xupdate.listener;

import android.content.Context;
import android.support.annotation.NonNull;

import com.foodsecurity.xupdate.entity.DownloadEntity;
import com.foodsecurity.xupdate.entity.UpdateEntity;

import java.io.File;

/**
 * 安装监听
 *
 * @author zhujianwei134
 * @since 2018/6/29 下午4:14
 */
public interface OnInstallListener {

    /**
     * 开始安装apk的监听
     * @param context
     * @param apkFile          安装的apk文件
     * @param downloadEntity   文件下载信息
     * @return
     */
    boolean onInstall(@NonNull Context context, @NonNull File apkFile, UpdateEntity updateEntity, @NonNull DownloadEntity downloadEntity);

    /**
     * apk安装完毕
     */
    void onInstallSuccess();
}
