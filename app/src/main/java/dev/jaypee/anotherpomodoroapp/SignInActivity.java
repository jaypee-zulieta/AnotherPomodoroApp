package dev.jaypee.anotherpomodoroapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class SignInActivity extends AppCompatActivity {

    private static final String TAG = "SignInActivity";
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseAuth = FirebaseAuth.getInstance();

        TextInputLayout emailTextInputLayout = findViewById(R.id.til_email);
        TextInputLayout passwordTextInputLayout = findViewById(R.id.til_password);
        Button loginButton = findViewById(R.id.btn_login);
        Button googleSignInButton = findViewById(R.id.btn_google_signin);
        ProgressBar progressBar = findViewById(R.id.pb_loading);

        loginButton.setOnClickListener(v -> {
            String email = Objects.requireNonNull(emailTextInputLayout.getEditText()).getText().toString();
            String password = Objects.requireNonNull(passwordTextInputLayout.getEditText()).getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            loginButton.setEnabled(false);

            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        progressBar.setVisibility(View.GONE);
                        loginButton.setEnabled(true);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            Intent intent = new Intent(SignInActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(SignInActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        googleSignInButton.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            googleSignInButton.setEnabled(false);
            GoogleSignInLauncher googleSignInLauncher = new GoogleSignInLauncher(this)
                    .setCredentialManagerCallbackOnErrorHandler(
                            e -> {
                                progressBar.setVisibility(View.GONE);
                                googleSignInButton.setEnabled(true);
                                Log.d(TAG, Objects.requireNonNull(e.getLocalizedMessage()));
                            }
                    )
                    .setOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        googleSignInButton.setEnabled(true);
                        if (task.isSuccessful()) {
                            FirebaseUser user = task.getResult().getUser();
                            assert user != null;
                            Intent intent = new Intent(SignInActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
                        }
                    });
            googleSignInLauncher.launch();
        });

        TextView signUp = findViewById(R.id.tv_signup);
        signUp.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignUpActivity.class);
            startActivity(intent);
            finish();
        });

    }
}
