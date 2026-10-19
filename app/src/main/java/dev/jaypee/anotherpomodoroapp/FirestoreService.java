package dev.jaypee.anotherpomodoroapp;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

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

    public void deleteTask(String taskId, OnSuccessListener<Void> successListener, OnFailureListener failureListener) {
        db.collection("tasks")
                .document(taskId)
                .delete()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    public void deleteAllUserTasks(String userId, OnSuccessListener<Void> successListener, OnFailureListener failureListener) {
        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        successListener.onSuccess(null);
                        return;
                    }
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        batch.delete(document.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(successListener)
                            .addOnFailureListener(failureListener);
                })
                .addOnFailureListener(failureListener);
    }
}
