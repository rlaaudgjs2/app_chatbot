package com.example.chatbot;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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


    private Pattern checkId = Pattern.compile("[ㄱ-ㅎㅏ-ㅣ가-힣~₩!@#$%^&*()+._=,/\"':;'><]");

    private Pattern specialPattern = Pattern.compile("[!@#$%^&*()_+\\-=\\\\|{}\\[\\]:\";'<>?,./]");
    private Pattern number = Pattern.compile("[0-9]");
    private Pattern english = Pattern.compile("[a-z]");

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
            private StringBuilder formmatPhonenum;
            int cur_start;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                s = "010";

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                cur_start = start;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) {
                    return;
                }

                // 포맷 변경 중인 경우 true로 설정하여 재귀 호출 방지
                isFormatting = true;

                // 입력된 전화번호를 포맷에 맞게 변경
                String inputPhoneNumber = s.toString().replaceAll("[^\\d]", ""); // 숫자만 남기고 제거
                formmatPhonenum = new StringBuilder();

                int length = inputPhoneNumber.length();
                for (int i = 0; i < length; i++) {
                    formmatPhonenum.append(inputPhoneNumber.charAt(i));
                    if (cur_start == 13) {
                        if (formmatPhonenum.length() == 3 || formmatPhonenum.length() == 8) {
                            // 전화번호 포맷에 맞게 "-" 추가
                            formmatPhonenum.append("-");
                        }
                    }
                }

                // 포맷된 전화번호를 EditText에 설정
                signUp_phone_number.setText(formmatPhonenum.toString());
                signUp_phone_number.setSelection(formmatPhonenum.length()); // 커서 위치 조정

                // 포맷 변경 완료 후 isFormatting을 false로 설정하여 다시 입력 가능하도록 함
                isFormatting = false;
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

    // 이름 유효성 검사
    private boolean validateName(String name) {
        if (name.isEmpty()) {
            showToast("이름이 입력되지 않았습니다.");
            signUp_name.requestFocus();
            return false;
        }
        // 추가적인 유효성 검사 로직을 여기에 추가할 수 있습니다.
        return true;
    }

    // 닉네임 유효성 검사
    private boolean validateNickname(String nickname) {
        if (nickname.isEmpty()) {
            showToast("닉네임이 입력되지 않았습니다.");
            signUp_nickname.requestFocus();
            return false;
        }
        if (nickname.length() < 2 || nickname.length() > 10) {
            showToast("닉네임은 최소 2글자 이상 10글자 이하입니다.");
            signUp_nickname.requestFocus();
            return false;
        }
        if (nickname.contains(" ")) {
            showToast("닉네임에 공백은 불가능합니다.");
            signUp_nickname.requestFocus();
            return false;
        }
        if (specialPattern.matcher(nickname).find()) {
            showToast(".과 _과 -를 제외한 특수문자는 사용할 수 없습니다.");
            signUp_nickname.requestFocus();
            return false;
        }
        if (nickname.matches("\\d+")) {
            showToast("닉네임은 숫자로만 구성될 수 없습니다.");
            signUp_nickname.requestFocus();
            return false;
        }
        // 추가적인 유효성 검사 로직을 여기에 추가할 수 있습니다.
        return true;
    }

    // 전화번호 유효성 검사
    private boolean validatePhoneNumber(String phoneNumber) {
        if (phoneNumber.isEmpty()) {
            showToast("전화번호가 입력되지 않았습니다.");
            signUp_phone_number.requestFocus();
            return false;
        }
        if (phoneNumber.length() < 13) {
            showToast("올바른 형식이 아닙니다.");
            signUp_phone_number.requestFocus();
            return false;
        }
        // 추가적인 유효성 검사 로직을 여기에 추가할 수 있습니다.
        return true;
    }

    // 아이디 유효성 검사
    private boolean validateId(String id) {
        if (id.isEmpty()) {
            showToast("아이디가 입력되지 않았습니다.");
            signUp_id.requestFocus();
            return false;
        }
        if (id.contains(" ")) {
            showToast("아이디에 공백은 불가능합니다.");
            signUp_id.requestFocus();
            return false;
        }
        if (checkId.matcher(id).find()) {
            showToast("올바른 아이디 형식이 아닙니다.");
            signUp_id.requestFocus();
            return false;
        }
        // 추가적인 유효성 검사 로직을 여기에 추가할 수 있습니다.
        return true;
    }

    // 비밀번호 유효성 검사
    private boolean validatePassword(String password) {
        if (password.isEmpty()) {
            showToast("비밀번호가 입력되지 않았습니다.");
            signUp_password.requestFocus();
            return false;
        }
        if (password.length() < 8 || password.length() > 20) {
            showToast("비밀번호는 8~20자리 이내로 입력 바랍니다.");
            signUp_password.requestFocus();
            return false;
        }
        if (!number.matcher(password).find() || !english.matcher(password).find() || !specialPattern.matcher(password).find()) {
            showToast("비밀번호는 영문, 숫자, 특수문자를 혼합하여 8~20자리 이내로 입력하세요.");
            signUp_password.requestFocus();
            return false;
        }
        // 추가적인 유효성 검사 로직을 여기에 추가할 수 있습니다.
        return true;
    }

    // 비밀번호 확인 유효성 검사
    private boolean validatePasswordConfirmation(String password, String passwordDuple) {
        if (!password.equals(passwordDuple)) {
            showToast("비밀번호가 같지 않습니다.");
            signUp_password_duple.requestFocus();
            return false;
        }
        // 추가적인 유효성 검사 로직을 여기에 추가할 수 있습니다.
        return true;
    }

    // 중복 확인 유효성 검사
    private boolean validateDuplicateCheck(int duple_checkID, int duple_checkNickname) {
        if (duple_checkID != 1 || duple_checkNickname != 1) {
            showToast("아이디 중복확인바랍니다.");
            signUp_id.requestFocus();
            return false;
        }
        // 추가적인 유효성 검사 로직을 여기에 추가할 수 있습니다.
        return true;
    }

    // 토스트 메시지 표시
    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void newUsersignup() {
        String name = signUp_name.getText().toString();
        String nickname = signUp_nickname.getText().toString();
        String phoneNumber = signUp_phone_number.getText().toString();
        String id = signUp_id.getText().toString();
        String password = signUp_password.getText().toString();
        String passwordDuple = signUp_password_duple.getText().toString();

        // 각 입력 필드의 유효성 검사
        if (validateName(name) && validateNickname(nickname) && validatePhoneNumber(phoneNumber) &&
                validateId(id) && validatePassword(password) && validatePasswordConfirmation(password, passwordDuple) &&
                validateDuplicateCheck(duple_checkID, duple_checkNickname)) {

            FirebaseAuth.getInstance().createUserWithEmailAndPassword(id + "@timproject.co.kr", password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                // 모든 조건이 만족되었을 때 Firestore에 데이터 추가
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("name", name);
                                userData.put("nickname", nickname);
                                userData.put("phoneNumber", phoneNumber);
                                userData.put("id", id);

                                // Firestore 데이터베이스의 'users' 컬렉션에 새 문서 추가
                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                db.collection("users")
                                        .add(userData)
                                        .addOnSuccessListener(documentReference -> {
                                            showToast("Cats가입이 되셨습니다!");
                                            // 데이터 삽입이 성공했을 때 할 작업 추가
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("Firestore", "Error adding document", e);
                                            // 데이터 삽입 중 오류가 발생했을 때 처리
                                        });
                            }
                        } else {
                            // 회원가입 실패 처리
                            showToast("회원가입에 실패하였습니다: " + task.getException().getMessage());
                        }
                    });
        }
    }
}