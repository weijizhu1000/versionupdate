package com.android.components.bundle.version;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.foodsecurity.xupdate.UpdateFacade;
import com.foodsecurity.xupdate.Xupdate;
import com.foodsecurity.xupdate.entity.PromptEntity;
import com.foodsecurity.xupdate.entity.UpdateEntity;
import com.foodsecurity.xupdate.logs.UpdateLog;
import com.foodsecurity.xupdate.proxy.IUpdateBundlePrompter;
import com.foodsecurity.xupdate.proxy.IUpdateProxy;
import com.foodsecurity.xupdate.proxy.impl.BundleUpdateChecker;
import com.foodsecurity.xupdate.utils.UpdateUtils;
import com.foodsecurity.xupdate.widget.UpdateBundleMgr;
import com.pingan.foodsecurity.bundle.version.R;
import com.qihoo360.replugin.RePlugin;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String BUNDLE_ALIAS_COMMON = "common";
    public static final String BUNDLE_ALIAS_STATISTICS = "statistics_bundle";

    private String baseUrl = "http://192.168.1.105:8000";

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUpdate();

        checkMainAppVersion();
        checkBundlesVersion();

        testPluginVersion();
    }

    private void testPluginVersion() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        List<UpdateEntity> pluginVersions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UpdateEntity updateEntity = new UpdateEntity();
            updateEntity.setAlias("aaaaaaa" + i);
            updateEntity.setName("name" + i);
            pluginVersions.add(updateEntity);
        }
        PluginAdapter pluginAdapter = new PluginAdapter(this, pluginVersions);
        recyclerView.setAdapter(pluginAdapter);
    }

    /**
     * 检测主程序版本信息
     */
    private void checkMainAppVersion() {
        Xupdate.newBuild(this)
                .param("versionCode", "" + UpdateUtils.getVersionCode(this))
                .param("appKey", getPackageName())
                .supportBackgroundUpdate(true)
                .updateUrl(baseUrl + "/version/queryversion")
                .update();
    }

    /**
     * 检测插件版本信息
     */
    private void checkBundlesVersion() {
        Xupdate.newBuild(this)
                .param("versionCode", "" + UpdateUtils.getVersionCode(this))
                .param("appKey", getPackageName())
                .param("pluginAlias", "")
                .param("pluginVersionCode", "")
                .updateBundlePrompter(new BundleUpdatePrompter())
                .updateChecker(new BundleUpdateChecker())
                .updateUrl(baseUrl + "/version/querybundleversion")
                .updateBundle();
    }


    /**
     * 插件检测版本信息，下载更新回调
     */
    public class BundleUpdatePrompter implements IUpdateBundlePrompter {

        public BundleUpdatePrompter() {

        }

        @Override
        public void showBundlePrompt(@NonNull List<UpdateEntity> updateEntity, @NonNull IUpdateProxy updateProxy, @NonNull PromptEntity promptEntity) {
            UpdateLog.i("showBundlePrompt");
            PluginAdapter pluginAdapter = new PluginAdapter(MainActivity.this, updateEntity);
            recyclerView.setAdapter(pluginAdapter);
        }

        @Override
        public void beforDownloadStart(UpdateEntity updateEntity) {

        }

        @Override
        public void downloadProgress(UpdateEntity updateEntity, float progress) {

        }

        @Override
        public void installCompleted(UpdateEntity updateEntity) {
            Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
            intent.putExtra("bundle", updateEntity.getAlias());
            startActivity(intent);
        }

        @Override
        public void downloadError(UpdateEntity updateEntity, Throwable throwable) {

        }
    }

    /**
     * 初始化版本更新组件
     */
    private void initUpdate() {
        Xupdate.get()
                .debug(true)
                .isWifiOnly(true)
                .isGet(false)
                .isAutoMode(false)
                .param("versionCode", "" + UpdateUtils.getVersionCode(this))
                .param("appKey", getPackageName())
                .init(this.getApplication());
    }

    public class PluginAdapter extends RecyclerView.Adapter<PluginAdapter.ViewHolder> {

        private List<UpdateEntity> mPluginList;
        private Context mContext;

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView newVersionImage;
            TextView pluginName;

            public ViewHolder(View view) {
                super(view);
                newVersionImage = (ImageView) view.findViewById(R.id.newversion);
                pluginName = (TextView) view.findViewById(R.id.name);
            }
        }

        public PluginAdapter(Context context, List<UpdateEntity> fruitList) {
            mContext = context;
            mPluginList = fruitList;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plugin, parent, false);
            ViewHolder holder = new ViewHolder(view);
            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final UpdateEntity updateEntity = mPluginList.get(position);
            holder.pluginName.setText(updateEntity.getName());
            holder.newVersionImage.setVisibility(updateEntity.isHasUpdate() ? View.VISIBLE : View.GONE);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (updateEntity.isHasUpdate()) {
                        Xupdate.get().updateBundlesVersion(updateEntity);
                    } else {
                        if (Xupdate.get()
                                .canOpenBundle(updateEntity.getAlias())) {
                            // 原生插件
                            if (updateEntity.getType() == UpdateBundleMgr.PLUGIN_TYPE_NATIVE) {
                                RePlugin.startActivity(mContext, RePlugin.createIntent(updateEntity.getAlias(), "com.qihoo360.replugin.sample.demo3.MainActivity"));
                            } else if (updateEntity.getType() == UpdateBundleMgr.PLUGIN_TYPE_NATIVE_H5) {
                                Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
                                intent.putExtra("bundle", updateEntity.getAlias());
                                intent.putExtra("type", updateEntity.getType());
                                startActivity(intent);
                            } else if (updateEntity.getType() == UpdateBundleMgr.PLUGIN_TYPE_NATIVE_H5) {
                                Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
                                intent.putExtra("url", updateEntity.getAlias());
                                intent.putExtra("type", updateEntity.getType());
                                startActivity(intent);
                            }
                        }
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mPluginList.size();
        }
    }
}
