package com.example.chatbot;

import android.os.Bundle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
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
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class SignUp extends Fragment {

    private EditText signUp_id;
    private EditText signUp_password;
    private EditText signUp_password_duple;
    private EditText signUp_name;
    private EditText signUp_nickname;
    private EditText signUp_phone_number;

    private int duple_checkID;
    private int duple_checkNickname;

    private TextView signin_change;
    private Button duple_check_name;
    private Button duple_check_id;
    private Button signUpButton;

    private Pattern checkId = Pattern.compile("[ㄱ-ㅎㅏ-ㅣ가-힣~₩!@#$%^&*()+._=,/\"':;'><]");
    private Pattern specialPattern = Pattern.compile("[!@#$%^&*()_+\\-=\\\\|{}\\[\\]:\";'<>?,./]");
    private Pattern number = Pattern.compile("[0-9]");
    private Pattern english = Pattern.compile("[a-z]");

    public SignUp() {
        // Required empty public constructor
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
        signin_change = view.findViewById(R.id.signup_change);
        signUpButton = view.findViewById(R.id.signUp_button);

        signin_change.setOnClickListener(v -> {
            Fragment signIn = new SignIn();
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.container, signIn);
            transaction.addToBackStack(null);
            transaction.commit();
        });


        signUpButton.setOnClickListener(v -> newUsersignup());
        duple_check_id.setOnClickListener(v -> dupleCheckID());
        duple_check_name.setOnClickListener(v -> dupleCheckName());

        return view;
    }


    private void dupleCheckName() {
        String userNickname = signUp_nickname.getText().toString();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .whereEqualTo("nickName", userNickname)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if(userNickname.isEmpty()){
                            showToast("닉네임을 입력해주세요");
                        }
                        else if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            showToast("중복하는 닉네임이 있습니다.");
                            duple_checkNickname = 0;
                        } else {
                            showToast("사용 가능합니다.");
                            duple_checkNickname = 1;
                        }
                    } else {
                        Log.e("FirebaseError", "에러 데이터", task.getException());
                    }
                });
    }

    private void dupleCheckID() {
        String userId = signUp_id.getText().toString();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .whereEqualTo("id", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if(userId.isEmpty()){
                            showToast("아이디를 입력해주세요");
                        }
                        else if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            showToast("중복하는 아이디가 있습니다.");
                            duple_checkID = 0;
                        } else {
                            showToast("사용 가능합니다.");
                            duple_checkID = 1;
                        }
                    } else {
                        Log.e("FirebaseError", "에러 데이터", task.getException());
                    }
                });
    }

    private boolean validateName(String name) {
        if (name.isEmpty()) {
            showToast("이름이 입력되지 않았습니다.");
            signUp_name.requestFocus();
            return false;
        }
        return true;
    }

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
        return true;
    }

    private boolean validatePhoneNumber(String phoneNumber) {
        if (phoneNumber.isEmpty()) {
            showToast("전화번호가 입력되지 않았습니다.");
            signUp_phone_number.requestFocus();
            return false;
        }
        String digitsOnly = phoneNumber.replaceAll("\\D", "");
        if (digitsOnly.length() != 11 || !digitsOnly.startsWith("010")) {
            showToast("올바른 전화번호 형식이 아닙니다. (010-XXXX-XXXX)");
            signUp_phone_number.requestFocus();
            return false;
        }
        return true;
    }

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
        return true;
    }

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
        return true;
    }

    private boolean validatePasswordConfirmation(String password, String passwordDuple) {
        if (!password.equals(passwordDuple)) {
            showToast("비밀번호가 같지 않습니다.");
            signUp_password_duple.requestFocus();
            return false;
        }
        return true;
    }

    private boolean validateDuplicateCheck(int duple_checkID, int duple_checkNickname) {
        if (duple_checkID != 1 || duple_checkNickname != 1) {
            showToast("중복확인바랍니다.");
            signUp_id.requestFocus();
            return false;
        }
        return true;
    }

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

        if (validateName(name) && validateNickname(nickname) && validatePhoneNumber(phoneNumber) &&
                validateId(id) && validatePassword(password) && validatePasswordConfirmation(password, passwordDuple) &&
                validateDuplicateCheck(duple_checkID, duple_checkNickname)) {

            FirebaseAuth.getInstance().createUserWithEmailAndPassword(id + "@timproject.co.kr", password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("name", name);
                                userData.put("nickName", nickname);
                                userData.put("phoneNumber", phoneNumber);
                                userData.put("id", id);

                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                db.collection("users")
                                        .document(user.getUid())
                                        .set(userData)
                                        .addOnSuccessListener(aVoid -> {
                                            showToast("Cats가입이 되셨습니다!");
                                            showToast("로그인 페이지로 이동합니다.");
                                            Fragment signIn = new SignIn();
                                            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                                            transaction.replace(R.id.container,signIn);
                                            transaction.addToBackStack(null);
                                            transaction.commit();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("Firestore", "Error adding document", e);
                                            showToast("회원가입 중 오류가 발생했습니다.");
                                        });
                            }
                        } else {
                            showToast("회원가입에 실패하였습니다: " + task.getException().getMessage());
                        }
                    });
        }
    }
}