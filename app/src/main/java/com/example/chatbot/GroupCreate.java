package com.example.chatbot;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link GroupCreate#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GroupCreate extends Fragment {
    private EditText newGroupEditText;
    private EditText oldGroupEditText;


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public GroupCreate() {
        // Required empty public constructor
    }

    public static GroupCreate newInstance(String param1, String param2) {
        GroupCreate fragment = new GroupCreate();
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
        View view = inflater.inflate(R.layout.fragment_group_create, container, false);

        oldGroupEditText = view.findViewById(R.id.old_group);
        newGroupEditText = view.findViewById(R.id.new_group);


        Button createButton = view.findViewById(R.id.button_create);
        Button joinButton = view.findViewById(R.id.button_join);

        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewGroup();
            }
        });
        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                joinExistingGroup();
            }
        });

        return view;
    }
   private void createNewGroup() {
           String newGroupName = newGroupEditText.getText().toString();
       Log.e("GroupCreate", "he null");
           // 나머지 로직...
    }

    private void joinExistingGroup() {
        String existingGroupCode = oldGroupEditText.getText().toString();
        // 여기에 기존 그룹에 입장하기 위한 로직을 추가하세요.
        // 예를 들어, 서버에 입장 요청을 보내는 API 호출 등이 포함될 수 있습니다.
        Log.e("GroupCreate", "het is null");
    }
}