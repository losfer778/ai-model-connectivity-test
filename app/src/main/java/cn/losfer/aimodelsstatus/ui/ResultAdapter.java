package cn.losfer.aimodelsstatus.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.losfer.aimodelsstatus.R;
import cn.losfer.aimodelsstatus.ModelResult;

public class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.Holder> {
    private final List<ModelResult> items = new ArrayList<ModelResult>();

    public void submit(List<ModelResult> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public void updateByModel(ModelResult item) {
        if (item == null || item.model == null) return;
        for (int i = 0; i < items.size(); i++) {
            if (item.model.equals(items.get(i).model)) {
                items.set(i, item);
                notifyItemChanged(i);
                return;
            }
        }
    }

    @Override public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_model_result, parent, false));
    }

    @Override public void onBindViewHolder(Holder holder, int position) {
        ModelResult result = items.get(position);
        holder.name.setText(result.model);
        if (result.state == ModelResult.OK) {
            holder.icon.setImageResource(R.drawable.ic_check);
            holder.latency.setText(Ui.formatLatency(result.latencyMs));
            holder.latency.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.success));
            String text = result.reply == null ? "" : result.reply.trim();
            holder.response.setText(text.length() == 0 ? "HTTP " + result.httpCode + "，未返回文本内容" : text);
            holder.metrics.setText(metrics(result));
        } else if (result.state == ModelResult.FAIL) {
            holder.icon.setImageResource(R.drawable.ic_error);
            holder.latency.setText(result.latencyMs > 0 ? Ui.formatLatency(result.latencyMs) : "失败");
            holder.latency.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.error));
            String error = result.error == null ? "" : result.error.trim();
            holder.response.setText(error.length() == 0 ? "HTTP " + result.httpCode : error);
            holder.metrics.setText(result.httpCode > 0 ? "HTTP " + result.httpCode + "  ·  耗时 " + Ui.formatLatency(result.latencyMs) : "请求未完成");
        } else if (result.state == ModelResult.RUNNING) {
            holder.icon.setImageResource(R.drawable.ic_pending);
            holder.latency.setText("请求中");
            holder.latency.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.outline));
            holder.response.setText("正在调用 /v1/chat/completions");
            holder.metrics.setText("");
        } else {
            holder.icon.setImageResource(R.drawable.ic_pending);
            holder.latency.setText("排队");
            holder.latency.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.outline));
            holder.response.setText("等待测试");
            holder.metrics.setText("");
        }
    }

    private String metrics(ModelResult result) {
        return "HTTP " + result.httpCode + "  ·  耗时 " + Ui.formatLatency(result.latencyMs)
            + "  ·  输入 " + result.promptTokens
            + "  输出 " + result.completionTokens
            + "  缓存 " + result.cachedTokens
            + "  总计 " + result.totalTokens;
    }

    @Override public int getItemCount() { return items.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name, response, metrics, latency;
        Holder(View view) {
            super(view);
            icon = (ImageView) view.findViewById(R.id.icon);
            name = (TextView) view.findViewById(R.id.name);
            response = (TextView) view.findViewById(R.id.response);
            metrics = (TextView) view.findViewById(R.id.metrics);
            latency = (TextView) view.findViewById(R.id.latency);
        }
    }
}
