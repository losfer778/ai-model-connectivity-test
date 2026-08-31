package cn.losfer.aimodelsstatus.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.losfer.aimodelsstatus.R;
import cn.losfer.aimodelsstatus.Site;

public class SiteAdapter extends RecyclerView.Adapter<SiteAdapter.Holder> {
    public interface OnClick { void onSite(Site site); }
    private final List<Site> items = new ArrayList<Site>();
    private final OnClick click;

    public SiteAdapter(OnClick callback) { click = callback; }

    public void submit(List<Site> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @Override public Holder onCreateViewHolder(ViewGroup parent, int type) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_site, parent, false);
        return new Holder(view);
    }

    @Override public void onBindViewHolder(Holder holder, int position) {
        final Site site = items.get(position);
        holder.title.setText(site.displayName());
        holder.subtitle.setText(site.baseUrl);
        holder.avatar.setText(site.initial());
        Ui.paintAvatar(holder.avatar, Ui.avatarColor(holder.itemView.getContext(), site.avatarColorIndex()));
        int count = site.models.size();
        holder.chipModels.setText(count == 0 ? "尚未拉取模型" : count + " 个模型");
        if (site.lastTestAt > 0) {
            holder.chipStatus.setText("上次 " + site.lastOk + " 正常 / " + site.lastFail + " 异常");
            holder.chipStatus.setBackgroundResource(site.lastFail == 0 ? R.drawable.bg_chip_ok : R.drawable.bg_chip_fail);
        } else {
            holder.chipStatus.setText("未测试");
            holder.chipStatus.setBackgroundResource(R.drawable.bg_chip_neutral);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (click != null) click.onSite(site);
            }
        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView avatar, title, subtitle, chipModels, chipStatus;
        Holder(View view) {
            super(view);
            avatar = (TextView) view.findViewById(R.id.avatar);
            title = (TextView) view.findViewById(R.id.title);
            subtitle = (TextView) view.findViewById(R.id.subtitle);
            chipModels = (TextView) view.findViewById(R.id.chip_models);
            chipStatus = (TextView) view.findViewById(R.id.chip_status);
        }
    }
}
