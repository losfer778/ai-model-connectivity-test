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

import cn.losfer.aimodelsstatus.HistoryDetailActivity;
import cn.losfer.aimodelsstatus.R;
import cn.losfer.aimodelsstatus.AppStorage;
import cn.losfer.aimodelsstatus.TestRun;

public class HistoryFragment extends Fragment {
    private HistoryAdapter adapter;
    private View empty;
    private RecyclerView recycler;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
        return inflater.inflate(R.layout.fragment_history, parent, false);
    }

    @Override public void onViewCreated(View view, Bundle state) {
        recycler = (RecyclerView) view.findViewById(R.id.recycler);
        empty = view.findViewById(R.id.empty);
        adapter = new HistoryAdapter(new HistoryAdapter.OnClick() {
            @Override public void onRun(TestRun run) {
                Intent intent = new Intent(requireContext(), HistoryDetailActivity.class);
                intent.putExtra("runId", run.id);
                startActivity(intent);
            }
        });
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);
    }

    @Override public void onResume() { super.onResume(); reload(); }

    public void reload() {
        if (!isAdded() || adapter == null) return;
        List<TestRun> list = AppStorage.get(requireContext()).history();
        adapter.submit(list);
        Ui.setVisible(empty, list.isEmpty());
        Ui.setVisible(recycler, !list.isEmpty());
    }
}
