package dev.jaypee.anotherpomodoroapp;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class FirestoreService {

    private final FirebaseFirestore db;

    public FirestoreService() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void saveTask(PomodoroTask task, OnSuccessListener<Void> successListener, OnFailureListener failureListener) {
        if (task.getId() == null || task.getId().isEmpty()) {
            task.setId(db.collection("tasks").document().getId());
        }
        db.collection("tasks")
                .document(task.getId())
                .set(task)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public ListenerRegistration getTasks(String userId, EventListener<QuerySnapshot> listener) {
        return db.collection("tasks")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }
}
