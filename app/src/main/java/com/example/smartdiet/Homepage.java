package com.example.smartdiet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Homepage extends AppCompatActivity {

    private TextView tvWelcomeMessage;
    private ImageView iconMenu, iconProfile, iconScan,iconTrophy;
    private FirebaseFirestore firestore;
    private String userId;
    private EditText etPostInput;
    private Button btnPost;
    private RecyclerView recyclerViewPosts;
    private PostAdapter postAdapter;
    private List<Post> postList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        // Initialize UI components
        tvWelcomeMessage = findViewById(R.id.tvWelcomeMessage);
        iconProfile = findViewById(R.id.iconProfile);
        iconMenu = findViewById(R.id.iconMenu);
        iconScan = findViewById(R.id.iconScan);
        etPostInput = findViewById(R.id.etPostInput);
        btnPost = findViewById(R.id.btnPost);
        iconTrophy = findViewById(R.id.iconTrophy);

        firestore = FirebaseFirestore.getInstance();

        recyclerViewPosts = findViewById(R.id.recyclerViewPosts);
        recyclerViewPosts.setLayoutManager(new LinearLayoutManager(this));
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList, userId);
        recyclerViewPosts.setAdapter(postAdapter);

        userId = getIntent().getStringExtra("userId");
        if (userId != null) {
            getUserName(userId);
        }

        loadPosts();

        iconProfile.setOnClickListener(v -> {
            Intent intent = new Intent(Homepage.this, Profile_page.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        iconScan.setOnClickListener(v -> {
            Intent intent = new Intent(Homepage.this, FoodScanScreen.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        iconMenu.setOnClickListener(v -> {
            Intent intent = new Intent(Homepage.this, RecipePage.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        iconTrophy.setOnClickListener(v -> {
            Intent intent = new Intent(Homepage.this, ProgressPage.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });


        btnPost.setOnClickListener(v -> submitPost());
    }

    private void getUserName(String userId) {
        DocumentReference docRef = firestore.collection("users").document(userId);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String userName = documentSnapshot.getString("name");
                tvWelcomeMessage.setText("Welcome " + userName + "!");
            }
        }).addOnFailureListener(e -> tvWelcomeMessage.setText("Error fetching user data"));
    }

    private void submitPost() {
        String postContent = etPostInput.getText().toString().trim();
        if (!postContent.isEmpty()) {
            Map<String, Object> post = new HashMap<>();
            post.put("userId", userId);
            post.put("content", postContent);
            post.put("likes", 0);
            post.put("likedBy", new ArrayList<String>());

            firestore.collection("posts").add(post)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(Homepage.this, "Post submitted", Toast.LENGTH_SHORT).show();
                        etPostInput.setText("");
                        loadPosts();
                    })
                    .addOnFailureListener(e -> Toast.makeText(Homepage.this, "Failed to submit post", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Post cannot be empty", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadPosts() {
        firestore.collection("posts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    postList.clear();  // Clear existing posts

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // Extract data from the document
                        String postId = document.getId();
                        String userId = document.getString("userId");
                        String content = document.getString("content");
                        int likes = document.getLong("likes") != null ? document.getLong("likes").intValue() : 0;

                        // Safely handle the likedBy list, ensuring no null errors
                        List<String> likedBy = (List<String>) document.get("likedBy");
                        if (likedBy == null) {
                            likedBy = new ArrayList<>();  // Initialize to empty list if null
                        }

                        // Fetch the username from the 'users' collection
                        fetchUserAndCreatePost(postId, userId, content, likes, likedBy);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Homepage.this, "Failed to load posts", Toast.LENGTH_SHORT).show();
                });
    }

    // Helper method to fetch user data and create a Post object
    private void fetchUserAndCreatePost(String postId, String userId, String content, int likes, List<String> likedBy) {
        firestore.collection("users").document(userId).get()
                .addOnSuccessListener(userDocument -> {
                    if (userDocument.exists()) {
                        String username = userDocument.getString("name");

                        // Create the Post object
                        Post post = new Post(postId, userId, username, content, likes, likedBy);
                        postList.add(post);  // Add to post list
                        postAdapter.notifyDataSetChanged();  // Refresh RecyclerView
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Homepage.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                });
    }
}
