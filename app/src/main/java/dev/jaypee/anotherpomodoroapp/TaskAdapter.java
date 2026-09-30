package dev.jaypee.anotherpomodoroapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface OnTaskActionListener {
        void onTaskEdit(PomodoroTask task);
        void onTaskDelete(PomodoroTask task);
    }

    private List<PomodoroTask> tasks = new ArrayList<>();
    private OnTaskActionListener listener;

    public void setOnTaskActionListener(OnTaskActionListener listener) {
        this.listener = listener;
    }

    public void setTasks(List<PomodoroTask> tasks) {
        this.tasks = tasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        PomodoroTask task = tasks.get(position);
        holder.tvTitle.setText(task.getTitle());
        holder.tvDuration.setText(task.getWorkTime() + "m / " + task.getBreakTime() + "m");
        holder.tvDescription.setText("Duration: " + task.getWorkTime() + " mins focus, " + task.getBreakTime() + " mins break");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskEdit(task);
            }
        });

        holder.ivEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskEdit(task);
            }
        });

        holder.ivDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskDelete(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDuration, tvDescription;
        ImageView ivEdit, ivDelete;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvDuration = itemView.findViewById(R.id.tv_task_duration);
            tvDescription = itemView.findViewById(R.id.tv_task_description);
            ivEdit = itemView.findViewById(R.id.iv_edit_task);
            ivDelete = itemView.findViewById(R.id.iv_delete_task);
        }
    }
}
