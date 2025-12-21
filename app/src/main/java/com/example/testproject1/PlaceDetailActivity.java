package com.example.testproject1;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class PlaceDetailActivity extends AppCompatActivity
        implements OnMapReadyCallback{

    private static final int PICK_IMAGE_REQUEST = 1001;

    // UI
    TextView tvName, tvAddress, tvRating;
    ImageView ivPlaceImage, btnSendComment, btnAddImage, btnBack;
    EditText etComment;
    RatingBar ratingUser;

    // Nút thêm vào lịch trình

    RecyclerView rvComments, rvPreviewImages;

    // DATA
    ArrayList<CommentModel> comments = new ArrayList<>();
    CommentAdapter adapter;

    ArrayList<Uri> selectedImages = new ArrayList<>();
    PreviewImageAdapter previewAdapter;

    // Danh sách chuyến đi để chọn
    List<TripModel> myTrips = new ArrayList<>();
    PlaceModel currentPlace;

    // FIREBASE
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseAuth auth = FirebaseAuth.getInstance();

    // PLACE INFO
    String placeId, name, address;
    double rating;
    private GoogleMap mMap;
    private double lat, lon;
    private String placeName;

    // Interface callback để nhận kết quả kiểm tra trùng giờ
    interface OnCheckCallback {
        void onResult(boolean isDuplicate);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_detail);

        // Nhận dữ liệu từ Intent
        placeId = getIntent().getStringExtra("id");
        name    = getIntent().getStringExtra("name");
        address = getIntent().getStringExtra("address");
        rating  = getIntent().getDoubleExtra("rating", 0);
        lat     = getIntent().getDoubleExtra("lat", 0);
        lon     = getIntent().getDoubleExtra("lon", 0);

        currentPlace = new PlaceModel(name, address, rating, lat, lon);
        currentPlace.setId(placeId);


        placeName = getIntent().getStringExtra("name");

        initViews();
        setupPreviewImageList();
        loadComments();
        calculateRatingAverage();

        btnAddImage.setOnClickListener(v -> openGallery());
        btnSendComment.setOnClickListener(v -> sendComment());
        btnBack.setOnClickListener(v -> finish());
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.placeMap);

        if (mapFragment != null) {
            mapFragment.getMapAsync((OnMapReadyCallback) this);
        }



    }

    private void initViews() {
//        ivPlaceImage = findViewById(R.id.ivPlaceImage);
        tvName = findViewById(R.id.tvPlaceName);
        tvAddress = findViewById(R.id.tvPlaceAddress);
        tvRating = findViewById(R.id.tvPlaceRating);
        etComment = findViewById(R.id.etComment);
        ratingUser = findViewById(R.id.ratingUser);
        btnSendComment = findViewById(R.id.btnSendComment);
        btnAddImage = findViewById(R.id.btnAddImage);
        btnBack = findViewById(R.id.ivBack);
        rvComments = findViewById(R.id.rvComments);
        rvPreviewImages = findViewById(R.id.rvPreviewImages);

        tvName.setText(name);
        tvAddress.setText(address);
        tvRating.setText("⭐ " + rating);

//        Glide.with(this).load(R.drawable.sample_place).into(ivPlaceImage);

        adapter = new CommentAdapter(comments, placeId);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(adapter);
    }

    // ============================================================
    // LOGIC THÊM VÀO LỊCH TRÌNH (CÓ KIỂM TRA TRÙNG GIỜ)
    // ============================================================
    private void showAddToTripDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_to_trip, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        Spinner spTrip = view.findViewById(R.id.spTrip);
        EditText etDate = view.findViewById(R.id.etVisitDate);
        EditText etStartTime = view.findViewById(R.id.etStartTime);
        EditText etEndTime = view.findViewById(R.id.etEndTime);
        EditText etNote = view.findViewById(R.id.etNote);
        Button btnConfirm = view.findViewById(R.id.btnConfirmAdd);

        // Load chuyến đi
        List<String> tripNames = new ArrayList<>();
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tripNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTrip.setAdapter(spinnerAdapter);

        String uid = auth.getUid();
        db.collection("trips")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    myTrips.clear();
                    tripNames.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        TripModel trip = doc.toObject(TripModel.class);
                        myTrips.add(trip);
                        tripNames.add(trip.getName());
                    }
                    spinnerAdapter.notifyDataSetChanged();
                    if (myTrips.isEmpty()) {
                        Toast.makeText(this, "Bạn chưa có chuyến đi nào. Hãy tạo mới!", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    }
                });

        // Chọn Ngày
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view1, year, month, dayOfMonth) -> {
                String dateStr = dayOfMonth + "/" + (month + 1) + "/" + year;
                etDate.setText(dateStr);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Chọn Giờ
        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime));
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime));

        // XỬ LÝ LƯU (CÓ CHECK TRÙNG)
        btnConfirm.setOnClickListener(v -> {
            int selectedIndex = spTrip.getSelectedItemPosition();
            if (selectedIndex < 0) return;

            TripModel selectedTrip = myTrips.get(selectedIndex);
            String date = etDate.getText().toString();
            String start = etStartTime.getText().toString();
            String end = etEndTime.getText().toString();
            String note = etNote.getText().toString();

            // 1. Validate nhập liệu
            if (date.isEmpty() || start.isEmpty() || end.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ ngày giờ!", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Validate Start < End
            if (convertTimeToMinutes(end) <= convertTimeToMinutes(start)) {
                Toast.makeText(this, "Giờ kết thúc phải sau giờ bắt đầu!", Toast.LENGTH_SHORT).show();
                return;
            }

            // 3. GỌI HÀM KIỂM TRA TRÙNG GIỜ
            checkDuplicateTime(selectedTrip.getTripId(), date, start, end, isDuplicate -> {
                if (isDuplicate) {
                    Toast.makeText(this, "Lỗi: Khoảng thời gian này đã bị trùng!", Toast.LENGTH_LONG).show();
                } else {
                    // Nếu không trùng -> Tiến hành lưu
                    saveToSchedule(selectedTrip.getTripId(), date, start, end, note, dialog);
                }
            });
        });

        dialog.show();
    }

    // ⭐ Hàm kiểm tra trùng giờ (MỚI THÊM)
    private void checkDuplicateTime(String tripId, String date, String newStart, String newEnd, OnCheckCallback callback) {
        int newStartMin = convertTimeToMinutes(newStart);
        int newEndMin = convertTimeToMinutes(newEnd);

        db.collection("schedule")
                .whereEqualTo("tripId", tripId)
                .whereEqualTo("visitDate", date)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean isDup = false;
                    for (DocumentSnapshot doc : querySnapshot) {
                        String dbStart = doc.getString("visitTime");
                        String dbEnd = doc.getString("endTime");

                        if (dbStart != null && dbEnd != null) {
                            int dbStartMin = convertTimeToMinutes(dbStart);
                            int dbEndMin = convertTimeToMinutes(dbEnd);

                            // Logic trùng: (StartMới < EndCũ) VÀ (StartCũ < EndMới)
                            if (newStartMin < dbEndMin && dbStartMin < newEndMin) {
                                isDup = true;
                                break;
                            }
                        }
                    }
                    callback.onResult(isDup);
                })
                .addOnFailureListener(e -> {
                    // Nếu lỗi mạng, tạm thời cho qua
                    callback.onResult(false);
                });
    }

    private void saveToSchedule(String tripId, String date, String startTime, String endTime, String note, AlertDialog dialog) {
        String itemId = db.collection("schedule").document().getId();
        ScheduleItemModel item = new ScheduleItemModel(itemId, tripId, currentPlace, date, startTime, endTime);
        item.setNote(note);

        db.collection("schedule").document(itemId).set(item)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã thêm vào lịch trình!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ============================================================
    // CÁC HÀM TIỆN ÍCH
    // ============================================================
    private void showTimePicker(EditText et) {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            et.setText(String.format("%02d:%02d", hourOfDay, minute));
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    private int convertTimeToMinutes(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return 0;
        }
    }

    // ============================================================
    // CÁC HÀM CŨ KHÔNG ĐỔI
    // ============================================================
    private void setupPreviewImageList() {
        rvPreviewImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
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
                for (int i = 0; i < count; i++) selectedImages.add(data.getClipData().getItemAt(i).getUri());
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
            while ((len = input.read(buffer)) > 0) out.write(buffer, 0, len);
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
                    value.getDocuments().forEach(d -> {
                        CommentModel c = d.toObject(CommentModel.class);
                        if (c == null) return;

                        c.setId(d.getId());   // cần thiết cho LIKE/DISLIKE
                        comments.add(c);
                    });
                    adapter.notifyDataSetChanged();
                    calculateRatingAverage();
                });
    }

    private void sendComment() {
        String msg = etComment.getText().toString().trim();
        if (msg.isEmpty()) {
            Toast.makeText(this, "Bạn chưa nhập nội dung", Toast.LENGTH_SHORT).show();
            return;
        }
        int ratingValue = (int) ratingUser.getRating();
        String username = auth.getCurrentUser() != null ? auth.getCurrentUser().getDisplayName() : "Ẩn danh";
        String uid = auth.getUid();
        long time = System.currentTimeMillis();

        if (selectedImages.isEmpty()) {
            CommentModel c = new CommentModel(username, msg, time, ratingValue, new ArrayList<>());
            c.setUid(uid);
            pushComment(c);
        } else {
            uploadImagesThenSend(username, uid, msg, time, ratingValue);
        }
    }

