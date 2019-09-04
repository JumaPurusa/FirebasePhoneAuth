package com.example.firebasephoneauth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chaos.view.PinView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.shuhart.stepview.StepView;

import java.util.concurrent.TimeUnit;

public class Authentication extends AppCompatActivity {

    private final static String TAG = Authentication.class.getSimpleName();

    private StepView stepView;
    private LinearLayout layout1, layout2, layout3;
    private EditText phoneNumberEditText;
    private Button nextButton, continueButton, continue2;
    private TextView phoneNumberText;
    private PinView pinView;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    //private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    private int currentSteps = 0;

    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private String mVerificationID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        mAuth = FirebaseAuth.getInstance();

         initViews();

         layout1.setVisibility(View.VISIBLE);

        stepView.setStepsNumber(3);
        stepView.go(0, true);

        nextButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        String phoneNumber = phoneNumberEditText.getText().toString().trim();
                        phoneNumberText.setText(phoneNumber);

                        if(TextUtils.isEmpty(phoneNumber)){
                            phoneNumberEditText.setError("Please Enter a phone number");
                            phoneNumberEditText.requestFocus();
                        }else if(phoneNumber.length() < 10){
                            phoneNumberEditText.setError("Please enter a valid phone number");
                        }else{

                                if(currentSteps < stepView.getStepCount() - 1){
                                    currentSteps++;
                                    stepView.go(currentSteps, true);
                                }else{
                                    stepView.done(true);
                                }

                                layout1.setVisibility(View.GONE);
                                layout2.setVisibility(View.VISIBLE);
                                progressBar.setVisibility(View.VISIBLE);

                                sendVerificationCode(phoneNumber);
                        }

                    }
                }
        );

        continueButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        String code = pinView.getText().toString();
                        if(code.isEmpty() || code.length() < 6){
                            pinView.setError("Please enter a valid verification code");
                            pinView.requestFocus();
                            return;
                        }

                        verifyCode(code);
                    }
                }
        );

        continue2.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        new Handler().postDelayed(
                                new Runnable() {
                                    @Override
                                    public void run() {

                                        AlertDialog.Builder builder =
                                                new AlertDialog.Builder(Authentication.this);
                                        View layout =
                                                LayoutInflater.from(Authentication.this)
                                                .inflate(R.layout.profile_create_dialog, null);
                                        builder.setView(layout);
                                        builder.create().show();

                                        Intent intent =
                                                new Intent(Authentication.this, ProfileActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                    }
                                }
                        ,2000);
                    }
                }
        );

    }

    private void verifyCode(String code){

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationID, code);
        signInWithPhoneAuthCredential(credential);
    }


    private void sendVerificationCode(String phoneNumber){

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber, // phone number to verify
                60,    // timeout duration
                TimeUnit.SECONDS, // units of timeout
                TaskExecutors.MAIN_THREAD, // activity (callback binding
                mCallbacks
        );
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks
            = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {

            Log.d(TAG, "onVerificationCompleted: " + phoneAuthCredential.getSmsCode());
            String smsCode = phoneAuthCredential.getSmsCode();
            Toast.makeText(Authentication.this, smsCode.toString(), Toast.LENGTH_LONG).show();

            if(smsCode != null){
                pinView.setText(smsCode);
                verifyCode(smsCode);
            }

        }

        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {

            Log.d(TAG, "onVerificationFailed: Error : " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(Authentication.this, "Verification Failed", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {

            mVerificationID = s;
            mResendToken = forceResendingToken;
        }
    };

    private void initViews(){
        stepView = findViewById(R.id.step_view);
        layout1 =  findViewById(R.id.layout1);
        layout2 = findViewById(R.id.layout2);
        layout3 = findViewById(R.id.layout3);
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        nextButton = findViewById(R.id.nextButton);
        phoneNumberText = findViewById(R.id.phoneNumberText);
        pinView = findViewById(R.id.pinView);
        continueButton = findViewById(R.id.continueButton);
        continue2 = findViewById(R.id.continue2);
        progressBar = findViewById(R.id.progressBar);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential){

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(
                        new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                Log.d(TAG, "onComplete: " + task.toString());
                                progressBar.setVisibility(View.GONE);
                                if(task.isSuccessful()){

                                    if(currentSteps < stepView.getStepCount() -1){
                                        currentSteps++;
                                        stepView.go(currentSteps, true);
                                    }else{
                                        stepView.done(true);
                                    }

                                    layout1.setVisibility(View.GONE);
                                    layout2.setVisibility(View.GONE);
                                    layout3.setVisibility(View.VISIBLE);

                                }else{

                                    Toast.makeText(Authentication.this, "Oops! Something went Wrong", Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "onComplete: Exception: " + task.getException().getMessage());

                                    if(task.getException() instanceof FirebaseAuthInvalidCredentialsException){
                                        Toast.makeText(Authentication.this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }
                );
    }

}
