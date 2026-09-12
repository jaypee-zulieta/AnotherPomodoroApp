package dev.jaypee.anotherpomodoroapp;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser user = firebaseAuth.getCurrentUser();

        ImageView profileImageView = findViewById(R.id.iv_profile_picture);
        TextView initialTextView = findViewById(R.id.tv_profile_initial);
        ProgressBar progressBar = findViewById(R.id.pb_profile_loading);
        TextView nameTextView = findViewById(R.id.tv_profile_name);
        TextView emailTextView = findViewById(R.id.tv_profile_email);
        Button backHomeButton = findViewById(R.id.btn_back_home);
        Button logoutButton = findViewById(R.id.btn_logout);

        if (user != null) {
            String name = user.getDisplayName();
            String email = user.getEmail();
            Uri photoUrl = user.getPhotoUrl();

            nameTextView.setText(name != null ? name : "No Name");
            emailTextView.setText(email != null ? email : "No Email");

            if (photoUrl != null) {
                initialTextView.setVisibility(View.GONE);
                profileImageView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(photoUrl)
                        .circleCrop()
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                progressBar.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                progressBar.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(profileImageView);
            } else {
                profileImageView.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                initialTextView.setVisibility(View.VISIBLE);
                if (name != null && !name.isEmpty()) {
                    initialTextView.setText(String.valueOf(name.charAt(0)).toUpperCase());
                } else {
                    initialTextView.setText("?");
                }
            }
        } else {
            // If no user is signed in, go back to sign in
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        }

        backHomeButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        logoutButton.setOnClickListener(v -> {
            firebaseAuth.signOut();
            Intent intent = new Intent(ProfileActivity.this, SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}
