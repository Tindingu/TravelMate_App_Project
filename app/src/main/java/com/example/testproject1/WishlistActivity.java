package com.example.testproject1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class WishlistActivity extends AppCompatActivity {

    // UI
    RecyclerView rvWishlist;
    LinearLayout navHome, navBookmark, navCalendar, navNotification;
    LinearLayout layoutEmpty; // ⭐ View danh sách trống
    TextView tvItemCount;     // ⭐ Text đếm số lượng

    // Data
    PlaceAdapter adapter;
    ArrayList<PlaceModel> wishlist;

    // Firebase
    FirebaseFirestore db;
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wishlist);

        // Init Views
        rvWishlist = findViewById(R.id.rvWishlist);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvItemCount = findViewById(R.id.tvItemCount);

        // Setup RecyclerView
        rvWishlist.setLayoutManager(new LinearLayoutManager(this));
        wishlist = new ArrayList<>();

        adapter = new PlaceAdapter(wishlist,
                place -> { /* Click item -> Có thể mở chi tiết */ },
                place -> removeFromWishlist(place) // Click tim -> Xóa
        );
        rvWishlist.setAdapter(adapter);

        // Setup Navigation
        setupBottomNav();

        // Init Firebase & Load Data
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            loadData();
        }
    }

    private void loadData() {
        db.collection("users").document(uid).collection("wishlist")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    wishlist.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("name");
                        String address = doc.getString("address");
                        double rating = doc.getDouble("rating");
                        double lat = doc.getDouble("lat");
                        double lon = doc.getDouble("lon");

                        PlaceModel p = new PlaceModel(name, address, rating, lat, lon);
                        p.setFavorite(true);
                        wishlist.add(p);
                    }
                    adapter.notifyDataSetChanged();

                    // ⭐ Kiểm tra để hiện Empty State
                    checkEmptyState();
                });
    }

    private void removeFromWishlist(PlaceModel place) {
        String docId = place.getName().replaceAll("[^a-zA-Z0-9]", "_");
        db.collection("users").document(uid).collection("wishlist").document(docId)
                .delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Removed", Toast.LENGTH_SHORT).show();
                    wishlist.remove(place);
                    adapter.notifyDataSetChanged();

                    // ⭐ Kiểm tra lại sau khi xóa
                    checkEmptyState();
                });
    }

    // ⭐ HÀM KIỂM TRA DANH SÁCH TRỐNG
    private void checkEmptyState() {
        if (wishlist.isEmpty()) {
            rvWishlist.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            tvItemCount.setText("0 places saved");
        } else {
            rvWishlist.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            tvItemCount.setText(wishlist.size() + " places saved");
        }
    }

    // === LOGIC NAVIGATION ===
    private void setupBottomNav() {
        navHome = findViewById(R.id.navHome);
        navBookmark = findViewById(R.id.navBookmark);
        navCalendar = findViewById(R.id.navCalendar);
        navNotification = findViewById(R.id.navNotification);

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(WishlistActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
    }
}