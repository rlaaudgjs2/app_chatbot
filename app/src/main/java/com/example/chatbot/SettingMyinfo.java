    package com.example.chatbot;

    import android.content.Intent;
    import android.net.Uri;
    import android.os.Bundle;

    import androidx.annotation.NonNull;
    import androidx.annotation.Nullable;
    import androidx.fragment.app.Fragment;

    import android.util.Log;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.ImageButton;
    import android.widget.Toast;

    import com.bumptech.glide.Glide;
    import com.google.android.gms.tasks.OnCompleteListener;
    import com.google.android.gms.tasks.Task;
    import com.google.firebase.firestore.DocumentSnapshot;
    import com.google.firebase.firestore.FirebaseFirestore;
    import com.google.firebase.firestore.QuerySnapshot;
    import com.google.firebase.storage.FirebaseStorage;
    import com.google.firebase.storage.StorageReference;

    import java.util.regex.Pattern;

    /**
     * A simple {@link Fragment} subclass.
     * Use the {@link SettingMyinfo#newInstance} factory method to
     * create an instance of this fragment.
     */
    public class SettingMyinfo extends Fragment {
        private static final int PICK_IMAGE_REQUEST = 1;
        private Uri imageUri; // 선택된 이미지 URI
        private ImageButton myPageImage;
        private FirebaseStorage storage;
        private StorageReference storageReference;

        private Pattern specialPattern = Pattern.compile("[!@#$%^&*()_+\\-=\\\\|{}\\[\\]:\";'<>?,./]");
        private  Pattern number = Pattern.compile("[0-9]");
        private int user_nickname_check;
        private EditText user_name;
        private EditText user_PhoneNumber;
        private EditText user_Nickname;
        private Button amend_button;
        private Button modify_button;
        private Button complete;
         public static SettingMyinfo newInstance(String param1, String param2) {
            SettingMyinfo fragment = new SettingMyinfo();
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            View view = inflater.inflate(R.layout.fragment_setting_myinfo, container, false);
            user_name = view.findViewById(R.id.user_name);
            user_PhoneNumber = view.findViewById(R.id.user_phone);
            user_Nickname = view.findViewById(R.id.user_nickname);
            myPageImage = view.findViewById(R.id.mypage_image);
            amend_button = view.findViewById(R.id.user_amend);
            modify_button = view.findViewById(R.id.user_modify);
            complete = view.findViewById(R.id.settingComplete);
            user_name.setEnabled(false);
            user_PhoneNumber.setEnabled(false);
            user_Nickname.setEnabled(false);
            loadUserInfo();
            myPageImage.setOnClickListener(v -> chooseImage());
            storage = FirebaseStorage.getInstance();
            storageReference = storage.getReference();
            amend_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dupleCheckName();
                }
            });
            modify_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    modifyCheck();
                }
            });
            complete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    complete();
                    uploadImageToFirebase();
                }
            });

    return view;
        }

        private void chooseImage() {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
        }
        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
                imageUri = data.getData();
                myPageImage.setImageURI(imageUri); // 선택된 이미지를 ImageButton에 표시
            }
        }

        private void loadUserInfo() {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String userId = UidSingleton.getInstance().getUid(); // 사용자의 UID를 가져옵니다.

            db.collection("users").document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                // 사용자 정보 로드
                                String name = document.getString("name");
                                String phone = document.getString("phoneNumber");
                                String nickname = document.getString("nickName");
                                String profileUrl = document.getString("profile"); // 프로필 URL

                                user_name.setText(name);
                                user_PhoneNumber.setText(phone);
                                user_Nickname.setText(nickname);

                                // 프로필 사진 로드
                                if (profileUrl != null && !profileUrl.isEmpty()) {
                                    Glide.with(requireContext())
                                            .load(profileUrl)
                                            .placeholder(R.drawable.mypage) // 기본 이미지
                                            .error(R.drawable.profile) // 에러 시 표시할 이미지
                                            .into(myPageImage); // myPageImage는 ImageButton
                                }
                            } else {
                                showToast("사용자 정보를 찾을 수 없습니다.");
                            }
                        } else {
                            Log.e("FirebaseError", "데이터 로드 실패", task.getException());
                        }
                    });
        }


        private void complete() {
            String updatedName = user_name.getText().toString();
            String updatedPhone = user_PhoneNumber.getText().toString();
            String updatedNickname = user_Nickname.getText().toString();

            if (validateInput(updatedName, updatedPhone, updatedNickname)) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                String userId = UidSingleton.getInstance().getUid();

                db.collection("users").document(userId)
                        .update("name", updatedName,
                                "phoneNumber", updatedPhone,
                                "nickName", updatedNickname)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    showToast("정보가 성공적으로 업데이트되었습니다.");
                                    user_name.setEnabled(false);
                                    user_PhoneNumber.setEnabled(false);
                                    user_Nickname.setEnabled(false);
                                } else {
                                    Log.e("FirebaseError", "업데이트 실패", task.getException());
                                    showToast("정보 업데이트 중 오류가 발생했습니다.");
                                }
                            }
                        });
            }
        }private boolean validateInput(String name, String phone, String nickname) {
            if (name.isEmpty()) {
                showToast("이름을 입력해주세요.");
                user_name.requestFocus();
                return false;
            }
            if (phone.isEmpty() || !phone.matches("^\\d{10,11}$")) {
                showToast("유효한 전화번호를 입력해주세요.");
                user_PhoneNumber.requestFocus();
                return false;
            }
            return true;
        }



        private void modifyCheck() {
            user_name.setEnabled(true);
            user_PhoneNumber.setEnabled(true);
            user_Nickname.setEnabled(true);
            showToast("정보를 수정할 수 있습니다. 완료 후 '저장' 버튼을 눌러주세요.");
        }


        private void dupleCheckName() {
            String userNickname = user_Nickname.getText().toString();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            if(validateNickname(userNickname)) {
                db.collection("users")
                        .whereEqualTo("nickName", userNickname)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    QuerySnapshot querySnapshot = task.getResult();
                                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                        //중복 아이디 존재
                                        Toast.makeText(getContext(), "중복하는 닉네임이 있습니다.", Toast.LENGTH_SHORT).show();
                                        user_nickname_check = 0;
                                    } else {
                                        // 중복 아이디 없음
                                        Toast.makeText(getContext(), "사용 가능합니다.", Toast.LENGTH_SHORT).show();
                                        user_nickname_check = 1;
                                    }
                                } else {
                                    Log.e("FirebaseError", "에러 데이터", task.getException());
                                }
                            }
                        });
            }
        }
        private boolean validateNickname(String nickname) {
            if (nickname.isEmpty()) {
                showToast("닉네임이 입력되지 않았습니다.");
                user_Nickname.requestFocus();
                return false;
            }
            if (nickname.length() < 2 || nickname.length() > 10) {
                showToast("닉네임은 최소 2글자 이상 10글자 이하입니다.");
                user_Nickname.requestFocus();
                return false;
            }
            if (nickname.contains(" ")) {
                showToast("닉네임에 공백은 불가능합니다.");
                user_Nickname.requestFocus();
                return false;
            }
            if (nickname.matches("\\d+")) {
                showToast("닉네임은 숫자로만 구성될 수 없습니다.");
                user_Nickname.requestFocus();
                return false;
            }
            if (specialPattern.matcher(nickname).find()) {
                showToast(".과 _과 -를 제외한 특수문자는 사용할 수 없습니다.");
                user_Nickname.requestFocus();
                return false;
            }
            // 추가적인 유효성 검사 로직을 여기에 추가할 수 있습니다.
            return true;
        }
        private void uploadImageToFirebase() {
            if (imageUri != null) {
                String userId = UidSingleton.getInstance().getUid();
                String fileName = "profiles/" + userId + ".jpg";
                StorageReference fileReference = storageReference.child(fileName);

                fileReference.putFile(imageUri)
                        .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl()
                                .addOnSuccessListener(uri -> saveProfileUrlToFirestore(uri.toString())))
                        .addOnFailureListener(e -> {
                            Log.e("FirebaseError", "Image upload failed", e);
                            showToast("이미지 업로드 실패");
                        });
            } else {
                showToast("이미지를 선택하세요.");
            }
        }

        private void saveProfileUrlToFirestore(String downloadUrl) {
            String userId = UidSingleton.getInstance().getUid();

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(userId)
                    .update("profile", downloadUrl)
                    .addOnSuccessListener(aVoid -> showToast("프로필 이미지가 저장되었습니다."))
                    .addOnFailureListener(e -> {
                        Log.e("FirebaseError", "Failed to save profile URL", e);
                        showToast("프로필 이미지 저장 실패");
                    });
        }


        private void showToast(String message) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }