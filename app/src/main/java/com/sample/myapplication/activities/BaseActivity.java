package com.sample.myapplication.activities;

import android.os.Bundle;
import android.os.ConditionVariable;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sample.myapplication.utilities.Constants;
import com.sample.myapplication.utilities.PreferneceManager;

public class BaseActivity extends AppCompatActivity {
    private DocumentReference documentReference;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferneceManager preferneceManager= new PreferneceManager(getApplicationContext());
        FirebaseFirestore database= FirebaseFirestore.getInstance();
        documentReference= database.collection(Constants.KEY_COLLECTION_USER)
                .document(preferneceManager.getString(Constants.KEY_USER_ID));
    }

    @Override
    protected void onPause() {
        super.onPause();
        documentReference.update(Constants.KEY_AVAIBILITY,0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        documentReference.update(Constants.KEY_AVAIBILITY, 1);
    }
}
