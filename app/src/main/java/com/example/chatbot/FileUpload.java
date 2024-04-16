package com.example.chatbot;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.File;
import java.io.IOException;

public class FileUpload extends Fragment {
    private static final int PICK_FILE_REQUEST = 1;
    private FileUploadInterface fileUploadInterface;
    private OkHttpClient client;
    Button uploadFileButton;
    Button selectFileButton;
    TextView selectedFileNameTextView;
    Uri selectedFileUri; // 선택된 파일의 Uri를 저장하기 위한 변수

    public FileUpload() {
        this.client = new OkHttpClient();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_file_upload, container, false);

        uploadFileButton = view.findViewById(R.id.uploadButton);
        selectFileButton = view.findViewById(R.id.selectFileButton);
        selectedFileNameTextView = view.findViewById(R.id.viewFilename);

        selectFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFileChooser();
            }
        });

        uploadFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 선택된 파일이 있으면 업로드 진행
                if (selectedFileUri != null) {
                    uploadFile(selectedFileUri);
                } else {
                    Toast.makeText(getActivity(), "Please select a file first", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    private void showFileChooser() {
        Log.e("FileUpload", "showFileChooser() 호출");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf"); // Only PDF files
        filePickerLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        selectedFileUri = data.getData();
                        // Check if the selected file is a PDF
                        String mimeType = getActivity().getContentResolver().getType(selectedFileUri);
                        if (mimeType != null && mimeType.equals("application/pdf")) {
                            // 파일명을 TextView에 설정
                            String selectedFileName = getFileName(selectedFileUri);
                            selectedFileNameTextView.setText(selectedFileName);
                        } else {
                            // Show warning message for invalid file type
                            Toast.makeText(getActivity(), "Only PDF files are allowed", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
    );

    private void uploadFile(Uri fileUri) {
        // 파일을 실제로 업로드하는 메서드 구현
        // 여기에는 파일을 서버에 업로드하는 로직을 구현해야 합니다.
        // 아래는 업로드 예제 코드이며, 실제로는 해당 서버의 API에 맞게 수정해야 합니다.
        File file = new File(fileUri.getPath());
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), RequestBody.create(MediaType.parse("application/pdf"), file))
                .build();

        Request request = new Request.Builder()
                .url("http://localhost:8002/upload")
                .post(requestBody)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                Log.d("FileUpload", "File uploaded successfully");
                // 업로드 성공 시 작업 수행
            } else {
                Log.e("FileUpload", "Failed to upload file");
                // 업로드 실패 시 작업 수행
            }
        } catch (IOException e) {
            Log.e("FileUpload", "Error uploading file: " + e.getMessage());
            // 업로드 중 오류 발생 시 작업 수행
        }
    }

    // Uri에서 파일 이름 가져오는 메서드
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            getActivity().getContentResolver().query(uri, null, null, null, null);
            try (Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow("_display_name"));
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
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 파일 선택 결과를 처리하는 코드 추가
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            // 파일 선택 결과를 처리하는 코드 작성
        }
    }
}
