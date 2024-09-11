package com.saitechnology.investo.util;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.saitechnology.investo.R;
import com.saitechnology.investo.activity.LogoutActivity;

public class ProfileImageUtil {

    // Method to load the user's profile image into an ImageView
    public static void loadProfileImage(Context context, ImageView userProfileIcon) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getPhotoUrl() != null) {
            Glide.with(context)
                    .load(user.getPhotoUrl())
                    .apply(RequestOptions.circleCropTransform()) // Circular image
                    .placeholder(R.drawable.baseline_account_circle_24) // Default image
                    .into(userProfileIcon);
        }
    }

    // Method to handle profile icon click event (opens a bottom sheet with logout option)
    public static void setupProfileIconClick(Context context, ImageView userProfileIcon) {
        userProfileIcon.setOnClickListener(v -> {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
            View bottomSheetView = ((AppCompatActivity) context).getLayoutInflater().inflate(R.layout.bottom_sheet_profile, null);
            bottomSheetDialog.setContentView(bottomSheetView);

            // Set up Logout option
            bottomSheetView.findViewById(R.id.logout).setOnClickListener(view -> {
                logoutUser(context);
                bottomSheetDialog.dismiss();
            });

            // Set up Dismiss option
            bottomSheetView.findViewById(R.id.dismiss).setOnClickListener(view -> bottomSheetDialog.dismiss());

            bottomSheetDialog.show();
        });
    }

    // Method to handle logout logic
    private static void logoutUser(Context context) {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show();
        context.startActivity(new Intent(context, LogoutActivity.class));
        ((AppCompatActivity) context).finish(); // Close the current activity
    }
}
