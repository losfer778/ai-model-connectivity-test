package cn.losfer.aimodelsstatus;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import cn.losfer.aimodelsstatus.ModelResult;
import cn.losfer.aimodelsstatus.TestRun;
import cn.losfer.aimodelsstatus.ProbeEngine;
import cn.losfer.aimodelsstatus.ui.ResultAdapter;
import cn.losfer.aimodelsstatus.ui.Ui;

public class TestRunActivity extends AppCompatActivity implements ProbeEngine.Listener {
    private LinearProgressIndicator progress;
    private TextView progressLabel, chipOk, chipFail, chipElapsed;
    private MaterialButton btnStop;
    private ResultAdapter adapter;
    private final Handler ticker = new Handler(Looper.getMainLooper());
    private long startedAt;
    private boolean finished;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (finished) return;
            TestRun run = ProbeEngine.get().current();
            if (run != null) render(run);
            ticker.postDelayed(this, 400);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_test_run);
        MaterialToolbar toolbar = (MaterialToolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        progress = (LinearProgressIndicator) findViewById(R.id.progress);
        progressLabel = (TextView) findViewById(R.id.progress_label);
        chipOk = (TextView) findViewById(R.id.chip_ok);
        chipFail = (TextView) findViewById(R.id.chip_fail);
        chipElapsed = (TextView) findViewById(R.id.chip_elapsed);
        btnStop = (MaterialButton) findViewById(R.id.btn_stop);
        RecyclerView recycler = (RecyclerView) findViewById(R.id.recycler);
        adapter = new ResultAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { stopOrFinish(); }
        });
        ProbeEngine.get().setListener(this);
        TestRun run = ProbeEngine.get().current();
        if (run != null) {
            startedAt = run.startedAt;
            adapter.submit(run.results);
            render(run);
            if (!ProbeEngine.get().isRunning()) onFinished(run);
        } else {
            progressLabel.setText("没有正在进行的测试");
            btnStop.setText("返回");
        }
    }

    private void stopOrFinish() {
        if (ProbeEngine.get().isRunning()) {
            ProbeEngine.get().cancel();
            btnStop.setEnabled(false);
            btnStop.setText("正在停止…");
        } else finish();
    }

    @Override protected void onResume() {
        super.onResume();
        ProbeEngine.get().setListener(this);
        ticker.post(tick);
    }

    @Override protected void onPause() {
        super.onPause();
        ticker.removeCallbacks(tick);
        if (isFinishing()) ProbeEngine.get().setListener(null);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        ticker.removeCallbacks(tick);
        ProbeEngine.get().setListener(null);
    }

    @Override public void onPrepared(TestRun run) {
        startedAt = run.startedAt;
        adapter.submit(run.results);
        render(run);
    }

    @Override public void onItem(ModelResult item, TestRun run) { adapter.updateByModel(item); }
    @Override public void onTick(TestRun run) { render(run); }

    @Override public void onFinished(final TestRun run) {
        finished = true;
        ticker.removeCallbacks(tick);
        adapter.submit(run.results);
        render(run);
        progress.setProgressCompat(100, true);
        String title = run.cancelled ? "测试已停止" : "测试完成";
        progressLabel.setText(title + "  ·  " + run.okCount + " 正常 / " + run.failCount + " 异常");
        btnStop.setEnabled(true);
        btnStop.setText("完成");
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
    }

    private void render(TestRun run) {
        int done = run.okCount + run.failCount;
        int total = Math.max(1, run.total);
        int percent = Math.min(100, done * 100 / total);
        progress.setProgressCompat(percent, true);
        progressLabel.setText("正在测试  " + done + " / " + run.total);
        chipOk.setText("正常 " + run.okCount);
        chipFail.setText("异常 " + run.failCount);
        long base = startedAt == 0 ? System.currentTimeMillis() : startedAt;
        long elapsed = run.durationMs > 0 ? run.durationMs : System.currentTimeMillis() - base;
        chipElapsed.setText(Ui.formatDuration(elapsed));
    }
}
