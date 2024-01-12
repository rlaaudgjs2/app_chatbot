package com.example.chatbot;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import java.util.List;

public class Document extends Fragment implements FileUploadInterface {

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
        fileUploadFragment.setFileUploadListener(this);

        // FileUpload 프래그먼트를 추가
        getChildFragmentManager().beginTransaction()
                .replace(R.id.fileUploadContainer, fileUploadFragment)
                .commit();

        return view;
    }

    @Override
    public void onFileUpload(Uri fileUri) {
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
