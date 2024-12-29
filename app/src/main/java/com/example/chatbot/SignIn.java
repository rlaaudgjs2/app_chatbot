package com.example.chatbot;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
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
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SignIn extends Fragment  {
    private static final int RC_SIGN_IN = 123;
    private GoogleSignInClient mGoogleSignInClient;
    private View view;
    private String id;
    private TextView signup_change;

    private EditText userIdEditText;
    private EditText userPasswordEditText;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_sign_in, container, false);

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
        spannableString.setSpan(clickableSpan, startIndex,endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
                            String uid = user.getUid();
                            Toast.makeText(getContext(), "환영합니다",Toast.LENGTH_SHORT).show();
                            checkGPTKeyInFirebase(uid);
//                            groupCheck();
                            UidSingleton singleton = UidSingleton.getInstance();
                            singleton.setUid(uid);
                        } else {
                            // 로그인 실패
                            Toast.makeText(getContext(), "로그인 실패: "+ "정보를 다시확인해주세요", Toast.LENGTH_SHORT).show();
                        }
                    });
        }


    }

    private void groupCheck() {

        String id = userIdEditText.getText().toString();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference usersRef = db.collection("users");
        Query query = usersRef.whereEqualTo("id",id);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        // 해당 사용자의 문서를 찾음
                        Map<String, Object> userData = document.getData();
                        if (userData != null && userData.containsKey("groups")) {
                            Intent intent = new Intent(requireContext(), MainChat.class);
                            startActivity(intent);
                            // 현재 Fragment 종료
                            requireActivity().finish();

                        } else {

                            // 사용자의 그룹 정보가 없는 경우
                            Toast.makeText(getContext(),  "그룹이 없어 그룹 생성 페이지로 이동합니다.", Toast.LENGTH_SHORT).show();


                            Bundle bundle = new Bundle();
                            bundle.putString("uid", document.getId());
                            bundle.putString("userId", id);

                            // Fragment 전환을 위해 아이디 정보를 포함한 Bundle을 인자로 넘김
                            Fragment groupCreate = new GroupCreate();
                            groupCreate.setArguments(bundle);

                            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                            transaction.replace(R.id.container, groupCreate);
                            transaction.addToBackStack(null);
                            transaction.commit();
                        }
                    }
                } else {
                    // 해당 사용자를 찾을 수 없는 경우
                    Log.d(TAG, "해당 사용자를 찾을 수 없습니다.");
                }
            } else {
                // 쿼리 실행 실패 시 처리
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
                            if (!hasGPTKey) {
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

    private static void showGPTKeyDialog(Context context, String uid) {
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
        TextView dialogTitle = dialogView.findViewById(R.id.GPT_title);
        EditText inputCode = dialogView.findViewById(R.id.input_code);
        Button buttonSubmit = dialogView.findViewById(R.id.gptbutton_input);
        Button buttonCancel = dialogView.findViewById(R.id.gptbutton_cancel);
        TextView hompage = dialogView.findViewById(R.id.homepage);

        // 버튼 클릭 리스너 설정
        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String gptCode = inputCode.getText().toString().trim();
                if(gptCode.isEmpty()) {
                    Toast.makeText(context, "코드가 입력되지 않았습니다", Toast.LENGTH_SHORT).show();
                    return;
                }
                validateAndSaveKey(dialog.getContext(),uid,gptCode);

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
    private static void validateAndSaveKey(Context context, String uid, String apiKey) {
        String lambdaUrl = "https://omkxvd5y1g.execute-api.us-east-2.amazonaws.com/manageKey";

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "사용자 인증 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        user.getIdToken(true).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String firebaseIdToken = task.getResult().getToken();
                JSONObject requestBody = new JSONObject();

                try {
                    requestBody.put("action", "set");
                    requestBody.put("uid", uid);
                    requestBody.put("apiKey", apiKey);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, lambdaUrl, requestBody,
                        response -> {
                            try {
                                String message = response.getString("message");
                                if ("API Key saved successfully".equals(message)) {
                                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                                    Map<String, Object> update = new HashMap<>();
                                    update.put("hasGPTKey", true);

                                    db.collection("users").document(uid).update(update)
                                            .addOnSuccessListener(aVoid -> Toast.makeText(context, "키가 저장되었습니다", Toast.LENGTH_SHORT).show())
                                            .addOnFailureListener(e -> Log.e("FirebaseError", "Firebase 업데이트 실패", e));
                                } else {
                                    Toast.makeText(context, "키 검증 실패: " + response.getString("error"), Toast.LENGTH_SHORT).show();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        },
                        error -> {
                            Log.e("LambdaError", "키 검증 및 저장 실패: " + error.getMessage());
                            if (error.networkResponse != null) {
                                Log.e("LambdaError", "Response Code: " + error.networkResponse.statusCode);
                                if (error.networkResponse.data != null) {
                                    String responseBody = new String(error.networkResponse.data);
                                    Log.e("LambdaError", "Response Body: " + responseBody);
                                }
                            }
                        }
                ) {
                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Content-Type", "application/json");
                        headers.put("Authorization", "Bearer " + firebaseIdToken);
                        return headers;
                    }
                };

                Volley.newRequestQueue(context).add(jsonObjectRequest);

            } else {
                Log.e("FirebaseError", "ID 토큰 가져오기 실패", task.getException());
                Toast.makeText(context, "ID 토큰을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }


}


