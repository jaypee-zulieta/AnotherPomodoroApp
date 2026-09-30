package dev.jaypee.anotherpomodoroapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import io.noties.markwon.Markwon;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<ChatMessage> messages = new ArrayList<>();
    private final Markwon markwon;

    public ChatAdapter(Markwon markwon) {
        this.markwon = markwon;
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (message.isUser()) {
            holder.llUserMessage.setVisibility(View.VISIBLE);
            holder.llAiMessage.setVisibility(View.GONE);
            holder.tvUserMessageText.setText(message.getText());
        } else {
            holder.llAiMessage.setVisibility(View.VISIBLE);
            holder.llUserMessage.setVisibility(View.GONE);
            markwon.setMarkdown(holder.tvAiMessageText, message.getText());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        View llAiMessage, llUserMessage;
        TextView tvAiMessageText, tvUserMessageText;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            llAiMessage = itemView.findViewById(R.id.ll_ai_message);
            llUserMessage = itemView.findViewById(R.id.ll_user_message);
            tvAiMessageText = itemView.findViewById(R.id.tv_ai_message_text);
            tvUserMessageText = itemView.findViewById(R.id.tv_user_message_text);
        }
    }
}
