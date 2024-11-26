package com.example.smartdiet;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterPage extends AppCompatActivity {

    private ImageView imgBack;
    private Button btnBMICalculator, btnRegister;
    private EditText etName, etEmail, etPassword, etPhoneNumber, etAge, etCurrentWeight, etCurrentHeight;
    private EditText etWeight, etHeight;
    private RadioGroup rgGoals, rgHealthConditions;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_page);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI components
        imgBack = findViewById(R.id.imgBack);
        btnBMICalculator = findViewById(R.id.btnBMICalculator);
        btnRegister = findViewById(R.id.btnRegister);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etAge = findViewById(R.id.etAge);
        etCurrentWeight = findViewById(R.id.etCurrentWeight);
        etCurrentHeight = findViewById(R.id.etCurrentHeight);

        // Initialize RadioGroups
        rgGoals = findViewById(R.id.rgGoals);
        rgHealthConditions = findViewById(R.id.rgHealthConditions);

        // Back button listener
        imgBack.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterPage.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        // Register button listener
        btnRegister.setOnClickListener(v -> registerUser());

        // BMI Calculator button listener
        btnBMICalculator.setOnClickListener(v -> showBMICalculatorDialog());
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String weightStr = etCurrentWeight.getText().toString().trim();
        String heightStr = etCurrentHeight.getText().toString().trim();
        String goal = getSelectedGoal();
        String healthCondition = getSelectedHealthConditions();

        if (validateInputs(name, email, password, phoneNumber, ageStr, weightStr, heightStr, goal, healthCondition)) {
            try {
                int age = Integer.parseInt(ageStr);
                float weight = Float.parseFloat(weightStr);
                float height = Float.parseFloat(heightStr);

                Map<String, Object> user = new HashMap<>();
                user.put("name", name);
                user.put("email", email);
                user.put("password", password);
                user.put("phoneNumber", phoneNumber);
                user.put("age", age);
                user.put("weight", weight);
                user.put("height", height);
                user.put("goal", goal);
                user.put("healthConditions", healthCondition);

                firestore.collection("users")
                        .add(user)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(this, "Data saved! Proceeding...", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RegisterPage.this, Homepage.class);
                            intent.putExtra("userId", documentReference.getId());
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("RegisterPage", "Error saving data", e);
                            Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show();
                        });
            } catch (NumberFormatException e) {
                Log.e("RegisterPage", "Number format error", e);
                Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean validateInputs(String name, String email, String password, String phoneNumber,
                                   String ageStr, String weightStr, String heightStr,
                                   String goal, String healthCondition) {
        if (name.isEmpty() || !name.matches("[a-zA-Z\\s]+")) {
            Toast.makeText(this, "Please enter a valid name", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (password.isEmpty() || password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (phoneNumber.isEmpty() || !phoneNumber.matches("\\d{3}-\\d{7,8}")) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (ageStr.isEmpty() || !ageStr.matches("\\d+")) {
            Toast.makeText(this, "Please enter a valid age", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (weightStr.isEmpty() || !weightStr.matches("\\d+(\\.\\d+)?")) {
            Toast.makeText(this, "Please enter a valid weight", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (heightStr.isEmpty() || !heightStr.matches("\\d+(\\.\\d+)?")) {
            Toast.makeText(this, "Please enter a valid height", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (goal.equals("Not Specified")) {
            Toast.makeText(this, "Please select a goal", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (healthCondition.isEmpty()) {
            Toast.makeText(this, "Please select a health condition", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private String getSelectedGoal() {
        int selectedId = rgGoals.getCheckedRadioButtonId();
        RadioButton radioButton = findViewById(selectedId);
        return radioButton != null ? radioButton.getText().toString() : "Not Specified";
    }

    private String getSelectedHealthConditions() {
        int selectedId = rgHealthConditions.getCheckedRadioButtonId();
        RadioButton radioButton = findViewById(selectedId);
        return radioButton != null ? radioButton.getText().toString() : "None";
    }

    private void showBMICalculatorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.bmi_calculator_dialog, null);
        builder.setView(dialogView);

        etWeight = dialogView.findViewById(R.id.etWeight);
        etHeight = dialogView.findViewById(R.id.etHeight);
        Button btnCalculateBMI = dialogView.findViewById(R.id.btnCalculateBMI);
        Button btnCancelDialog = dialogView.findViewById(R.id.btnCancel);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnCalculateBMI.setOnClickListener(v -> {
            calculateBMI();
            dialog.dismiss();
        });

        btnCancelDialog.setOnClickListener(v -> dialog.dismiss());
    }

    private void calculateBMI() {
        String weightStr = etWeight.getText().toString();
        String heightStr = etHeight.getText().toString();

        if (!weightStr.isEmpty() && !heightStr.isEmpty()) {
            try {
                float weight = Float.parseFloat(weightStr);
                float heightCm = Float.parseFloat(heightStr);

                if (weight <= 0 || heightCm <= 0) {
                    Toast.makeText(this, "Weight and height must be positive numbers", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Convert height from cm to meters
                float height = heightCm / 100;

                // Calculate BMI
                float bmi = weight / (height * height);

                // Format BMI to 2 decimal places
                String bmiResult = String.format("Your BMI: %.2f", bmi);

                // Provide feedback based on BMI
                String feedback;
                if (bmi < 18.5) {
                    feedback = bmiResult + ". You're encouraged to gain weight.";
                } else if (bmi >= 18.5 && bmi <= 24.9) {
                    feedback = bmiResult + ". You're encouraged to maintain weight.";
                } else {
                    feedback = bmiResult + ". You're encouraged to lose weight.";
                }

                // Show feedback in a Toast
                Toast.makeText(this, feedback, Toast.LENGTH_LONG).show();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numeric values", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Enter both weight and height", Toast.LENGTH_SHORT).show();
        }
    }
}

