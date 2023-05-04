package com.sample.myapplication.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sample.myapplication.databinding.ItemContainerRecentChatBinding;
import com.sample.myapplication.listeners.ConversationListener;
import com.sample.myapplication.models.ChatMessaging;
import com.sample.myapplication.models.User;

import java.util.List;

public class RecentConversationaAdapter extends RecyclerView.Adapter<RecentConversationaAdapter.ConversionViewHolder>{

    private final List<ChatMessaging> chatMessagingList;
    private final ConversationListener conversationListener;

    public RecentConversationaAdapter(List<ChatMessaging> chatMessagingList, ConversationListener conversationListener){
        this.chatMessagingList=chatMessagingList;
        this.conversationListener=conversationListener;
    }

    @NonNull
    @Override
    public ConversionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversionViewHolder(
                ItemContainerRecentChatBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ConversionViewHolder holder, int position) {
        holder.setData(chatMessagingList.get(position));

    }

    @Override
    public int getItemCount() {
        return chatMessagingList.size();
    }

    class ConversionViewHolder extends RecyclerView.ViewHolder{
        ItemContainerRecentChatBinding binding;
        ConversionViewHolder(ItemContainerRecentChatBinding itemContainerRecentChatBinding){
            super(itemContainerRecentChatBinding.getRoot());
            binding=itemContainerRecentChatBinding;
        }

        void setData(ChatMessaging chatMessaging){
            binding.imageprofile.setImageBitmap(getConversionImage((chatMessaging.conversationImage)));
            binding.textName.setText(chatMessaging.conversationName);
            binding.textRecentMessage.setText(chatMessaging.message);
            binding.getRoot().setOnClickListener(v->{
                User user= new User();
                user.id= chatMessaging.conversationId;
                user.name= chatMessaging.conversationName;
                user.image= chatMessaging.conversationImage;
                conversationListener.onConversionClicked(user);
            });
        }
    }

    private Bitmap getConversionImage(String encodedImage){
        byte[] bytes= Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
