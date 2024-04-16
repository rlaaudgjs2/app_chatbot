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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.io.File;

public class Document extends Fragment {
    private EditText document_name;
    private Button document_commit;
    private TextView selectedFileNameTextView;
    private Uri selectedFileUri;

    private static final int PICK_FILE_REQUEST = 1;

    public Document() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_document, container, false);

        // Initialize views
        document_name = view.findViewById(R.id.document_text);
        document_commit = view.findViewById(R.id.document_commit);
        selectedFileNameTextView = view.findViewById(R.id.viewFilename);

        // Set click listener for document_commit button
        document_commit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFileUpload();
            }
        });

        // Set click listener for selectFileButton
        view.findViewById(R.id.selectFileButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFileChooser();
            }
        });

        // Set click listener for uploadButton
        view.findViewById(R.id.uploadButton).setOnClickListener(new View.OnClickListener() {
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

    // Method to show file chooser dialog
    private void showFileChooser() {
        Log.e("FileUpload", "showFileChooser() 호출");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf"); // Only PDF files
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    // Method to handle file selection result
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
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

    // Method to upload file
    private void uploadFile(Uri fileUri) {
        // 파일을 실제로 업로드하는 메서드 구현
        // 여기에는 파일을 서버에 업로드하는 로직을 구현해야 합니다.
        // 아래는 업로드 예제 코드이며, 실제로는 해당 서버의 API에 맞게 수정해야 합니다.
        File file = new File(fileUri.getPath());
        // 파일 업로드 로직 작성
    }

    // Method to get file name from Uri
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            getActivity().getContentResolver().query(uri, null, null, null, null);
            try (Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null)) {
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

    // Method to handle file upload event
    private void onFileUpload() {
        // 파일 업로드 버튼 클릭 시 작업 수행
    }
}
