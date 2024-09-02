package com.saitechnology.investo.util;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class TransactionHistoryManager {

    public interface TransactionUpdateListener {
        void onTransactionUpdateSuccess();
        void onTransactionUpdateFailure(String errorMessage);
    }

    private final DatabaseReference databaseReference;

    public TransactionHistoryManager() {
        // Initialize Firebase database reference
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("TransactionHistory");
    }

    // Method to update a field in the transaction history
    public void updateTransactionField(String userId, String transactionTime, String fieldToUpdate, Object newValue, TransactionUpdateListener listener) {
        // Get reference to the specific node based on userId and transactionTime
        DatabaseReference transactionRef = databaseReference.child(userId).child(transactionTime);

        // Update the field with the new value
        transactionRef.child(fieldToUpdate).setValue(newValue, (databaseError, databaseReference) -> {
            if (databaseError != null) {
                // If there is an error, invoke the failure callback
                listener.onTransactionUpdateFailure(databaseError.getMessage());
            } else {
                // If the update is successful, invoke the success callback
                listener.onTransactionUpdateSuccess();
            }
        });
    }
}
