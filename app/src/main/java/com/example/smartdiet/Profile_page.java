package com.example.smartdiet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.bumptech.glide.Glide;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Profile_page extends AppCompatActivity {

    private ImageView ivProfilePicture;
    private EditText etName, etEmail, etPhoneNumber, etAge, etWeight, etHeight;
    private Button btnChangePicture, btnSaveChanges;
    private RadioGroup rgGoals, rgHealthConditions;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private Uri selectedImageUri;
    private String userId;
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_page);

        // Initialize Firestore and Firebase Storage
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Initialize UI components
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        btnChangePicture = findViewById(R.id.btnChangePicture);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etAge = findViewById(R.id.etAge);
        etWeight = findViewById(R.id.etWeight);
        etHeight = findViewById(R.id.etHeight);
        rgGoals = findViewById(R.id.rgGoals);
        rgHealthConditions = findViewById(R.id.rgHealthConditions);

        // Retrieve userId from Intent
        userId = getIntent().getStringExtra("userId");

        loadUserProfile();

        // Set listeners for changing picture and saving profile
        btnChangePicture.setOnClickListener(v -> openFileChooser());
        btnSaveChanges.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                uploadImageToFirebase();
            } else {
                saveUserProfile(); // Save profile if no new image is selected
            }
        });
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            ivProfilePicture.setImageURI(selectedImageUri);  // Display the selected image
        }
    }

    private void uploadImageToFirebase() {
        String imageFileName = "profile_pictures/" + UUID.randomUUID().toString();
        StorageReference imageRef = storageReference.child(imageFileName);

        imageRef.putFile(selectedImageUri).addOnSuccessListener(taskSnapshot ->
                imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    // Save the image download URL to Firestore
                    saveProfileImageUrl(uri.toString());
                }).addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to get image URL", Toast.LENGTH_SHORT).show()
                )
        ).addOnFailureListener(e ->
                Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
        );
    }

    private void saveProfileImageUrl(String imageUrl) {
        firestore.collection("users").document(userId)
                .update("profileImageUrl", imageUrl)
                .addOnSuccessListener(aVoid -> saveUserProfile())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save image URL", Toast.LENGTH_SHORT).show()
                );
    }

    private void setSelectedRadioButton(RadioGroup radioGroup, String value) {
        int count = radioGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            RadioButton rb = (RadioButton) radioGroup.getChildAt(i);
            if (rb.getText().toString().equals(value)) {
                rb.setChecked(true);
                break;
            }
        }
    }

    private void loadUserProfile() {
        firestore.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        etName.setText(documentSnapshot.getString("name"));
                        etEmail.setText(documentSnapshot.getString("email"));
                        etPhoneNumber.setText(documentSnapshot.getString("phoneNumber"));
                        etAge.setText(String.valueOf(documentSnapshot.getLong("age")));
                        etWeight.setText(String.valueOf(documentSnapshot.getDouble("weight")));
                        etHeight.setText(String.valueOf(documentSnapshot.getDouble("height")));

                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                        if (profileImageUrl != null) {
                            loadProfileImage(profileImageUrl);
                        }

                        setSelectedRadioButton(rgGoals, documentSnapshot.getString("goal"));
                        setSelectedRadioButton(rgHealthConditions, documentSnapshot.getString("healthConditions"));
                    }
                }).addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadProfileImage(String imageUrl) {
        Glide.with(this)
                .load(imageUrl)
                .into(ivProfilePicture);  // Load the image using Glide
    }

    private void saveUserProfile() {
        Map<String, Object> user = new HashMap<>();
        user.put("name", etName.getText().toString().trim());
        user.put("email", etEmail.getText().toString().trim());
        user.put("phoneNumber", etPhoneNumber.getText().toString().trim());
        user.put("age", Integer.parseInt(etAge.getText().toString().trim()));
        user.put("weight", Float.parseFloat(etWeight.getText().toString().trim()));
        user.put("height", Float.parseFloat(etHeight.getText().toString().trim()));
        user.put("goal", getSelectedGoal());
        user.put("healthConditions", getSelectedHealthConditions());

        firestore.collection("users").document(userId)
                .set(user, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                ).addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
                );
    }

    private String getSelectedGoal() {
        int selectedId = rgGoals.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton selectedRadioButton = findViewById(selectedId);
            return selectedRadioButton.getText().toString();
        }
        return "Not Specified";
    }

    private String getSelectedHealthConditions() {
        int selectedId = rgHealthConditions.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton selectedRadioButton = findViewById(selectedId);
            return selectedRadioButton.getText().toString();
        }
        return "None";
    }

}
