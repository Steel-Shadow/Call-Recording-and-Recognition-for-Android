package org.tfri;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.tfri.data.ChatContent;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private final Context mContext;
    private final List<ChatContent> chatContent;

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final LinearLayout lLayoutReceive;
        private final LinearLayout lLayoutSend;
        private final TextView tvReceive;
        private final TextView tvSend;

        public ViewHolder(View itemView) {
            super(itemView);
            lLayoutReceive = itemView.findViewById(R.id.Layout_Item_Content_lLayoutReceive);
            lLayoutSend = itemView.findViewById(R.id.Layout_Item_Content_lLayoutSend);
            tvReceive = itemView.findViewById(R.id.Layout_Item_Content_tvContentReceive);
            tvSend = itemView.findViewById(R.id.Layout_Item_Content_tvContentSend);
        }
    }

    public ChatAdapter(Context mContext, List<ChatContent> chatContent) {
        this.mContext = mContext;
        this.chatContent = chatContent;
    }

    @NonNull
    @Override
    public ChatAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.layout_item_content, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatAdapter.ViewHolder holder, int position) {
        ChatContent chatContent = this.chatContent.get(position);
        if (chatContent.getType() == ChatContent.Type.RECEIVE) {
            holder.lLayoutReceive.setVisibility(View.VISIBLE);
            holder.lLayoutSend.setVisibility(View.GONE);
            holder.tvReceive.setText(chatContent.getContent());
        } else {
            holder.lLayoutReceive.setVisibility(View.GONE);
            holder.lLayoutSend.setVisibility(View.VISIBLE);
            holder.tvSend.setText(chatContent.getContent());
        }
    }

    @Override
    public int getItemCount() {
        return chatContent.size();
    }
}