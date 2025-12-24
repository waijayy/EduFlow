package com.example.eduflow;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.eduflow.adapters.AuthPagerAdapter;
import com.example.eduflow.auth.SupabaseManager;
import com.example.eduflow.databinding.ActivityLoginBinding;
import com.google.android.material.tabs.TabLayoutMediator;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Supabase
        SupabaseManager.initialize(this);

        // Check if already logged in
        if (SupabaseManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        EdgeToEdge.enable(this);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupViewPager();
    }

    private void setupViewPager() {
        AuthPagerAdapter adapter = new AuthPagerAdapter(this);
        binding.viewPager.setAdapter(adapter);

        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText(getString(R.string.login));
            } else {
                tab.setText(getString(R.string.sign_up));
            }
        }).attach();
    }

    public void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    public void switchToSignUp() {
        binding.viewPager.setCurrentItem(1);
    }

    public void switchToLogin() {
        binding.viewPager.setCurrentItem(0);
    }
}
