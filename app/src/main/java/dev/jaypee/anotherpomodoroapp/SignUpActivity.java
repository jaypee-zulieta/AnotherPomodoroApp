package dev.jaypee.anotherpomodoroapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Objects;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "AnotherPomodoroApp";
    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseAuth = FirebaseAuth.getInstance();

        TextInputLayout fullNameTextInputLayout = findViewById(R.id.til_name);
        TextInputLayout emailTextInputLayout = findViewById(R.id.til_email);
        TextInputLayout passwordTextInputLayout = findViewById(R.id.til_password);
        TextInputLayout confirmPasswordTextInputLayout = findViewById(R.id.til_confirm_password);
        TextInputLayout profilePictureTextInputLayout = findViewById(R.id.til_profile_picture);

        assert fullNameTextInputLayout.getEditText() != null;
        assert emailTextInputLayout.getEditText() != null;
        assert passwordTextInputLayout.getEditText() != null;
        assert confirmPasswordTextInputLayout.getEditText() != null;
        assert profilePictureTextInputLayout.getEditText() != null;

        EditText passwordEditText = passwordTextInputLayout.getEditText();

        passwordEditText.addTextChangedListener(new PasswordTextWatcher(passwordTextInputLayout));

        Button signUpButton = findViewById(R.id.btn_register);
        ProgressBar progressBar = findViewById(R.id.pb_loading);

        signUpButton.setOnClickListener(v -> {
            String fullName = fullNameTextInputLayout
                    .getEditText()
                    .getText()
                    .toString();

            String email = emailTextInputLayout
                    .getEditText()
                    .getText()
                    .toString();

            String password = passwordTextInputLayout
                    .getEditText()
                    .getText()
                    .toString();

            String confirmPassword = confirmPasswordTextInputLayout
                    .getEditText()
                    .getText()
                    .toString();

            String profilePictureUrl = profilePictureTextInputLayout
                    .getEditText()
                    .getText()
                    .toString();

            if(!password.equals(confirmPassword)) {
                confirmPasswordTextInputLayout.setError("Passwords must match.");
                return;
            } else confirmPasswordTextInputLayout.setError(null);

            progressBar.setVisibility(View.VISIBLE);
            signUpButton.setEnabled(false);

            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()) {
                            FirebaseUser user = task.getResult().getUser();
                            UserProfileChangeRequest.Builder profileUpdatesBuilder = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName);

                            if (!profilePictureUrl.isEmpty()) {
                                profileUpdatesBuilder.setPhotoUri(Uri.parse(profilePictureUrl));
                            }

                            UserProfileChangeRequest profileUpdates = profileUpdatesBuilder.build();

                            assert user != null;
                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(updateTask -> {
                                        progressBar.setVisibility(View.GONE);
                                        signUpButton.setEnabled(true);
                                        if(updateTask.isSuccessful()) {
                                            FirebaseUser updatedUser = task.getResult().getUser();
                                            assert updatedUser != null;
                                            String displayName = updatedUser.getDisplayName();
                                            Log.d(TAG,"User created and display name updated to: " + displayName);
                                            
                                            // Redirect to ProfileActivity
                                            Intent intent = new Intent(SignUpActivity.this, ProfileActivity.class);
                                            startActivity(intent);
                                            finish();
                                        }
                                    });
                        }
                        else {
                            progressBar.setVisibility(View.GONE);
                            signUpButton.setEnabled(true);
                            Exception e = task.getException();
                            if(e instanceof FirebaseAuthUserCollisionException)
                                emailTextInputLayout.setError("Email is already taken");
                            Log.d(TAG, "something went wrong", e);
                        }
                    });
        });



        TextView signIn = findViewById(R.id.tv_login);
        signIn.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignInActivity.class);
            startActivity(intent);
            finish();
        });

        Button signUpWithGoogleButton = findViewById(R.id.btn_google_signup);

        signUpWithGoogleButton.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            signUpWithGoogleButton.setEnabled(false);
            GoogleSignInLauncher googleSignInLauncher = new GoogleSignInLauncher(this)
                    .setCredentialManagerCallbackOnErrorHandler(
                           e -> {
                               progressBar.setVisibility(View.GONE);
                               signUpWithGoogleButton.setEnabled(true);
                               Log.d("Something", Objects.requireNonNull(e.getLocalizedMessage()));
                           }
                    )
                    .setOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        signUpWithGoogleButton.setEnabled(true);
                        if(task.isSuccessful()) {
                            FirebaseUser user = task.getResult().getUser();
                            assert user != null;
                            String displayName = user.getDisplayName();
                            Toast.makeText(this, "Welcome " + displayName, Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(this, ProfileActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        else Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
                    });
            googleSignInLauncher.launch();
        });
    }
}