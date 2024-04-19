package com.example.chatbot;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Old_Document extends Fragment {

    private static final int PICK_FILE_REQUEST = 1;
    private Uri selectedFileUri;
    private List<Uri> fileUris = new ArrayList<>();
    private TextView viewFilename;
    private Spinner documentSpinner;
    private String selectedFolderName;

    public Old_Document() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_old__document, container, false);

        viewFilename = view.findViewById(R.id.viewFilename);
        documentSpinner = view.findViewById(R.id.documentSpinner);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        documentSpinner.setAdapter(spinnerAdapter);
        loadSpinnerData(spinnerAdapter);

        view.findViewById(R.id.selectFileButton).setOnClickListener(v -> showFileChooser());
        view.findViewById(R.id.uploadButton).setOnClickListener(v -> uploadFiles());

        return view;
    }

    private void loadSpinnerData(ArrayAdapter<String> spinnerAdapter) {
        // 데이터베이스에서 spinner에 표시될 데이터 가져오는 코드
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "파일 선택"), PICK_FILE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                selectedFileUri = data.getData();
                    String selectedFileName = getFileName(selectedFileUri);
                    viewFilename.setText(selectedFileName);
                    fileUris.add(selectedFileUri);

            }
        }
    }


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

    private void uploadFiles() {
        if (fileUris.isEmpty()) {
            Toast.makeText(getActivity(), "파일을 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8002/upload/")// 서버 주소 입력
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        FileUploadService service = retrofit.create(FileUploadService.class);

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        for (Uri uri : fileUris) {
            String filePath = getPathFromUri(uri);
            if (filePath != null) {
                File file = new File(filePath);
                RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
                builder.addFormDataPart("files", file.getName(), requestFile);
            }
        }

        String groupName = "groupName"; // 그룹 이름
        String folderName = "folderName"; // 폴더 이름
        builder.addFormDataPart("text", groupName + folderName);

        MultipartBody requestBody = builder.build();

        Call<Void> call = service.uploadFiles(requestBody);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getActivity(), "파일 업로드 성공", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "파일 업로드 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(getActivity(), "네트워크 오류 발생",Toast.LENGTH_SHORT).show();
                Log.e("API_CALL", "네트워크 오류 발생: " + t.getMessage());
            }
        });
    }
    private String getPathFromUri(Uri uri) {
        String filePath = null;
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            filePath = cursor.getString(columnIndex);
            cursor.close();
        }
        return filePath;
    }


}
