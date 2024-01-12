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

import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class SignIn extends Fragment implements View.OnClickListener {
    private static final int RC_SIGN_IN = 123;
    private GoogleSignInClient mGoogleSignInClient;
    private View view;

    private EditText userIdEditText;
    private EditText userPasswordEditText;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_sign_in, container, false); // 'view' 변수를 클래스 전역 변수로 변경

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso);

        SignInButton signInButton = view.findViewById(R.id.buttonGoogle);
        signInButton.setSize(SignInButton.SIZE_STANDARD);

        Button signCat = view.findViewById(R.id.cats_login);

        userIdEditText = view.findViewById(R.id.input_id);
        userPasswordEditText = view.findViewById(R.id.input_password);

        signCat.setOnClickListener(this);
        signInButton.setOnClickListener(this);

        return view;
    }

    public void onClick(View view) {
        Log.d(TAG, "onClick: Button Clicked");
        String userName = userIdEditText.getText().toString();
        String userID = userPasswordEditText.getText().toString();
        if (view.getId() == R.id.buttonGoogle) {
            signIn();
        } else if (view.getId() == R.id.cats_login) {
            catsSign(userName, userID);
        }
    }

    private void catsSign(String userName, String userID) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 요청할 PHP 서버의 URL
                    String url = "http://10.0.2.2/cats_login.php";

                    // HTTP POST 요청을 위한 객체 생성
                    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);

                    // 데이터 전송
                    OutputStream outputStream = connection.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                    String postData = URLEncoder.encode("userName", "UTF-8") + "=" + URLEncoder.encode(userName, "UTF-8") + "&" +
                            URLEncoder.encode("userID", "UTF-8") + "=" + URLEncoder.encode(userID, "UTF-8");
                    writer.write(postData);
                    writer.flush();
                    writer.close();
                    outputStream.close();

                    // 응답 수신
                    InputStream inputStream = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    inputStream.close();

                    // 응답 결과 처리
                    String result = response.toString();

                    // UI 업데이트는 UI 스레드에서 수행
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // result를 가지고 UI 업데이트 또는 다른 동작 수행
                            if (result.equals("success")) {
                                // 로그인 성공
                                Log.d(TAG, "catsSign: 로그인 성공");
                                // 여기에 성공 시 수행할 동작 추가
                            } else {
                                // 로그인 실패
                                Log.d(TAG, "catsSign: 로그인 실패");
                                // 여기에 실패 시 수행할 동작 추가
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
        } else {
            // 사용자가 Google로 로그인되어 있지 않습니다.
            // 원하는 작업을 수행하십시오.
        }
    }
}
