package com.sample.myapplication.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sample.myapplication.R;
import com.sample.myapplication.databinding.ActivitySignInBinding;
import com.sample.myapplication.utilities.Constants;
import com.sample.myapplication.utilities.PreferneceManager;

import java.util.HashMap;


// Sign In activity
public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private PreferneceManager preferneceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferneceManager= new PreferneceManager(getApplicationContext());

        // if login then dont show signin
        if(preferneceManager.getBoolean(Constants.KEY_IS_SIGN_IN))
        {
            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
            startActivity(intent);
            finish();
        }
        binding=ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
    }

    private void setListeners(){
        binding.CreateNewAccount.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), SignUpActivity.class)));
        binding.signInButton.setOnClickListener(v ->{
            if(isValidSignInDetails()){
                signIn();
            }
        });

    }

    private void signIn(){
        loading(true);
        FirebaseFirestore database=FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USER)
                .whereEqualTo(Constants.KEY_EMAIL, binding.inputEmail.getText().toString())
                .whereEqualTo(Constants.KEY_PASSWORD, binding.inputPassword.getText().toString())
                .get()
                .addOnCompleteListener(task -> {
                   if (task.isSuccessful() && task.getResult()!=null && task.getResult().getDocuments().size()>0){
                       DocumentSnapshot documentSnapshot= task.getResult().getDocuments().get(0);
                       preferneceManager.putBoolean(Constants.KEY_IS_SIGN_IN, true);
                       preferneceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                       preferneceManager.putString(Constants.KEY_NAME, documentSnapshot.getString(Constants.KEY_NAME));
                       preferneceManager.putString(Constants.KEY_IMAGE, documentSnapshot.getString(Constants.KEY_IMAGE));
                       Intent intent= new Intent(getApplicationContext(), MainActivity.class);
                       intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                       startActivity(intent);
                   }
                   else{
                       loading(false);
                       showToast("Unable to SignIN");
                   }
                });

    }

    private void loading(Boolean isLoading){
        if(isLoading){
            binding.signInButton.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        }
        else{
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.signInButton.setVisibility(View.VISIBLE);
        }

    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private Boolean isValidSignInDetails(){
        if(binding.inputEmail.getText().toString().trim().isEmpty()){
            showToast("Enter Email");
            return false;
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()){
            showToast("Enter valid email");
            return false;
        }
        else if(binding.inputPassword.getText().toString().trim().isEmpty()){
            showToast("Enter valid password");
            return false;
        }
        else return true;
    }
}