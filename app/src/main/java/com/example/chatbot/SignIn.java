package com.example.chatbot;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class SignIn extends Fragment  {
    private static final int RC_SIGN_IN = 123;
    private GoogleSignInClient mGoogleSignInClient;
    private View view;
    private TextView signup_change;

    private EditText userIdEditText;
    private EditText userPasswordEditText;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_sign_in, container, false); // 'view' 변수를 클래스 전역 변수로 변경

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso);

        SignInButton googleButton = view.findViewById(R.id.buttonGoogle);
        googleButton.setSize(SignInButton.SIZE_STANDARD);

        Button signCat = view.findViewById(R.id.cats_login);

        userIdEditText = view.findViewById(R.id.input_id);
        userPasswordEditText = view.findViewById(R.id.input_password);
        signup_change = view.findViewById(R.id.signup_change);

        signup_change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChangeSignUp();
            }
        });

        signCat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signCat();
            }
        });

        return view;
    }

    private void ChangeSignUp() {
        Fragment signUp = new SignUp();
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, signUp);

        transaction.addToBackStack(null);

        transaction.commit();

    }


    private void signCat(){
        String userID = userIdEditText.getText().toString();
        String userPassword = userPasswordEditText.getText().toString();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if(userID.isEmpty()){
            Toast.makeText(getContext(),"아이디를 입력해주세요", + Toast.LENGTH_SHORT).show();
            userIdEditText.requestFocus();
        } else if (userPassword.isEmpty()) {
            Toast.makeText(getContext(),"비밀번호를 입력해주세요", + Toast.LENGTH_SHORT).show();
            userPasswordEditText.requestFocus();
        }else {
            mAuth.signInWithEmailAndPassword(userID + "@timproject.co.kr",userPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // 로그인 성공
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(getContext(), "로그인 성공: " + user.getEmail(), Toast.LENGTH_SHORT).show();

                        } else {
                            // 로그인 실패
                            Toast.makeText(getContext(), "로그인 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }


    }


}
