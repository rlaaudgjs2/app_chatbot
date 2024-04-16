package com.example.chatbot;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Document extends Fragment  {
    private String userId;
    private String userName;
    private String groupName;
    private EditText document_name;
    private Button document_commit;
    private List<String> folderNames = new ArrayList<>();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    public Document() {
        // Required empty public constructor
    }

    public static Document newInstance(String param1, String param2) {
        Document fragment = new Document();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_document, container, false);
        // FileUpload 프래그먼트를 생성하고 listener를 설정
        FileUpload fileUploadFragment = new FileUpload();
        document_name = view.findViewById(R.id.document_text);
        document_commit = view.findViewById(R.id.document_commit);

        // FileUpload 프래그먼트를 추가
        getChildFragmentManager().beginTransaction()
                .replace(R.id.fileUploadContainer, fileUploadFragment)
                .commit();
        document_commit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFileUpload();
            }
        });

        return view;
    }

    private void onFileUpload() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String document_text = document_name.getText().toString();


        // Firestore 데이터베이스에 삽입할 데이터 가져오기
        Map<String, Object> userData = new HashMap<>();
        userData.put("folderName", document_text);

        db.collection("documents")
                .add(userData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d("Firestore", "DocumentSnapshot added with ID: " + documentReference.getId());
                        // 데이터 삽입이 성공했을 때 할 작업 추가
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Firestore", "Error adding document", e);
                        // 데이터 삽입 중 오류가 발생했을 때 처리
                    }
                });
        Log.e("FileUpload", "onFileUpload() 호출");
        // TODO: 파일 업로드 이벤트를 처리하는 로직 추가
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.e("Document", "onActivityResult() 호출");

        List<Fragment> fragments = getChildFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof FileUpload) {
                FileUpload fileUploadFragment = (FileUpload) fragment;
                fileUploadFragment.onActivityResult(requestCode, resultCode, data);
            }
        }
    }


}
