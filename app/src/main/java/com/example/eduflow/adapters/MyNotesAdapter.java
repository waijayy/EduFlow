package com.example.eduflow.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eduflow.R;
import com.example.eduflow.models.VideoNoteSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying video note summaries in My Notes page.
 */
public class MyNotesAdapter extends RecyclerView.Adapter<MyNotesAdapter.NoteCardViewHolder> {

    private List<VideoNoteSummary> allNotes;
    private List<VideoNoteSummary> filteredNotes;
    private final OnNoteClickListener onNoteClick;

    public interface OnNoteClickListener {
        void onNoteClick(VideoNoteSummary note);
    }

    public MyNotesAdapter(List<VideoNoteSummary> notes, OnNoteClickListener onNoteClick) {
        this.allNotes = new ArrayList<>(notes); // Create a copy, not a reference
        this.filteredNotes = new ArrayList<>(notes);
        this.onNoteClick = onNoteClick;
    }

    @NonNull
    @Override
    public NoteCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_note_card, parent, false);
        return new NoteCardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteCardViewHolder holder, int position) {
        VideoNoteSummary note = filteredNotes.get(position);
        holder.tvVideoTitle.setText(note.getVideoTitle());
        holder.tvNotePreview.setText(note.getNotePreview());
        holder.tvDate.setText(note.getFormattedDate());

        holder.itemView.setOnClickListener(v -> {
            if (onNoteClick != null) {
                onNoteClick.onNoteClick(note);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredNotes.size();
    }

    public void updateNotes(List<VideoNoteSummary> newNotes) {
        allNotes.clear();
        allNotes.addAll(newNotes);
        filteredNotes.clear();
        filteredNotes.addAll(newNotes);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        filteredNotes.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredNotes.addAll(allNotes);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (VideoNoteSummary note : allNotes) {
                if (note.getVideoTitle().toLowerCase().contains(lowerQuery) ||
                        note.getNotePreview().toLowerCase().contains(lowerQuery)) {
                    filteredNotes.add(note);
                }
            }
        }
        notifyDataSetChanged();
    }

    static class NoteCardViewHolder extends RecyclerView.ViewHolder {
        final TextView tvVideoTitle;
        final TextView tvNotePreview;
        final TextView tvDate;

        NoteCardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVideoTitle = itemView.findViewById(R.id.tvVideoTitle);
            tvNotePreview = itemView.findViewById(R.id.tvNotePreview);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}
