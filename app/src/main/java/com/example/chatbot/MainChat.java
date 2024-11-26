package com.example.chatbot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.google.firebase.firestore.FirebaseFirestore;

import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatbot.adapter.MessageAdapter;
import com.example.chatbot.model.Message;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainChat extends AppCompatActivity {
    private static final String MY_SECRET_KEY = "";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private RecyclerView recyclerView;
    private TextView catGpt;
    private EditText submitAnswer;
    private ImageButton submitButton;

    private List<Message> messageList;
    private MessageAdapter messageAdapter;
    private FirebaseFirestore db;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private OkHttpClient client;
    private boolean isWaitingForResponse = false; // 중복 호출 방지 플래그

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.recycler_view);
        catGpt = findViewById(R.id.cat_gpt);
        submitAnswer = findViewById(R.id.submit_answer);
        submitButton = findViewById(R.id.submit_button);
        db = FirebaseFirestore.getInstance();
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        drawerLayout = findViewById(R.id.drawer_layout);

        // NavigationView 초기화
        navigationView = findViewById(R.id.navigation_view);

        // FragmentManager 가져오기
        FragmentManager fragmentManager = getSupportFragmentManager();
        NavigationHelper.setupNavigationDrawer(this, drawerLayout, navigationView, fragmentManager,db);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        loadMessagesFromFirestore();
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.open,
                R.string.closed
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        submitAnswer.setOnKeyListener((view, keyCode, keyEvent) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                submitButton.callOnClick();
                return true;
            }
            return false;
        });

        submitButton.setOnClickListener(view -> {
            String question = submitAnswer.getText().toString().trim();
            if (!question.isEmpty()) {
                addToChat(question, Message.SENT_BY_ME);
                submitAnswer.setText(""); // 입력 필드 초기화
                callAPI(question); // API 호출
                catGpt.setVisibility(View.GONE);
            }
        });

        client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.menuicon);
        }
    }

    private void loadMessagesFromFirestore() {
        String userId = UidSingleton.getInstance().getUid(); // Singleton에서 UID 가져오기

        if (userId == null || userId.isEmpty()) {
            Log.e("Firestore", "User ID is null or empty. Cannot load messages.");
            return;
        }

        db.collection("messages")
                .document(userId)
                .collection("chatArr")
                .orderBy("createdAt") // 시간 순 정렬
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String content = document.getString("content");
                            String sentBy = document.getString("sentBy");
                            messageList.add(new Message(content, sentBy));
                        }
                        messageAdapter.notifyDataSetChanged();
                    } else {
                        Log.e("Firestore", "Error loading messages", task.getException());
                    }
                });
    }



    void addToChat(String message, String sentBy) {
        runOnUiThread(() -> {
            Message newMassage = new Message(message, sentBy);
            messageList.add(newMassage);
            messageAdapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.smoothScrollToPosition(messageList.size() - 1);
            saveMassageToDB(newMassage);
        });
    }

    private void saveMassageToDB(Message message) {
        String userId = UidSingleton.getInstance().getUid(); // Singleton에서 UID 가져오기

        if (userId == null || userId.isEmpty()) {
            Log.e("Firestore", "User ID is null or empty. Message not saved.");
            return;
        }

        // Firestore에서 사용자 이름 가져오기
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userName = documentSnapshot.getString("name");

                        if (userName == null || userName.isEmpty()) {
                            userName = "Unknown User"; // 이름이 없을 경우 기본값
                        }

                        // 메시지 데이터 준비
                        Map<String, Object> messageMap = new HashMap<>();
                        messageMap.put("content", message.getMessage());
                        messageMap.put("sentBy", message.getSentBy());
                        messageMap.put("createdAt", System.currentTimeMillis());
                        messageMap.put("userName", userName);

                        // Firestore에 저장
                        db.collection("messages")
                                .document(userId)
                                .collection("chatArr")
                                .add(messageMap)
                                .addOnSuccessListener(documentReference -> Log.d("Firestore", "Message added: " + documentReference.getId()))
                                .addOnFailureListener(e -> Log.e("Firestore", "Error adding message", e));
                    } else {
                        Log.e("Firestore", "User document does not exist.");
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error fetching user name", e));
    }


    void addResponse(String response) {
        runOnUiThread(() -> {
            removeLastIfPlaceholder(); // Placeholder("...") 제거
            Message botMessage = new Message(response, Message.SENT_BY_BOT);
            messageList.add(botMessage);
            messageAdapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.smoothScrollToPosition(messageList.size() - 1);
            saveMassageToDB(botMessage); // Firestore에 저장
        });
    }


    void callAPI(String question) {
        if (isWaitingForResponse) {
            Toast.makeText(this, "응답을 기다리는 중입니다. 잠시만 기다려주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        isWaitingForResponse = true;
        addToChat("...", Message.SENT_BY_BOT);

        JSONArray messagesArray = new JSONArray();
        try {
            messagesArray.put(new JSONObject().put("role", "system").put("content", "You are a helpful assistant."));
            messagesArray.put(new JSONObject().put("role", "user").put("content", question));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("messages", messagesArray);
            requestBody.put("max_tokens", 150);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + MY_SECRET_KEY)
                .post(RequestBody.create(requestBody.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                isWaitingForResponse = false;
                addResponse("응답을 로드하지 못했습니다. 오류: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                isWaitingForResponse = false;
                if (response.isSuccessful()) {
                    try {
                        String result = new JSONObject(response.body().string())
                                .getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");
                        addResponse(result.trim());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    addResponse("응답 실패: " + response.body().string());
                }
            }
        });
    }

    void removeLastIfPlaceholder() {
        if (!messageList.isEmpty() && messageList.get(messageList.size() - 1).getMessage().equals("...")) {
            messageList.remove(messageList.size() - 1);
            messageAdapter.notifyItemRemoved(messageList.size());
        }
    }
}
