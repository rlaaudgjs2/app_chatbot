package com.example.chatbot;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

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
    private String uid;
    private Map<String, Object> userInfo = new HashMap<>();

    // 새로운  그룹 생성할 때 필요한 데이터
    private EditText new_groupName;
    private String newCode;


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
        newCode = UUID.randomUUID().toString().substring(0,10);
        if (bundle != null) {
            final String uid = bundle.getString("uid");

            // Firestore 인스턴스 가져오기
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Firestore에서 사용자 문서 가져오기
            DocumentReference userRef = db.collection("users").document(uid);

            // 사용자 문서에서 이름 가져오기
            userRef.get().addOnCompleteListener(userTask -> {
                if (userTask.isSuccessful()) {
                    DocumentSnapshot userDoc = userTask.getResult();
                    if (userDoc.exists()) {
                        String name = userDoc.getString("name");

                        // 그룹 데이터 설정
                        Map<String, Object> memberData = new HashMap<>();
                        memberData.put("name", name);
                        memberData.put("uid", uid);

                        List<Map<String, Object>> memberList = new ArrayList<>();
                        memberList.add(memberData);

                        Map<String, Object> groupdata = new HashMap<>();
                        groupdata.put("enterCode", newCode);
                        groupdata.put("groupName", newGroupName);
                        groupdata.put("members", memberList);
                        groupdata.put("owner", uid);

                        // 그룹 추가
                        db.collection("group")
                                .add(groupdata)
                                .addOnSuccessListener(documentReference -> {
                                    // 그룹 추가 성공
                                    Toast.makeText(getContext(), newGroupName+ " "+ "그룹 생성이 완료되었습니다.", Toast.LENGTH_SHORT).show();


                                    // 사용자의 그룹 목록 업데이트
                                    Map<String, Object> groupInfo = new HashMap<>();
                                    groupInfo.put("enterCode", newCode);
                                    groupInfo.put("groupName", newGroupName);
                                    userRef.update("groups", FieldValue.arrayUnion(groupInfo))
                                            .addOnSuccessListener(aVoid -> {
                                                // 사용자 데이터 업데이트 성공
                                                Log.d("Firestore", "User data updated successfully");

                                                // 그룹 추가 및 사용자 데이터 업데이트 완료 메시지 표시
                                                Toast.makeText(getContext(), "그룹 생성 및 사용자 데이터 업데이트 완료", Toast.LENGTH_SHORT).show();

                                                // MainActivity로 이동
                                                Activity currentActivity = getActivity();
                                                if (currentActivity != null) {
                                                    Intent intent = new Intent(currentActivity, MainActivity.class);
                                                    currentActivity.startActivity(intent);
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                // 사용자 데이터 업데이트 실패
                                                Log.e("Firestore", "Error updating user data", e);
                                                Toast.makeText(getContext(), "사용자 데이터 업데이트 실패", Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    // 그룹 추가 실패
                                    Log.e("Firestore", "Error adding group", e);
                                    Toast.makeText(getContext(), "그룹 생성 실패", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Log.d(TAG, "No such user document");
                    }
                } else {
                    Log.d(TAG, "Error getting user document", userTask.getException());
                }
            });
        }
    }



    private void joinExistingGroup() {
        String oldcode_name = oldcode.getText().toString();
        Bundle bundle = getArguments();
        if (bundle != null) {
            uid = bundle.getString("uid");
            // userId를 사용하여 필요한 작업을 수행

            // Firestore 인스턴스 가져오기
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            DocumentReference docRef = db.collection("users").document(uid);

            docRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String name = document.getString("name");

                        // 새로운 멤버 데이터 생성 및 추가
                        Map<String, Object> memberData = new HashMap<>();
                        memberData.put("name", name);  // 기존 문서에서 가져온 이름 사용
                        memberData.put("uid", uid); // 새로운 멤버의 UID
                        // 필요한 경우 새로운 멤버의 다른 필드를 추가할 수 있습니다.

                        // 기존 그룹에 새 멤버 추가
                        db.collection("group")
                                .whereEqualTo("enterCode", oldcode_name)
                                .get()
                                .addOnCompleteListener(groupTask -> {
                                    if (groupTask.isSuccessful()) {
                                        QuerySnapshot groupQuerySnapshot = groupTask.getResult();
                                        if (groupQuerySnapshot != null && !groupQuerySnapshot.isEmpty()) {
                                            DocumentSnapshot groupDoc = groupQuerySnapshot.getDocuments().get(0);
                                            String groupId = groupDoc.getId();

                                            // 그룹에 멤버 추가
                                            db.collection("group")
                                                    .document(groupId)
                                                    .update("member", FieldValue.arrayUnion(memberData))
                                                    .addOnSuccessListener(aVoid -> {
                                                        // 멤버 추가 성공
                                                        Toast.makeText(getContext(), "그룹에 가입되었습니다.", Toast.LENGTH_SHORT).show();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        // 멤버 추가 실패
                                                        Log.e("GroupSetFragment", "Error adding member to group", e);
                                                        Toast.makeText(getContext(), "그룹 가입에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                                                    });
                                        } else {
                                            // 그룹이 존재하지 않는 경우
                                            Toast.makeText(getContext(), "존재하지 않는 입장코드입니다.", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        // Firestore에서 그룹 데이터를 가져오는 도중 오류가 발생한 경우
                                        Log.e("GroupSetFragment", "Error getting group data", groupTask.getException());
                                    }
                                });
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }

            });

        }
    }

}