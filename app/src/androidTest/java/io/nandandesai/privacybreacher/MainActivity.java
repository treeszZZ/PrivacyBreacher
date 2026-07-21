package io.nandandesai.privacybreacher;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    private final HomeFragment homeFragment = new HomeFragment();
    private final AddFragment addFragment = new AddFragment();
    private final SettingsFragment settingsFragment = new SettingsFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // 默认显示首页
        if (savedInstanceState == null) {
            loadFragment(homeFragment);
        }

        // 底部导航切换监听
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.navigation_home) {
                    loadFragment(homeFragment);
                    return true;
                } else if (itemId == R.id.navigation_add) {
                    loadFragment(addFragment);
                    return true;
                } else if (itemId == R.id.navigation_settings) {
                    loadFragment(settingsFragment);
                    return true;
                }
                return false;
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    // 供 AddFragment 调用的方法：添加记录后切回首页
    public void switchToHome() {
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        loadFragment(homeFragment);
    }

    // 供外部调用的刷新方法（如设置页面修改阈值后刷新首页显示）
    public void refreshHome() {
        if (homeFragment.isAdded() && !homeFragment.isDetached()) {
            homeFragment.refreshData();
        }
    }
}
