package com.example.eduflow.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eduflow.R;
import com.example.eduflow.auth.SupabaseManager.QuizResult;

import java.util.List;

public class QuizHistoryAdapter extends RecyclerView.Adapter<QuizHistoryAdapter.ViewHolder> {

    private final List<QuizResult> history;

    public QuizHistoryAdapter(List<QuizResult> history) {
        this.history = history;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quiz_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuizResult result = history.get(position);

        // Format quiz name: replace underscores with spaces
        String displayName = result.quizName.replace("_", " ");
        holder.tvQuizName.setText(displayName);
        holder.tvDate.setText(result.date);

        holder.tvScore.setText(result.scorePercent + "%");

        // Color coding for score
        int colorRes;
        if (result.scorePercent >= 90)
            colorRes = R.color.success_green;
        else if (result.scorePercent >= 70)
            colorRes = R.color.warning_yellow;
        else
            colorRes = R.color.error_red;

        holder.tvScore.setTextColor(holder.itemView.getContext().getResources().getColor(colorRes));
    }

    @Override
    public int getItemCount() {
        return history.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuizName, tvDate, tvScore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuizName = itemView.findViewById(R.id.tvQuizName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvScore = itemView.findViewById(R.id.tvScore);
        }
    }
}
