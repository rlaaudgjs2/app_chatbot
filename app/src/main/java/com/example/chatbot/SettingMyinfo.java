package com.example.chatbot;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingMyinfo#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingMyinfo extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private Pattern specialPattern = Pattern.compile("[!@#$%^&*()_+\\-=\\\\|{}\\[\\]:\";'<>?,./]");
    private  Pattern number = Pattern.compile("[0-9]");
    private int user_nickname_check;
    private EditText user_name;
    private EditText user_PhoneNumber;
    private EditText user_Nickname;
    private Button amend_button;
    private Button modify_button;
    private Button go_back;

    public SettingMyinfo() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SettingMyinfo.
     */
    // TODO: Rename and change types and number of parameters
    public static SettingMyinfo newInstance(String param1, String param2) {
        SettingMyinfo fragment = new SettingMyinfo();
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
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_setting_myinfo, container, false);
        user_name = view.findViewById(R.id.user_name);
        user_PhoneNumber = view.findViewById(R.id.user_phone);
        user_Nickname = view.findViewById(R.id.user_nickname);
        amend_button = view.findViewById(R.id.user_amend);
        modify_button = view.findViewById(R.id.user_modify);
        go_back = view.findViewById(R.id.go_back);

        amend_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dupleCheckName();
            }
        });
        modify_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modifyCheck();
            }
        });
        go_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBackPage();
            }
        });
return view;
    }

    private void goBackPage() {
    }

    private void modifyCheck() {
    }

    private void dupleCheckName() {
        String userNickname = user_Nickname.getText().toString();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if(validateNickname(userNickname)) {
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
                                    user_nickname_check = 0;
                                } else {
                                    // 중복 아이디 없음
                                    Toast.makeText(getContext(), "사용 가능합니다.", Toast.LENGTH_SHORT).show();
                                    user_nickname_check = 1;
                                }
                            } else {
                                Log.e("FirebaseError", "에러 데이터", task.getException());
                            }
                        }
                    });
        }
    }
    private boolean validateNickname(String nickname) {
        if (nickname.isEmpty()) {
            showToast("닉네임이 입력되지 않았습니다.");
            user_Nickname.requestFocus();
            return false;
        }
        if (nickname.length() < 2 || nickname.length() > 10) {
            showToast("닉네임은 최소 2글자 이상 10글자 이하입니다.");
            user_Nickname.requestFocus();
            return false;
        }
        if (nickname.contains(" ")) {
            showToast("닉네임에 공백은 불가능합니다.");
            user_Nickname.requestFocus();
            return false;
        }
        if (nickname.matches("\\d+")) {
            showToast("닉네임은 숫자로만 구성될 수 없습니다.");
            user_Nickname.requestFocus();
            return false;
        }
        if (specialPattern.matcher(nickname).find()) {
            showToast(".과 _과 -를 제외한 특수문자는 사용할 수 없습니다.");
            user_Nickname.requestFocus();
            return false;
        }
        // 추가적인 유효성 검사 로직을 여기에 추가할 수 있습니다.
        return true;
    }
    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}