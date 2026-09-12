package dev.jaypee.anotherpomodoroapp;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputLayout;

public class PasswordMatchTextWatcher implements TextWatcher {

    private String passwordToMatch;
    private TextInputLayout confirmPasswordTextInputLayout;

    public PasswordMatchTextWatcher(
            TextInputLayout passwordTextInputLayout,
            TextInputLayout confirmPasswordTextInputLayout
    ) {
        EditText passwordEditText = passwordTextInputLayout.getEditText();
        assert passwordEditText != null;
        this.passwordToMatch = passwordEditText.getText().toString();
        this.confirmPasswordTextInputLayout = confirmPasswordTextInputLayout;
    }
    @Override
    public void afterTextChanged(Editable editable) {
        // Do Nothing
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        // Do Nothing
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        if(!charSequence.equals(this.passwordToMatch))
            this.confirmPasswordTextInputLayout.setError("Passwords must match");
        else this.confirmPasswordTextInputLayout.setError(null);
    }
}
