package com.example.chatbot;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link GroupCreate#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GroupCreate extends Fragment {
    // 아이디 정보
    private String userId;
    private String uid;
    private Map<String, Object> userInfo = new HashMap<>();

    // 새로운  그룹 생성할 때 필요한 데이터
    private EditText new_groupName;
    private String newCode;
    private String name;

    // 기존 그룹 입장
    private EditText oldcode;
    private Map<String, Object> groupInfo = new HashMap<>();

    private FirebaseFirestore db;


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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_group_create, container, false);

        oldcode = view.findViewById(R.id.old_group);
        new_groupName = view.findViewById(R.id.new_group);


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
        String newGroupName = new_groupName.getText().toString();
        Bundle bundle = getArguments();
        if (bundle != null){
            uid = bundle.getString("uid");
            // userId를 사용하여 필요한 작업을 수행
        }


        newCode = UUID.randomUUID().toString().substring(0,10); //10개의 랜덤 코드 발행
        // Firestore 인스턴스 가져오기
        FirebaseFirestore db = FirebaseFirestore.getInstance();


        DocumentReference docRef = db.collection("users").document(uid);

        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                         name = document.getString("name");
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });

        // 그룹 데이터 설정
        Map<String , Object> enterCode = new HashMap<>();
        enterCode.put("enterCode", newCode);


        Map<String, Object> memberData = new HashMap<>();
        memberData.put("name", name);
        memberData.put("uid", uid);


        List<Map<String, Object>> memberList = new ArrayList<>();
        memberList.add(memberData);

        Map<String, Object> groupdata = new HashMap<>();
        groupdata.put("enterCode", newCode);
        groupdata.put("groupName", newGroupName);
        groupdata.put("member", memberList);
        groupdata.put("owner", uid);

        // 그룹 추가
        db.collection("group")
                .add(groupdata)
                .addOnSuccessListener(documentReference -> {
                    // 그룹 추가 성공 시 처리
                    Log.d("Firestore", "DocumentSnapshot added with ID: " + documentReference.getId());
                    // 데이터 삽입이 성공했을 때 할 작업 추가
                    Toast.makeText(getContext(), "그룹 생성 완료", Toast.LENGTH_SHORT).show();
                    // 메인 채팅 화면으로 이동

                })
                .addOnFailureListener(e -> {
                    // 그룹 추가 실패 시 처리
                    Log.e("Firestore", "Error adding document", e);
                    // 데이터 삽입 중 오류가 발생했을 때 처리
                    Toast.makeText(getContext(), "그룹 생성 실패", Toast.LENGTH_SHORT).show();
                });

    }


    private void joinExistingGroup() {
        String oldcode_name = oldcode.getText().toString();
        Bundle bundle = getArguments();
        if (bundle != null) {
         uid = bundle.getString("userId");
        }
        // Firestore 인스턴스 가져오기
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("group")
                .whereEqualTo("enterCode", oldcode_name)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            // 그룹이 존재하는 경우
                            DocumentSnapshot doc = task.getResult().getDocuments().get(0);
                            groupInfo = doc.getData();
//                            setGroupMember(); // 그룹 멤버로 가입하는 메소드 호출
                            Toast.makeText(getContext(), "그룹이 존재합니다", Toast.LENGTH_SHORT).show();

                        } else {
                            // 그룹이 존재하지 않는 경우
                            Toast.makeText(getContext(), "존재하지 않는 입장코드입니다.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Firestore에서 그룹 데이터를 가져오는 도중 오류가 발생한 경우
                        Log.e("GroupSetFragment", "Error getting group data", task.getException());
                    }
                });
    }
}