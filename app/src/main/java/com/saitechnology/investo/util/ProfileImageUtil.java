package com.saitechnology.investo.util;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.saitechnology.investo.R;
import com.saitechnology.investo.activity.LogoutActivity;

public class ProfileImageUtil {

    // Method to load the user's profile image into an ImageView
    public static void loadProfileImage(Context context, ImageView userProfileIcon) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.getPhotoUrl() != null) {
            // If user is logged in and has a profile image
            Glide.with(context)
                    .load(user.getPhotoUrl())
                    .apply(RequestOptions.circleCropTransform()) // Circular image
                    .placeholder(R.drawable.baseline_account_circle_24) // Default image
                    .into(userProfileIcon);

            // Set profile icon to be clickable only when user is logged in
            setupProfileIconClick(context, userProfileIcon);
        } else {
            // If user is not logged in, show default image and make it non-clickable
            userProfileIcon.setImageResource(R.drawable.baseline_account_circle_24);
            userProfileIcon.setOnClickListener(null); // Remove click functionality when not logged in
        }
    }

    // Method to handle profile icon click event (opens a bottom sheet with logout option)
    public static void setupProfileIconClick(Context context, ImageView userProfileIcon) {
        // Check if the user is logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
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
        } else {
            // Make the icon non-clickable when the user is not logged in
            userProfileIcon.setOnClickListener(null);
        }
    }

    // Method to handle logout logic
    private static void logoutUser(Context context) {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show();
        context.startActivity(new Intent(context, LogoutActivity.class));
        ((AppCompatActivity) context).finish(); // Close the current activity
    }
}
