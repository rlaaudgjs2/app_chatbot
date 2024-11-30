package com.example.chatbot;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatData> chatList;

    // Constructor
    public ChatAdapter(List<ChatData> chatList) {
        this.chatList = chatList;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatData chatData = chatList.get(position);

        // Bind data to the view
        holder.chatName.setText(chatData.getChatName());
        holder.lastMessage.setText(chatData.getLastMessage());
        holder.messageTime.setText(chatData.getMessageTime());

        // Set default profile image (optional)
        holder.chatProfileImage.setImageResource(R.drawable.mypage);
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    // ViewHolder class
    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView chatProfileImage;
        TextView chatName, lastMessage, messageTime;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            chatProfileImage = itemView.findViewById(R.id.chat_profile_image);
            chatName = itemView.findViewById(R.id.chat_name);
            lastMessage = itemView.findViewById(R.id.last_message);
            messageTime = itemView.findViewById(R.id.message_time);
        }
    }
}
