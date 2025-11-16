package com.example.testproject1;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;

public class LoadingDialog {
    private Dialog dialog;
    private Context context;

    public LoadingDialog(@NonNull Context context) {
        this.context = context;
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null);
        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    public void showDialog(String message) {
        dialog.show();
    }

    public void dismissDialog() {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
