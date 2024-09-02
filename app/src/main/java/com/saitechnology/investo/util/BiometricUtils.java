package com.saitechnology.investo.util;

import android.content.Context;
import androidx.biometric.BiometricManager;

public class BiometricUtils {

    public static boolean isBiometricAuthAvailable(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        int canAuthenticate = biometricManager.canAuthenticate();
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS;
    }
}

