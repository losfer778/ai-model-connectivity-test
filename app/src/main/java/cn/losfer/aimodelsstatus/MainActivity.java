package cn.losfer.aimodelsstatus;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import cn.losfer.aimodelsstatus.ui.HistoryFragment;
import cn.losfer.aimodelsstatus.ui.SettingsFragment;
import cn.losfer.aimodelsstatus.ui.SitesFragment;

public class MainActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private FloatingActionButton fab;
    private int currentTab = R.id.nav_sites;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        toolbar = (MaterialToolbar) findViewById(R.id.toolbar);
        fab = (FloatingActionButton) findViewById(R.id.fab_add);
        BottomNavigationView nav = (BottomNavigationView) findViewById(R.id.bottom_nav);
        toolbar.setTitle(R.string.app_name);
        if (state == null) showTab(R.id.nav_sites);
        else {
            currentTab = state.getInt("tab", R.id.nav_sites);
            showTab(currentTab);
            nav.setSelectedItemId(currentTab);
        }
        nav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override public boolean onNavigationItemSelected(android.view.MenuItem item) {
                showTab(item.getItemId());
                return true;
            }
        });
        fab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SiteEditActivity.class));
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("tab", currentTab);
    }

    private void showTab(int id) {
        currentTab = id;
        Fragment fragment;
        if (id == R.id.nav_history) fragment = new HistoryFragment();
        else if (id == R.id.nav_settings) fragment = new SettingsFragment();
        else {
            currentTab = R.id.nav_sites;
            fragment = new SitesFragment();
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment, "tab").commit();
        applyChrome(currentTab);
    }

    private void applyChrome(int id) {
        if (id == R.id.nav_history) {
            toolbar.setTitle("历史测试");
            toolbar.setSubtitle("每次跑完都会留档");
            fab.setVisibility(View.GONE);
        } else if (id == R.id.nav_settings) {
            toolbar.setTitle("设置");
            toolbar.setSubtitle("默认并发、间隔与探测词");
            fab.setVisibility(View.GONE);
        } else {
            toolbar.setTitle(R.string.app_name);
            toolbar.setSubtitle("OpenAI 兼容站点");
            fab.setVisibility(View.VISIBLE);
        }
    }
}
