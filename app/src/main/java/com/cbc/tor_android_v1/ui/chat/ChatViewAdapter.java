package com.cbc.tor_android_v1.ui.chat;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.cbc.tor_android_v1.R;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class ChatViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<String> localDataSet;
    private ChatViewActivity activity;
    private Context context;

    private static final int VIEW_TYPE_ONE = 0;
    private static final int VIEW_TYPE_TWO = 1;


    public ChatViewAdapter(ArrayList<String> dataSet,ChatViewActivity activity) {
        localDataSet = dataSet;
        this.activity = activity;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        if (viewType == VIEW_TYPE_ONE) {
            View view = inflater.inflate(R.layout.list_item_chat_request_view, viewGroup, false);
            return new ChatRequestViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.list_item_chatbot_reply_view, viewGroup, false);
            return new ChatResponseViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        String currentDataOBJ = localDataSet.get(position);
        if (holder.getItemViewType() == VIEW_TYPE_ONE) {
            ChatRequestViewHolder requestHolder = (ChatRequestViewHolder) holder;
            String[] splitData = currentDataOBJ.split("##DELIMITER##");
            requestHolder.textView.setText(splitData[0]);
            requestHolder.timeView.setText(getCurrentTime());
           // configureDocsView(requestHolder.docsView, splitData.length > 1 ? splitData[1] : "");
        } else {
            ChatResponseViewHolder responseHolder = (ChatResponseViewHolder) holder;
            responseHolder.textView.setText(currentDataOBJ);
            responseHolder.timeView.setText(getCurrentTime());
        }
    }

    public String getCurrentTime() {
        DateTimeFormatter formatter = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            formatter = DateTimeFormatter.ofPattern("hh.mma");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return LocalTime.now().format(formatter);
        }
        return "";
    }

    @Override
    public int getItemCount() {
        return localDataSet.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position % 2 == 0 ? VIEW_TYPE_ONE : VIEW_TYPE_TWO;
    }

    public void setContext(Context context) {
        this.context = context;
    }


    public void addItem(String item) {
        localDataSet.add(item);
        notifyItemInserted(localDataSet.size() - 1);
    }


    public void removeItemFromList() {
        localDataSet.remove(localDataSet.size() - 1);
        notifyDataSetChanged();
    }

    static class ChatRequestViewHolder extends RecyclerView.ViewHolder {
        final TextView textView;
        final TextView timeView;
        final LinearLayout docsView;

        ChatRequestViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.chat_text_view);
            docsView = view.findViewById(R.id.chat_text_view_docs);
            timeView = view.findViewById(R.id.chat_text_time_view);
        }
    }


    static class ChatResponseViewHolder extends RecyclerView.ViewHolder {
        final TextView textView;
        final TextView timeView;

        ChatResponseViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.chat_reply_text_view);
            timeView = view.findViewById(R.id.chat_reply_time_view);
        }
    }

}