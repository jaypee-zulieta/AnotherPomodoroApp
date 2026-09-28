package dev.jaypee.anotherpomodoroapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<PomodoroTask> tasks = new ArrayList<>();

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
        // For description, we use title as well or if we had a separate field. 
        // The requirements say item_task has tv_task_description.
        // Let's use a default description for now or use the title if we don't have a separate field.
        // Actually PomodoroTask only has title.
        holder.tvDescription.setText("Duration: " + task.getWorkTime() + " mins focus");
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDuration, tvDescription;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvDuration = itemView.findViewById(R.id.tv_task_duration);
            tvDescription = itemView.findViewById(R.id.tv_task_description);
        }
    }
}
