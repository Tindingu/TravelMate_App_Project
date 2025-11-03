package com.example.testproject1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton; // Dùng AppCompatButton

import com.facebook.CallbackManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

// === IMPORT CỦA FACEBOOK ===
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import java.util.Arrays;

public class WelcomeActivity extends AppCompatActivity {

    Button btnGoToLogin, btnGoToSignUp;
    AppCompatButton btnGoogleSignIn; // Nút Google
    AppCompatButton btnFacebookSignIn;

    // Khai báo Firebase Auth
    private FirebaseAuth mAuth;

    // Khai báo Google Sign-In
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private CallbackManager mCallbackManager;
    private static final String TAG = "WelcomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Ánh xạ các nút
        btnGoToLogin = findViewById(R.id.btnGoToLogin);
        btnGoToSignUp = findViewById(R.id.btnGoToSignUp);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn); // ID từ layout của bạn
        btnFacebookSignIn = findViewById(R.id.btnFacebookSignIn);
        // --- CẤU HÌNH GOOGLE SIGN-IN ---
        // 1. Cấu hình Google Sign-In Options
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Lấy token
                .requestEmail()
                .build();

        // 2. Tạo GoogleSignInClient
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 3. Khởi tạo ActivityResultLauncher
        // Đây là cách MỚI để nhận kết quả trả về từ cửa sổ Google
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                            try {
                                // Đăng nhập Google thành công
                                GoogleSignInAccount account = task.getResult(ApiException.class);
                                Toast.makeText(WelcomeActivity.this, "Đăng nhập Google thành công!", Toast.LENGTH_SHORT).show();

                                // Gọi hàm xác thực với Firebase
                                firebaseAuthWithGoogle(account.getIdToken());
                            } catch (ApiException e) {
                                // Đăng nhập Google thất bại
                                Log.w(TAG, "Google sign in failed", e);
                                Toast.makeText(WelcomeActivity.this, "Đăng nhập Google thất bại.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
        mCallbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(mCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Log.d(TAG, "Facebook onSuccess:" + loginResult);
                        Toast.makeText(WelcomeActivity.this, "Facebook đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        firebaseAuthWithFacebook(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "Facebook onCancel.");
                        Toast.makeText(WelcomeActivity.this, "Đăng nhập Facebook đã bị hủy.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull FacebookException e) {
                        Log.w(TAG, "Facebook onError", e);
                        Toast.makeText(WelcomeActivity.this, "Lỗi đăng nhập Facebook: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
        // --- GÁN SỰ KIỆN CLICK ---

        // Click nút Google
        btnGoogleSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });
        btnFacebookSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Bắt đầu luồng đăng nhập Facebook
                LoginManager.getInstance().logOut();
                LoginManager.getInstance()
                        .setAuthType("reauthenticate");
                LoginManager.getInstance().logInWithReadPermissions(WelcomeActivity.this, Arrays.asList("email", "public_profile"));
            }
        });

        // Click các nút cũ (Login/Sign Up)
        btnGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
            }
        });

        btnGoToSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeActivity.this, SignUpActivity.class));
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Chuyển kết quả về cho Facebook CallbackManager
        // Nó sẽ không ảnh hưởng đến googleSignInLauncher
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }
    // Hàm gọi cửa sổ pop-up đăng nhập của Google
    private void signInWithGoogle() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            }
        });
    }

    // Hàm xác thực với Firebase sau khi có token từ Google
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Đăng nhập Firebase thành công
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(WelcomeActivity.this, "Đăng nhập Firebase thành công: " + user.getDisplayName(), Toast.LENGTH_SHORT).show();

                            // Chuyển đến HomeActivity
                            goToHomeActivity();
                        } else {
                            // Đăng nhập Firebase thất bại
                            Toast.makeText(WelcomeActivity.this, "Xác thực Firebase thất bại.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    private void firebaseAuthWithFacebook(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(WelcomeActivity.this, "Đăng nhập Firebase (FB) thành công: " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
                            goToHomeActivity();
                        } else {
                            // Nếu lỗi, đăng xuất khỏi Facebook để người dùng có thể thử lại
                            LoginManager.getInstance().logOut();
                            Toast.makeText(WelcomeActivity.this, "Xác thực Firebase (FB) thất bại: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
    // (Khuyên dùng) Kiểm tra xem user đã đăng nhập chưa khi khởi động app
//    @Override
//    protected void onStart() {
//        super.onStart();
//        FirebaseUser currentUser = mAuth.getCurrentUser();
//        if(currentUser != null){
//            goToHomeActivity();
//        }
//    }
//
    // Hàm chuyển sang HomeActivity và xóa các activity cũ
    private void goToHomeActivity() {
        Intent intent = new Intent(WelcomeActivity.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
