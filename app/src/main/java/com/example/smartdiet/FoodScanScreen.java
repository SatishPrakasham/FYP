package com.example.smartdiet;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import okhttp3.*;

public class FoodScanScreen extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int CAMERA_PERMISSION_CODE = 101;

    private ImageView ivCapturedImage;
    private TextView tvDetectedLabel, tvNutritionInfo;
    private Spinner spinnerDetectedItems;
    private ConnectivityManager cm;
    private Uri imageUri;
    private String userId; // Store the userId
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_scan_screen);

        // Initialize views
        Button btnOpenCamera = findViewById(R.id.btnOpenCamera);
        ivCapturedImage = findViewById(R.id.ivCapturedImage);
        tvDetectedLabel = findViewById(R.id.tvDetectedLabel);
        spinnerDetectedItems = findViewById(R.id.spinnerDetectedItems);
        tvNutritionInfo = findViewById(R.id.tvNutritionInfo);
        Button btnSaveNutrition = findViewById(R.id.btnSaveNutrition);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Get the userId from the intent
        userId = getIntent().getStringExtra("userId");

        // Initialize ConnectivityManager
        cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // Open camera button click listener
        btnOpenCamera.setOnClickListener(v -> openCamera());

        // Handle Spinner item selection
        spinnerDetectedItems.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                tvDetectedLabel.setText("Selected: " + selectedItem);
                fetchNutritionData(selectedItem);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Save nutrition data button click listener
        btnSaveNutrition.setOnClickListener(v -> {
            String selectedItem = spinnerDetectedItems.getSelectedItem().toString();
            String nutritionInfo = tvNutritionInfo.getText().toString();
            saveNutritionDataToUser(userId, selectedItem, nutritionInfo);
        });
    }

    private void saveNutritionDataToUser(String userId, String foodName, String nutritionInfo) {
        if (userId == null) {
            Toast.makeText(this, "User ID not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a HashMap to store the nutrition data
        HashMap<String, Object> nutritionData = new HashMap<>();
        nutritionData.put("foodName", foodName);
        nutritionData.put("nutritionInfo", nutritionInfo);
        nutritionData.put("timestamp", new java.util.Date());

        // Save the nutrition data inside the user's document in Firestore
        firestore.collection("users").document(userId).collection("NutritionData")
                .add(nutritionData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(FoodScanScreen.this, "Nutrition data saved!", Toast.LENGTH_SHORT).show();

                    // Redirect to Homepage
                    Intent intent = new Intent(FoodScanScreen.this, Homepage.class);
                    intent.putExtra("userId", userId); // Pass userId to Homepage
                    startActivity(intent);
                    finish(); // Finish the current activity to prevent going back
                })
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Error saving nutrition data", e));
    }


    private void openCamera() {
        try {
            File photoFile = createImageFile();
            imageUri = FileProvider.getUriForFile(this, "com.example.smartdiet.fileprovider", photoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        } catch (IOException e) {
            Log.e("SmartDiet", "Error creating image file", e);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            try {
                Bitmap photo = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                ivCapturedImage.setImageBitmap(photo);
                processImage(photo);
            } catch (IOException e) {
                Log.e("SmartDiet", "Error loading image", e);
            }
        }
    }

    private void processImage(Bitmap bitmap) {
        String base64Image = bitmapToBase64(bitmap);
        callVisionAPI(base64Image);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
    }

    private void callVisionAPI(String base64Image) {
        String apiKey = "AIzaSyBXqVSPFhVkYDHldZ3e24Al54AobIH6Eog";
        String url = "https://vision.googleapis.com/v1/images:annotate?key=" + apiKey;

        // Request JSON updated to maxResults = 10
        String jsonRequest = "{ \"requests\": [{ \"image\": { \"content\": \"" + base64Image + "\" }, \"features\": [{ \"type\": \"LABEL_DETECTION\", \"maxResults\": 15 }] }] }";

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonRequest);
        Request request = new Request.Builder().url(url).post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("VisionAPI", "Request Failed: " + e.getMessage(), e);
                runOnUiThread(() -> tvDetectedLabel.setText("Failed to connect to Vision API"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    List<String> detectedLabels = parseLabelsFromResponse(jsonResponse);

                    runOnUiThread(() -> {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(FoodScanScreen.this,
                                android.R.layout.simple_spinner_item, detectedLabels);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerDetectedItems.setAdapter(adapter);
                    });
                } else {
                    Log.e("VisionAPI", "Response Error: " + response.code());
                    runOnUiThread(() -> tvDetectedLabel.setText("Error detecting objects"));
                }
            }
        });
    }

    private List<String> parseLabelsFromResponse(String jsonResponse) {
        List<String> labels = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray responsesArray = jsonObject.optJSONArray("responses");

            if (responsesArray != null && responsesArray.length() > 0) {
                JSONArray labelAnnotations = responsesArray.getJSONObject(0).optJSONArray("labelAnnotations");

                if (labelAnnotations != null) {
                    for (int i = 0; i < labelAnnotations.length(); i++) {
                        JSONObject labelObj = labelAnnotations.getJSONObject(i);
                        String description = labelObj.optString("description", "Unknown");
                        double score = labelObj.optDouble("score", 0.0);

                        String labelWithScore = description + " (" + String.format(Locale.getDefault(), "%.2f", score * 100) + "%)";
                        labels.add(labelWithScore);
                    }
                }
            }
        } catch (JSONException e) {
            Log.e("VisionAPI", "JSON Parsing Error: " + e.getMessage());
        }
        return labels;
    }

    private void fetchNutritionData(String selectedItem) {
        String url = "https://api.calorieninjas.com/v1/nutrition?query=" + Uri.encode(selectedItem);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Api-Key", "ryjVRHVKw5S4yz7Gi6ekqQ==ywKoLkPLHZlhldZN")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("NutritionAPI", "Request Failed: " + e.getMessage(), e);
                runOnUiThread(() -> tvNutritionInfo.setText("Failed to connect to Nutrition API"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    String nutritionInfo = parseNutritionResponse(jsonResponse);
                    runOnUiThread(() -> tvNutritionInfo.setText(nutritionInfo));
                } else {
                    Log.e("NutritionAPI", "Response Error: " + response.code());
                    runOnUiThread(() -> tvNutritionInfo.setText("Error: Food not found or unsupported item."));
                }
            }
        });
    }

    private String parseNutritionResponse(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray items = jsonObject.optJSONArray("items");

            if (items != null && items.length() > 0) {
                JSONObject item = items.getJSONObject(0);
                String name = item.optString("name", "N/A");
                String calories = item.optString("calories", "N/A");
                String fat = item.optString("fat_total_g", "N/A");
                String protein = item.optString("protein_g", "N/A");
                String carbs = item.optString("carbohydrates_total_g", "N/A");

                return String.format(Locale.getDefault(),
                        "Name: %s\nCalories: %s kcal\nFat: %s g\nProtein: %s g\nCarbs: %s g",
                        name, calories, fat, protein, carbs);
            }
        } catch (JSONException e) {
            Log.e("NutritionAPI", "JSON Parsing Error: " + e.getMessage());
        }
        return "Error parsing nutrition data.";
    }
}