package cn.losfer.aimodelsstatus;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.losfer.aimodelsstatus.Site;
import cn.losfer.aimodelsstatus.AppStorage;
import cn.losfer.aimodelsstatus.OpenAiApi;
import cn.losfer.aimodelsstatus.ProbeEngine;
import cn.losfer.aimodelsstatus.ui.ModelListAdapter;
import cn.losfer.aimodelsstatus.ui.Ui;

public class SiteDetailActivity extends AppCompatActivity {
    private String siteId;
    private Site site;
    private TextView heroName, heroUrl, statModels, statConcurrency, modelCount, emptyModels, removedTitle;
    private ChipGroup removedGroup;
    private ModelListAdapter modelAdapter;
    private MaterialButton btnFetch, btnTest;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private boolean fetching;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_site_detail);
        siteId = getIntent().getStringExtra("siteId");
        MaterialToolbar toolbar = (MaterialToolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        toolbar.setOnMenuItemClickListener(new MaterialToolbar.OnMenuItemClickListener() {
            @Override public boolean onMenuItemClick(MenuItem item) { return onMenu(item); }
        });
        heroName = (TextView) findViewById(R.id.hero_name);
        heroUrl = (TextView) findViewById(R.id.hero_url);
        statModels = (TextView) findViewById(R.id.stat_models);
        statConcurrency = (TextView) findViewById(R.id.stat_concurrency);
        modelCount = (TextView) findViewById(R.id.model_count);
        emptyModels = (TextView) findViewById(R.id.empty_models);
        removedTitle = (TextView) findViewById(R.id.removed_title);
        removedGroup = (ChipGroup) findViewById(R.id.removed_group);
        RecyclerView modelRecycler = (RecyclerView) findViewById(R.id.model_recycler);
        modelAdapter = new ModelListAdapter(new ModelListAdapter.Callback() {
            @Override public void onRemove(String model) { removeModel(model); }
            @Override public void onMove(int from, int to) { saveOrder(); }
        });
        modelRecycler.setLayoutManager(new LinearLayoutManager(this));
        modelRecycler.setAdapter(modelAdapter);
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override public boolean onMove(RecyclerView recycler, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
                modelAdapter.move(source.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }
            @Override public void onSwiped(RecyclerView.ViewHolder holder, int direction) { }
        });
        helper.attachToRecyclerView(modelRecycler);
        btnFetch = (MaterialButton) findViewById(R.id.btn_fetch);
        btnTest = (MaterialButton) findViewById(R.id.btn_test);
        btnFetch.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { fetchModels(); }
        });
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { startTest(); }
        });
        bind();
    }

    @Override protected void onResume() { super.onResume(); bind(); }
    @Override protected void onDestroy() { super.onDestroy(); io.shutdownNow(); }

    private boolean onMenu(MenuItem item) {
        if (item.getItemId() == R.id.action_edit) {
            Intent intent = new Intent(this, SiteEditActivity.class);
            intent.putExtra("siteId", siteId);
            startActivity(intent);
            return true;
        }
        if (item.getItemId() == R.id.action_delete) {
            new MaterialAlertDialogBuilder(this).setTitle("删除站点")
                .setMessage("只删除站点配置，历史测试记录会保留。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        AppStorage.get(SiteDetailActivity.this).deleteSite(siteId);
                        finish();
                    }
                }).show();
            return true;
        }
        return false;
    }

    private void bind() {
        site = AppStorage.get(this).findSite(siteId);
        if (site == null) { Ui.toast(this, "站点不存在"); finish(); return; }
        heroName.setText(site.displayName());
        heroUrl.setText(site.baseUrl + "   ·   " + site.maskedKey());
        statModels.setText(String.valueOf(site.models.size()));
        statConcurrency.setText(site.concurrency + " / " + site.intervalMs + "ms");
        modelCount.setText(site.models.isEmpty() ? "点击 × 移出" : site.models.size() + " 个 · 长按拖动排序");
        modelAdapter.submit(site.models);
        bindRemoved();
        boolean empty = site.models.isEmpty();
        Ui.setVisible(emptyModels, empty);
        Ui.setVisible(findViewById(R.id.model_recycler), !empty);
        Ui.setVisible(removedTitle, !site.removedModels.isEmpty());
        Ui.setVisible(removedGroup, !site.removedModels.isEmpty());
        btnTest.setEnabled(!empty && !ProbeEngine.get().isRunning());
        btnTest.setText(ProbeEngine.get().isRunning() ? "测试进行中…" : getString(R.string.start_test));
    }

    private void bindRemoved() {
        removedGroup.removeAllViews();
        for (int i = 0; i < site.removedModels.size(); i++) {
            final String model = site.removedModels.get(i);
            Chip chip = new Chip(this);
            chip.setText(model);
            chip.setCheckable(false);
            chip.setClickable(true);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { restoreModel(model); }
            });
            removedGroup.addView(chip);
        }
    }

    private void removeModel(String model) {
        if (model == null || site == null) return;
        Site live = AppStorage.get(this).findSite(siteId);
        if (live == null) return;
        for (int i = live.models.size() - 1; i >= 0; i--) {
            if (model.equals(live.models.get(i))) live.models.remove(i);
        }
        if (!live.removedModels.contains(model)) live.removedModels.add(model);
        AppStorage.get(this).upsertSite(live);
        bind();
    }

    private void restoreModel(String model) {
        Site live = AppStorage.get(this).findSite(siteId);
        if (live == null) return;
        live.removedModels.remove(model);
        if (!live.models.contains(model)) live.models.add(model);
        AppStorage.get(this).upsertSite(live);
        Ui.toast(this, "已加入测试列表");
        bind();
    }

    private void saveOrder() {
        Site live = AppStorage.get(this).findSite(siteId);
        if (live == null) return;
        live.models.clear();
        live.models.addAll(modelAdapter.values());
        AppStorage.get(this).upsertSite(live);
    }

    private void fetchModels() {
        if (fetching || site == null) return;
        fetching = true;
        btnFetch.setEnabled(false);
        btnFetch.setText("正在获取聊天模型 …");
        final Site snapshot = site;
        io.execute(new Runnable() {
            @Override public void run() {
                try {
                    final List<String> ids = OpenAiApi.fetchModels(snapshot);
                    final List<String> chatModels = OpenAiApi.removeImageOnlyModels(ids);
                    final List<String> imageModels = new ArrayList<String>();
                    for (int i = 0; i < ids.size(); i++) {
                        String id = ids.get(i);
                        if (OpenAiApi.isImageOnlyModel(id)) imageModels.add(id);
                    }
                    main.post(new Runnable() {
                        @Override public void run() {
                            fetching = false;
                            btnFetch.setEnabled(true);
                            btnFetch.setText("获取聊天模型");
                            Site live = AppStorage.get(SiteDetailActivity.this).findSite(siteId);
                            if (live == null) return;
                            int added = 0;
                            for (int i = 0; i < chatModels.size(); i++) {
                                String id = chatModels.get(i);
                                if (!live.models.contains(id) && !live.removedModels.contains(id)) {
                                    live.models.add(id);
                                    added++;
                                }
                            }
                            for (int i = 0; i < imageModels.size(); i++) {
                                String id = imageModels.get(i);
                                for (int j = live.models.size() - 1; j >= 0; j--) {
                                    if (id.equals(live.models.get(j))) live.models.remove(j);
                                }
                                if (!live.removedModels.contains(id)) live.removedModels.add(id);
                            }
                            live.lastFetchAt = System.currentTimeMillis();
                            AppStorage.get(SiteDetailActivity.this).upsertSite(live);
                            String message = "已加入 " + added + " 个聊天模型";
                            if (!imageModels.isEmpty()) message += "，移出 " + imageModels.size() + " 个图片模型";
                            Ui.toast(SiteDetailActivity.this, message);
                            bind();
                        }
                    });
                } catch (final Exception error) {
                    main.post(new Runnable() {
                        @Override public void run() {
                            fetching = false;
                            btnFetch.setEnabled(true);
                            btnFetch.setText("获取聊天模型");
                            new MaterialAlertDialogBuilder(SiteDetailActivity.this).setTitle("获取聊天模型失败")
                                .setMessage(error.getMessage() == null ? "拉取失败" : error.getMessage())
                                .setPositiveButton("知道了", null).show();
                        }
                    });
                }
            }
        });
    }

    private void startTest() {
        if (site == null) return;
        if (site.models.isEmpty()) { Ui.toast(this, "请先加入聊天模型"); return; }
        if (ProbeEngine.get().isRunning()) {
            startActivity(new Intent(this, TestRunActivity.class).putExtra("siteId", siteId));
            return;
        }
        new MaterialAlertDialogBuilder(this).setTitle("开始连通测试")
            .setMessage("将对 " + site.models.size() + " 个聊天模型调用 /v1/chat/completions，并发 " + site.concurrency + "，间隔 " + site.intervalMs + "ms。")
            .setNegativeButton("取消", null)
            .setPositiveButton("开始", new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    boolean ok = ProbeEngine.get().start(SiteDetailActivity.this, site, new ArrayList<String>(site.models));
                    if (!ok) { Ui.toast(SiteDetailActivity.this, "无法启动测试"); return; }
                    startActivity(new Intent(SiteDetailActivity.this, TestRunActivity.class).putExtra("siteId", siteId));
                }
            }).show();
    }
}
