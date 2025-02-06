package com.example.chatbot;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatbot.adapter.MessageAdapter;
import com.example.chatbot.model.Message;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private NavigationView navigationView;
    private EditText submitAnswer;
    private ImageButton submitButton;

    private List<Message> messageList;
    private MessageAdapter messageAdapter;

    private OkHttpClient client;
    private FirebaseFirestore db;

    public ChatFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // fragment_chat.xml 레이아웃 파일을 inflate 합니다.
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // UI 요소 초기화
        recyclerView = view.findViewById(R.id.recycler_view);
        submitAnswer = view.findViewById(R.id.submit_answer);
        submitButton = view.findViewById(R.id.submit_button);
        drawerLayout = view.findViewById(R.id.drawer_layout);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        }

        navigationView = view.findViewById(R.id.navigation_view);

        // Firebase 초기화 및 Firestore 인스턴스 가져오기
        FirebaseApp.initializeApp(requireContext());
        db = FirebaseFirestore.getInstance();

        // 네비게이션 드로어 설정 (NavigationHelper는 별도로 구현되어 있어야 합니다)
        NavigationHelper.setupNavigationDrawer(getActivity(), drawerLayout, navigationView, getChildFragmentManager(), db);

        // 리사이클러 뷰 설정
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(messageAdapter);

        // OkHttpClient 초기화
        client = new OkHttpClient();

        // ActionBarDrawerToggle 설정
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                getActivity(),
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
            Toast.makeText(getContext(), "그룹과 문서집을 설정해주세요.", Toast.LENGTH_SHORT).show();
        }
    }
    //버튼 클릭 활성/비활성화
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
            Toast.makeText(getContext(), "그룹과 문서집을 설정해주세요.", Toast.LENGTH_SHORT).show();
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
            Log.d(TAG, "Request JSON: " + requestBody.toString());

            Request request = new Request.Builder()
                    .url("http://10.0.2.2:8080/api/chat")
                    .post(RequestBody.create(requestBody.toString(), JSON))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    requireActivity().runOnUiThread(() ->
                            addToChat("서버 요청 실패: " + e.getMessage(), Message.SENT_BY_BOT));
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try (ResponseBody responseBody = response.body()) {
                            if (responseBody != null) {
                                String responseText = responseBody.string();
                                requireActivity().runOnUiThread(() -> {
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
                                requireActivity().runOnUiThread(() ->
                                        addToChat("서버 응답이 비어 있습니다.", Message.SENT_BY_BOT));
                            }
                        }
                    } else {
                        requireActivity().runOnUiThread(() ->
                                addToChat("서버 요청 실패: " + response.message(), Message.SENT_BY_BOT));
                    }
                }
            });
        } catch (JSONException e) {
            addToChat("요청 생성 중 오류가 발생했습니다: " + e.getMessage(), Message.SENT_BY_BOT);
            e.printStackTrace();
        }
    }

    private void addToChat(String message, String sentBy) {
        requireActivity().runOnUiThread(() -> {
            Message newMessage = new Message(message, sentBy);
            messageList.add(newMessage);
            messageAdapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.smoothScrollToPosition(messageList.size() - 1);
        });
    }
}
