package com.example.eduflow.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eduflow.databinding.ItemVideoBinding;
import com.example.eduflow.models.Video;

import java.util.List;

public class ExploreVideoAdapter extends RecyclerView.Adapter<ExploreVideoAdapter.VH> {

    private List<Video> videos;
    private final OnVideoClickListener onClick;

    public interface OnVideoClickListener {
        void onVideoClick(Video video);
    }

    public ExploreVideoAdapter(List<Video> videos, OnVideoClickListener onClick) {
        this.videos = videos;
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVideoBinding binding = ItemVideoBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new VH(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Video v = videos.get(position);

        holder.b.tvVideoTitle.setText(v.getTitle());
        holder.b.tvVideoMeta.setText(v.getCategory() + " • " + v.getAuthor());

        holder.b.btnWatch.setOnClickListener(view -> onClick.onVideoClick(v));
        holder.b.getRoot().setOnClickListener(view -> onClick.onVideoClick(v));
    }

    @Override
    public int getItemCount() {
        return videos == null ? 0 : videos.size();
    }

    public void updateList(List<Video> newList) {
        this.videos = newList;
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemVideoBinding b;

        VH(ItemVideoBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
