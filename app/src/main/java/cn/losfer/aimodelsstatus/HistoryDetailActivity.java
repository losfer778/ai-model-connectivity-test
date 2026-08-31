package cn.losfer.aimodelsstatus;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import cn.losfer.aimodelsstatus.AppStorage;
import cn.losfer.aimodelsstatus.TestRun;
import cn.losfer.aimodelsstatus.ui.ResultAdapter;
import cn.losfer.aimodelsstatus.ui.Ui;

public class HistoryDetailActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_history_detail);
        MaterialToolbar toolbar = (MaterialToolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        String id = getIntent().getStringExtra("runId");
        TestRun run = AppStorage.get(this).findRun(id);
        if (run == null) {
            Ui.toast(this, "记录不存在");
            finish();
            return;
        }
        ((TextView) findViewById(R.id.site_name)).setText(run.siteName);
        String meta = (run.baseUrl == null ? "" : run.baseUrl) + "\n" + Ui.formatFull(run.startedAt)
            + "  ·  并发 " + run.concurrency + "  ·  间隔 " + run.intervalMs + "ms"
            + (run.cancelled ? "  ·  中途停止" : "");
        ((TextView) findViewById(R.id.site_meta)).setText(meta);
        ((TextView) findViewById(R.id.stat_total)).setText(String.valueOf(run.total));
        ((TextView) findViewById(R.id.stat_ok)).setText(String.valueOf(run.okCount));
        ((TextView) findViewById(R.id.stat_fail)).setText(String.valueOf(run.failCount));
        ((TextView) findViewById(R.id.stat_time)).setText(Ui.formatDuration(run.durationMs));
        RecyclerView recycler = (RecyclerView) findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        ResultAdapter adapter = new ResultAdapter();
        recycler.setAdapter(adapter);
        adapter.submit(run.results);
    }
}
