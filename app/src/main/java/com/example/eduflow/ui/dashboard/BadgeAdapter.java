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

    private final List<BadgeGroup> badgeGroups;

    public BadgeAdapter(List<BadgeGroup> badgeGroups) {
        this.badgeGroups = badgeGroups;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_badge_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BadgeGroup group = badgeGroups.get(position);
        holder.tvGroupTitle.setText(group.groupTitle);

        // Bind first badge
        if (group.badges.size() > 0) {
            Badge badge1 = group.badges.get(0);
            holder.tvIcon1.setText(badge1.icon);
            holder.tvTitle1.setText(badge1.title);
            applyBadgeStyle(holder, badge1, 1);
        }

        // Bind second badge
        if (group.badges.size() > 1) {
            Badge badge2 = group.badges.get(1);
            holder.tvIcon2.setText(badge2.icon);
            holder.tvTitle2.setText(badge2.title);
            applyBadgeStyle(holder, badge2, 2);
        }
    }

    private void applyBadgeStyle(ViewHolder holder, Badge badge, int badgeNumber) {
        TextView tvStatus = (badgeNumber == 1) ? holder.tvStatus1 : holder.tvStatus2;
        TextView tvTitle = (badgeNumber == 1) ? holder.tvTitle1 : holder.tvTitle2;
        TextView tvIcon = (badgeNumber == 1) ? holder.tvIcon1 : holder.tvIcon2;

        if (badge.isUnlocked) {
            tvStatus.setText("🔓 Unlocked");
            tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.warning_yellow));
            tvTitle.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.white));
            tvIcon.setAlpha(1.0f);
        } else {
            tvStatus.setText("🔒 Locked");
            tvStatus.setTextColor(android.graphics.Color.parseColor("#555555"));
            tvTitle.setTextColor(android.graphics.Color.parseColor("#555555"));
            tvIcon.setAlpha(0.3f);
        }
    }

    @Override
    public int getItemCount() {
        return badgeGroups.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvGroupTitle;
        TextView tvIcon1, tvTitle1, tvStatus1;
        TextView tvIcon2, tvTitle2, tvStatus2;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupTitle = itemView.findViewById(R.id.tvGroupTitle);
            tvIcon1 = itemView.findViewById(R.id.tvIcon1);
            tvTitle1 = itemView.findViewById(R.id.tvTitle1);
            tvStatus1 = itemView.findViewById(R.id.tvStatus1);
            tvIcon2 = itemView.findViewById(R.id.tvIcon2);
            tvTitle2 = itemView.findViewById(R.id.tvTitle2);
            tvStatus2 = itemView.findViewById(R.id.tvStatus2);
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

    public static class BadgeGroup {
        public String groupTitle;
        public List<Badge> badges;

        public BadgeGroup(String groupTitle, List<Badge> badges) {
            this.groupTitle = groupTitle;
            this.badges = badges;
        }
    }
}
