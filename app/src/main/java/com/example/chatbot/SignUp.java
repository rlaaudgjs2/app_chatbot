package com.example.chatbot;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import java.util.HashMap;
import java.util.Map;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SignUp#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SignUp extends Fragment {

    private Map<String, String> parameters;
    private EditText signUp_id;
    private EditText signUp_password;
    private EditText signUp_password_duple;
    private EditText signUp_name;
    private EditText signUp_nickname;
    private EditText signUp_phone_number;

    private int duple_check;

    private Button duple_check_name;
    private Button duple_check_id;
    private Button signUpButton;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public SignUp() {


        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SignUp.
     */
    // TODO: Rename and change types and number of parameters
    public static SignUp newInstance(String param1, String param2) {
        SignUp fragment = new SignUp();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_up, container, false);
        signUp_name = view.findViewById(R.id.signUp_name);
        signUp_nickname = view.findViewById(R.id.signUp_nickname);
        signUp_phone_number = view.findViewById(R.id.signUp_phone_number);
        signUp_id = view.findViewById(R.id.signUp_id);
        signUp_password = view.findViewById(R.id.signUp_password);
        signUp_password_duple = view.findViewById(R.id.signUp_password_duple);
        duple_check_id = view.findViewById(R.id.duple_check_id);
        duple_check_name = view.findViewById(R.id.duple_check_name);


        signUpButton = view.findViewById(R.id.signUp_button);
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newUsersignup();
            }
        });

        duple_check_id.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dupleCheckID();
            }
        });

        duple_check_name.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                dupleCheckName();
            }
        });

        return view; // View 객체 반환
    }

    private void dupleCheckName() {
    }

    private void dupleCheckID() {
        String userId = signUp_id.getText().toString();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .whereEqualTo("id", userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if(querySnapshot != null && !querySnapshot.isEmpty()) {
                                //중복 아이디 확인
                                Log.d("Duple check", "중복 아이디 존재")
                            }
                        }
                    }
                });
    }

    private void newUsersignup() {
            String name = signUp_name.getText().toString();
            String nickname = signUp_nickname.getText().toString();
            String phoneNumber = signUp_phone_number.getText().toString();
            String id = signUp_id.getText().toString();
            String password = signUp_password.getText().toString();
            String passwordDuple = signUp_password_duple.getText().toString();

            // Firestore 데이터베이스에 삽입할 데이터 가져오기
            Map<String, Object> userData = new HashMap<>();
            userData.put("name", name);
            userData.put("nickname", nickname);
            userData.put("phoneNumber", phoneNumber);
            userData.put("id", id);
            userData.put("password", password);
            userData.put("passwordDuple", passwordDuple);

            // Firestore 데이터베이스의 'users' 컬렉션에 새 문서 추가
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .add(userData)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d("Firestore", "DocumentSnapshot added with ID: " + documentReference.getId());
                            // 데이터 삽입이 성공했을 때 할 작업 추가
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("Firestore", "Error adding document", e);
                            // 데이터 삽입 중 오류가 발생했을 때 처리
                        }
                    });
        }
    }


