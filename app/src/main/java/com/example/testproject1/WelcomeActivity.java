package com.example.testproject1;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class WelcomeActivity extends AppCompatActivity {

    private static final String TAG = "WelcomeActivity";

    private Button btnGoToLogin, btnGoToSignUp;
    private AppCompatButton btnGoogleSignIn, btnFacebookSignIn;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Google & Facebook
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private CallbackManager mCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        initFirebase();
        initViews();
        setupGoogleSignIn();
        setupFacebookSignIn();
        setupButtonListeners();
    }

    // ============================================================
    // 1️⃣ Khởi tạo Firebase
    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    // ============================================================
    // 2️⃣ Ánh xạ view
    private void initViews() {
        btnGoToLogin = findViewById(R.id.btnGoToLogin);
        btnGoToSignUp = findViewById(R.id.btnGoToSignUp);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        btnFacebookSignIn = findViewById(R.id.btnFacebookSignIn);
    }

    // ============================================================
    // 3️⃣ Cấu hình Google Sign-In
    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleGoogleSignInResult
        );

        btnGoogleSignIn.setOnClickListener(v -> startGoogleSignIn());
    }

    private void startGoogleSignIn() {
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void handleGoogleSignInResult(ActivityResult result) {
        if (result.getResultCode() != RESULT_OK) return;

        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            Toast.makeText(this, "Đăng nhập Google thành công!", Toast.LENGTH_SHORT).show();
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            Log.w(TAG, "Google sign in failed", e);
            Toast.makeText(this, "Đăng nhập Google thất bại.", Toast.LENGTH_SHORT).show();
        }
    }

    // ============================================================
    // 4️⃣ Cấu hình Facebook Sign-In
    private void setupFacebookSignIn() {
        mCallbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Toast.makeText(WelcomeActivity.this, "Facebook đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                firebaseAuthWithFacebook(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Toast.makeText(WelcomeActivity.this, "Đăng nhập Facebook đã bị hủy.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull FacebookException e) {
                Log.w(TAG, "Facebook onError", e);
                Toast.makeText(WelcomeActivity.this, "Lỗi đăng nhập Facebook: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        btnFacebookSignIn.setOnClickListener(v -> {
            LoginManager.getInstance().logOut();
            LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    // ============================================================
    // 5️⃣ Firebase xác thực Google
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        saveUserToFirestore(user);
                        goToHomeActivity();
                    } else {
                        Toast.makeText(this, "Xác thực Firebase thất bại.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ============================================================
    // 6️⃣ Firebase xác thực Facebook
    private void firebaseAuthWithFacebook(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        saveUserToFirestore(user);
                        goToHomeActivity();
                    } else {
                        LoginManager.getInstance().logOut();
                        Toast.makeText(this, "Xác thực Firebase (Facebook) thất bại.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ============================================================
    // 7️⃣ Lưu user vào Firestore
    private void saveUserToFirestore(FirebaseUser user) {
        if (user == null) return;

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("name", user.getDisplayName());
        userData.put("email", user.getEmail());
        userData.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
        userData.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users").document(user.getUid())
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User saved to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save user", e));
    }

    // ============================================================
    // 8️⃣ Gán sự kiện nút chuyển trang Login / SignUp
    private void setupButtonListeners() {
        btnGoToLogin.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));

        btnGoToSignUp.setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));
    }

    // ============================================================
    // 9️⃣ Chuyển sang HomeActivity
    private void goToHomeActivity() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        builder.setView(view);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            dialog.dismiss();
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, 2000);
    }
}
