package com.example.chatbot;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
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

public class MainActivity extends AppCompatActivity {
    private static final String MY_SECRET_KEY = "SecretKey";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private RecyclerView recyclerView;
    private TextView catGpt;
    private EditText submitAnswer;
    private ImageButton submitButton;

    private List<Message> messageList;
    private MessageAdapter messageAdapter;
    private Spinner groupSpinner;

    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.recycler_view);
        catGpt = findViewById(R.id.cat_gpt);
        submitAnswer = findViewById(R.id.submit_answer);
        submitButton = findViewById(R.id.submit_button);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);




        submitAnswer.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                switch (keyCode){
                    case KeyEvent.KEYCODE_ENTER:
                        submitButton.callOnClick();
                }
                return false;
            }
        });
        submitButton.setOnClickListener(view -> {
            String question = submitAnswer.getText().toString().trim();
            addToChat(question, Message.SENT_BY_ME);
            submitAnswer.setText("");
            callAPI(question);
            catGpt.setVisibility(View.GONE);
        });
        submitAnswer.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                switch (keyCode){
                    case KeyEvent.KEYCODE_ENTER:
                        submitButton.callOnClick();
                }
                return false;
            }
        });

        //시간제한을 두어 응답 대기 시간을 조절하였다.
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

        // Side Navigation Bar 설정
        settingSideNavBar();
    }
    public void settingSideNavBar() {
        // 사이드 메뉴를 오픈하기위한 아이콘 추가
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.menuicon);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        // 사이드 네브바 구현
        DrawerLayout drawLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);
        View headerView = navigationView.getHeaderView(0);
        groupSpinner = headerView.findViewById(R.id.group_spinner);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        UidSingleton singleton = UidSingleton.getInstance();
        String uid = singleton.getUid();
        DocumentReference userGroupSetting = db.collection("users").document(uid);
        userGroupSetting.get().addOnCompleteListener(userTask -> {
            if (userTask.isSuccessful()) {
                DocumentSnapshot userDoc = userTask.getResult();
                if (userDoc.exists()) {
                    List<Map<String, Object>> groupsList = (List<Map<String, Object>>) userDoc.get("groups");
                    List<String> groupNames = new ArrayList<>();
                    groupNames.add("그룹을 선택해주세요");
                    for (Map<String, Object> group : groupsList) {
                        String groupName = (String) group.get("groupName");
                        groupNames.add(groupName);
                    }
                    // 여기에 groupNames 리스트를 스피너에 설정하는 코드 추가
                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, groupNames);
                    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    groupSpinner.setAdapter(spinnerAdapter);

                    groupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                            String selectedGroupName = groupNames.get(position);

                            String enterCode = "";
                            for (Map<String, Object> group : groupsList) {
                                String groupName = (String) group.get("groupName");
                               if(groupName.equals(selectedGroupName)) {
                                    enterCode = (String) group.get("enterCode");
                                    break;
                                }
                            }
                            FirebaseFirestore.getInstance()
                                    .collection("group")
                                    .whereEqualTo("enterCode", enterCode)
                                    .get()
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            for (QueryDocumentSnapshot document : task.getResult()) {
                                                String uid = document.getId();
                                                if(!uid.isEmpty()){
                                                    GroupSingleton groupSingleton = GroupSingleton.getInstance();
                                                    groupSingleton.setUid(uid);
                                                }
                                            }
                                        } else {
                                            Log.d("Firestore", "Error getting documents: ", task.getException());
                                        }
                                    });
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });
                }
            }

        });


        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
                MainActivity.this,
                drawLayout,
                toolbar,  // 여기에 툴바를 전달
                R.string.open,
                R.string.closed
        );

        actionBarDrawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DrawerLayout drawer = findViewById(R.id.drawer_layout);
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else {
                    drawer.openDrawer(GravityCompat.START);
                }
            }
        });

        // 사이드 네브바 클릭 리스너
        drawLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        // -> 사이드 네브바 아이템 클릭 이벤트 설정
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == R.id.group_setting) {
                    openGroupCreateFragment();
                } else if (id == R.id.document) {
                    openOldDocument();
                } else if (id == R.id.new_chat) {
                    openChatList();
                } else if (id == R.id.info_setting) {
                    openSettingMyinfo();
                } else if (id == R.id.group_manag) {
                   openFileUpload();
                } else if (id == R.id.document_upload) {
                    openDocumentFragment();
                } else if (id == R.id.logout) {
                    openSignupFragment();
                }

                DrawerLayout drawer = findViewById(R.id.drawer_layout);
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }

            private void openFileUpload() {
                FragmentManager fragmentManager = getSupportFragmentManager();
                SignIn signIn = new SignIn();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.fragment_container, signIn);
                transaction.addToBackStack(null);
                transaction.commit();
            }

            private void openDocumentFragment() {
                FragmentManager fragmentManager = getSupportFragmentManager();
                Document document = new Document();

                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.fragment_container, document);
                transaction.addToBackStack(null);
                transaction.commit();
            }

            private void openLoginInFragment() {
                FragmentManager fragmentManager = getSupportFragmentManager();
                SignIn signIn = new SignIn();

                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.fragment_container, signIn);
                transaction.addToBackStack(null);
                transaction.commit();
            }

            private void openGroupCreateFragment() {
                FragmentManager fragmentManager = getSupportFragmentManager();
                GroupCreate groupCreateFragment = new GroupCreate();

                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.fragment_container, groupCreateFragment);
                transaction.addToBackStack(null);
                transaction.commit();



            }
            private void openSignupFragment() {
                FragmentManager fragmentManager = getSupportFragmentManager();
                SignUp signUp = new SignUp();

                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.fragment_container, signUp);
                transaction.addToBackStack(null);
                transaction.commit();


            }
            private  void  openOldDocument() {
                FragmentManager fragmentManager = getSupportFragmentManager();
                Old_Document oldDocument = new Old_Document();

                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.fragment_container, oldDocument);
                transaction.addToBackStack(null);
                transaction.commit();

            }
        });
    }

    private void openSettingMyinfo() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        SettingMyinfo settingMyinfo = new SettingMyinfo();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, settingMyinfo);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void openChatList() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        ChatList chatList = new ChatList();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, chatList);
        transaction.addToBackStack(null);
        transaction.commit();
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    //람다식 Thread를 사용하였다.
    void addToChat(String message, String sentBy) {
        runOnUiThread(() -> {
            messageList.add(new Message(message, sentBy));
            messageAdapter.notifyDataSetChanged();
            recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
        });
    }
    //ai 응답 처리 부분
    void addResponse(String response) {
        messageList.remove(messageList.size() - 1);
        addToChat(response, Message.SENT_BY_BOT);
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // 햄버거 아이콘 클릭 시 동작 설정
                DrawerLayout drawer = findViewById(R.id.drawer_layout);
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else {
                    drawer.openDrawer(GravityCompat.START);
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
    //ai 호출함수
    void callAPI(String question) {
        // okhttp
        messageList.add(new Message("...", Message.SENT_BY_BOT));

        // 새로운 부분: 채팅 기록을 배열에 담아 전송
        JSONArray arr = new JSONArray();
        JSONObject baseAi = new JSONObject();
        JSONObject userMsg = new JSONObject();
        try {
            // AI 속성 설정
            baseAi.put("role", "system");
            baseAi.put("content", "You are a helpful and kind AI Assistant.");

            // 사용자 메시지
            userMsg.put("role", "user");
            userMsg.put("content", question);

            // 배열에 추가
            arr.put(baseAi);
            arr.put(userMsg);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JSONObject object = new JSONObject();
        try {
            // 모델명 변경
            object.put("model", "gpt-3.5-turbo");
            object.put("messages", arr);
            object.put("max_tokens", 150);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(object.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions") // 이전 v1/completions 였으나 v1/chat/completions로 명확하게 설정
                .header("Authorization", "Bearer " + MY_SECRET_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                addResponse("응답을 로드하지 못했습니다. 오류: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");

                        String result = jsonObject.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                        addResponse(result.trim());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    String responseBody = response.body().string();
                    Log.d("API_RESPONSE", responseBody);
                    addResponse("Failed to load response due to " + responseBody);
                }
            }
        });
    }

}