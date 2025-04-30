package com.cbc.tor_android_v1.ui.chat;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cbc.tor_android_v1.R;

import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ChatViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final ArrayList<ChatMessage> chatMessages;

    public ChatViewAdapter(ArrayList<ChatMessage> chatMessages, Context context) {
        this.chatMessages = chatMessages;
    }

    @Override
    public int getItemViewType(int position) {
        return chatMessages.get(position).isUser ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_SENT) {
            View view = inflater.inflate(R.layout.list_item_chat_request_view, parent, false);
            return new SentMessageHolder(view);
        } else {
            View view = inflater.inflate(R.layout.list_item_chatbot_reply_view, parent, false);
            return new ReceivedMessageHolder(view);
        }
    }

    @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ChatMessage message = chatMessages.get(position);
            String time = getCurrentTime();

            if (holder instanceof SentMessageHolder) {
                ((SentMessageHolder) holder).textView.setText(message.message);
                ((SentMessageHolder) holder).timeView.setText(time);
            } else {
                ((ReceivedMessageHolder) holder).textView.setText(message.message);
                ((ReceivedMessageHolder) holder).timeView.setText(time);
            }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    public void addItem(String message, boolean isUser) {
        chatMessages.add(new ChatMessage(message, isUser));
        notifyItemInserted(chatMessages.size() - 1);
    }

    public void removeLastItemIfExists() {
        if (!chatMessages.isEmpty()) {
            chatMessages.remove(chatMessages.size() - 1);
            notifyItemRemoved(chatMessages.size());
        }
    }

    private String getCurrentTime() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));
        } else {
            return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        }
    }

    static class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView textView, timeView;


        SentMessageHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.chat_text_view);
            timeView = view.findViewById(R.id.chat_text_time_view);
        }
    }

    static class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView textView, timeView;

        ReceivedMessageHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.chat_reply_text_view);
            timeView = view.findViewById(R.id.chat_reply_time_view);
        }
    }
}


//    static class ChatRequestViewHolder extends RecyclerView.ViewHolder {
//        final TextView textView;
//        final TextView timeView;
//        final LinearLayout docsView;
//
//        ChatRequestViewHolder(View view) {
//            super(view);
//            textView = view.findViewById(R.id.chat_text_view);
//            docsView = view.findViewById(R.id.chat_text_view_docs);
//            timeView = view.findViewById(R.id.chat_text_time_view);
//        }
//    }
//
//
//    static class ChatResponseViewHolder extends RecyclerView.ViewHolder {
//        final TextView textView;
//        final TextView timeView;
//
//        ChatResponseViewHolder(View view) {
//            super(view);
//            textView = view.findViewById(R.id.chat_reply_text_view);
//            timeView = view.findViewById(R.id.chat_reply_time_view);
//        }
//    }

