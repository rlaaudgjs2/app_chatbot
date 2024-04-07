package com.example.chatbot;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

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

    private int duple_checkID;

    private int duple_checkNickname;

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
        signUp_phone_number.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting;
            private String phoneNumberFormat = "";
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                // 포맷 변경 중인 경우에는 무시
                if (isFormatting) return;

                // 전화번호를 포맷에 맞게 변경
                isFormatting = true;
                String formattedPhoneNumber = formatPhoneNumber(s.toString());

                if (!s.toString().equals(formattedPhoneNumber)) {
                    signUp_phone_number.setText(formattedPhoneNumber);
                    signUp_phone_number.setSelection(formattedPhoneNumber.length());
                }
                isFormatting = false;
            }

            private String formatPhoneNumber(String phoneNumber) {
                StringBuilder formatted = new StringBuilder();
                int len = phoneNumber.length();
                for (int i = 0; i < len; i++) {
                    char c = phoneNumber.charAt(i);
                    if (Character.isDigit(c) || c == '-') {
                        // 숫자인 경우에만 포맷에 추가
                        formatted.append(c);
                        if (formatted.length() == 3 || formatted.length() == 7) {
                            // 포맷에 '-' 추가
                            formatted.append("-");
                        }
                    }
                }
                return formatted.toString();
            }
        });


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

        duple_check_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dupleCheckName();
            }
        });

        return view; // View 객체 반환
    }

    private void dupleCheckName() {
        String userNickname = signUp_nickname.getText().toString();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .whereEqualTo("nickName", userNickname)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                //중복 아이디 존재
                                Toast.makeText(getContext(), "중복하는 닉네임이 있습니다.", Toast.LENGTH_SHORT).show();
                                duple_checkNickname = 0;
                            } else {
                                // 중복 아이디 없음
                                Toast.makeText(getContext(), "사용 가능합니다.", Toast.LENGTH_SHORT).show();
                                duple_checkNickname = 1;
                            }
                        } else {
                            Log.e("FirebaseError", "에러 데이터", task.getException());
                        }
                    }
                });
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
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                //중복 아이디 존재
                                Toast.makeText(getContext(), "중복하는 아이디가 있습니다.", Toast.LENGTH_SHORT).show();
                                duple_checkID = 0;
                            } else {
                                // 중복 아이디 없음
                                Toast.makeText(getContext(), "사용 가능합니다.", Toast.LENGTH_SHORT).show();
                                duple_checkID = 1;
                            }
                        } else {
                            Log.e("FirebaseError", "에러 데이터", task.getException());
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

        Pattern checkId = Pattern.compile("[ㄱ-ㅎㅏ-ㅣ가-힣~₩!@#$%^&*()+._=,/\"':;'><]");
        Pattern number = Pattern.compile("0-9");
        Pattern namePattern = Pattern.compile("^[가-힣]{2,4}$");
        Pattern specialPattern = Pattern.compile("[^a-zA-Z0-9_.-]");
        Pattern english = Pattern.compile("[a-z]");

        // 입력 확인
        if (name.isEmpty()) {
            Toast.makeText(getContext(), "이름이 입력되지 않았습니다.", Toast.LENGTH_SHORT).show();
            signUp_name.requestFocus(); // 이름 입력칸으로 포커스 이동
        } else if (nickname.isEmpty()) {
            Toast.makeText(getContext(), "닉네임이 입력되지 않았습니다.", Toast.LENGTH_SHORT).show();
            signUp_nickname.requestFocus(); // 닉네임 입력칸으로 포커스 이동
        } else if (phoneNumber.isEmpty()) {
            Toast.makeText(getContext(), "전화번호가 입력되지 않았습니다.", Toast.LENGTH_SHORT).show();
            signUp_phone_number.requestFocus(); // 전화번호 입력칸으로 포커스 이동
        } else if (id.isEmpty()) {
            Toast.makeText(getContext(), "아이디가 입력되지 않았습니다.", Toast.LENGTH_SHORT).show();
            signUp_id.requestFocus(); // 아이디 입력칸으로 포커스 이동
        } else if (password.isEmpty()) {
            Toast.makeText(getContext(), "비밀번호가 입력되지 않았습니다.", Toast.LENGTH_SHORT).show();
            signUp_password.requestFocus(); // 비밀번호 입력칸으로 포커스 이동
        } else if (passwordDuple.isEmpty()) {
            Toast.makeText(getContext(), "비밀번호 확인 바랍니다.", Toast.LENGTH_SHORT).show();
            signUp_password_duple.requestFocus(); // 비밀번호 확인 입력칸으로 포커스 이동
        } else if (!password.equals(passwordDuple)) {
            Toast.makeText(getContext(), "비밀번호가 같지 않습니다.", Toast.LENGTH_SHORT).show();
            signUp_password_duple.requestFocus(); // 비밀번호 확인 입력칸으로 포커스 이동
        } else if (duple_checkID != 1 || duple_checkNickname != 1) {
            Toast.makeText(getContext(), "아이디 중복확인바랍니다.", Toast.LENGTH_SHORT).show();
            signUp_id.requestFocus();// 중복 확인
        } else if (!namePattern.matcher(name).matches()) {
            Toast.makeText(getContext(), "이름이 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
            signUp_name.requestFocus(); // 이름 입력칸으로 포커스 이동
            signUp_name.getText().clear(); // 입력된 내용 삭제
        } else if (nickname.length() < 2 || nickname.length() > 10) {
            // 최소 또는 최대 길이에 맞지 않을 경우
            Toast.makeText(getContext(), "닉네임은 최소 2글자 이상 10글자 이하입니다.", Toast.LENGTH_SHORT).show();
            signUp_nickname.requestFocus(); // 닉네임 입력칸으로 포커스 이동
            signUp_nickname.getText().clear(); // 입력된 내용 삭제
        }else if (phoneNumber.length() < 13) {
            // 최소 또는 최대 길이에 맞지 않을 경우
            Toast.makeText(getContext(), "올바른 형식이 아닙니다.", Toast.LENGTH_SHORT).show();
            signUp_nickname.requestFocus(); // 닉네임 입력칸으로 포커스 이동
            signUp_nickname.getText().clear(); // 입력된 내용 삭제
        } else if (password.length() < 8 || password.length() > 20) {
            // 최소 또는 최대 길이에 맞지 않을 경우
            Toast.makeText(getContext(), "비밀번호는 8~20자리 이내로 입력 바랍니다.", Toast.LENGTH_SHORT).show();
            signUp_password.requestFocus();
            signUp_password.getText().clear();
        } else if (!number.matcher(password).find() || !english.matcher(password).find() || !specialPattern.matcher(password).find()) {
            // 최소 또는 최대 길이에 맞지 않을 경우
            Toast.makeText(getContext(), "비밀번호는 영문, 숫자, 특수문자를 혼합하여 8~20자리 이내로 입력하세요.", Toast.LENGTH_SHORT).show();
            signUp_password.requestFocus();
            signUp_password.getText().clear();
        } else if (nickname.contains(" ")) {
            // 닉네임에 공백이 포함되어 있는지 확인
            Toast.makeText(getContext(), "닉네임에 공백은 불가능합니다.", Toast.LENGTH_SHORT).show();
            signUp_nickname.requestFocus(); // 닉네임 입력칸으로 포커스 이동
            signUp_nickname.getText().clear(); // 입력된 내용 삭제
        } else if (specialPattern.matcher(nickname).find()) {
            // 닉네임에 특수문자가 포함되어 있는지 확인
            Toast.makeText(getContext(), ".과 _과 -를 제외한 특수문자는 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
            signUp_nickname.requestFocus(); // 닉네임 입력칸으로 포커스 이동
            signUp_nickname.getText().clear(); // 입력된 내용 삭제
        }else if (id.contains(" ")) {
            // 최소 또는 최대 길이에 맞지 않을 경우
            Toast.makeText(getContext(), "아이디에 공백은 불가능합니다.", Toast.LENGTH_SHORT).show();
            signUp_password.requestFocus();
            signUp_password.getText().clear();
        } else if (checkId.matcher(id).find()) {
            // 최소 또는 최대 길이에 맞지 않을 경우
            Toast.makeText(getContext(), "올바른 아이디 형식이 아닙니다.", Toast.LENGTH_SHORT).show();
            signUp_password.requestFocus();
            signUp_password.getText().clear();
        }else {
            // 모든 조건이 만족되었을 때 Firestore에 데이터 추가
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
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(getContext(), "Cats가입이 되셨습니다!", Toast.LENGTH_SHORT).show();
                        // 데이터 삽입이 성공했을 때 할 작업 추가
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error adding document", e);
                        // 데이터 삽입 중 오류가 발생했을 때 처리
                    });
        }
    }
}



