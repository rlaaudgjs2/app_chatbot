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

import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateDocument extends Fragment {
    private EditText document_text;
    private Button document_commit;


    public CreateDocument() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_document, container, false);
        document_text = view.findViewById(R.id.document_text);
        document_commit = view.findViewById(R.id.document_commit);

        document_commit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commitDocument();
            }




        });
        return view;
    }

    private void commitDocument() {
        String text = document_text.getText().toString();
        if (text.isEmpty()) {
            Toast.makeText(getContext(), "문서집 이름을 작성해주세요", Toast.LENGTH_SHORT).show();
        } else {
            UidSingleton singleton = UidSingleton.getInstance();
            String uid = singleton.getUid();

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                DocumentReference userRef = db.collection("users").document(uid);
                userRef.get().addOnCompleteListener(userTask -> {
                    if (userTask.isSuccessful()) {
                        DocumentSnapshot userDoc = userTask.getResult();
                        if (userDoc.exists()) {
                            List<Map<String, Object>> groupsList = (List<Map<String, Object>>) userDoc.get("groups");

                            if (groupsList == null || groupsList.isEmpty()) {
                                Toast.makeText(getContext(), "그룹 정보가 없습니다. 메뉴에서 그룹을 설정해주세요.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                                // SidebarSingleton에서 선택된 그룹 이름 가져오기
                            String groupName = SidebarSingleton.getInstance().getSelectedGroupName();

                            if (groupName == null || groupName.isEmpty()) {
                                Toast.makeText(getContext(), "메뉴에서 그룹을 설정해주세요.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                                // 사용자 이름 가져오기
                            String name = userDoc.getString("name");
                            if (name == null || name.isEmpty()) {
                                name = "Unknown User"; // 사용자 이름이 없을 경우 기본값 설정
                            }

                            // Firestore에 저장할 데이터 구성
                            Map<String, Object> document = new HashMap<>();
                            document.put("folderName", text);
                            document.put("groupName", groupName);
                            document.put("userName", name);


                            db.collection("documents")
                                    .add(document)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getContext(), "새로운 문서집 생성 완료", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        // 사용자 데이터 업데이트 실패
                                        Log.e("Firestore", "Error updating user data", e);
                                        Toast.makeText(getContext(), "사용자 데이터 업데이트 실패", Toast.LENGTH_SHORT).show();
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



}
