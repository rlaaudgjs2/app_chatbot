package com.example.chatbot;

import static androidx.core.content.ContextCompat.startActivity;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NavigationHelper {

    public static void setupNavigationDrawer(
            Context context,
            DrawerLayout drawerLayout,
            com.google.android.material.navigation.NavigationView navigationView,
            FragmentManager fragmentManager,
            FirebaseFirestore db) {
        View headerView = navigationView.getHeaderView(0);
        TextView userNameTextView = headerView.findViewById(R.id.current_name);
        Spinner groupSpinner = headerView.findViewById(R.id.current_group);
        Spinner documentSpinner = headerView.findViewById(R.id.current_document);
        loadUserData(context, db, userNameTextView, groupSpinner, documentSpinner);
        loadGroupAndDocumentInfo(context, groupSpinner, documentSpinner);
        Button codeCopy = headerView.findViewById(R.id.codeCopy);

        View logoutSection = navigationView.findViewById(R.id.logout_section); // 분리된 로그아웃 섹션
        codeCopy.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                bringCode(context,db,groupSpinner);
            }

        });

        if (logoutSection != null) {
            logoutSection.setOnClickListener(v -> logOut(context, drawerLayout, navigationView));
        }
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();
            if(id == R.id.mainChat){
                openFragment(fragmentManager, new ChatFragment(), "CHAT_FRAGMENT");
            } else if (id == R.id.document) {
                openFragment(fragmentManager, new CreateDocument(), "CreateDocument");
            } else if (id == R.id.chat_list) {
                openFragment(fragmentManager, new ChatList(), "ChatList");
            } else if (id == R.id.info_setting) {
                openFragment(fragmentManager, new SettingMyinfo(), "ettingMyinfo");
            } else if (id == R.id.group_manag) {
                openFragment(fragmentManager, new GroupCreate(), "GroupCreate");
            } else if (id == R.id.document_upload) {
                openFragment(fragmentManager, new Old_Document(), "Old_Document");
            }  else {
                Toast.makeText(context, "알 수 없는 메뉴입니다.", Toast.LENGTH_SHORT).show();
            }

            drawerLayout.closeDrawers();
            return true;
        });
    }


    private static void loadUserData(Context context, FirebaseFirestore db, TextView userNameTextView, Spinner groupSpinner, Spinner documentSpinner) {
        String userId = UidSingleton.getInstance().getUid();
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(context, "사용자 정보를 로드할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // 사용자 이름 설정
                        String userName = documentSnapshot.getString("nickName");
                        userNameTextView.setText(userName != null ? userName : "알 수 없는 사용자");

                        // 그룹 리스트 설정
                        List<Map<String, Object>> groupList = (List<Map<String, Object>>) documentSnapshot.get("groups");
                        if (groupList != null) {
                            List<String> groups = new ArrayList<>();
                            groups.add("그룹 선택");

                            for (Map<String, Object> group : groupList) {
                                String groupName = (String) group.get("groupName");
                                if (groupName != null) {
                                    groups.add(groupName);
                                }
                            }

                            ArrayAdapter<String> groupAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, groups);
                            groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            groupSpinner.setAdapter(groupAdapter);

                            groupSpinner.setOnItemSelectedListener(new GroupSelectionListener(context, db, groupSpinner, documentSpinner));

                            // 어댑터 설정 후 저장된 그룹, 문서 정보 로드
                            loadGroupAndDocumentInfo(context, groupSpinner, documentSpinner);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("FirebaseError", "사용자 데이터 로드 실패", e));
    }

    public static void loadGroupAndDocumentInfo(Context context, Spinner groupSpinner, Spinner documentSpinner) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserSettings", Context.MODE_PRIVATE);

        // 저장된 그룹과 문서 정보 불러오기
        String savedGroup = sharedPreferences.getString("selectedGroup", "그룹 선택");
        String savedDocument = sharedPreferences.getString("selectedDocument", "문서 선택");

        // 스피너에 설정하기
        ArrayAdapter<CharSequence> groupAdapter = (ArrayAdapter<CharSequence>) groupSpinner.getAdapter();
        if (groupAdapter != null) {
            int groupPosition = groupAdapter.getPosition(savedGroup);
            groupSpinner.setSelection(groupPosition);
        } else {
            Log.e("NavigationHelper", "groupSpinner의 어댑터가 아직 설정되지 않았습니다.");
        }

        ArrayAdapter<CharSequence> documentAdapter = (ArrayAdapter<CharSequence>) documentSpinner.getAdapter();
        if (documentAdapter != null) {
            int documentPosition = documentAdapter.getPosition(savedDocument);
            documentSpinner.setSelection(documentPosition);
        } else {
            Log.e("NavigationHelper", "documentSpinner의 어댑터가 아직 설정되지 않았습니다.");
        }
    }




    private static void openFragment(FragmentManager fragmentManager, androidx.fragment.app.Fragment fragment, String tag) {
        androidx.fragment.app.Fragment existingFragment = fragmentManager.findFragmentByTag(tag);
        if (existingFragment != null) {
            fragment = existingFragment;
        }

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment, tag);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private static void logOut(Context context, DrawerLayout drawerLayout, com.google.android.material.navigation.NavigationView navigationView) {
        UidSingleton.getInstance().reset();
        context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                .edit().clear().apply();

        if (drawerLayout != null) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
        if (navigationView != null) {
            navigationView.setVisibility(View.GONE);
        }

        Intent intent = new Intent(context, SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

        Toast.makeText(context, "로그아웃 완료", Toast.LENGTH_SHORT).show();
    }
    private static void bringCode(Context context, FirebaseFirestore db, Spinner groupSpinner) {
        String userId = UidSingleton.getInstance().getUid();
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(context, "사용자 정보를 로드할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 선택된 그룹 가져오기
        String selectedGroup = groupSpinner.getSelectedItem().toString();
        if (selectedGroup.equals("그룹 선택")) {
            Toast.makeText(context, "그룹을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Firestore 쿼리: groupName 필드가 selectedGroup과 일치하는 문서 검색
        db.collection("group")
                .whereEqualTo("groupName", selectedGroup)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // 그룹 이름이 일치하는 첫 번째 문서 가져오기
                        Map<String, Object> groupData = queryDocumentSnapshots.getDocuments().get(0).getData();
                        if (groupData != null && groupData.containsKey("enterCode")) {
                            String enterCode = (String) groupData.get("enterCode");
                            if (enterCode != null && !enterCode.isEmpty()) {
                                showCopyDialog(context, enterCode);
                            } else {
                                Toast.makeText(context, "선택한 그룹의 인증 코드가 없습니다.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(context, "선택한 그룹의 인증 코드가 없습니다.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context, "선택한 그룹이 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseError", "그룹 데이터 로드 실패", e);
                    Toast.makeText(context, "코드를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
    }


    private static void showCopyDialog(Context context, String code) {
        // 다이얼로그 빌더 생성
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        // 커스텀 레이아웃 인플레이트
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.copycodeview, null);
        builder.setView(dialogView);

        // 다이얼로그 생성
        AlertDialog dialog = builder.create();

        // 다이얼로그 배경을 투명하게 설정
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // 뷰 참조
        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
        TextView dialogCode = dialogView.findViewById(R.id.dialog_code);
        Button buttonCopy = dialogView.findViewById(R.id.button_copy);
        Button buttonCancel = dialogView.findViewById(R.id.button_cancel);

        // 데이터 설정
        dialogTitle.setText("인증 코드 복사");
        dialogCode.setText(code);

        // 버튼 클릭 리스너 설정
        buttonCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 클립보드에 코드 복사
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("인증 코드", code);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "코드가 클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        // 다이얼로그 표시
        dialog.show();
    }
}
