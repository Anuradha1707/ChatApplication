package com.sample.myapplication.activities;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.sample.myapplication.adapters.RecentConversationaAdapter;
import com.sample.myapplication.databinding.ActivityMainBinding;
import com.sample.myapplication.listeners.ConversationListener;
import com.sample.myapplication.models.ChatMessaging;
import com.sample.myapplication.models.User;
import com.sample.myapplication.utilities.Constants;
import com.sample.myapplication.utilities.PreferneceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


// Home page Activity
public class MainActivity extends BaseActivity implements ConversationListener {
    private ActivityMainBinding binding;
    private PreferneceManager preferneceManager;
    private List<ChatMessaging> conversations;
    private RecentConversationaAdapter conersationAdapter;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferneceManager= new PreferneceManager(getApplicationContext());
        init();
        LoadUserDetails();
        getToken();
        setListeners();
        listenerConversations();
    }

    private void init(){
        conversations= new ArrayList<>();
        conersationAdapter= new RecentConversationaAdapter(conversations, this);
        binding.conversationRecycleView.setAdapter(conersationAdapter);
        database= FirebaseFirestore.getInstance();
    }

    private void setListeners(){

        binding.imagesignout.setOnClickListener(v -> signOut());
        binding.facNewChat.setOnClickListener(v-> startActivity(new Intent (getApplicationContext(),UserActivity.class)));
    }
    private void LoadUserDetails(){
        binding.textname.setText(preferneceManager.getString(Constants.KEY_NAME));
        byte[] bytes= Base64.decode(preferneceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap= BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        binding.imageprofile.setImageBitmap(bitmap);
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void listenerConversations(){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferneceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferneceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private  final EventListener<QuerySnapshot> eventListener = (value, error) -> {
      if(error != null)
          return;
      if(value != null){
          for(DocumentChange documentChange : value.getDocumentChanges()){
              if(documentChange.getType() == DocumentChange.Type.ADDED){
                  String senderId= documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                  String receiverId= documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                  ChatMessaging chatMessaging = new ChatMessaging();
                  chatMessaging.senderId=senderId;
                  chatMessaging.receiverId=receiverId;
                  if(preferneceManager.getString(Constants.KEY_USER_ID).equals(senderId)){
                      chatMessaging.conversationImage= documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE);
                      chatMessaging.conversationName= documentChange.getDocument().getString(Constants.KEY_RECEIVE_NAME);
                      chatMessaging.conversationId= documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                  }
                  else{
                      chatMessaging.conversationImage= documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
                      chatMessaging.conversationName= documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                      chatMessaging.conversationId= documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                  }
                  chatMessaging.message= documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                  chatMessaging.dataeObject= documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                  conversations.add(chatMessaging);
              }
              else if(documentChange.getType() == DocumentChange.Type.MODIFIED){
                  for(int i=0; i<conversations.size(); i++){
                      String senderId= documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                      String receiverId= documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                      if(conversations.get(i).senderId.equals(senderId) && conversations.get(i).receiverId.equals(receiverId)){
                          conversations.get(i).message= documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                          conversations.get(i).dataeObject= documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                          break;

                      }
                  }
              }
          }
          Collections.sort(conversations, (obj1, obj2)-> obj2.dataeObject.compareTo((obj1.dataeObject)));
          conersationAdapter.notifyDataSetChanged();
          binding.conversationRecycleView.smoothScrollToPosition(0);
          binding.conversationRecycleView.setVisibility(View.VISIBLE);
          binding.progressBar.setVisibility(View.GONE);
      }
    };
    private void getToken(){
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }
    private void updateToken(String token){
        preferneceManager.putString(Constants.KEY_FCM_TOKEN, token);
        FirebaseFirestore database= FirebaseFirestore.getInstance();
        DocumentReference documentReference=
                database.collection(Constants.KEY_COLLECTION_USER).document(
                        preferneceManager.getString(Constants.KEY_USER_ID)
                );
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnFailureListener(e-> showToast("Unable to update token"));
    }
    private void signOut(){
        showToast("Sign Out....");
        FirebaseFirestore database= FirebaseFirestore.getInstance();
        DocumentReference documentReference=
                database.collection(Constants.KEY_COLLECTION_USER).document(
                        preferneceManager.getString(Constants.KEY_USER_ID)
                );
        HashMap<String, Object> updates= new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused-> {
                    preferneceManager.clear();
                    startActivity(new Intent(getApplicationContext(),SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Unable to logout"));
    }

    @Override
    public void onConversionClicked(User user) {
        Intent intent= new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
    }
}