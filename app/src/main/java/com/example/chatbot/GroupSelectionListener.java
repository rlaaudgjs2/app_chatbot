package com.example.chatbot;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroupSelectionListener implements android.widget.AdapterView.OnItemSelectedListener {
    private final Context context;
    private final FirebaseFirestore db;
    private final Spinner groupSpinner;
    private final Spinner documentSpinner;

    public GroupSelectionListener(Context context, FirebaseFirestore db, Spinner groupSpinner, Spinner documentSpinner) {
        this.context = context;
        this.db = db;
        this.groupSpinner = groupSpinner;
        this.documentSpinner = documentSpinner;

        // 그룹 로드
        loadUserGroups();
    }

    @Override
    public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
        String selectedGroup = (String) parent.getItemAtPosition(position);
        if (!"그룹 선택".equals(selectedGroup)) {
            loadDocumentsForGroup(selectedGroup);

        } else {
            clearDocuments();
        }
    }

    @Override
    public void onNothingSelected(android.widget.AdapterView<?> parent) {
        clearDocuments();
    }

    /**
     * 사용자 그룹을 로드하여 그룹 Spinner에 설정합니다.
     */
    private void loadUserGroups() {
        String userId = UidSingleton.getInstance().getUid(); // 사용자 UID 가져오기
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(context, "사용자 인증이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<Object> groupDataList = (List<Object>) documentSnapshot.get("groups");
                        updateGroupSpinner(groupDataList);
                    } else {
                        Toast.makeText(context, "사용자 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Log.e("FirebaseError", "사용자 그룹 로드 실패", e));
    }

    /**
     * 그룹 데이터를 기반으로 Spinner를 업데이트합니다.
     */
    private void updateGroupSpinner(List<Object> groupDataList) {
        List<String> groupNames = new ArrayList<>();
        groupNames.add("그룹 선택"); // 기본 선택 항목

        if (groupDataList != null) {
            for (Object groupData : groupDataList) {
                if (groupData instanceof Map) {
                    Map<String, Object> groupMap = (Map<String, Object>) groupData;
                    String groupName = (String) groupMap.get("groupName");
                    if (groupName != null && !groupName.isEmpty()) {
                        groupNames.add(groupName);
                    }
                }
            }
        }

        if (groupNames.size() == 1) { // 기본 선택 항목만 있을 경우
            Toast.makeText(context, "가입된 그룹이 없습니다.", Toast.LENGTH_SHORT).show();
        }

        ArrayAdapter<String> groupAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, groupNames);
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupSpinner.setAdapter(groupAdapter);
    }


    private void loadDocumentsForGroup(String groupName) {
        db.collection("documents") // 'documents' 컬렉션에서 데이터 가져오기
                .whereEqualTo("groupName", groupName) // groupName 필드가 선택된 그룹과 같은지 확인
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> folderNames = new ArrayList<>();
                    folderNames.add("문서 선택"); // 기본 선택 항목 추가

                    // 각 문서에서 folderName 필드 값을 추출
                    querySnapshot.forEach(document -> {
                        String folderName = document.getString("folderName");
                        if (folderName != null && !folderName.isEmpty()) {
                            folderNames.add(folderName);
                        }
                    });

                    // Spinner에 데이터 설정
                    ArrayAdapter<String> documentAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, folderNames);
                    documentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    documentSpinner.setAdapter(documentAdapter);

                    // Spinner에서 선택된 값을 Old_Document로 전달
                    documentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (position != 0) { // "문서 선택" 항목이 아닌 경우
                                String selectedFolderName = folderNames.get(position);

                                // Old_Document 프래그먼트를 찾고 데이터 업데이트
                                FragmentManager fragmentManager = ((AppCompatActivity) context).getSupportFragmentManager();
                                Old_Document oldDocumentFragment = (Old_Document) fragmentManager.findFragmentById(R.id.fragment_container); // Old_Document가 추가된 컨테이너 ID
                                if (oldDocumentFragment != null) {
                                    oldDocumentFragment.updateDocumentTitle(selectedFolderName); // TextView 업데이트
                                } else {
                                    Log.e("FragmentError", "Old_Document 프래그먼트를 찾을 수 없습니다.");
                                }
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            // 아무 항목도 선택되지 않은 경우 처리 필요 없음
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseError", "문서 데이터 로드 실패", e);
                    Toast.makeText(context, "문서 데이터를 로드하는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    private void clearDocuments() {
        List<String> documents = new ArrayList<>();
        documents.add("문서집 선택");

        ArrayAdapter<String> documentAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, documents);
        documentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        documentSpinner.setAdapter(documentAdapter);



    }
}
