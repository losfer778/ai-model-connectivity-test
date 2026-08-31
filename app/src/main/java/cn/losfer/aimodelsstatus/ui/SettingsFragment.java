package cn.losfer.aimodelsstatus.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import cn.losfer.aimodelsstatus.R;
import cn.losfer.aimodelsstatus.Defaults;
import cn.losfer.aimodelsstatus.AppStorage;

public class SettingsFragment extends Fragment {
    private EditText inConcurrency, inInterval, inTimeout, inMaxTokens, inPrompt;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
        return inflater.inflate(R.layout.fragment_settings, parent, false);
    }

    @Override public void onViewCreated(View view, Bundle state) {
        inConcurrency = (EditText) view.findViewById(R.id.input_concurrency);
        inInterval = (EditText) view.findViewById(R.id.input_interval);
        inTimeout = (EditText) view.findViewById(R.id.input_timeout);
        inMaxTokens = (EditText) view.findViewById(R.id.input_max_tokens);
        inPrompt = (EditText) view.findViewById(R.id.input_prompt);
        MaterialButton save = (MaterialButton) view.findViewById(R.id.btn_save_defaults);
        View authorRow = view.findViewById(R.id.author_row);
        bind();
        authorRow.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.losfer.cn")));
                } catch (Exception e) {
                    Ui.toast(requireContext(), "未找到可打开链接的浏览器");
                }
            }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { persist(); }
        });
    }

    @Override public void onResume() { super.onResume(); bind(); }

    private void bind() {
        if (!isAdded() || inConcurrency == null) return;
        Defaults d = AppStorage.get(requireContext()).defaults();
        inConcurrency.setText(String.valueOf(d.concurrency));
        inInterval.setText(String.valueOf(d.intervalMs));
        inTimeout.setText(String.valueOf(d.timeoutMs));
        inMaxTokens.setText(String.valueOf(d.maxTokens));
        inPrompt.setText(d.prompt);
    }

    private void persist() {
        Defaults d = new Defaults();
        d.concurrency = Math.max(1, Math.min(32, Ui.parseInt(inConcurrency.getText(), 3)));
        d.intervalMs = Math.max(0, Ui.parseInt(inInterval.getText(), 200));
        d.timeoutMs = Math.max(1000, Ui.parseInt(inTimeout.getText(), 30000));
        d.maxTokens = Math.max(1, Math.min(256, Ui.parseInt(inMaxTokens.getText(), 8)));
        d.prompt = Ui.text(inPrompt);
        if (d.prompt.length() == 0) d.prompt = "Reply with exactly the two letters OK and nothing else.";
        AppStorage.get(requireContext()).saveDefaults(d);
        Ui.toast(requireContext(), "默认参数已保存");
    }
}
