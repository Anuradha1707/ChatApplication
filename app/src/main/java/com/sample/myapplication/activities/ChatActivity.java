package com.sample.myapplication.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import com.sample.myapplication.adapters.ChatAdapter;
import com.sample.myapplication.databinding.ActivityChatBinding;
import com.sample.myapplication.models.ChatMessaging;
import com.sample.myapplication.models.User;
import com.sample.myapplication.network.ApiClient;
import com.sample.myapplication.network.ApiService;
import com.sample.myapplication.utilities.Constants;
import com.sample.myapplication.utilities.PreferneceManager;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
// chat activity
public class ChatActivity extends BaseActivity {
    private ActivityChatBinding binding;
    private User receiveUser;
    private List<ChatMessaging> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferneceManager preferneceManager;
    private FirebaseFirestore database;
    private String conversionId=null;
    private Boolean isReceiverAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding= ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListners();
        loadingReceiverDetail();
        init();
        listenMessages();
    }


    private void init(){
        preferneceManager= new PreferneceManager(getApplicationContext());
        chatMessages= new ArrayList<>();
        chatAdapter= new ChatAdapter(
                chatMessages,
                getBitmapFromEncodedString(receiveUser.image),
                preferneceManager.getString(Constants.KEY_USER_ID)
        );
        binding.chatRecycleView.setAdapter(chatAdapter);
        database= FirebaseFirestore.getInstance();
    }

    private void sendMessage(){
        //set a message in hashmap
        HashMap<String, Object> message= new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferneceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiveUser.id);
        message.put(Constants.KEY_MESSAGE,binding.inputMessage.getText().toString());
        message.put(Constants.KEY_TIMESTAMP, new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
        if(conversionId!= null)
        {
            //if user are chatting 1st time
            updateConversion(binding.inputMessage.getText().toString());
        }else{

            HashMap<String, Object> conversion= new HashMap<>();
            conversion.put(Constants.KEY_SENDER_ID, preferneceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME, preferneceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE, preferneceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID, receiveUser.id);
            conversion.put(Constants.KEY_RECEIVE_NAME, receiveUser.name);
            conversion.put(Constants.KEY_RECEIVER_IMAGE, receiveUser.image);
            conversion.put(Constants.KEY_LAST_MESSAGE, binding.inputMessage.getText().toString());
            conversion.put(Constants.KEY_TIMESTAMP, new Date());
            addConversaion(conversion);
        }
        if(!isReceiverAvailable){
            try{
                JSONArray token= new JSONArray();
                token.put(receiveUser.token);

                JSONObject data = new JSONObject();
                data.put(Constants.KEY_USER_ID, preferneceManager.getString(Constants.KEY_USER_ID));
                data.put(Constants.KEY_NAME, preferneceManager.getString(Constants.KEY_NAME));
                data.put(Constants.KEY_FCM_TOKEN, preferneceManager.getString(Constants.KEY_FCM_TOKEN));
                data.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());

                JSONObject body= new JSONObject();
                body.put(Constants.REMOTE_MSG_DATA, data);
                body.put(Constants.REMOTE_MSG_REGISTRATION_IDs, token);

                sendNotification(body.toString());


            }catch (Exception e){
                showToast(e.getMessage());
            }
        }
        binding.inputMessage.setText(null);
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void sendNotification(String messageBody){
        ApiClient.getClient().create(ApiService.class).sendMessage(
                Constants.getRemoteMsgHeader(),
                messageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NotNull Call<String> call, @NotNull Response<String> response) {
             if(response.isSuccessful()){
                 try{
                     if(response.body() != null){
                         JSONObject responseJson= new JSONObject(response.body());
                         JSONArray results= responseJson.getJSONArray("results");
                         if(responseJson.getInt("failure") == 1){
                             JSONObject error= (JSONObject) results.get(0);
                             showToast(error.getString("error"));
                             return;
                         }
                     }

                 }catch (JSONException e){
                     e.printStackTrace();
                 }
                 showToast("Notification sent successfully");
             }else{
                 showToast("Error: "+ response.code());
             }
            }


            @Override
            public void onFailure(@NotNull Call<String> call, @NotNull Throwable t) {
                showToast(t.getMessage());

            }
        });
    }
    private void listenAvailabilityOfReceiver(){
        database.collection(Constants.KEY_COLLECTION_USER).document(
                receiveUser.id
        ).addSnapshotListener(ChatActivity.this, (value, error)->{
           if(error != null)
               return;
           if(value != null){
               if(value.getLong(Constants.KEY_AVAIBILITY) != null){
                   int availability= Objects.requireNonNull(
                           value.getLong(Constants.KEY_AVAIBILITY)
                   ).intValue();
                   isReceiverAvailable= availability ==1;
               }
               receiveUser.token = value.getString(Constants.KEY_FCM_TOKEN);
               if(receiveUser.image == null){
                   receiveUser.image= value.getString(Constants.KEY_IMAGE);
                   chatAdapter.setReceiverProfileImage(getBitmapFromEncodedString(receiveUser.image));
                   chatAdapter.notifyItemChanged(0, chatMessages.size());
               }
           }
           if(isReceiverAvailable){
               binding.textAvailability.setVisibility(View.VISIBLE);
           }else{
               binding.textAvailability.setVisibility(View.GONE);
           }
        });
    }


    private void listenMessages(){
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferneceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiveUser.id)
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiveUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferneceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener= (value, error) -> {
      if(error != null)
          return;
      if(value != null){
          int count= chatMessages.size();
          for(DocumentChange documentChange: value.getDocumentChanges() ){
           if(documentChange.getType() == DocumentChange.Type.ADDED){
               ChatMessaging chatMessaging = new ChatMessaging();
               chatMessaging.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
               chatMessaging.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
               chatMessaging.message= documentChange.getDocument().getString(Constants.KEY_MESSAGE);
               chatMessaging.dateTime= getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
               chatMessaging.dataeObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
               chatMessages.add(chatMessaging);
           }
          }

          Collections.sort(chatMessages, (obj1, obj2)-> obj1.dataeObject.compareTo(obj2.dataeObject));
          if(count == 0){
              chatAdapter.notifyDataSetChanged();
          }
          else{
              chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
              binding.chatRecycleView.smoothScrollToPosition(chatMessages.size()-1);
          }
          binding.chatRecycleView.setVisibility(View.VISIBLE);
      }
      binding.progressBar.setVisibility(View.GONE);
      if(conversionId == null){
          checkForConversion();
      }
    };
    private Bitmap getBitmapFromEncodedString(String encodedImage){
        if(encodedImage != null){
            byte[] bytes= Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        }else{
            return null;
        }

    }

    private void loadingReceiverDetail(){
        receiveUser= (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receiveUser.name);
    }

    private void setListners(){
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> sendMessage());
    }

    private String getReadableDateTime(Date date){
        return new SimpleDateFormat("MMMM dd, yyyy- hh:mm a", Locale.getDefault()).format(date);
    }

    private void addConversaion(HashMap<String, Object> conversion){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversionId= documentReference.getId());
    }

    private void updateConversion(String message){
        DocumentReference documentReference=
                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversionId);
        documentReference.update(
          Constants.KEY_LAST_MESSAGE, message, Constants.KEY_TIMESTAMP, new Date()
        );
    }
    private void checkForConversion(){
        if(chatMessages.size() != 0){
            checkForConversionRemotely(
                    preferneceManager.getString(Constants.KEY_USER_ID),
                    receiveUser.id
            );
            checkForConversionRemotely(
                    receiveUser.id,
                    preferneceManager.getString(Constants.KEY_USER_ID)
            );
        }

    }

    private void checkForConversionRemotely(String senderId, String receiverId){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID,senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(coversionOnCompleteListerner);
    }

    private final OnCompleteListener<QuerySnapshot> coversionOnCompleteListerner= task -> {
        if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size()>0){
            DocumentSnapshot documentSnapshot= task.getResult().getDocuments().get(0);
            conversionId= documentSnapshot.getId();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
    }
}