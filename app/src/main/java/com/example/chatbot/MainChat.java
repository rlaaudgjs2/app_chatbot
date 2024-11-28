package com.example.chatbot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.firebase.firestore.FieldValue;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
        navigationView = findViewById(R.id.navigation_view);
        FragmentManager fragmentManager = getSupportFragmentManager();
        NavigationHelper.setupNavigationDrawer(this, drawerLayout, navigationView, fragmentManager, db);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        loadMessagesFromFirestore();

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

                // Drawer가 닫힐 때 그룹과 문서집 설정 여부를 다시 확인
                checkAndSetButtonState();
            }
        };
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        setButtonAndEnterKeyEnabled(false); // 초기 버튼 및 엔터키 비활성화

        submitButton.setOnClickListener(view -> {
            if (isGroupAndDocumentSet()) {
                String question = submitAnswer.getText().toString().trim();
                if (!question.isEmpty()) {
                    addToChat(question, Message.SENT_BY_ME);
                    submitAnswer.setText(""); // 입력 필드 초기화
                    callAPI(question); // API 호출
                    catGpt.setVisibility(View.GONE);
                }
            } else {
                Toast.makeText(this, "그룹과 문서집을 설정해주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        submitAnswer.setOnKeyListener((view, keyCode, keyEvent) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                if (isGroupAndDocumentSet()) {
                    submitButton.callOnClick();
                } else {
                    Toast.makeText(this, "그룹과 문서집을 설정해주세요.", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
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


    private void setButtonAndEnterKeyEnabled(boolean enabled) {
        submitButton.setEnabled(enabled);
        submitButton.setAlpha(enabled ? 1.0f : 0.5f); // 활성화 시 투명도 변경
        submitAnswer.setEnabled(enabled);
    }

    private boolean isGroupAndDocumentSet() {
        String groupName = SidebarSingleton.getInstance().getSelectedGroupName();
        String documentName = SidebarSingleton.getInstance().getSelectedFolderName();
        return groupName != null && !groupName.isEmpty() && documentName != null && !documentName.isEmpty();
    }


    private void checkAndSetButtonState() {
        if (isGroupAndDocumentSet()) {
            setButtonAndEnterKeyEnabled(true); // 활성화
        } else {
             setButtonAndEnterKeyEnabled(false); // 비활성화
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
            Message newMessage = new Message(message, sentBy);
            messageList.add(newMessage);
            messageAdapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.smoothScrollToPosition(messageList.size() - 1);

            // sentBy가 봇인 경우 userName을 Cat-GPT로 설정
            if (Message.SENT_BY_BOT.equals(sentBy)) {
                saveMassageToDB(newMessage, "Cat-GPT");
            } else {
                saveMassageToDB(newMessage, null); // 사용자 이름은 Firestore에서 가져옴
            }
        });

    }


    private void saveMassageToDB(Message message, String userNameOverride) {
        final String userId = UidSingleton.getInstance().getUid(); // Singleton에서 UID 가져오기

        if (userId == null || userId.isEmpty()) {
            Log.e("Firestore", "User ID is null or empty. Message not saved.");
            return;
        }

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        final String userName = userNameOverride != null ? userNameOverride :
                                (documentSnapshot.getString("name") == null ? "Unknown User" : documentSnapshot.getString("name"));

                        String groupName = SidebarSingleton.getInstance().getSelectedGroupName();
                        String document = SidebarSingleton.getInstance().getSelectedFolderName();
                        final String myName = UidSingleton.getInstance().getUid();

                        // 첫 번째 사용자 메시지와 첫 번째 봇 응답 찾기
                        String userMessage = null;
                        String botResponse = null;
                        for (Message msg : messageList) {
                            if (userMessage == null && Message.SENT_BY_ME.equals(msg.getSentBy())) {
                                userMessage = msg.getMessage();
                            }
                            if (botResponse == null && Message.SENT_BY_BOT.equals(msg.getSentBy())) {
                                botResponse = msg.getMessage();
                            }
                            if (userMessage != null && botResponse != null) {
                                break;
                            }
                        }

                        // 기본값 설정
                        if (userMessage == null) userMessage = "User Chat";
                        if (botResponse == null) botResponse = "Bot Response";

                        // GPT API로 chatName 생성
                        generateChatNameWithAPI(userMessage, botResponse, new ChatNameCallback() {
                            @Override
                            public void onSuccess(String chatName) {
                                saveToFirestore(userId, message, userName, chatName, groupName, document, myName);
                            }

                            @Override
                            public void onFailure(String error) {
                                Log.e("ChatName", "Failed to generate chat name: " + error);
                                saveToFirestore(userId, message, userName, "Default Chat", groupName, document, myName);
                            }
                        });
                    } else {
                        Log.e("Firestore", "User document does not exist.");
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error fetching user data", e));
    }

    private void saveToFirestore(String userId, Message message, String userName, String chatName, String groupName, String document, String myName) {
        // 현재 시간 형식 지정
        final String formattedDate = new SimpleDateFormat("yyyy년 M월 d일 a h시 m분 s초", Locale.KOREA).format(new Date());

        // 새 메시지 데이터
        final Map<String, Object> chatData = new HashMap<>();
        chatData.put("content", message.getMessage());
        chatData.put("createdAt", formattedDate); // 변환된 날짜 추가
        chatData.put("userName", userName);

        // Firestore 업데이트
        db.collection("messages")
                .document(userId)
                .update(
                        "chatArr", FieldValue.arrayUnion(chatData),
                        "chatName", chatName,
                        "groupName", groupName,
                        "myName", myName
                )
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Message added to chatArr successfully."))
                .addOnFailureListener(e -> {
                    // 문서가 없는 경우 새로 생성
                    final Map<String, Object> finalData = new HashMap<>();
                    List<Map<String, Object>> newChatArr = new ArrayList<>();
                    newChatArr.add(chatData);
                    finalData.put("chatArr", newChatArr);
                    finalData.put("chatName", chatName);
                    finalData.put("documentName", document);
                    finalData.put("groupName", groupName);
                    finalData.put("myName", myName);

                    db.collection("messages")
                            .document(userId)
                            .set(finalData)
                            .addOnSuccessListener(aVoid2 -> Log.d("Firestore", "Message saved successfully."))
                            .addOnFailureListener(e2 -> Log.e("Firestore", "Error saving message", e2));
                });
    }



    private void generateChatNameWithAPI(String userMessage, String botResponse, ChatNameCallback callback) {
        JSONArray messagesArray = new JSONArray();
        try {
            // 시스템 메시지로 키워드 생성 요청
            messagesArray.put(new JSONObject()
                    .put("role", "system")
                    .put("content", "Create a short and descriptive title based on the following conversation."));
            messagesArray.put(new JSONObject()
                    .put("role", "user")
                    .put("content", "User: " + userMessage + "\nBot: " + botResponse));
        } catch (JSONException e) {
            e.printStackTrace();
            callback.onFailure("JSON creation failed");
            return;
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("messages", messagesArray);
            requestBody.put("max_tokens", 20); // 제목 길이를 짧게 제한
        } catch (JSONException e) {
            e.printStackTrace();
            callback.onFailure("Request body creation failed");
            return;
        }

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + MY_SECRET_KEY)
                .post(RequestBody.create(requestBody.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("GPT API", "Failed to fetch chat name: " + e.getMessage());
                callback.onFailure("API call failed");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String result = new JSONObject(response.body().string())
                                .getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content")
                                .trim();
                        callback.onSuccess(result);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callback.onFailure("Response parsing failed");
                    }
                } else {
                    callback.onFailure("API response unsuccessful: " + response.body().string());
                }
            }
        });
    }



    void addResponse(String response) {
        runOnUiThread(() -> {
            Message botMessage = new Message(response, Message.SENT_BY_BOT);
            messageList.add(botMessage);
            messageAdapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.smoothScrollToPosition(messageList.size() - 1);
            saveMassageToDB(botMessage, "Cat-GPT");
        });
    }


    void callAPI(String question) {


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
    interface ChatNameCallback {
        void onSuccess(String chatName);
        void onFailure(String error);
    }


}