//    private void uploadImagesThenSend(String username, String uid, String msg, long time, int ratingValue) {
//        ArrayList<String> uploadedUrls = new ArrayList<>();
//        for (Uri uri : selectedImages) {
//            File file = uriToFile(uri);
//            if (file == null) {
//                Toast.makeText(this, "Không đọc được file", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            String filename = UUID.randomUUID() + ".jpg";
//            PreSignService.getPreSignedUrl("http://10.0.2.2:3000/presign", filename, new PreSignService.Callback() {
//                @Override
//                public void onSuccess(String uploadUrl, String finalUrl) {
//                    S3Uploader.uploadImage(file, uploadUrl, finalUrl, new S3Uploader.UploadCallback() {
//                        @Override
//                        public void onUploaded(String url) {
//                            uploadedUrls.add(url);
//                            if (uploadedUrls.size() == selectedImages.size()) {
//                                CommentModel c = new CommentModel(username, msg, time, ratingValue, uploadedUrls);
//                                c.setUid(uid);
//                                pushComment(c);
//                            }
//                        }
//                        @Override
//                        public void onError(String err) {
//                            Toast.makeText(PlaceDetailActivity.this, "Upload lỗi: " + err, Toast.LENGTH_SHORT).show();
//                        }
//                    });
//                }
//                @Override
//                public void onError(String err) {
//                    Toast.makeText(PlaceDetailActivity.this, "Không lấy được presigned URL: " + err, Toast.LENGTH_SHORT).show();
//                }
//            });
//        }
//    }
private void uploadImagesThenSend(
        String username,
        String uid,
        String msg,
        long time,
        int ratingValue
) {
    ArrayList<String> uploadedUrls = new ArrayList<>();
    FirebaseStorage storage = FirebaseStorage.getInstance();

    for (Uri uri : selectedImages) {

        String filename = UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = storage
                .getReference()
                .child("comments")
                .child(placeId)
                .child(filename);

        ref.putFile(uri)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl().addOnSuccessListener(downloadUri -> {

                            uploadedUrls.add(downloadUri.toString());

                            // ✅ Khi upload đủ ảnh → gửi comment
                            if (uploadedUrls.size() == selectedImages.size()) {
                                CommentModel c = new CommentModel(
                                        username,
                                        msg,
                                        time,
                                        ratingValue,
                                        uploadedUrls
                                );
                                c.setUid(uid);
                                pushComment(c);
                            }
                        })
                )
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Upload ảnh thất bại: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }
}


    private void pushComment(CommentModel c) {
        db.collection("places").document(placeId).collection("comments").add(c)
                .addOnSuccessListener(a -> {
                    etComment.setText("");
                    ratingUser.setRating(0);
                    selectedImages.clear();
                    rvPreviewImages.setVisibility(View.GONE);
                    previewAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Đã gửi bình luận!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lưu thất bại!", Toast.LENGTH_SHORT).show());
    }

    private void calculateRatingAverage() {
        db.collection("places").document(placeId).collection("comments").get()
                .addOnSuccessListener(snap -> {
                    int total = 0, count = 0;
                    for (var doc : snap.getDocuments()) {
                        Long r = doc.getLong("rating");
                        if (r != null) {
                            total += r;
                            count++;
                        }
                    }
                    if (count == 0) {
                        tvRating.setText("⭐ Chưa có đánh giá");
                        return;
                    }
                    double avg = Math.round((total * 1.0 / count) * 10) / 10.0;
                    tvRating.setText("⭐ " + avg);
                    db.collection("places").document(placeId).update("ratingAvg", avg);
                });
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng placeLatLng = new LatLng(lat, lon);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(placeLatLng, 16f));

        mMap.addMarker(new MarkerOptions()
                .position(placeLatLng)
                .title(placeName));

        // Tắt UI không cần thiết cho map nhỏ
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setScrollGesturesEnabled(false);
        mMap.getUiSettings().setZoomGesturesEnabled(false);
    }

}