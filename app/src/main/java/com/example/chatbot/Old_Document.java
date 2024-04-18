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

public class Old_Document extends Fragment {

    private static final int PICK_FILE_REQUEST = 1;
    private Uri selectedFileUri;

    private TextView viewFilename;
    private Button selectFileButton;
    private Button uploadButton;
    private Button document_delete;

    public Old_Document() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_old__document, container, false);

        // Initialize views
        viewFilename = view.findViewById(R.id.viewFilename);

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
                    viewFilename.setText(selectedFileName);
                } else {
                    // Show warning message for invalid file type
                    Toast.makeText(getActivity(), "Only PDF files are allowed", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // Method to upload file
    private void uploadFile(Uri fileUri) {
        File file = new File(fileUri.getPath());
        // 파일 업로드 로직을 여기에 추가합니다.
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
}
