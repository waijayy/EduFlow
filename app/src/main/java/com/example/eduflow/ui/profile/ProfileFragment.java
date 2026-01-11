package com.example.eduflow.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
        binding.tvProfileName.setText(userName);
        binding.tvEmail.setText(userEmail);
        binding.tvMemberSince.setText(memberSince);
        binding.tvAccountType.setText("Premium");

        // Set avatar initial
        String initial = !userName.isEmpty() ? String.valueOf(userName.charAt(0)).toUpperCase() : "U";
        binding.tvAvatarInitial.setText(initial);
    }

    private void setupClickListeners() {
        // Edit Name button
        binding.btnEditName.setOnClickListener(v -> toggleNameEdit());

        // Save Name button
        binding.btnSaveName.setOnClickListener(v -> saveName());

        // Edit Email button
        binding.btnEditEmail.setOnClickListener(v -> toggleEmailEdit());

        // Save Email button
        binding.btnSaveEmail.setOnClickListener(v -> saveEmail());

        binding.btnMyNotes.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(getContext(),
                    com.example.eduflow.MyNotesActivity.class);
            startActivity(intent);
        });

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

    private void toggleNameEdit() {
        boolean isEditing = binding.etProfileName.getVisibility() == View.VISIBLE;

        if (isEditing) {
            // Cancel edit - revert to view mode
            binding.tvProfileName.setVisibility(View.VISIBLE);
            binding.etProfileName.setVisibility(View.GONE);
            binding.btnEditName.setImageResource(R.drawable.ic_edit);
            binding.btnSaveName.setVisibility(View.GONE);
        } else {
            // Switch to edit mode
            binding.tvProfileName.setVisibility(View.GONE);
            binding.etProfileName.setVisibility(View.VISIBLE);
            binding.etProfileName.setText(binding.tvProfileName.getText());
            binding.etProfileName.requestFocus();
            binding.btnEditName.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            binding.btnSaveName.setVisibility(View.VISIBLE);
        }
    }

    private void toggleEmailEdit() {
        boolean isEditing = binding.etProfileEmail.getVisibility() == View.VISIBLE;

        if (isEditing) {
            // Cancel edit - revert to view mode
            binding.tvEmail.setVisibility(View.VISIBLE);
            binding.etProfileEmail.setVisibility(View.GONE);
            binding.btnEditEmail.setImageResource(R.drawable.ic_edit);
            binding.btnSaveEmail.setVisibility(View.GONE);
        } else {
            // Switch to edit mode
            binding.tvEmail.setVisibility(View.GONE);
            binding.etProfileEmail.setVisibility(View.VISIBLE);
            binding.etProfileEmail.setText(binding.tvEmail.getText());
            binding.etProfileEmail.requestFocus();
            binding.btnEditEmail.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            binding.btnSaveEmail.setVisibility(View.VISIBLE);
        }
    }

    private void saveName() {
        String newName = binding.etProfileName.getText().toString().trim();
        String currentEmail = binding.tvEmail.getVisibility() == View.VISIBLE ? binding.tvEmail.getText().toString()
                : binding.etProfileEmail.getText().toString().trim();

        if (newName.isEmpty()) {
            binding.etProfileName.setError("Name is required");
            return;
        }

        // Disable buttons during save
        binding.btnEditName.setEnabled(false);
        binding.btnSaveName.setEnabled(false);

        SupabaseManager.updateUserProfile(newName, currentEmail, new SupabaseManager.ProfileUpdateCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Update UI
                        binding.tvProfileName.setText(newName);
                        binding.tvProfileName.setVisibility(View.VISIBLE);
                        binding.etProfileName.setVisibility(View.GONE);
                        binding.btnEditName.setImageResource(R.drawable.ic_edit);
                        binding.btnSaveName.setVisibility(View.GONE);

                        // Update header
                        binding.tvUserName.setText(newName);
                        String initial = !newName.isEmpty() ? String.valueOf(newName.charAt(0)).toUpperCase() : "U";
                        binding.tvAvatarInitial.setText(initial);

                        // Re-enable buttons
                        binding.btnEditName.setEnabled(true);
                        binding.btnSaveName.setEnabled(true);

                        Toast.makeText(getContext(), R.string.profile_updated, Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        binding.btnEditName.setEnabled(true);
                        binding.btnSaveName.setEnabled(true);
                        Toast.makeText(getContext(), R.string.profile_update_failed, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void saveEmail() {
        String newEmail = binding.etProfileEmail.getText().toString().trim();
        String currentName = binding.tvProfileName.getVisibility() == View.VISIBLE
                ? binding.tvProfileName.getText().toString()
                : binding.etProfileName.getText().toString().trim();

        if (newEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            binding.etProfileEmail.setError("Valid email is required");
            return;
        }

        // Disable buttons during save
        binding.btnEditEmail.setEnabled(false);
        binding.btnSaveEmail.setEnabled(false);

        SupabaseManager.updateUserProfile(currentName, newEmail, new SupabaseManager.ProfileUpdateCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Update UI
                        binding.tvEmail.setText(newEmail);
                        binding.tvEmail.setVisibility(View.VISIBLE);
                        binding.etProfileEmail.setVisibility(View.GONE);
                        binding.btnEditEmail.setImageResource(R.drawable.ic_edit);
                        binding.btnSaveEmail.setVisibility(View.GONE);

                        // Re-enable buttons
                        binding.btnEditEmail.setEnabled(true);
                        binding.btnSaveEmail.setEnabled(true);

                        Toast.makeText(getContext(), R.string.profile_updated, Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        binding.btnEditEmail.setEnabled(true);
                        binding.btnSaveEmail.setEnabled(true);
                        Toast.makeText(getContext(), R.string.profile_update_failed, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
