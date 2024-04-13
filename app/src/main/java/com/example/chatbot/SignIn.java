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

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

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
                            groupCheck();
                        } else {
                            // 로그인 실패
                            Toast.makeText(getContext(), "로그인 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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
                            Intent intent = new Intent(requireContext(), MainActivity.class);
                            startActivity(intent);
                            // 현재 Fragment 종료
                            requireActivity().finish();

                        } else {

                            // 사용자의 그룹 정보가 없는 경우
                            Toast.makeText(getContext(), "로그인 실패: " + "그룹이 없어 그룹 생성 페이지로 이동합니다.", Toast.LENGTH_SHORT).show();


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



}


