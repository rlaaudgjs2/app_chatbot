package com.example.chatbot;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatbot.adapter.MessageAdapter;
import com.example.chatbot.model.Message;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainChat extends AppCompatActivity {

    private static final String TAG = "MainChat";

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private TextView catGpt;
    private NavigationView navigationView;
    private EditText submitAnswer;
    private ImageButton submitButton;

    private List<Message> messageList;
    private MessageAdapter messageAdapter;

    private OkHttpClient client;
    private FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI 요소 초기화
        recyclerView = findViewById(R.id.recycler_view);
        catGpt = findViewById(R.id.cat_gpt);
        submitAnswer = findViewById(R.id.submit_answer);
        submitButton = findViewById(R.id.submit_button);
        drawerLayout = findViewById(R.id.drawer_layout);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 네비게이션 뷰 설정
        navigationView = findViewById(R.id.navigation_view);
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();
        NavigationHelper.setupNavigationDrawer(this, drawerLayout, navigationView, getSupportFragmentManager(), db);

        // 리사이클러 뷰 설정
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);

        // OkHttpClient 초기화
        client = new OkHttpClient();

        // ActionBarDrawerToggle 설정
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.open,
                R.string.closed
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                checkGroupAndDocumentSettings(); // 사이드바 닫힐 때 설정 체크
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                enableInteraction(false); // 사이드바 열릴 때 비활성화
            }
        };
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // 제출 버튼 클릭 리스너
        submitButton.setOnClickListener(v -> handleUserInput());

        // Enter 키 이벤트 처리
        submitAnswer.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                handleUserInput();
                return true;
            }
            return false;
        });

        // 초기화 상태에서 그룹과 문서 설정 확인
        checkGroupAndDocumentSettings();
    }

    private void checkGroupAndDocumentSettings() {
        boolean isSet = isGroupAndDocumentSet();
        enableInteraction(isSet);
        if (isSet) {
            Log.d(TAG, "그룹 및 문서 설정 완료: 작업 가능.");
        } else {
            Log.w(TAG, "그룹 및 문서 설정 필요: 작업 불가.");
            Toast.makeText(this, "그룹과 문서집을 설정해주세요.", Toast.LENGTH_SHORT).show();
        }
    }

    private void enableInteraction(boolean enabled) {
        submitAnswer.setEnabled(enabled);
        submitButton.setEnabled(enabled);
        submitButton.setAlpha(enabled ? 1.0f : 0.5f); // 비활성화 시 투명도 낮추기
    }

    private void handleUserInput() {
        if (isGroupAndDocumentSet()) {
            String question = submitAnswer.getText().toString().trim();
            if (!question.isEmpty()) {
                addToChat(question, Message.SENT_BY_ME);
                submitAnswer.setText("");
                fetchFilesFromFirebaseAndSend(question);
            }
        } else {
            Toast.makeText(this, "그룹과 문서집을 설정해주세요.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isGroupAndDocumentSet() {
        String groupName = SidebarSingleton.getInstance().getSelectedGroupName();
        String folderName = SidebarSingleton.getInstance().getSelectedFolderName();
        return groupName != null && !groupName.isEmpty() && folderName != null && !folderName.isEmpty();
    }

    private void fetchFilesFromFirebaseAndSend(String query) {
        String groupName = SidebarSingleton.getInstance().getSelectedGroupName();
        String folderName = SidebarSingleton.getInstance().getSelectedFolderName();

        db.collection("documents")
                .whereEqualTo("folderName", folderName)
                .whereEqualTo("groupName", groupName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            sendChatRequest(query, groupName, folderName);

                        }
                    } else {
                        addToChat("문서를 찾을 수 없습니다.", Message.SENT_BY_BOT);
                    }
                });
    }

    private void sendChatRequest(String query, String groupName, String folderName) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("text", query);
            requestBody.put("groupName", groupName);
            requestBody.put("folderName", folderName);
            Log.d(TAG, "Request JSON: " + requestBody.toString()); // 로그 추가

            Request request = new Request.Builder()
                    .url("http://10.0.2.2:8080/api/chat")
                    .post(RequestBody.create(requestBody.toString(), JSON))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> addToChat("서버 요청 실패: " + e.getMessage(), Message.SENT_BY_BOT));
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try (ResponseBody responseBody = response.body()) {
                            if (responseBody != null) {
                                String responseText = responseBody.string();
                                runOnUiThread(() -> {
                                    try {
                                        JSONObject jsonResponse = new JSONObject(responseText);
                                        if (jsonResponse.has("status") && jsonResponse.getString("status").equals("success")) {
                                            String chatResponse = jsonResponse.optString("response", "응답이 없습니다.");
                                            addToChat(chatResponse, Message.SENT_BY_BOT);
                                        } else {
                                            String errorMsg = jsonResponse.optString("message", "오류 발생");
                                            addToChat(errorMsg, Message.SENT_BY_BOT);
                                        }
                                    } catch (JSONException je) {
                                        addToChat("JSON 파싱 중 오류 발생: " + je.getMessage(), Message.SENT_BY_BOT);
                                    }
                                });
                            } else {
                                runOnUiThread(() -> addToChat("서버 응답이 비어 있습니다.", Message.SENT_BY_BOT));
                            }
                        }
                    } else {
                        runOnUiThread(() -> addToChat("서버 요청 실패: " + response.message(), Message.SENT_BY_BOT));
                    }
                }
            });
        } catch (JSONException e) {
            addToChat("요청 생성 중 오류가 발생했습니다: " + e.getMessage(), Message.SENT_BY_BOT);
            e.printStackTrace();
        }
    }

    void addToChat(String message, String sentBy) {
        runOnUiThread(() -> {
            Message newMessage = new Message(message, sentBy);
            messageList.add(newMessage);
            messageAdapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.smoothScrollToPosition(messageList.size() - 1);
        });
    }

}
