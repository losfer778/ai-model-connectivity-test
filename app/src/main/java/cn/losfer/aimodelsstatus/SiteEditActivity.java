package cn.losfer.aimodelsstatus;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import cn.losfer.aimodelsstatus.Defaults;
import cn.losfer.aimodelsstatus.Site;
import cn.losfer.aimodelsstatus.AppStorage;
import cn.losfer.aimodelsstatus.OpenAiApi;
import cn.losfer.aimodelsstatus.ui.Ui;

public class SiteEditActivity extends AppCompatActivity {
    private EditText inName, inUrl, inKey, inConcurrency, inInterval, inTimeout, inMaxTokens;
    private Site editing;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_site_edit);
        MaterialToolbar toolbar = (MaterialToolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        inName = (EditText) findViewById(R.id.input_name);
        inUrl = (EditText) findViewById(R.id.input_base_url);
        inKey = (EditText) findViewById(R.id.input_api_key);
        inConcurrency = (EditText) findViewById(R.id.input_concurrency);
        inInterval = (EditText) findViewById(R.id.input_interval);
        inTimeout = (EditText) findViewById(R.id.input_timeout);
        inMaxTokens = (EditText) findViewById(R.id.input_max_tokens);
        MaterialButton save = (MaterialButton) findViewById(R.id.btn_save);
        String id = getIntent().getStringExtra("siteId");
        AppStorage store = AppStorage.get(this);
        Defaults defaults = store.defaults();
        if (id != null) editing = store.findSite(id);
        if (editing != null) {
            toolbar.setTitle(R.string.edit_site);
            inName.setText(editing.name);
            inUrl.setText(editing.baseUrl);
            inKey.setText(editing.apiKey);
            inConcurrency.setText(String.valueOf(editing.concurrency));
            inInterval.setText(String.valueOf(editing.intervalMs));
            inTimeout.setText(String.valueOf(editing.timeoutMs));
            inMaxTokens.setText(String.valueOf(editing.maxTokens));
        } else {
            toolbar.setTitle(R.string.add_site);
            inConcurrency.setText(String.valueOf(defaults.concurrency));
            inInterval.setText(String.valueOf(defaults.intervalMs));
            inTimeout.setText(String.valueOf(defaults.timeoutMs));
            inMaxTokens.setText(String.valueOf(defaults.maxTokens));
        }
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { persist(); }
        });
    }

    private void persist() {
        String url = OpenAiApi.normalizeBase(Ui.text(inUrl));
        String key = Ui.text(inKey);
        if (url.length() == 0) {
            Ui.toast(this, "请填写 API 域名");
            inUrl.requestFocus();
            return;
        }
        if (key.length() == 0) {
            Ui.toast(this, "请填写 API Key");
            inKey.requestFocus();
            return;
        }
        AppStorage store = AppStorage.get(this);
        Site site = editing == null ? Site.create() : editing;
        if (editing == null) store.defaults().applyTo(site);
        site.name = Ui.text(inName);
        site.baseUrl = url;
        site.apiKey = key;
        site.concurrency = Math.max(1, Math.min(32, Ui.parseInt(inConcurrency.getText(), 3)));
        site.intervalMs = Math.max(0, Ui.parseInt(inInterval.getText(), 200));
        site.timeoutMs = Math.max(1000, Ui.parseInt(inTimeout.getText(), 30000));
        site.maxTokens = Math.max(1, Math.min(256, Ui.parseInt(inMaxTokens.getText(), 8)));
        if (site.prompt == null || site.prompt.trim().length() == 0) site.prompt = store.defaults().prompt;
        store.upsertSite(site);
        Ui.toast(this, "已保存");
        finish();
    }
}
