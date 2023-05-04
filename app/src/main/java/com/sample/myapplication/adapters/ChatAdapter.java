package com.sample.myapplication.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sample.myapplication.databinding.ItemContainerReceiveBinding;
import com.sample.myapplication.databinding.ItemContainerSendMsgBinding;
import com.sample.myapplication.models.ChatMessaging;

import java.util.List;

// contain chat with user and bind it with item_container_send_msg layout

public class ChatAdapter extends  RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private final List<ChatMessaging> chatMessage;
    private Bitmap receiverProfileImage;
    private final String senderId;

    public static final int VIEW_TYPE_SEND = 1;
    public static final int VIEW_TYPE_RECEIVE =2;

    public void setReceiverProfileImage(Bitmap bitmap){
        receiverProfileImage= bitmap;
    }

    public ChatAdapter(List<ChatMessaging> chatMessage, Bitmap receiverProfileImage, String senderId) {
        this.chatMessage = chatMessage;
        this.receiverProfileImage = receiverProfileImage;
        this.senderId = senderId;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == VIEW_TYPE_SEND){
            return new SendMessageViewHolder(
                    ItemContainerSendMsgBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false)
            );
        }
        else{
            return new ReceivedMessageViewHolder(
                    ItemContainerReceiveBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
            );
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position)== VIEW_TYPE_SEND){
            ((SendMessageViewHolder)holder).setData(chatMessage.get(position));
        }else {
            ((ReceivedMessageViewHolder)holder).setData(chatMessage.get(position), receiverProfileImage);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessage.size();
    }

    @Override
    public int getItemViewType(int position) {
        if(chatMessage.get(position).senderId.equals(senderId))
        return VIEW_TYPE_SEND;
        else return VIEW_TYPE_RECEIVE;
    }

    static class SendMessageViewHolder extends RecyclerView.ViewHolder{

        private final ItemContainerSendMsgBinding binding;

        SendMessageViewHolder(ItemContainerSendMsgBinding itemContainerSendMsgBinding){
            super(itemContainerSendMsgBinding.getRoot());
            binding=itemContainerSendMsgBinding;
        }

        void setData(ChatMessaging chatMesaage){
            binding.textMessage.setText(chatMesaage.message);
            binding.textDateTime.setText(chatMesaage.dateTime);
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder{

        private final ItemContainerReceiveBinding binding;

        ReceivedMessageViewHolder(ItemContainerReceiveBinding itemContainerReceiveBinding){
            super(itemContainerReceiveBinding.getRoot());
            binding= itemContainerReceiveBinding;
        }

        void setData(ChatMessaging chatMessaging, Bitmap receiverProfileImage){
            binding.textMessage.setText(chatMessaging.message);
            binding.textDateTime.setText(chatMessaging.dateTime);
            if(receiverProfileImage != null){
                binding.imageProfile.setImageBitmap(receiverProfileImage);
            }
        }
    }
}
