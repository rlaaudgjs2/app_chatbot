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

import com.example.chatbot.utils.FileUtils;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.chatbot.network.ApiClient;
import com.example.chatbot.network.ApiService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.BufferedSink;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

        if (folderName == null || folderName.isEmpty() || groupName == null || groupName.isEmpty()) {
            Toast.makeText(getActivity(), "그룹과 문서집을 설정해주세요.", Toast.LENGTH_SHORT).show();
            uploadProgressBar.setVisibility(View.GONE);
            return;
        }

        try {
            // Google Drive URI인지 확인
            if (fileUri.getAuthority() != null && fileUri.getAuthority().contains("com.google.android.apps.docs.storage")) {
                // Google Drive 파일 처리
                handleGoogleDriveFile(fileUri, fileName, folderName);
            } else {
                // 로컬 파일 처리
                handleLocalFile(fileUri, fileName, folderName);
            }
        } catch (Exception e) {
            Log.e("FileUploadError", "Upload failed", e);
            Toast.makeText(getActivity(), "파일 처리 중 오류 발생: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            uploadProgressBar.setVisibility(View.GONE);
        }
    }
    private void handleGoogleDriveFile(Uri fileUri, String fileName, String folderName) {
        try {
            InputStream inputStream = getActivity().getContentResolver().openInputStream(fileUri);

            if (inputStream == null) {
                Toast.makeText(getActivity(), "Google Drive 파일을 읽을 수 없습니다.", Toast.LENGTH_SHORT).show();
                uploadProgressBar.setVisibility(View.GONE);
                return;
            }

            // InputStream 기반 RequestBody 생성
            RequestBody requestBody = createStreamRequestBody(inputStream);

            MultipartBody.Part body = MultipartBody.Part.createFormData("file", fileName, requestBody);
            RequestBody indexName = RequestBody.create(folderName, MediaType.parse("multipart/form-data"));

            uploadToServer(body, indexName);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Google Drive 파일 처리 중 오류 발생.", Toast.LENGTH_SHORT).show();
            uploadProgressBar.setVisibility(View.GONE);
        }
    }

    private RequestBody createStreamRequestBody(final InputStream inputStream) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return MediaType.parse("application/octet-stream");
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                byte[] buffer = new byte[4096];
                int bytesRead;

                try {
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        sink.write(buffer, 0, bytesRead);
                    }
                } finally {
                    inputStream.close(); // 스트림은 여기서만 닫음
                }
            }
        };
    }

    private void handleLocalFile(Uri fileUri, String fileName, String folderName) {
        String filePath = FileUtils.getPath(getActivity(), fileUri);

        if (filePath == null) {
            Toast.makeText(getActivity(), "로컬 파일 경로를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
            uploadProgressBar.setVisibility(View.GONE);
            return;
        }

        File file = new File(filePath);
        RequestBody requestFile = RequestBody.create(file, MediaType.parse("application/octet-stream"));
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", fileName, requestFile);
        RequestBody indexName = RequestBody.create(folderName, MediaType.parse("multipart/form-data"));

        uploadToServer(body, indexName);
    }

    private void uploadToServer(MultipartBody.Part body, RequestBody indexName) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<Void> call = apiService.uploadFile(body, indexName);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getActivity(), "파일 업로드 성공!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "업로드 실패: " + response.message(), Toast.LENGTH_SHORT).show();
                    Log.d("error",response.message());
                }
                uploadProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("UploadError", "Error: ", t);
                Toast.makeText(getActivity(), "업로드 중 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                uploadProgressBar.setVisibility(View.GONE);
            }
        });
    }





}
