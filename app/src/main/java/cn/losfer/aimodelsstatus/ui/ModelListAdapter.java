package cn.losfer.aimodelsstatus.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.losfer.aimodelsstatus.R;

public class ModelListAdapter extends RecyclerView.Adapter<ModelListAdapter.Holder> {
    public interface Callback {
        void onRemove(String model);
        void onMove(int from, int to);
    }

    private final List<String> items = new ArrayList<String>();
    private final Callback callback;

    public ModelListAdapter(Callback value) { callback = value; }

    public void submit(List<String> values) {
        items.clear();
        if (values != null) items.addAll(values);
        notifyDataSetChanged();
    }

    public List<String> values() { return new ArrayList<String>(items); }

    public void move(int from, int to) {
        if (from < 0 || to < 0 || from >= items.size() || to >= items.size()) return;
        String value = items.remove(from);
        items.add(to, value);
        notifyItemMoved(from, to);
        if (callback != null) callback.onMove(from, to);
    }

    @Override public Holder onCreateViewHolder(ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_site_model, parent, false));
    }

    @Override public void onBindViewHolder(Holder holder, int position) {
        final String model = items.get(position);
        holder.name.setText(model);
        holder.remove.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { if (callback != null) callback.onRemove(model); }
        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView name;
        final ImageButton remove;
        Holder(View view) {
            super(view);
            name = (TextView) view.findViewById(R.id.model_name);
            remove = (ImageButton) view.findViewById(R.id.btn_remove);
        }
    }
}
