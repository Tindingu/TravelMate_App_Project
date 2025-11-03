//package com.example.travelmate_app; // Thay bằng package của bạn
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.example.travelmate_app.R;
//
//public class WelcomeActivity extends AppCompatActivity {
//
//    Button btnGoToLogin, btnGoToSignUp,btnFBLogin,btnGGLogin;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_welcome);
//
//        btnGoToLogin = findViewById(R.id.btnGoToLogin);
//        btnGoToSignUp = findViewById(R.id.btnGoToSignUp);
//        btnFBLogin = findViewById(R.id.btnFacebookSignIn);
//        btnGGLogin = findViewById(R.id.btnGoogleSignIn);
//
//        btnGoToLogin.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Chuyển đến màn hình Login
//                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
//            }
//        });
//
//        btnGoToSignUp.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Chuyển đến màn hình Sign Up
//                startActivity(new Intent(WelcomeActivity.this, SignUpActivity.class));
//            }
//        });
//        btnFBLogin.setOnClickListener(v -> {
//            Intent intent = new Intent(this, LoginActivity.class);
//            startActivity(intent);
//        });
//        btnGoToLogin.setOnClickListener(v -> {
//            Intent intent = new Intent(this, LoginActivity.class);
//            startActivity(intent);
//        });
//        btnGGLogin.setOnClickListener(v -> {
//            Intent intent = new Intent(this, LoginActivity.class);
//            startActivity(intent);
//        });
//
//    }
//}
package com.example.travelmate_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import java.util.Arrays;

public class WelcomeActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private Button btnGoToLogin, btnGoToSignUp, btnGoogle, btnFacebook;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(getApplication());

        setContentView(R.layout.activity_welcome);

        btnGoToLogin = findViewById(R.id.btnGoToLogin);
        btnGoToSignUp = findViewById(R.id.btnGoToSignUp);
        btnGoogle = findViewById(R.id.btnGoogleSignIn);
        btnFacebook = findViewById(R.id.btnFacebookSignIn);
        auth = FirebaseAuth.getInstance();

        // 👉 Chuyển trang
        btnGoToLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
        btnGoToSignUp.setOnClickListener(v -> startActivity(new Intent(this, SignUpActivity.class)));

        // ✅ GOOGLE LOGIN
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        btnGoogle.setOnClickListener(v -> signInWithGoogle());

        // ✅ FACEBOOK LOGIN
        callbackManager = CallbackManager.Factory.create();
        LoginButton fbButtonHidden = new LoginButton(this);
        fbButtonHidden.setPermissions(Arrays.asList("email", "public_profile"));
        btnFacebook.setOnClickListener(v -> fbButtonHidden.performClick());

        fbButtonHidden.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Toast.makeText(WelcomeActivity.this, "Facebook login cancelled", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(WelcomeActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            try {
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        goToMain(auth.getCurrentUser());
                    } else {
                        Toast.makeText(this, "Google auth failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        goToMain(auth.getCurrentUser());
                    } else {
                        Toast.makeText(this, "Facebook auth failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void goToMain(FirebaseUser user) {
        if (user != null) {
            Toast.makeText(this, "Welcome " + user.getEmail(), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}
