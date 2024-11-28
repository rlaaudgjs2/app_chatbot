package com.example.chatbot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
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

import androidx.fragment.app.Fragment;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

public class Old_Document extends Fragment {

    private static final int PICK_FILE_REQUEST = 1;
    private static final String TAG = "Old_Document";
    private Uri selectedFileUri;
    private TextView documentTitleView;
    private TextView viewFilename;
    private Button selectFileButton;
    private Button uploadButton;
    private ProgressBar uploadProgressBar;

    public Old_Document() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_old__document, container, false);

        viewFilename = view.findViewById(R.id.viewFilename);
        selectFileButton = view.findViewById(R.id.selectFileButton);
        uploadButton = view.findViewById(R.id.uploadButton);
        uploadProgressBar = view.findViewById(R.id.uploadProgressBar);
        documentTitleView = view.findViewById(R.id.documentTitle);
        String selectedFolderName = SidebarSingleton.getInstance().getSelectedFolderName();

        // Singleton 값이 존재하면 UI 업데이트
        if (selectedFolderName != null && !selectedFolderName.isEmpty()) {
            documentTitleView.setText(selectedFolderName);
        }
        selectFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFileChooser();
            }
        });

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedFileUri != null) {
                    uploadFile(selectedFileUri);
                } else {
                    Toast.makeText(getActivity(), "Please select a file first", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }
    public void updateDocumentTitle(String selectedFolderName) {
        if (documentTitleView != null) {
            documentTitleView.setText(selectedFolderName); // TextView 업데이트
        }
    }


    private void showFileChooser() {
        Log.d(TAG, "showFileChooser() 호출");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");  // 모든 파일 형식 허용
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        String[] mimeTypes = {"application/pdf", "application/x-hwp"};  // PDF와 HWP 파일 허용
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                selectedFileUri = data.getData();
                String mimeType = getActivity().getContentResolver().getType(selectedFileUri);
                if (mimeType != null && (mimeType.equals("application/pdf") || mimeType.equals("application/x-hwp"))) {
                    String selectedFileName = getFileName(selectedFileUri);
                    viewFilename.setText(selectedFileName);
                } else {
                    Toast.makeText(getActivity(), "Only PDF and HWP files are allowed", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
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

    private void uploadFile(Uri fileUri) {
        try {
            File tempFile = createTempFileFromUri(fileUri);
            if (tempFile != null) {
                new FileUploadTask().execute(tempFile);
            } else {
                Toast.makeText(getActivity(), "Failed to create temp file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error uploading file", e);
            Toast.makeText(getActivity(), "Error uploading file", Toast.LENGTH_SHORT).show();
        }
    }

    private File createTempFileFromUri(Uri uri) throws Exception {
        InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
        String fileName = getFileName(uri);
        String fileExtension = fileName.substring(fileName.lastIndexOf("."));
        File tempFile = File.createTempFile(fileName, fileExtension, getActivity().getCacheDir());
        tempFile.deleteOnExit();
        FileOutputStream outputStream = new FileOutputStream(tempFile);
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.close();
        inputStream.close();
        return tempFile;
    }

    private class FileUploadTask extends AsyncTask<File, Integer, String> {
        private static final int BUFFER_SIZE = 4096;
        private static final int MAX_RETRIES = 3;
        private static final int RETRY_DELAY_MS = 5000; // 5 seconds

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            uploadProgressBar.setVisibility(View.VISIBLE);
            uploadProgressBar.setProgress(0);
        }

        @Override
        protected String doInBackground(File... files) {
            if (!isNetworkAvailable()) {
                return "Error: No network connection";
            }

            if (!testServerConnection()) {
                return "Error: Cannot connect to server";
            }

            int retryCount = 0;
            while (retryCount < MAX_RETRIES) {
                try {
                    File file = files[0];
                    Log.d(TAG, "Attempting to upload file: " + file.getName() + ", Size: " + file.length() + " bytes");

                    OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(2, TimeUnit.MINUTES)
                            .writeTimeout(2, TimeUnit.MINUTES)
                            .readTimeout(2, TimeUnit.MINUTES)
                            .build();

                    RequestBody requestBody = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("indexName", "yourIndexName")
                            .addFormDataPart("title", file.getName())
                            .addFormDataPart("content", file.getName(), new RequestBody() {
                                @Override
                                public MediaType contentType() {
                                    return MediaType.parse("application/octet-stream");
                                }

                                @Override
                                public void writeTo(BufferedSink sink) throws IOException {
                                    try (FileInputStream inputStream = new FileInputStream(file)) {
                                        byte[] buffer = new byte[BUFFER_SIZE];
                                        long totalBytes = file.length();
                                        long bytesWritten = 0;
                                        int read;
                                        while ((read = inputStream.read(buffer)) != -1) {
                                            sink.write(buffer, 0, read);
                                            bytesWritten += read;
                                            publishProgress((int) (100 * bytesWritten / totalBytes));
                                            Log.d(TAG, "Uploaded " + bytesWritten + " of " + totalBytes + " bytes");
                                        }
                                    }
                                }
                            })
                            .build();

                    Request request = new Request.Builder()
                            .url("http://10.0.2.2:8080/upload")
                            .post(requestBody)
                            .build();

                    Log.d(TAG, "Sending request to server...");
                    Response response = client.newCall(request).execute();
                    Log.d(TAG, "Received response from server. Status code: " + response.code());
                    return response.body().string();
                } catch (SocketTimeoutException e) {
                    Log.e(TAG, "Timeout occurred. Retrying... (Attempt " + (retryCount + 1) + " of " + MAX_RETRIES + ")", e);
                    retryCount++;
                    if (retryCount < MAX_RETRIES) {
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return "Error: Upload interrupted";
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error uploading file", e);
                    return "Error: " + e.getMessage();
                }
            }
            return "Error: Max retries reached. Upload failed.";
        }

        private boolean isNetworkAvailable() {
            ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }

        private boolean testServerConnection() {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .build();
                Request request = new Request.Builder()
                        .url("http://10.0.2.2:8080")  // Adjust this URL as needed
                        .build();
                Response response = client.newCall(request).execute();
                return response.isSuccessful();
            } catch (Exception e) {
                Log.e(TAG, "Error testing server connection", e);
                return false;
            }
        }

        // onProgressUpdate and onPostExecute methods remain the same
    }

    private static class ProgressRequestBody extends RequestBody {
        private final RequestBody delegate;
        private final ProgressListener listener;

        public ProgressRequestBody(RequestBody delegate, ProgressListener listener) {
            this.delegate = delegate;
            this.listener = listener;
        }

        @Override
        public MediaType contentType() {
            return delegate.contentType();
        }

        @Override
        public long contentLength() throws IOException {
            return delegate.contentLength();
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            BufferedSink bufferedSink = Okio.buffer(sink(sink));
            delegate.writeTo(bufferedSink);
            bufferedSink.flush();
        }

        private Sink sink(Sink sink) {
            return new ForwardingSink(sink) {
                long bytesWritten = 0L;
                long contentLength = 0L;

                @Override
                public void write(Buffer source, long byteCount) throws IOException {
                    super.write(source, byteCount);
                    if (contentLength == 0) {
                        contentLength = contentLength();
                    }
                    bytesWritten += byteCount;
                    listener.onProgressUpdate((int) (100 * bytesWritten / contentLength));
                }
            };
        }

        interface ProgressListener {
            void onProgressUpdate(int percentage);
        }
    }
}