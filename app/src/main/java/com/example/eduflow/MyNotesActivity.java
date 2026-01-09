package com.example.eduflow;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eduflow.adapters.MyNotesAdapter;
import com.example.eduflow.auth.SupabaseManager;
import com.example.eduflow.models.VideoNoteSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity to display all user's notes grouped by video.
 */
public class MyNotesActivity extends AppCompatActivity {

    private RecyclerView recyclerNotes;
    private TextView tvEmptyState;
    private MyNotesAdapter notesAdapter;
    private List<VideoNoteSummary> notes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_notes);

        setupViews();
        loadNotes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes(); // Refresh notes when returning from VideoNotesActivity
    }

    private void setupViews() {
        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // RecyclerView
        recyclerNotes = findViewById(R.id.recyclerNotes);
        recyclerNotes.setLayoutManager(new LinearLayoutManager(this));
        notesAdapter = new MyNotesAdapter(notes, this::openVideoNotes);
        recyclerNotes.setAdapter(notesAdapter);

        // Empty state
        tvEmptyState = findViewById(R.id.tvEmptyState);

        // Search
        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                notesAdapter.filter(s.toString());
                updateEmptyState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadNotes() {
        android.util.Log.d("MyNotesActivity", "loadNotes called");
        SupabaseManager.getAllVideoNotes(loadedNotes -> {
            android.util.Log.d("MyNotesActivity", "Callback received with " + loadedNotes.size() + " notes");
            runOnUiThread(() -> {
                android.util.Log.d("MyNotesActivity", "Updating UI on main thread");
                notes.clear();
                notes.addAll(loadedNotes);
                android.util.Log.d("MyNotesActivity", "Notes list now has " + notes.size() + " items");
                notesAdapter.updateNotes(notes);
                android.util.Log.d("MyNotesActivity", "Adapter updated, item count: " + notesAdapter.getItemCount());
                updateEmptyState();
            });
        });
    }

    private void updateEmptyState() {
        android.util.Log.d("MyNotesActivity", "updateEmptyState - notes.isEmpty: " + notes.isEmpty()
                + ", adapter count: " + notesAdapter.getItemCount());
        if (notes.isEmpty()) {
            recyclerNotes.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerNotes.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
        android.util.Log.d("MyNotesActivity",
                "RecyclerView visibility: " + (recyclerNotes.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
    }

    private void openVideoNotes(VideoNoteSummary note) {
        Intent intent = new Intent(this, VideoNotesActivity.class);
        intent.putExtra(VideoNotesActivity.EXTRA_VIDEO_ID, note.getVideoId());
        intent.putExtra(VideoNotesActivity.EXTRA_VIDEO_TITLE, note.getVideoTitle());
        startActivity(intent);
    }
}
