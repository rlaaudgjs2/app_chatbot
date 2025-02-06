package com.example.chatbot;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SignIn extends Fragment {

    private View view;
    private String id;
    private TextView signup_change;
    private GoogleSignInClient mGoogleSignInClient;

    private EditText userIdEditText;
    private static final int RC_SIGN_IN = 9001;
    private EditText userPasswordEditText;
    private FirebaseAuth mAuth;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_sign_in, container, false);
        mAuth = FirebaseAuth.getInstance();

        // App Check 디버그 토큰 설정
        FirebaseApp.initializeApp(requireContext());
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
        );
        // 🔹 Google 로그인 옵션 설정
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))  // Firebase에서 생성된 Web Client ID 필요
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso);

        SignInButton googleButton = view.findViewById(R.id.buttonGoogle);
        googleButton.setSize(SignInButton.SIZE_STANDARD);
        googleButton.setOnClickListener(v -> signInWithGoogle());
   Button signCat = view.findViewById(R.id.cats_login);

        userIdEditText = view.findViewById(R.id.input_id);
        userPasswordEditText = view.findViewById(R.id.input_password);
        signup_change = view.findViewById(R.id.signup_change);
        String fullText = "회원이 아니신가요? 회원가입";
        SpannableString spannableString = new SpannableString(fullText);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                ChangeSignUp();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false); // 밑줄 제거
                ds.setColor(ContextCompat.getColor(requireContext(), R.color.black)); // 색상 설정
            }
        };
        int startIndex = fullText.indexOf("회원가입");
        int endIndex = startIndex + "회원가입".length();
        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        signup_change.setText(spannableString);
        signup_change.setMovementMethod(LinkMovementMethod.getInstance());

        signCat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signCat();
            }
        });

        return view;
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            try {
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w(TAG, "Google sign-in failed", e);
                Toast.makeText(getContext(), "Google 로그인 실패", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user);
                        }
                    } else {
                        Toast.makeText(getContext(), "Firebase 인증 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToFirestore(FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("email", user.getEmail());
        userData.put("name", user.getDisplayName());

        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "환영합니다, " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
                    groupCheck();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "데이터 저장 오류", Toast.LENGTH_SHORT).show();
                });
    }



    private void ChangeSignUp() {
        Fragment signUp = new SignUp();
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, signUp);

        transaction.addToBackStack(null);

        transaction.commit();

    }


    private void signCat() {
        String userID = userIdEditText.getText().toString();
        String userPassword = userPasswordEditText.getText().toString();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (userID.isEmpty()) {
            Toast.makeText(getContext(), "아이디를 입력해주세요", +Toast.LENGTH_SHORT).show();
            userIdEditText.requestFocus();
        } else if (userPassword.isEmpty()) {
            Toast.makeText(getContext(), "비밀번호를 입력해주세요", +Toast.LENGTH_SHORT).show();
            userPasswordEditText.requestFocus();
        } else {
            mAuth.signInWithEmailAndPassword(userID + "@timproject.co.kr", userPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // 로그인 성공
                            FirebaseUser user = mAuth.getCurrentUser();
                            String uid = user.getUid();
                            Toast.makeText(getContext(), "환영합니다", Toast.LENGTH_SHORT).show();
                            checkGPTKeyInFirebase(uid);
//
                            UidSingleton singleton = UidSingleton.getInstance();
                            singleton.setUid(uid);
                        } else {
                            // 로그인 실패
                            Toast.makeText(getContext(), "로그인 실패: " + "정보를 다시확인해주세요", Toast.LENGTH_SHORT).show();
                        }
                    });
        }


    }

    private void groupCheck() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "로그인한 사용자가 없습니다.");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            if (document.contains("groups")) {
                                // 그룹 정보가 있으면 ChatFragment로 이동
                                FragmentTransaction transaction = requireActivity()
                                        .getSupportFragmentManager()
                                        .beginTransaction();
                                transaction.replace(R.id.container, new ChatFragment());
                                transaction.commit();
                            } else {
                                // 그룹 정보가 없으면 그룹 생성 Fragment로 이동
                                Toast.makeText(getContext(), "그룹이 없어 그룹 생성 페이지로 이동합니다.", Toast.LENGTH_SHORT).show();
                                Bundle bundle = new Bundle();
                                bundle.putString("uid", document.getId());
                                // 필요하다면 추가 정보도 번들에 담기
                                Fragment groupCreate = new GroupCreate();
                                groupCreate.setArguments(bundle);
                                FragmentTransaction transaction = requireActivity()
                                        .getSupportFragmentManager()
                                        .beginTransaction();
                                transaction.replace(R.id.container, groupCreate);
                                transaction.addToBackStack(null);
                                transaction.commit();
                            }
                        } else {
                            Log.d(TAG, "해당 사용자를 찾을 수 없습니다.");
                        }
                    } else {
                        Log.e(TAG, "사용자 그룹 정보 확인 중 오류 발생: " + task.getException());
                    }
                });
    }


    private void checkGPTKeyInFirebase(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists() && document.getBoolean("hasGPTKey") != null) {
                            boolean hasGPTKey = document.getBoolean("hasGPTKey");
                            if (hasGPTKey) {
                                // hasGPTKey가 true이면 바로 다음 과정으로 이동
                                groupCheck();
                            } else {
                                showGPTKeyDialog(requireContext(), uid);
                            }
                        } else {
                            showGPTKeyDialog(requireContext(), uid);
                        }
                    } else {
                        Toast.makeText(getContext(), "키 확인 중 오류 발생", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void showGPTKeyDialog(Context context, String uid) {
        // 다이얼로그 빌더 생성
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        // 커스텀 레이아웃 인플레이트
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.inputgptcode, null);
        builder.setView(dialogView);

        // 다이얼로그 생성
        AlertDialog dialog = builder.create();

        // 다이얼로그 배경을 투명하게 설정
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // 뷰 참조

        EditText inputCode = dialogView.findViewById(R.id.input_code);
        Button buttonSubmit = dialogView.findViewById(R.id.gptbutton_input);
        Button buttonCancel = dialogView.findViewById(R.id.gptbutton_cancel);
        TextView hompage = dialogView.findViewById(R.id.homepage);

        // 버튼 클릭 리스너 설정
        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String gptCode = inputCode.getText().toString().trim();
                if (gptCode.isEmpty()) {
                    Toast.makeText(context, "코드가 입력되지 않았습니다", Toast.LENGTH_SHORT).show();
                    return;
                }
                validateAndSaveKey(dialog.getContext(), uid, gptCode);

            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        hompage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://velog.io/@yule/OpenAI-API-%EB%B0%9C%EA%B8%89"; // 이동할 URL
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                context.startActivity(intent);
            }
        });


        // 다이얼로그 표시
        dialog.show();
    }

    private void validateAndSaveKey(Context context, String uid, String apiKey) {
        String lambdaUrl = "https://l3k3tdlonf.execute-api.us-east-2.amazonaws.com/default/save_api_key_lambda";

        ;

        JSONObject requestBody = new JSONObject();

        try {
            // 요청 바디에 UID와 API 키 포함
            requestBody.put("uid", uid);
            requestBody.put("apiKey", apiKey);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(context, "요청 생성 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lambda 요청
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                lambdaUrl,
                requestBody,
                response -> {
                    try {
                        // Lambda 응답 처리
                        int statusCode = response.optInt("statusCode", 200);
                        String message = response.getString("message");

                        if (statusCode == 200) {
                            // 성공 처리
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            db.collection("users").document(uid)
                                            .update("hasGPTKey",true)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(context, "API 키가 저장되었습니다.", Toast.LENGTH_SHORT).show();
                                        groupCheck();  // 다음 단계로 이동
                                    })
                                    .addOnFailureListener(e ->{
                                        Toast.makeText(context, "저장에 문제가 생겼습니다.", Toast.LENGTH_SHORT).show();
                                    });

                        } else if (statusCode == 400) {
                            // 유효하지 않은 API 키 처리
                            Toast.makeText(context, "올바른 키를 입력해주세요: " + message, Toast.LENGTH_SHORT).show();
                        } else {
                            // 기타 오류 처리
                            Toast.makeText(context, "알 수 없는 오류가 발생했습니다: " + message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(context, "응답 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    // 네트워크 오류 처리
                    Log.e("LambdaError", "Lambda 호출 실패: " + error.getMessage());
                    if (error.networkResponse != null) {
                        String responseBody = new String(error.networkResponse.data);
                        Log.e("LambdaError", "Response Body: " + responseBody);
                    }
                    Toast.makeText(context, "Lambda 호출 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        // 요청 추가
        Volley.newRequestQueue(context).add(jsonObjectRequest);
    }
}