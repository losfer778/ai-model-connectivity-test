package cn.losfer.aimodelsstatus;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.losfer.aimodelsstatus.ModelResult;
import cn.losfer.aimodelsstatus.Site;
import cn.losfer.aimodelsstatus.AppStorage;
import cn.losfer.aimodelsstatus.TestRun;
import cn.losfer.aimodelsstatus.OpenAiApi;

public class ProbeEngine {
    public interface Listener {
        void onPrepared(TestRun run);
        void onItem(ModelResult item, TestRun run);
        void onTick(TestRun run);
        void onFinished(TestRun run);
    }

    private static ProbeEngine instance;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private ExecutorService pool;
    private Listener listener;
    private TestRun current;
    private Site site;
    private long startedAt;

    public static synchronized ProbeEngine get() {
        if (instance == null) instance = new ProbeEngine();
        return instance;
    }

    public synchronized boolean isRunning() {
        return running.get();
    }

    public synchronized TestRun current() {
        return current;
    }

    public synchronized void setListener(Listener value) {
        listener = value;
    }

    public synchronized boolean start(final Context context, Site value, List<String> models) {
        if (running.get() || value == null || models == null || models.isEmpty()) return false;
        site = value;
        final List<String> modelList = new ArrayList<String>(models);
        cancelled.set(false);
        running.set(true);
        startedAt = System.currentTimeMillis();
        current = TestRun.create(site, modelList.size());
        current.results.clear();
        for (int i = 0; i < modelList.size(); i++) {
            ModelResult result = new ModelResult();
            result.model = modelList.get(i);
            result.state = ModelResult.PENDING;
            current.results.add(result);
        }
        int workers = Math.max(1, Math.min(site.concurrency, modelList.size()));
        pool = Executors.newFixedThreadPool(workers);
        final Listener callback = listener;
        final TestRun initialRun = current;
        if (callback != null) {
            main.post(new Runnable() {
                @Override public void run() {
                    callback.onPrepared(initialRun);
                }
            });
        }
        Thread dispatcher = new Thread(new Runnable() {
            @Override public void run() {
                dispatch(context, modelList);
            }
        }, "probe-dispatch");
        dispatcher.start();
        return true;
    }

    public void cancel() {
        cancelled.set(true);
        ExecutorService service = pool;
        if (service != null) service.shutdownNow();
    }

    private void dispatch(Context context, List<String> models) {
        int interval = Math.max(0, site.intervalMs);
        for (int i = 0; i < models.size(); i++) {
            if (cancelled.get()) break;
            final int index = i;
            final String model = models.get(i);
            try {
                pool.execute(new Runnable() {
                    @Override public void run() {
                        runOne(index, model);
                    }
                });
            } catch (Exception e) {
                failLocal(index, model, e.getMessage());
            }
            if (interval > 0 && i < models.size() - 1) {
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }
        ExecutorService service = pool;
        if (service != null) {
            service.shutdown();
            try {
                service.awaitTermination(2, TimeUnit.HOURS);
            } catch (InterruptedException ignored) {
            }
        }
        finishRun(context);
    }

    private void runOne(int index, String model) {
        if (cancelled.get()) {
            failLocal(index, model, "已取消");
            return;
        }
        markRunning(index);
        ModelResult result;
        try {
            result = OpenAiApi.probeChat(site, model);
        } catch (Exception e) {
            result = new ModelResult();
            result.model = model;
            result.ok = false;
            result.state = ModelResult.FAIL;
            result.error = e.getMessage();
        }
        apply(index, result);
    }

    private synchronized void markRunning(int index) {
        if (current == null || index < 0 || index >= current.results.size()) return;
        ModelResult result = current.results.get(index);
        result.state = ModelResult.RUNNING;
        notifyItem(result);
    }

    private void failLocal(int index, String model, String message) {
        ModelResult result = new ModelResult();
        result.model = model;
        result.ok = false;
        result.state = ModelResult.FAIL;
        result.error = message == null ? "失败" : message;
        apply(index, result);
    }

    private synchronized void apply(int index, ModelResult result) {
        if (current == null || index < 0 || index >= current.results.size()) return;
        current.results.set(index, result);
        if (result.ok) current.okCount++;
        else current.failCount++;
        current.durationMs = System.currentTimeMillis() - startedAt;
        notifyItem(result);
    }

    private void notifyItem(final ModelResult result) {
        final Listener callback = listener;
        final TestRun run = current;
        if (callback == null || run == null) return;
        main.post(new Runnable() {
            @Override public void run() {
                callback.onItem(result, run);
                callback.onTick(run);
            }
        });
    }

    private void finishRun(Context context) {
        TestRun run;
        synchronized (this) {
            run = current;
            if (run != null) {
                run.finishedAt = System.currentTimeMillis();
                run.durationMs = run.finishedAt - startedAt;
                run.cancelled = cancelled.get();
                for (int i = 0; i < run.results.size(); i++) {
                    ModelResult result = run.results.get(i);
                    if (result.state == ModelResult.PENDING || result.state == ModelResult.RUNNING) {
                        result.state = ModelResult.FAIL;
                        result.ok = false;
                        if (result.error == null || result.error.length() == 0) result.error = "未完成";
                        run.failCount++;
                    }
                }
            }
        }
        if (run != null) {
            try {
                AppStorage store = AppStorage.get(context);
                store.addRun(run);
                Site saved = store.findSite(run.siteId);
                if (saved != null) {
                    saved.lastTestAt = run.finishedAt;
                    saved.lastOk = run.okCount;
                    saved.lastFail = run.failCount;
                    saved.lastDurationMs = run.durationMs;
                    store.upsertSite(saved);
                }
            } catch (Exception ignored) {
            }
        }
        running.set(false);
        final Listener callback = listener;
        final TestRun finishedRun = run;
        if (callback != null && finishedRun != null) {
            main.post(new Runnable() {
                @Override public void run() {
                    callback.onFinished(finishedRun);
                }
            });
        }
    }
}
