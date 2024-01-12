package com.example.chatbot;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

public class FileUpload extends Fragment {
    private static final int PICK_FILE_REQUEST = 1;
    private FileUploadInterface fileUploadInterface;

    public FileUpload() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_file_upload, container, false);

        Button chooseFileButton = view.findViewById(R.id.uploadButton);
        chooseFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFileChooser();
                Log.e("FileUpload", "버튼");
            }
        });

        return view;
    }

    private void showFileChooser() {
        Log.e("FileUpload", "showFileChooser() 호출");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // All file types
        filePickerLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        Uri selectedFileUri = data.getData();
                        // Perform upload using the selected file's Uri
                        uploadFile(selectedFileUri);
                    }
                }
            }
    );

    private void uploadFile(Uri fileUri) {
        // Implement file upload logic using the file's Uri
        // You need to write the actual implementation here.
        // For example, you might want to upload the file to a server.
        // uploadFileToServer(fileUri);
        Log.e("FileUpload", "onFileUpload() 호출");

        // Notify the listener (Document class) about the file upload
        if (fileUploadInterface != null) {
            fileUploadInterface.onFileUpload(fileUri);
        }
    }

    // Setter method for setting the listener
    public void setFileUploadListener(FileUploadInterface listener) {
        this.fileUploadInterface = listener;
    }
}
