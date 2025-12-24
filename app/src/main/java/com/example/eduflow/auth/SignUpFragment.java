package com.example.eduflow.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eduflow.LoginActivity;
import com.example.eduflow.databinding.FragmentSignupBinding;

public class SignUpFragment extends Fragment {

    private FragmentSignupBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentSignupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnSignUp.setOnClickListener(v -> {
            String name = binding.etName.getText().toString().trim();
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(getContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.btnSignUp.setEnabled(false);
            binding.progressBar.setVisibility(View.VISIBLE);

            SupabaseManager.signUp(name, email, password)
                    .thenAccept(success -> {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                binding.btnSignUp.setEnabled(true);
                                binding.progressBar.setVisibility(View.GONE);
                                if (success && getActivity() instanceof LoginActivity) {
                                    ((LoginActivity) getActivity()).navigateToMain();
                                }
                            });
                        }
                    })
                    .exceptionally(error -> {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                binding.btnSignUp.setEnabled(true);
                                binding.progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                        return null;
                    });
        });

        binding.tvLogin.setOnClickListener(v -> {
            if (getActivity() instanceof LoginActivity) {
                ((LoginActivity) getActivity()).switchToLogin();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
