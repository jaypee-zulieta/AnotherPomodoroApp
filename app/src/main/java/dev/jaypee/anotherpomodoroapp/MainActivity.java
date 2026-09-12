package dev.jaypee.anotherpomodoroapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already signed in
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        InitializeSignInWithGoogleButton(R.id.sign_in_with_google_button);
        Button signInButton = findViewById(R.id.btn_sign_in);
        Button signUpButton = findViewById(R.id.btn_sign_up);

        signInButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignInActivity.class);
            startActivity(intent);
            finish();
        });

        signUpButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignUpActivity.class);
            startActivity(intent);
            finish();
        });

    }

    // This method was made to declutter the onCreate method.
    private void InitializeSignInWithGoogleButton(@IdRes int id)  {
        Button button = findViewById(id);
        ProgressBar progressBar = findViewById(R.id.pb_loading);
        button.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            button.setEnabled(false);
            GoogleSignInLauncher googleSignInLauncher = new GoogleSignInLauncher(this)
                    .setCredentialManagerCallbackOnErrorHandler(e -> {
                            progressBar.setVisibility(View.GONE);
                            button.setEnabled(true);
                            Log.e("TAG", Objects.requireNonNull(e.getLocalizedMessage()));
                        }
                    )
                    .setOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        button.setEnabled(true);
                        if(task.isSuccessful()) {
                            AuthResult authResult = task.getResult();
                            FirebaseUser user = authResult.getUser();
                            assert user != null;
                            String displayName = user.getDisplayName();
                            Toast.makeText(this, "Welcome " + displayName, Toast.LENGTH_SHORT).show();
                            
                            startActivity(new Intent(this, HomeActivity.class));
                            finish();
                        }
                        else Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                    });

            googleSignInLauncher.launch();
        });
    }

}