package com.example.travelmate.auth;

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

import com.example.travelmate.R;
import com.example.travelmate.home.HomeActivity;
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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
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

    // UI
    private Button btnGoToLogin, btnGoToSignUp;
    private AppCompatButton btnGoogleSignIn, btnFacebookSignIn;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Google / Facebook
    private GoogleSignInClient googleClient;
    private ActivityResultLauncher<Intent> googleLauncher;
    private CallbackManager callbackManager;

    // STATE GUARD (QUAN TRỌNG)
    private boolean isSigningIn = false;
    private boolean hasNavigated = false;
    private AlertDialog loadingDialog;

    // ============================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        initFirebase();
        initViews();
        setupGoogleSignIn();
        setupFacebookSignIn();
        setupButtons();
    }

    // ============================================================
    // 🔒 AUTO LOGIN – CHỈ CHẠY 1 LẦN
    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && !hasNavigated) {
            hasNavigated = true;
            goHomeImmediately();
        }
    }

    // ============================================================
    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void initViews() {
        btnGoToLogin = findViewById(R.id.btnGoToLogin);
        btnGoToSignUp = findViewById(R.id.btnGoToSignUp);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        btnFacebookSignIn = findViewById(R.id.btnFacebookSignIn);
    }

    // ============================================================
    // GOOGLE SIGN IN
    private void setupGoogleSignIn() {
        GoogleSignInOptions gso =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();

        googleClient = GoogleSignIn.getClient(this, gso);

        googleLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleGoogleResult
        );

        btnGoogleSignIn.setOnClickListener(v -> startGoogleSignIn());
    }

    private void startGoogleSignIn() {
        if (isSigningIn) return;

        isSigningIn = true;
        btnGoogleSignIn.setEnabled(false);

        googleClient.signOut().addOnCompleteListener(task -> {
            googleLauncher.launch(googleClient.getSignInIntent());
        });
    }

    private void handleGoogleResult(ActivityResult result) {
        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
            resetSignInState();
            return;
        }

        Task<GoogleSignInAccount> task =
                GoogleSignIn.getSignedInAccountFromIntent(result.getData());

        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            resetSignInState();
            Toast.makeText(this, "Đăng nhập Google thất bại", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential =
                GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        saveUserToFirestore(user);
                        goToHomeWithLoading();
                    } else {
                        resetSignInState();
                        Toast.makeText(this, "Xác thực Firebase thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ============================================================
    // FACEBOOK SIGN IN
    private void setupFacebookSignIn() {
        callbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult result) {
                        firebaseAuthWithFacebook(result.getAccessToken());
                    }

                    @Override
                    public void onCancel() {}

                    @Override
                    public void onError(@NonNull FacebookException e) {
                        Toast.makeText(WelcomeActivity.this,
                                "Facebook lỗi: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        btnFacebookSignIn.setOnClickListener(v ->
                LoginManager.getInstance()
                        .logInWithReadPermissions(
                                this,
                                Arrays.asList("email", "public_profile")
                        ));
    }

    private void firebaseAuthWithFacebook(AccessToken token) {
        AuthCredential credential =
                FacebookAuthProvider.getCredential(token.getToken());

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        saveUserToFirestore(user);
                        goToHomeWithLoading();
                    } else {
                        LoginManager.getInstance().logOut();
                        Toast.makeText(this,
                                "Firebase Facebook thất bại",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (callbackManager != null) {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    // ============================================================
    // FIRESTORE USER
    private void saveUserToFirestore(FirebaseUser user) {
        if (user == null) return;

        Map<String, Object> map = new HashMap<>();
        map.put("uid", user.getUid());
        map.put("name", user.getDisplayName());
        map.put("email", user.getEmail());
        map.put("photoUrl",
                user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
        map.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users")
                .document(user.getUid())
                .set(map, SetOptions.merge());
    }

    // ============================================================
    // NAVIGATION (CHỐNG DOUBLE)
    private void goHomeImmediately() {
        startActivity(new Intent(this, HomeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    private void goToHomeWithLoading() {
        if (hasNavigated) return;
        hasNavigated = true;

        if (loadingDialog == null) {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            View v = getLayoutInflater().inflate(R.layout.dialog_loading, null);
            b.setView(v);
            b.setCancelable(false);
            loadingDialog = b.create();
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow()
                        .setBackgroundDrawableResource(android.R.color.transparent);
            }
        }

        loadingDialog.show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing()) {
                loadingDialog.dismiss();
                goHomeImmediately();
            }
        }, 800);
    }

    private void resetSignInState() {
        isSigningIn = false;
        btnGoogleSignIn.setEnabled(true);
    }

    // ============================================================
    private void setupButtons() {
        btnGoToLogin.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));

        btnGoToSignUp.setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
}
