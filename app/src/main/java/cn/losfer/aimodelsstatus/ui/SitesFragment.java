package cn.losfer.aimodelsstatus.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.losfer.aimodelsstatus.R;
import cn.losfer.aimodelsstatus.SiteDetailActivity;
import cn.losfer.aimodelsstatus.Site;
import cn.losfer.aimodelsstatus.AppStorage;

public class SitesFragment extends Fragment {
    private SiteAdapter adapter;
    private View empty;
    private RecyclerView recycler;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
        return inflater.inflate(R.layout.fragment_sites, parent, false);
    }

    @Override public void onViewCreated(View view, Bundle state) {
        recycler = (RecyclerView) view.findViewById(R.id.recycler);
        empty = view.findViewById(R.id.empty);
        adapter = new SiteAdapter(new SiteAdapter.OnClick() {
            @Override public void onSite(Site site) {
                Intent intent = new Intent(requireContext(), SiteDetailActivity.class);
                intent.putExtra("siteId", site.id);
                startActivity(intent);
            }
        });
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);
    }

    @Override public void onResume() { super.onResume(); reload(); }

    public void reload() {
        if (!isAdded() || adapter == null) return;
        List<Site> list = AppStorage.get(requireContext()).sites();
        adapter.submit(list);
        Ui.setVisible(empty, list.isEmpty());
        Ui.setVisible(recycler, !list.isEmpty());
    }
}
