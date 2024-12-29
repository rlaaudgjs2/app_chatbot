package com.example.chatbot;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatList extends Fragment {

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private TextView warningMessage;
    private ChatListAdapter chatListAdapter;
    private List<ChatData> chatList;

    private FirebaseFirestore db;

    public ChatList() {
        // Required empty public constructor
    }

    public static ChatList newInstance() {
        return new ChatList();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        chatList = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        // Toolbar 설정
        toolbar = view.findViewById(R.id.chatToolbar);
        toolbar.setTitle("Chat List");

        // RecyclerView 설정
        recyclerView = view.findViewById(R.id.chatRecycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        chatListAdapter = new ChatListAdapter(chatList);
        recyclerView.setAdapter(chatListAdapter);

        // 경고 메시지 설정
        warningMessage = view.findViewById(R.id.warning_message);

        // Firestore 데이터 로드
        loadChatData();

        return view;
    }

    private void loadChatData() {
        // SidebarSingleton에서 그룹 이름과 문서집 가져오기
        String groupName = SidebarSingleton.getInstance().getSelectedGroupName();
        String documentName = SidebarSingleton.getInstance().getSelectedFolderName();

        // 그룹 이름과 문서집이 설정되지 않았을 경우 경고 메시지 표시
        if (groupName == null || groupName.isEmpty() || documentName == null || documentName.isEmpty()) {
            warningMessage.setVisibility(View.VISIBLE);
            warningMessage.setText("그룹과 문서집을 설정해주세요.");
            recyclerView.setVisibility(View.GONE);
            return;
        }

        // 그룹과 문서집이 설정된 경우 데이터 로드
        warningMessage.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        String userId = UidSingleton.getInstance().getUid(); // Singleton에서 UID 가져오기

        if (userId == null || userId.isEmpty()) {
            return;
        }

        db.collection("messages")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("chatArr")) {
                        String fetchedGroupName = documentSnapshot.getString("groupName");
                        String fetchedDocumentName = documentSnapshot.getString("documentName");

                        // 그룹과 문서집 이름이 정확히 일치하는지 확인
                        if (fetchedGroupName != null && fetchedGroupName.equals(groupName)
                                && fetchedDocumentName != null && fetchedDocumentName.equals(documentName)) {

                            List<Map<String, Object>> chatArr = (List<Map<String, Object>>) documentSnapshot.get("chatArr");
                            if (chatArr != null && !chatArr.isEmpty()) {
                                chatList.clear(); // 이전 데이터를 초기화

                                // 마지막 메시지와 시간을 추출
                                Map<String, Object> lastChat = chatArr.get(chatArr.size() - 1); // 마지막 메시지 가져오기
                                String lastMessage = (String) lastChat.get("content");
                                String messageTime = (String) lastChat.get("createdAt");

                                // 채팅 이름은 최상위 `chatName` 필드를 사용
                                String chatName = documentSnapshot.getString("chatName");
                                if (chatName == null) {
                                    chatName = "Unknown Chat";
                                }

                                // ChatData 객체 추가
                                chatList.add(new ChatData(chatName, lastMessage, messageTime));

                                // RecyclerView 업데이트
                                chatListAdapter.notifyDataSetChanged();
                            } else {
                                // chatArr가 비어 있을 경우
                                warningMessage.setVisibility(View.VISIBLE);
                                warningMessage.setText("해당 그룹과 문서집에 대한 채팅이 없습니다.");
                                recyclerView.setVisibility(View.GONE);
                            }
                        } else {
                            // 그룹 이름 또는 문서집 이름이 일치하지 않을 경우
                            warningMessage.setVisibility(View.VISIBLE);
                            warningMessage.setText("그룹 또는 문서집 정보가 일치하지 않습니다.");
                            recyclerView.setVisibility(View.GONE);
                        }
                    } else {
                        // 문서가 없을 경우
                        warningMessage.setVisibility(View.VISIBLE);
                        warningMessage.setText("데이터를 찾을 수 없습니다.");
                        recyclerView.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    // Firestore 데이터 로드 실패 처리
                    warningMessage.setVisibility(View.VISIBLE);
                    warningMessage.setText("데이터를 가져오는 중 오류가 발생했습니다.");
                    recyclerView.setVisibility(View.GONE);
                });
    }


}
