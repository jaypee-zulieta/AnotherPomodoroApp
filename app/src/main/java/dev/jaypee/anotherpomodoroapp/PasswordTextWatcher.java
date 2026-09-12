package dev.jaypee.anotherpomodoroapp;

import android.text.Editable;
import android.text.TextWatcher;

import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

public class PasswordTextWatcher implements TextWatcher {

    List<TextInputLayout> textInputLayoutListeners;

    public PasswordTextWatcher(TextInputLayout... listeners) {
        textInputLayoutListeners = List.of(listeners);
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
        if(charSequence.length() < 8)
            for(TextInputLayout listener: textInputLayoutListeners)
                listener.setError("Password must be 8 characters or more");
        else
            for(TextInputLayout listener: textInputLayoutListeners)
                listener.setError(null);
    }
}
