package com.example.testproject1;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class PlaceDetailActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1001;

    TextView tvName, tvAddress, tvRating;
    ImageView ivPlaceImage, btnSendComment, btnAddImage;
    EditText etComment;
    RatingBar ratingUser;

    RecyclerView rvComments, rvPreviewImages;

    ArrayList<CommentModel> comments;
    CommentAdapter adapter;

    ArrayList<Uri> selectedImages = new ArrayList<>();
    PreviewImageAdapter previewAdapter;

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseAuth auth = FirebaseAuth.getInstance();

    String placeId, name, address;
    double rating, lat, lon;
    ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_detail);

        placeId = getIntent().getStringExtra("id");
        name = getIntent().getStringExtra("name");
        address = getIntent().getStringExtra("address");
        rating = getIntent().getDoubleExtra("rating", 0);

        initViews();
        setupPreviewImageList();
        loadComments();
        btnAddImage.setOnClickListener(v -> openGallery());
        btnSendComment.setOnClickListener(v -> sendComment());
        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        ivPlaceImage = findViewById(R.id.ivPlaceImage);
        tvName = findViewById(R.id.tvPlaceName);
        tvAddress = findViewById(R.id.tvPlaceAddress);
        tvRating = findViewById(R.id.tvPlaceRating);

        etComment = findViewById(R.id.etComment);
        ratingUser = findViewById(R.id.ratingUser);

        btnSendComment = findViewById(R.id.btnSendComment);
        btnAddImage = findViewById(R.id.btnAddImage);

        rvComments = findViewById(R.id.rvComments);
        rvPreviewImages = findViewById(R.id.rvPreviewImages);

        Glide.with(this).load(R.drawable.sample_place).into(ivPlaceImage);

        comments = new ArrayList<>();
        adapter = new CommentAdapter(comments);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(adapter);

         btnBack = findViewById(R.id.ivBack);
        Log.d("BACK_DEBUG", "btnBack = " + btnBack);

    }


    private void setupPreviewImageList() {
        rvPreviewImages.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        previewAdapter = new PreviewImageAdapter(selectedImages, pos -> {
            selectedImages.remove(pos);
            previewAdapter.notifyDataSetChanged();
            if (selectedImages.isEmpty()) rvPreviewImages.setVisibility(View.GONE);
        });

        rvPreviewImages.setAdapter(previewAdapter);
    }


    private void openGallery() {
        Intent I = new Intent(Intent.ACTION_GET_CONTENT);
        I.setType("image/*");
        I.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(I, "Chọn ảnh"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);

        if (req == PICK_IMAGE_REQUEST && res == Activity.RESULT_OK && data != null) {

            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    selectedImages.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                selectedImages.add(data.getData());
            }

            if (!selectedImages.isEmpty()) {
                rvPreviewImages.setVisibility(View.VISIBLE);
                previewAdapter.notifyDataSetChanged();
            }
        }
    }


    private File uriToFile(Uri uri) {
        try {
            InputStream input = getContentResolver().openInputStream(uri);
            if (input == null) return null;

            File temp = File.createTempFile("upload_", ".jpg", getCacheDir());
            OutputStream out = new FileOutputStream(temp);

            byte[] buffer = new byte[1024];
            int len;
            while ((len = input.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }

            out.close();
            input.close();

            return temp;

        } catch (Exception e) {
            return null;
        }
    }


    private void loadComments() {
        db.collection("places")
                .document(placeId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;

                    comments.clear();
                    value.getDocuments().forEach(d ->
                            comments.add(d.toObject(CommentModel.class))
                    );
                    adapter.notifyDataSetChanged();
                });
    }


    private void sendComment() {
        String msg = etComment.getText().toString().trim();
        if (msg.isEmpty()) {
            Toast.makeText(this, "Bạn chưa nhập nội dung", Toast.LENGTH_SHORT).show();
            return;
        }

        int ratingValue = (int) ratingUser.getRating();
        String user = auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : "Ẩn danh";
        long time = System.currentTimeMillis();

        if (selectedImages.isEmpty()) {
            pushComment(new CommentModel(user, msg, time, ratingValue, new ArrayList<>()));
        } else {
            uploadImagesThenSend(user, msg, time, ratingValue);
        }
    }


    private void uploadImagesThenSend(String user, String msg, long time, int ratingValue) {

        ArrayList<String> uploadedUrls = new ArrayList<>();

        for (Uri uri : selectedImages) {

            File file = uriToFile(uri);
            if (file == null) {
                Toast.makeText(this, "Không đọc được file", Toast.LENGTH_SHORT).show();
                return;
            }

            String filename = UUID.randomUUID() + ".jpg";

            PreSignService.getPreSignedUrl(
                    "http://10.0.2.2:3000/presign",
                    filename,
                    new PreSignService.Callback() {

                        @Override
                        public void onSuccess(String uploadUrl, String finalUrl) {

                            S3Uploader.uploadImage(
                                    file,
                                    uploadUrl,
                                    finalUrl,
                                    new S3Uploader.UploadCallback() {

                                        @Override
                                        public void onUploaded(String url) {
                                            uploadedUrls.add(url);

                                            if (uploadedUrls.size() == selectedImages.size()) {
                                                pushComment(new CommentModel(
                                                        user, msg, time, ratingValue, uploadedUrls
                                                ));
                                            }
                                        }

                                        @Override
                                        public void onError(String err) {
                                            runOnUiThread(() ->
                                                    Toast.makeText(PlaceDetailActivity.this,
                                                            "Upload lỗi: " + err,
                                                            Toast.LENGTH_SHORT).show());
                                        }
                                    }
                            );
                        }

                        @Override
                        public void onError(String err) {
                            runOnUiThread(() ->
                                    Toast.makeText(PlaceDetailActivity.this,
                                            "Không lấy được presigned URL: " + err,
                                            Toast.LENGTH_SHORT).show()
                            );
                        }
                    }
            );
        }
    }


    private void pushComment(CommentModel c) {
        db.collection("places")
                .document(placeId)
                .collection("comments")
                .add(c)
                .addOnSuccessListener(a -> {
                    etComment.setText("");
                    ratingUser.setRating(0);

                    selectedImages.clear();
                    rvPreviewImages.setVisibility(View.GONE);
                    previewAdapter.notifyDataSetChanged();

                    Toast.makeText(this, "Đã gửi bình luận!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lưu thất bại!", Toast.LENGTH_SHORT).show());
    }
}
