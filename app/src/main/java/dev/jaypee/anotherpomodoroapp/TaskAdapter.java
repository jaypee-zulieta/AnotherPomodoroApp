package dev.jaypee.anotherpomodoroapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface OnTaskActionListener {
        void onTaskEdit(PomodoroTask task);
        void onTaskDelete(PomodoroTask task);
        void onTaskStartTimer(PomodoroTask task);
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
        holder.tvTitle.setText(toSentenceCase(task.getTitle()));
        holder.chipDuration.setText(task.getWorkTime() + " min / " + task.getBreakTime() + " min");
        holder.tvDescription.setText("Duration: " + task.getWorkTime() + " mins focus, " + task.getBreakTime() + " mins break");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskStartTimer(task);
            }
        });

        holder.btnStartTimer.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskStartTimer(task);
            }
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskEdit(task);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskDelete(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    private static String toSentenceCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase(Locale.getDefault()) + text.substring(1).toLowerCase(Locale.getDefault());
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription;
        Chip chipDuration;
        MaterialButton btnStartTimer, btnEdit, btnDelete;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            chipDuration = itemView.findViewById(R.id.chip_task_duration);
            tvDescription = itemView.findViewById(R.id.tv_task_description);
            btnStartTimer = itemView.findViewById(R.id.btn_start_timer);
            btnEdit = itemView.findViewById(R.id.btn_edit_task);
            btnDelete = itemView.findViewById(R.id.btn_delete_task);
        }
    }
}