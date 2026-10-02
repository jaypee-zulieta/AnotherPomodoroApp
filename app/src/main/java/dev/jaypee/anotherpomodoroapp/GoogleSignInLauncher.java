package dev.jaypee.anotherpomodoroapp;

import static com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL;

import android.app.Activity;
import android.content.Context;
import android.content.MutableContextWrapper;
import androidx.credentials.exceptions.GetCredentialException;
import android.os.Bundle;
import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.NoCredentialException;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import java.security.SecureRandom;
import android.util.Base64;
import java.util.concurrent.Executors;

public class GoogleSignInLauncher {

    private final Activity activity;
    private final MutableContextWrapper contextWrapper;
    private final FirebaseAuth firebaseAuth;
    private final CredentialManager credentialManager;

    public GoogleSignInLauncher(Activity activity) {
        this.activity = activity;
        this.contextWrapper = new MutableContextWrapper(activity);
        firebaseAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this.contextWrapper);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(
                this.activity,
                onCompleteListener
        );
    }

    private void handleSignIn(Credential credential) throws NoCredentialException {
        if(credential instanceof CustomCredential && credential.getType().equals(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
            CustomCredential customCredential = (CustomCredential) credential;
            Bundle credentialData = customCredential.getData();
            GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credentialData);
            firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());
        }
        else throw new NoCredentialException("Credential is not of type GoogleId");
    }

    public void launch() {
        GetSignInWithGoogleOption signInWithGoogleOption = new GetSignInWithGoogleOption
                .Builder(contextWrapper.getString(R.string.default_web_client_id))
                .setNonce(generateSecureNonce())
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build();

        credentialManager.getCredentialAsync(
                contextWrapper,
                request,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(GetCredentialResponse getCredentialResponse) {
                        try {
                            handleSignIn(getCredentialResponse.getCredential());
                        }
                        catch (NoCredentialException e) {
                            System.err.println(e.getLocalizedMessage());
                        }
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                       credentialManagerCallbackOnErrorHandler.handle(e);
                    }
                }
        );
    }

    private String generateSecureNonce() {
        byte[] randomBytes = new byte[12];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.encodeToString(randomBytes, Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
    }

    public GoogleSignInLauncher setCredentialManagerCallbackOnErrorHandler(CredentialManagerCallbackOnErrorHandler handler) {
        this.credentialManagerCallbackOnErrorHandler = handler;
        return this;
    }

    public GoogleSignInLauncher setOnCompleteListener(OnCompleteListener<AuthResult> onCompleteListener) {
        this.onCompleteListener = onCompleteListener;
        return this;
    }

    public interface CredentialManagerCallbackOnErrorHandler {
        void handle(GetCredentialException exception);
    }

    // If client does not set this, then it will do nothing by default
    private CredentialManagerCallbackOnErrorHandler credentialManagerCallbackOnErrorHandler = e -> {};

    // If client does not set this, then it will do nothing by default
    private OnCompleteListener<AuthResult> onCompleteListener = t -> {};
}
