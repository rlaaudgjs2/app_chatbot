package com.example.chatbot;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
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

        navigationView.setNavigationItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();

            if (id == R.id.document) {
                openFragment(fragmentManager, new CreateDocument());
            } else if (id == R.id.chat_list) {
                openFragment(fragmentManager, new ChatList());
            } else if (id == R.id.info_setting) {
                openFragment(fragmentManager, new SettingMyinfo());
            } else if (id == R.id.group_manag) {
                openFragment(fragmentManager, new GroupCreate());
            } else if (id == R.id.document_upload) {
                openFragment(fragmentManager, new Old_Document());
            } else if (id == R.id.logout) {
                logOut(context, drawerLayout, navigationView);
            } else {
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

        // 사용자 이름 가져오기
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userName = documentSnapshot.getString("nickName");
                        userNameTextView.setText(userName != null ? userName : "알 수 없는 사용자");
                    }
                })
                .addOnFailureListener(e -> Log.e("FirebaseError", "사용자 이름 로드 실패", e));

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
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
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("FirebaseError", "사용자 그룹 로드 실패", e));

        }

    private static void openFragment(FragmentManager fragmentManager, androidx.fragment.app.Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
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
}
