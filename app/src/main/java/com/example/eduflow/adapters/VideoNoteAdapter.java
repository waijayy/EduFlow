package com.example.eduflow.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eduflow.R;
import com.example.eduflow.models.VideoNote;

import java.util.List;

/**
 * Adapter for displaying video notes in a RecyclerView.
 */
public class VideoNoteAdapter extends RecyclerView.Adapter<VideoNoteAdapter.NoteViewHolder> {

    private final List<VideoNote> notes;

    public VideoNoteAdapter(List<VideoNote> notes) {
        this.notes = notes;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        VideoNote note = notes.get(position);
        holder.tvTimestamp.setText(note.getFormattedTimestamp());
        holder.tvNoteContent.setText(note.getNoteContent());
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public void updateNotes(List<VideoNote> newNotes) {
        notes.clear();
        notes.addAll(newNotes);
        notifyDataSetChanged();
    }

    public void addNote(VideoNote note) {
        notes.add(note);
        notifyItemInserted(notes.size() - 1);
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTimestamp;
        final TextView tvNoteContent;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvNoteContent = itemView.findViewById(R.id.tvNoteContent);
        }
    }
}
