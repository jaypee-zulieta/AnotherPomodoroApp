package dev.jaypee.anotherpomodoroapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button siginInWithGoogleButton = findViewById(R.id.sign_in_with_google_button);

        siginInWithGoogleButton.setOnClickListener(v -> {
            GoogleSignInLauncher googleSignInLauncher = new GoogleSignInLauncher(this)
                    .setCredentialManagerCallbackOnErrorHandler(e ->
                        Log.e("TAG", Objects.requireNonNull(e.getLocalizedMessage()))
                    )
                    .setOnCompleteListener(task -> {
                        if(task.isSuccessful()) {
                            AuthResult authResult = task.getResult();
                            FirebaseUser user = authResult.getUser();
                            assert user != null;
                            String displayName = user.getDisplayName();
                            Toast.makeText(this, "Welcome " + displayName, Toast.LENGTH_SHORT).show();
                        }
                        else Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                    });

            googleSignInLauncher.launch();
        });
    }
}