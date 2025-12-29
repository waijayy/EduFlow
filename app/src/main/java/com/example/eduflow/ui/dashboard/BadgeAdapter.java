package com.example.eduflow.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eduflow.R;

import java.util.List;

public class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.ViewHolder> {

    private final List<Badge> badges;

    public BadgeAdapter(List<Badge> badges) {
        this.badges = badges;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_badge, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Badge badge = badges.get(position);
        holder.tvIcon.setText(badge.icon);
        holder.tvTitle.setText(badge.title);

        if (badge.isUnlocked) {
            holder.tvStatus.setText("🔓 Unlocked");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.warning_yellow));
            holder.tvTitle.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.white));
            holder.tvIcon.setAlpha(1.0f);
            holder.tvIcon.getPaint().setColorFilter(null); // Reset filter
        } else {
            holder.tvStatus.setText("🔒 Locked");
            // Darker gray for locked status
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#555555"));
            // Grayed out title
            holder.tvTitle.setTextColor(android.graphics.Color.parseColor("#555555"));

            // Lower alpha and grayscale logic (simulation for text/emoji)
            holder.tvIcon.setAlpha(0.3f);
            // Note: ColorMatrixColorFilter works on ImageViews/Drawables, but for TextView
            // emoji,
            // alpha + text color is best. If these were ImageViews, we'd use setFilter.
            // Since they are TextViews (Emojis), setting text color to transparent gray can
            // help too,
            // but emojis usually retain color unless we use a specialized spanned.
            // Simple alpha 0.3 is usually enough to look "off".

            // If we wanted true grayscale for emoji TextView, we'd need a LayerType hack or
            // just low alpha.
            // Let's stick to low alpha 0.3f which is quite dim.
        }
    }

    @Override
    public int getItemCount() {
        return badges.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvTitle, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }

    public static class Badge {
        public String icon;
        public String title;
        public boolean isUnlocked;

        public Badge(String icon, String title, boolean isUnlocked) {
            this.icon = icon;
            this.title = title;
            this.isUnlocked = isUnlocked;
        }
    }
}
