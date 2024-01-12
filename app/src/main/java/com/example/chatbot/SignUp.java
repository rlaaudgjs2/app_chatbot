package com.example.chatbot;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

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

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SignUp#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SignUp extends Fragment {

    private Map<String, String> parameters;
    private EditText signUpIdEditText;
    private EditText signUpPasswordEditText;
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
        signUpIdEditText = view.findViewById(R.id.signUp_id);
        signUpPasswordEditText = view.findViewById(R.id.signUp_password);
        signUpButton = view.findViewById(R.id.signUp_button);
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = signUpIdEditText.getText().toString();
                String userID = signUpPasswordEditText.getText().toString();


                // 회원가입 요청을 서버로 전송
                sendSignUpRequest(userName, userID);
            }
        });


        return view;
    }
    private void sendSignUpRequest(String userName, String userID) {
        String url = "http://10.0.2.2/UserInfo.php";  // 사용할 서버의 URL로 변경



        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d("SignUp", "Server Response: " + response);
                },
                error -> {
                    Log.e("SignUp", "Server Error: " + error.toString());
                }) {
            @Override
            protected Map<String, String> getParams() {
                // POST 요청에 포함될 매개변수 설정
                Map<String, String> params = new HashMap<>();
                params.put("userName", userName);
                params.put("userID", userID);
                return params;
            }
        };

        // Volley 요청 큐에 요청을 추가
        Volley.newRequestQueue(requireContext()).add(request);
    }

}