package com.example.chatbot;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Old_Document extends Fragment {

    private static final int PICK_FILE_REQUEST = 1;
    private static final String TAG = "Old_Document";

    private Uri selectedFileUri;
    private TextView documentTitleView;
    private TextView viewFilename;
    private Button selectFileButton;
    private Button uploadButton;
    private ProgressBar uploadProgressBar;

    private FirebaseFirestore db;

    public Old_Document() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_old__document, container, false);

        db = FirebaseFirestore.getInstance();

        viewFilename = view.findViewById(R.id.viewFilename);
        selectFileButton = view.findViewById(R.id.selectFileButton);
        uploadButton = view.findViewById(R.id.uploadButton);
        uploadProgressBar = view.findViewById(R.id.uploadProgressBar);
        documentTitleView = view.findViewById(R.id.documentTitle);

        // SidebarSingleton에서 folderName 가져오기
        String folderName = SidebarSingleton.getInstance().getSelectedFolderName();
        if (folderName != null && !folderName.isEmpty()) {
            documentTitleView.setText(folderName);
        }

        selectFileButton.setOnClickListener(v -> showFileChooser());

        uploadButton.setOnClickListener(v -> {
            if (selectedFileUri != null) {
                saveFileToFirestore(selectedFileUri);
            } else {
                Toast.makeText(getActivity(), "파일을 먼저 선택해주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            selectedFileUri = data.getData();
            String fileName = getFileName(selectedFileUri);
            viewFilename.setText(fileName);
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = requireActivity().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void saveFileToFirestore(Uri fileUri) {
        uploadProgressBar.setVisibility(View.VISIBLE);

        String fileName = getFileName(fileUri);
        String folderName = SidebarSingleton.getInstance().getSelectedFolderName();
        String groupName = SidebarSingleton.getInstance().getSelectedGroupName();
        String userName = UidSingleton.getInstance().getUid();

        if (folderName == null || folderName.isEmpty() || groupName == null || groupName.isEmpty()) {
            Toast.makeText(getActivity(), "그룹과 문서집을 설정해주세요.", Toast.LENGTH_SHORT).show();
            uploadProgressBar.setVisibility(View.GONE);
            return;
        }

        // Firestore에 데이터 저장
        Map<String, Object> documentData = new HashMap<>();
        documentData.put("folderName", folderName);
        documentData.put("groupName", groupName);
        documentData.put("userName", userName);

        // Firestore에서 파일 리스트 업데이트
        db.collection("documents")
                .document(folderName + "_" + groupName) // 폴더명과 그룹명을 조합하여 문서 식별
                .update("file", FieldValue.arrayUnion(fileName)) // 기존 배열에 새 파일 추가
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "파일이 성공적으로 저장되었습니다.", Toast.LENGTH_SHORT).show();
                    uploadProgressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    // 문서가 없으면 새로 생성
                    documentData.put("file", FieldValue.arrayUnion(fileName));
                    db.collection("documents")
                            .document(folderName + "_" + groupName)
                            .set(documentData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getActivity(), "파일이 성공적으로 저장되었습니다.", Toast.LENGTH_SHORT).show();
                                uploadProgressBar.setVisibility(View.GONE);
                            })
                            .addOnFailureListener(e2 -> {
                                Toast.makeText(getActivity(), "파일 저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                                uploadProgressBar.setVisibility(View.GONE);
                                Log.e(TAG, "Firestore 저장 실패", e2);
                            });
                });
    }
}
