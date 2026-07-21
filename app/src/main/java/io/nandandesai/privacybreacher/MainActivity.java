package io.nandandesai.privacybreacher;

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

    // 供 AddFragment 调用：添加记录后切回首页并刷新
    public void switchToHome() {
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        loadFragment(homeFragment);
        homeFragment.refreshData();
    }

    // 供 SettingsFragment 调用：修改阈值后刷新首页显示
    public void refreshHome() {
        homeFragment.refreshData();
    }
}
