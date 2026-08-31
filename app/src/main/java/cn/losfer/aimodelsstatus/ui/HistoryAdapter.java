package cn.losfer.aimodelsstatus.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.losfer.aimodelsstatus.R;
import cn.losfer.aimodelsstatus.TestRun;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {
    public interface OnClick { void onRun(TestRun run); }
    private final List<TestRun> items = new ArrayList<TestRun>();
    private final OnClick click;

    public HistoryAdapter(OnClick callback) { click = callback; }

    public void submit(List<TestRun> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @Override public Holder onCreateViewHolder(ViewGroup parent, int type) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new Holder(view);
    }

    @Override public void onBindViewHolder(Holder holder, int position) {
        final TestRun run = items.get(position);
        holder.title.setText(run.siteName == null || run.siteName.length() == 0 ? "站点测试" : run.siteName);
        holder.time.setText(Ui.formatTime(run.startedAt));
        holder.statOk.setText("正常 " + run.okCount);
        holder.statFail.setText("异常 " + run.failCount);
        holder.statDuration.setText(Ui.formatDuration(run.durationMs));
        int ok = Math.max(0, run.okCount);
        int fail = Math.max(0, run.failCount);
        if (ok + fail == 0) { ok = 1; fail = 0; }
        LinearLayout.LayoutParams okParams = (LinearLayout.LayoutParams) holder.barOk.getLayoutParams();
        LinearLayout.LayoutParams failParams = (LinearLayout.LayoutParams) holder.barFail.getLayoutParams();
        okParams.weight = ok;
        failParams.weight = fail;
        holder.barOk.setLayoutParams(okParams);
        holder.barFail.setLayoutParams(failParams);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (click != null) click.onRun(run);
            }
        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView title, time, statOk, statFail, statDuration;
        final View barOk, barFail;
        Holder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.title);
            time = (TextView) view.findViewById(R.id.time);
            statOk = (TextView) view.findViewById(R.id.stat_ok);
            statFail = (TextView) view.findViewById(R.id.stat_fail);
            statDuration = (TextView) view.findViewById(R.id.stat_duration);
            barOk = view.findViewById(R.id.bar_ok);
            barFail = view.findViewById(R.id.bar_fail);
        }
    }
}
