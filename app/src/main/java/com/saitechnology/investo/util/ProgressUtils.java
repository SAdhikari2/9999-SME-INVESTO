package com.saitechnology.investo.util;

import android.view.View;
import android.widget.ProgressBar;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ProgressUtils {
    public static boolean toggleProgressVisibility(ProgressBar progressBar, boolean isProgressVisible) {
        boolean newVisibility = !isProgressVisible;
        if (isProgressVisible) {
            progressBar.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.VISIBLE);
        }
        return newVisibility;
    }
}
