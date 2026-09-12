package dev.jaypee.anotherpomodoroapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class SignUpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView signIn = findViewById(R.id.tv_login);
        signIn.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignInActivity.class);
            startActivity(intent);
            finish();
        });

        Button signUpWithGoogleButton = findViewById(R.id.btn_google_signup);

        signUpWithGoogleButton.setOnClickListener(v -> {
            GoogleSignInLauncher googleSignInLauncher = new GoogleSignInLauncher(this)
                    .setCredentialManagerCallbackOnErrorHandler(
                           e -> Log.e("Something", Objects.requireNonNull(e.getLocalizedMessage()))
                    )
                    .setOnCompleteListener(task -> {
                        if(task.isSuccessful()) {
                            FirebaseUser user = task.getResult().getUser();
                            assert user != null;
                            String displayName = user.getDisplayName();
                            Toast.makeText(this, "Welcome " + displayName, Toast.LENGTH_SHORT).show();
                        }
                        else Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
                    });
            googleSignInLauncher.launch();
        });
    }
}