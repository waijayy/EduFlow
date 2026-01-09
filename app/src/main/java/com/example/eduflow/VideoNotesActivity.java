package com.example.eduflow;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eduflow.adapters.VideoNoteAdapter;
import com.example.eduflow.auth.SupabaseManager;
import com.example.eduflow.models.VideoNote;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity to display and manage notes for a specific video.
 */
public class VideoNotesActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_ID = "extra_video_id";
    public static final String EXTRA_VIDEO_TITLE = "extra_video_title";

    private String videoId;
    private String videoTitle;
    private RecyclerView recyclerNotes;
    private TextView tvEmptyState;
    private VideoNoteAdapter noteAdapter;
    private List<VideoNote> notes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_notes);

        // Get video info from intent
        videoId = getIntent().getStringExtra(EXTRA_VIDEO_ID);
        videoTitle = getIntent().getStringExtra(EXTRA_VIDEO_TITLE);

        if (videoId == null) {
            Toast.makeText(this, "Error: Video not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupViews();
        loadNotes();
    }

    private void setupViews() {
        // Header
        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText("Notes: " + (videoTitle != null ? videoTitle : "Video"));

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // RecyclerView
        recyclerNotes = findViewById(R.id.recyclerNotes);
        recyclerNotes.setLayoutManager(new LinearLayoutManager(this));
        noteAdapter = new VideoNoteAdapter(notes);
        recyclerNotes.setAdapter(noteAdapter);

        // Empty state
        tvEmptyState = findViewById(R.id.tvEmptyState);

        // Add Note button
        MaterialButton btnAddNote = findViewById(R.id.btnAddNote);
        btnAddNote.setOnClickListener(v -> showAddNoteDialog());
    }

    private void loadNotes() {
        SupabaseManager.getVideoNotes(videoId, loadedNotes -> {
            runOnUiThread(() -> {
                notes.clear();
                notes.addAll(loadedNotes);
                noteAdapter.notifyDataSetChanged();
                updateEmptyState();
            });
        });
    }

    private void updateEmptyState() {
        if (notes.isEmpty()) {
            recyclerNotes.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerNotes.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void showAddNoteDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_note, null);

        EditText etTimestamp = dialogView.findViewById(R.id.etTimestamp);
        EditText etNoteContent = dialogView.findViewById(R.id.etNoteContent);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_EduFlow_Dialog)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String timestampStr = etTimestamp.getText().toString().trim();
            String noteContent = etNoteContent.getText().toString().trim();

            if (timestampStr.isEmpty()) {
                etTimestamp.setError("Enter a timestamp");
                return;
            }

            if (noteContent.isEmpty()) {
                etNoteContent.setError("Enter a note");
                return;
            }

            // Parse timestamp (format: M:SS or MM:SS)
            int timestampSeconds = parseTimestamp(timestampStr);
            if (timestampSeconds < 0) {
                etTimestamp.setError("Invalid format. Use M:SS");
                return;
            }

            // Save to Supabase
            SupabaseManager.saveVideoNote(videoId, timestampSeconds, noteContent, success -> {
                runOnUiThread(() -> {
                    if (success) {
                        dialog.dismiss();
                        loadNotes(); // Reload notes
                        Toast.makeText(this, "Note added!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to save note", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        dialog.show();

        // Set dialog width to 85% of screen width
        if (dialog.getWindow() != null) {
            android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int width = (int) (displayMetrics.widthPixels * 0.85);
            dialog.getWindow().setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    /**
     * Parse timestamp string (M:SS or MM:SS) to seconds
     * 
     * @param timestamp String in format "M:SS" or "MM:SS"
     * @return Total seconds, or -1 if invalid format
     */
    private int parseTimestamp(String timestamp) {
        try {
            String[] parts = timestamp.split(":");
            if (parts.length != 2) {
                return -1;
            }
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            if (seconds < 0 || seconds >= 60) {
                return -1;
            }
            return minutes * 60 + seconds;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
