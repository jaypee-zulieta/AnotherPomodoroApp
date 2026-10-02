package dev.jaypee.anotherpomodoroapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface OnTaskActionListener {
        void onTaskEdit(PomodoroTask task);
        void onTaskDelete(PomodoroTask task);
        void onTaskStartTimer(PomodoroTask task);
    }

    public interface OnTaskSelectionListener {
        void onSelectionModeChanged(boolean active);
        void onSelectionCountChanged(int count);
    }

    private List<PomodoroTask> tasks = new ArrayList<>();
    private OnTaskActionListener listener;
    private OnTaskSelectionListener selectionListener;
    private boolean selectionMode = false;
    private Set<String> selectedTaskIds = new HashSet<>();

    public void setOnTaskActionListener(OnTaskActionListener listener) {
        this.listener = listener;
    }

    public void setOnTaskSelectionListener(OnTaskSelectionListener listener) {
        this.selectionListener = listener;
    }

    public void setTasks(List<PomodoroTask> tasks) {
        this.tasks = tasks;
        Set<String> currentIds = new HashSet<>();
        for (PomodoroTask task : tasks) {
            currentIds.add(task.getId());
        }
        selectedTaskIds.retainAll(currentIds);
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public int getSelectedCount() {
        return selectedTaskIds.size();
    }

    public List<PomodoroTask> getSelectedTasks() {
        List<PomodoroTask> selected = new ArrayList<>();
        for (PomodoroTask task : tasks) {
            if (selectedTaskIds.contains(task.getId())) {
                selected.add(task);
            }
        }
        return selected;
    }

    public void toggleSelection(String taskId) {
        if (selectedTaskIds.contains(taskId)) {
            selectedTaskIds.remove(taskId);
        } else {
            selectedTaskIds.add(taskId);
        }
        if (selectedTaskIds.isEmpty()) {
            selectionMode = false;
            if (selectionListener != null) {
                selectionListener.onSelectionModeChanged(false);
            }
        }
        if (selectionListener != null) {
            selectionListener.onSelectionCountChanged(selectedTaskIds.size());
        }
        notifyDataSetChanged();
    }

    public void selectAll() {
        for (PomodoroTask task : tasks) {
            selectedTaskIds.add(task.getId());
        }
        if (selectionListener != null) {
            selectionListener.onSelectionCountChanged(selectedTaskIds.size());
        }
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedTaskIds.clear();
        selectionMode = false;
        if (selectionListener != null) {
            selectionListener.onSelectionModeChanged(false);
            selectionListener.onSelectionCountChanged(0);
        }
        notifyDataSetChanged();
    }

    public void enterSelectionMode(String taskId) {
        selectionMode = true;
        selectedTaskIds.add(taskId);
        if (selectionListener != null) {
            selectionListener.onSelectionModeChanged(true);
            selectionListener.onSelectionCountChanged(selectedTaskIds.size());
        }
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

        boolean isSelected = selectedTaskIds.contains(task.getId());
        holder.cbSelect.setChecked(isSelected);
        holder.cbSelect.setVisibility(selectionMode ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (selectionMode) {
                toggleSelection(task.getId());
            } else if (listener != null) {
                listener.onTaskStartTimer(task);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!selectionMode) {
                enterSelectionMode(task.getId());
            } else {
                toggleSelection(task.getId());
            }
            return true;
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
        MaterialCheckBox cbSelect;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            chipDuration = itemView.findViewById(R.id.chip_task_duration);
            tvDescription = itemView.findViewById(R.id.tv_task_description);
            cbSelect = itemView.findViewById(R.id.cb_task_select);
        }
    }
}
