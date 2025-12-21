package com.example.travelmate.profile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.travelmate.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ChangeAvatar extends AppCompatActivity {
    private static final int REQUEST_PICK_IMAGE = 1001;
    private static final int REQUEST_CROP_IMAGE = UCrop.REQUEST_CROP;
    private static final int REQUEST_CAMERA = 1002;
    private static final String TAG = "ChangeAvatar";

    private ImageView ivAvatar;
    private Button btnChooseImage, btnCancelAvatar, btnSaveAvatar;
    private Toolbar toolbar;

    private Uri currentImageUri;
    private Bitmap currentBitmap;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_changeavatar);

        initFirebase();
        initViews();
        setupToolbar();
        loadCurrentAvatar();
        setupClickListeners();
    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_change_avatar);
        ivAvatar = findViewById(R.id.ivAvatar);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnCancelAvatar = findViewById(R.id.btnCancelAvatar);
        btnSaveAvatar = findViewById(R.id.btnSaveAvatar);
    }

    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> {
                setResult(Activity.RESULT_CANCELED);
                finish();
            });
        }
    }

    private void loadCurrentAvatar() {
        // Load sample avatar as default using Glide for circular cropping
        if (ivAvatar != null) {
            com.bumptech.glide.Glide.with(this)
                    .load(R.drawable.sample_avatar)
                    .circleCrop()
                    .into(ivAvatar);
        }

        // Try to load user's current avatar from Firestore
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String photoUrl = document.getString("photoUrl");
                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                // Load user's current avatar
                                com.bumptech.glide.Glide.with(this)
                                        .load(photoUrl)
                                        .circleCrop()
                                        .placeholder(R.drawable.sample_avatar)
                                        .error(R.drawable.sample_avatar)
                                        .into(ivAvatar);
                                Log.d(TAG, "Loaded user's existing photoUrl: " + photoUrl);
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to load user data", e));
        }
    }

    private void setupClickListeners() {
        // Choose image button
        btnChooseImage.setOnClickListener(v -> openImagePicker());

        // Click on avatar image -> go to uCrop directly
        ivAvatar.setOnClickListener(v -> {
            if (currentImageUri != null) {
                // User has selected an image, edit it
                startUCrop(currentImageUri);
            } else {
                // User wants to edit sample avatar or select new image
                // Convert sample avatar to temp file first
                Uri sampleUri = getSampleAvatarUri();
                if (sampleUri != null) {
                    currentImageUri = sampleUri;
                    startUCrop(sampleUri);
                } else {
                    // Fallback: open image picker
                    openImagePicker();
                }
            }
        });

        // Cancel button
        btnCancelAvatar.setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });

        // Save button
        btnSaveAvatar.setOnClickListener(v -> saveAvatar());
    }

    private void openImagePicker() {
        // Hiển thị dialog để chọn giữa chụp ảnh hoặc chọn từ máy
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn ảnh đại diện");
        builder.setMessage("Bạn muốn chụp ảnh mới hay chọn ảnh có sẵn?");

        builder.setPositiveButton("Chụp ảnh", (dialog, which) -> {
            openCamera();
        });

        builder.setNegativeButton("Chọn từ máy", (dialog, which) -> {
            openGallery();
        });

        builder.setNeutralButton("Hủy", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.show();
    }

    // Mở camera để chụp ảnh
    private void openCamera() {
        try {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                // Tạo file để lưu ảnh chụp từ camera
                File photoFile = new File(getCacheDir(), "camera_photo_" + System.currentTimeMillis() + ".jpg");
                Uri photoURI = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider", photoFile);

                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                currentImageUri = photoURI;
                startActivityForResult(cameraIntent, REQUEST_CAMERA);
            } else {
                Toast.makeText(this, "Camera không có sẵn", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening camera", e);
            Toast.makeText(this, "Lỗi khi mở camera", Toast.LENGTH_SHORT).show();
        }
    }

    // Mở gallery để chọn ảnh có sẵn
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), REQUEST_PICK_IMAGE);
    }

    private void startUCrop(Uri sourceUri) {
        try {
            File outFile = new File(getCacheDir(), "avatar_crop_" + System.currentTimeMillis() + ".jpg");
            Uri destinationUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider", outFile);

            // UCrop Options Configuration
            // UCrop provides built-in rotate, scale, and crop functionality:
            // - Rotate: User can rotate image 90° using built-in rotate button
            // - Scale: Pinch to zoom in/out
            // - Crop: Square aspect ratio (1:1) with draggable crop frame
//            UCrop.Options options = new UCrop.Options();
//            options.setToolbarTitle("Chỉnh sửa ảnh");
//            options.setShowCropGrid(true);
//            options.setShowCropFrame(true);
//            // Enable free style crop
//            options.setFreeStyleCropEnabled(true);
//            // Set compression quality
//            options.setCompressionQuality(90);
            UCrop.Options options = new UCrop.Options();
            options.setToolbarTitle("Chỉnh sửa ảnh");

// ⭐ QUAN TRỌNG
            options.setStatusBarColor(Color.BLACK);
            options.setToolbarColor(Color.BLACK);
            options.setToolbarWidgetColor(Color.WHITE);

// ⭐ TRÁNH DÍNH STATUS BAR
            options.setHideBottomControls(false);
            options.setFreeStyleCropEnabled(true);

            UCrop.of(sourceUri, destinationUri)
                    .withAspectRatio(1, 1) // Square crop for avatar
                    .withMaxResultSize(800, 800)
                    .withOptions(options)
                    .start(this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start uCrop", e);
            Toast.makeText(this, "Không thể mở trình chỉnh sửa ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private Uri getSampleAvatarUri() {
        try {
            // Create bitmap from sample avatar resource
            Bitmap sampleBitmap = android.graphics.BitmapFactory.decodeResource(getResources(), R.drawable.sample_avatar);
            if (sampleBitmap != null) {
                return saveBitmapToTemp(sampleBitmap);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to create URI from sample avatar", e);
        }
        return null;
    }

    private Uri saveBitmapToTemp(Bitmap bitmap) {
        try {
            File tempFile = new File(getCacheDir(), "temp_avatar_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();

            return FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider", tempFile);
        } catch (IOException e) {
            Log.e(TAG, "Failed to save bitmap to temp file", e);
            return null;
        }
    }

    private void saveAvatar() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Người dùng chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentImageUri == null) {
            Toast.makeText(this, "Vui lòng chọn ảnh để lưu", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        Toast.makeText(this, "Đang lưu ảnh...", Toast.LENGTH_SHORT).show();

        // Upload to Firebase Storage
        String fileName = "avatars/" + user.getUid() + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference avatarRef = storage.getReference().child(fileName);

        avatarRef.putFile(currentImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get download URL
                    avatarRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        // Update Firestore with new photoUrl
                        db.collection("users").document(user.getUid())
                                .update("photoUrl", downloadUri.toString())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Ảnh đã được lưu thành công", Toast.LENGTH_SHORT).show();

                                    // Return result with the new avatar URI
                                    Intent result = new Intent();
                                    result.putExtra("avatar_uri", currentImageUri.toString());
                                    result.putExtra("photo_url", downloadUri.toString());
                                    setResult(Activity.RESULT_OK, result);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to update photoUrl in Firestore", e);
                                    Toast.makeText(this, "Lỗi cập nhật thông tin người dùng", Toast.LENGTH_SHORT).show();
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload avatar", e);
                    Toast.makeText(this, "Lỗi tải ảnh lên server", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Xử lý kết quả chọn ảnh từ gallery
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                currentImageUri = uri;
                // Show preview with circular crop using Glide
                com.bumptech.glide.Glide.with(this)
                        .load(uri)
                        .circleCrop()
                        .into(ivAvatar);
                // Automatically go to uCrop after picking image
                startUCrop(uri);
            }
            return;
        }

        // Xử lý kết quả chụp ảnh từ camera
        if (requestCode == REQUEST_CAMERA && resultCode == Activity.RESULT_OK) {
            // Ảnh đã được lưu vào file thông qua FileProvider
            if (currentImageUri != null) {
                // Hiển thị preview ảnh vừa chụp
                com.bumptech.glide.Glide.with(this)
                        .load(currentImageUri)
                        .circleCrop()
                        .into(ivAvatar);
                // Tự động chuyển sang uCrop để chỉnh sửa
                startUCrop(currentImageUri);
            } else {
                Toast.makeText(this, "Không thể lấy ảnh từ camera", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (requestCode == REQUEST_CROP_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri resultUri = UCrop.getOutput(data);
                if (resultUri != null) {
                    currentImageUri = resultUri;
                    try {
                        // Load and display the cropped image using Glide for circular crop
                        currentBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), resultUri);
                        com.bumptech.glide.Glide.with(this)
                                .load(resultUri)
                                .circleCrop()
                                .into(ivAvatar);
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to load cropped image", e);
                        com.bumptech.glide.Glide.with(this)
                                .load(resultUri)
                                .circleCrop()
                                .into(ivAvatar);
                    }
                }
            } else if (resultCode == UCrop.RESULT_ERROR) {
                Throwable cropError = UCrop.getError(data);
                Log.e(TAG, "Crop error", cropError);
                Toast.makeText(this, "Lỗi chỉnh sửa ảnh", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
