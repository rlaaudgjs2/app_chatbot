package com.example.chatbot;


import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;

public class Sign extends Fragment implements View.OnClickListener {
    private static final int RC_SIGN_IN = 123;
    private GoogleSignInClient mGoogleSignInClient;
    private View view;


    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign, container, false);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN) // 사용자 데이터 요청
                .requestScopes(new Scope("https://www.googleapis.com/auth/pubsub"))
                .requestEmail()
                .build();

        // SignInButton을 찾아와서 버튼 크기를 설정합니다.
        SignInButton signInButton = view.findViewById(R.id.buttonGoogle);
        signInButton.setSize(SignInButton.SIZE_STANDARD);

        // SignInButton에 OnClickListener를 등록합니다.
        signInButton.setOnClickListener(this);
        return view;
    }
    public void onClick(View view) {
        switch (view.getId()) {
            case 1 : if (view.getId() == R.id.buttonGoogle) {
                signIn();
            }
                break;
        }
    }
        private void signIn() {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        }
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
            if (requestCode == RC_SIGN_IN) {
                // The Task returned from this call is always completed, no need to attach
                // a listener.
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                handleSignInResult(task);
            }
        }
        public void onStart() {
            super.onStart();

            // Check for existing Google Sign In account, if the user is already signed in
            // the GoogleSignInAccount will be non-null.
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
            updateUI(account);
        }



    private void handleSignInResult(Task<GoogleSignInAccount> task) {

        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            updateUI(account);
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            updateUI(null);
        }
    }

    private void updateUI(GoogleSignInAccount account) {
        if (account != null) {
            // 사용자가 이미 Google로 로그인되어 있습니다.
            // 원하는 작업을 수행하십시오.
            String displayName = account.getDisplayName();
            String email = account.getEmail();
            // 필요한 경우 추가 정보를 가져오거나 사용자에게 화면을 보여줍니다.

            // 예: 사용자 이름과 이메일을 TextView에 설정
            TextView userNameTextView = view.findViewById(R.id.input_id); // Fragment의 루트 뷰인 'view'를 사용하여 TextView를 찾음
            TextView emailTextView = view.findViewById(R.id.input_password); // Fragment의 루트 뷰인 'view'를 사용하여 TextView를 찾음

            userNameTextView.setText("이름: " + displayName);
            emailTextView.setText("이메일: " + email);
        } else {
            // 사용자가 Google로 로그인되어 있지 않습니다.
            // 원하는 작업을 수행하십시오.
        }
    }

}



