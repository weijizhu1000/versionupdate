package com.foodsecurity.xupdate.widget;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.foodsecurity.xupdate.UpdateFacade;
import com.foodsecurity.xupdate.entity.PromptEntity;
import com.foodsecurity.xupdate.entity.UpdateEntity;
import com.foodsecurity.xupdate.proxy.IUpdateProxy;
import com.foodsecurity.xupdate.service.OnFileDownloadListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 插件版本更新管理
 *
 * @author zhujianwei
 * @date 2019/4/29 20:28
 */
public class UpdateBundleMgr {

    /**
     * 插件已安装，没有新版本
     */
    public static final String BUNDLE_STATUS_DEFAULT = "default";
    /**
     * 插件没有安装
     */
    public static final String BUNDLE_STATUS_NOT_INSTALL = "not_install";
    /**
     * 插件已安装，有新版本
     */
    public static final String BUNDLE_STATUS_HAS_NEW_VERSION = "has_new_version";
    /**
     * 插件已安装，检测版本信息失败了
     */
    public static final String BUNDLE_STATUS_INSTALLED_NO_VERSION = "installed_no_version_info";

    private static UpdateBundleMgr sInstance;

    /**
     * 更新信息
     */
    private List<UpdateEntity> mUpdateEntity;
    /**
     * 更新代理
     */
    private IUpdateProxy mIUpdateProxy;
    /**
     * 提示器参数信息
     */
    private PromptEntity mPromptEntity;

    public static UpdateBundleMgr get() {
        if (sInstance == null) {
            synchronized (UpdateBundleMgr.class) {
                if (sInstance == null) {
                    sInstance = new UpdateBundleMgr();
                }
            }
        }
        return sInstance;
    }

    /**
     * 获取更新提示
     *
     * @param updateEntity 更新信息
     * @param promptEntity 提示器参数信息
     * @return
     */
    public void init(@NonNull List<UpdateEntity> updateEntity, PromptEntity promptEntity) {
        setPromptEntity(promptEntity);
        setUpdateEntity(updateEntity);
    }

    public void setIUpdateProxy(IUpdateProxy updateProxy) {
        this.mIUpdateProxy = updateProxy;
    }

    public void setUpdateEntity(List<UpdateEntity> updateEntity) {
        this.mUpdateEntity = updateEntity;
    }

    public void setPromptEntity(PromptEntity promptEntity) {
        this.mPromptEntity = promptEntity;
    }

    public boolean canOpen(String alias) {
        UpdateEntity newUpdate = getNewVersionInfoByAlias(alias);
        return canOpen(alias, newUpdate);
    }

    public boolean canOpen(String alias, UpdateEntity newUpdate) {
        String bundleStatus = getBundleStatus(alias, newUpdate);
        if (BUNDLE_STATUS_DEFAULT.equals(bundleStatus)) {
            return true;
        }

        if (BUNDLE_STATUS_INSTALLED_NO_VERSION.equals(bundleStatus)) {
            return true;
        }

        if (TextUtils.isEmpty(newUpdate.getApkCacheDir())) {
            newUpdate.setApkCacheDir(getBundlesRootPath());
        }
        mIUpdateProxy.setUpdateEntity(newUpdate);
        if (BUNDLE_STATUS_NOT_INSTALL.equals(bundleStatus)) {
            mIUpdateProxy.startDownload(newUpdate, mOnFileDownloadListener);
        } else if (BUNDLE_STATUS_HAS_NEW_VERSION.equals(bundleStatus)) {
            mIUpdateProxy.startDownload(newUpdate, mOnFileDownloadListener);
        }
        return false;
    }

    public String getBundleStatus(String alias, UpdateEntity newUpdate) {
        UpdateEntity localUpdate = getLocalVersionInfoByAlias(alias);
        if (null == localUpdate) {
            return BUNDLE_STATUS_NOT_INSTALL;
        }

        if (null == newUpdate) {
            return BUNDLE_STATUS_INSTALLED_NO_VERSION;
        }

        int localVersionCode = Integer.parseInt(
                localUpdate.getVersionName().replace(".", ""));
        int newVersionCode = Integer.parseInt(
                newUpdate.getVersionName().replace(".", ""));

        if (localVersionCode < newVersionCode) {
            return BUNDLE_STATUS_HAS_NEW_VERSION;
        }

        return BUNDLE_STATUS_DEFAULT;
    }

