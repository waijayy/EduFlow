package com.example.eduflow.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eduflow.MainActivity;
import com.example.eduflow.R;
import com.example.eduflow.auth.SupabaseManager;
import com.example.eduflow.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupProfile();
        setupClickListeners();
    }

    private void setupProfile() {
        String userName = SupabaseManager.getUserName();
        String userEmail = SupabaseManager.getUserEmail();
        String memberSince = SupabaseManager.getMemberSince();

        if (userName.isEmpty())
            userName = "User";
        if (userEmail.isEmpty())
            userEmail = "user@example.com";

        binding.tvUserName.setText(userName);
        binding.tvUserType.setText(getString(R.string.premium_member));
        binding.tvEmail.setText(userEmail);
        binding.tvMemberSince.setText(memberSince);
        binding.tvAccountType.setText("Premium");

        // Set avatar initial
        String initial = !userName.isEmpty() ? String.valueOf(userName.charAt(0)).toUpperCase() : "U";
        binding.tvAvatarInitial.setText(initial);
    }

    private void setupClickListeners() {
        binding.btnNotifications.setOnClickListener(v -> {
            // Handle notifications settings
        });

        binding.btnPrivacy.setOnClickListener(v -> {
            // Handle privacy settings
        });

        binding.btnLogout.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).logout();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
