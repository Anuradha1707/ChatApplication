package com.sample.myapplication.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sample.myapplication.R;
import com.sample.myapplication.databinding.ActivityMainBinding;
import com.sample.myapplication.utilities.Constants;
import com.sample.myapplication.utilities.PreferneceManager;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private PreferneceManager preferneceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferneceManager= new PreferneceManager(getApplicationContext());
       LoadUserDetails();

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

    private void updateToken(String token){
        FirebaseFirestore database= FirebaseFirestore.getInstance();
        DocumentReference documentReference=
                database.collection(Constants.KEY_COLLECTION_USER).document(
                        preferneceManager.getString(Constants.KEY_USER_ID)
                );
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnSuccessListener(unused-> showToast("Token is updated"))
                .addOnFailureListener(e-> showToast("Unable to update token"));
    }
}