    public UpdateEntity getNewVersionInfoByAlias(String alias) {
        if (null == mUpdateEntity) {
            return null;
        }
        for (int i = 0; i < mUpdateEntity.size(); i++) {
            if (mUpdateEntity.get(i).getAlias().equals(alias)) {
                return mUpdateEntity.get(i);
            }
        }
        return null;
    }

    public UpdateEntity getLocalVersionInfoByAlias(String alias) {
        List<UpdateEntity> localVersionInfo = getLocalBundleVersionInfo();
        for (int i = 0; i < localVersionInfo.size(); i++) {
            if (localVersionInfo.get(i).getAlias().equals(alias)) {
                return localVersionInfo.get(i);
            }
        }
        return null;
    }

    public void removeOldVersionBundle(UpdateEntity newUpdate) {
        List<UpdateEntity> localVersionInfo = getLocalBundleVersionInfo();
        int newVersionCode = Integer.parseInt(
                newUpdate.getVersionName().replace(".", ""));
        for (int i = 0; i < localVersionInfo.size(); i++) {
            UpdateEntity localUpdate = localVersionInfo.get(i);
            if (localUpdate.getAlias().equals(newUpdate.getAlias())) {
                int localVersionCode = Integer.parseInt(
                        localUpdate.getVersionName().replace(".", ""));
                if (localVersionCode != newVersionCode) {

                    File file = new File(getBundlesRootPath() +
                            File.separator + localUpdate.getAlias() + "_" + localUpdate.getVersionName());
                    if (null != file && file.exists()) {
                        deleteDirWihtFile(file);
                    }
                }
            }
        }
    }

    public static void deleteDirWihtFile(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return;
        }
        for (File file : dir.listFiles()) {
            if (file.isFile()) {
                // 删除所有文件
                file.delete();
            } else if (file.isDirectory()) {
                // 递规的方式删除文件夹
                deleteDirWihtFile(file);
            }
        }
        dir.delete();// 删除目录本身
    }

    public List<UpdateEntity> getLocalBundleVersionInfo() {
        List<UpdateEntity> localVersionInfo = new ArrayList<>();

        File rootPath = new File(getBundlesRootPath());
        File[] bundles = rootPath.listFiles();
        if (null == bundles) {
            return localVersionInfo;
        }
        for (int i = 0; i < bundles.length; i++) {
            if (!bundles[i].isDirectory()) {
                continue;
            }
            UpdateEntity updateEntity = new UpdateEntity();
            String bundle = bundles[i].getName();
            if (bundle.length() > 0 && bundle.indexOf("_") != -1) {
                String bundleAlias = bundle.substring(0, bundle.lastIndexOf("_"));
                updateEntity.setAlias(bundleAlias);

                String bundleVersion = bundle.substring(bundle.lastIndexOf("_") + 1, bundle.length());
                updateEntity.setVersionName(bundleVersion);
            } else {
                updateEntity.setAlias(bundle);
            }

            updateEntity.setFileName(bundle);
            localVersionInfo.add(updateEntity);
        }

        return localVersionInfo;
    }

    public String getBundlesRootPath() {
        return mIUpdateProxy.getContext().getFilesDir().getAbsolutePath() + File.separator + "jsbundles";
    }

    /**
     * 文件下载监听
     */
    private OnFileDownloadListener mOnFileDownloadListener = new OnFileDownloadListener() {

        @Override
        public void onStart() {

        }

        @Override
        public void onProgress(long total, float progress) {

        }

        @Override
        public boolean onCompleted(File file) {
            //返回true，自动进行apk安装
            UpdateFacade.startInstallBundle(mIUpdateProxy.getContext(), file);
            removeOldVersionBundle(mIUpdateProxy.getUpdateEntity());
            return false;
        }

        @Override
        public void onError(Throwable throwable) {

        }
    };
}