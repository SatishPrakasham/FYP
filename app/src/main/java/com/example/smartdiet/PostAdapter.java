package com.example.smartdiet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> postList;
    private String currentUserId;
    private FirebaseFirestore firestore;

    public PostAdapter(List<Post> postList, String currentUserId) {
        this.postList = postList;
        this.currentUserId = currentUserId;
        this.firestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        // Bind post content and likes
        holder.tvPostContent.setText(post.getContent());
        holder.tvLikes.setText("Likes: " + post.getLikes());

        // Check if the current user already liked the post
        boolean isAlreadyLiked = post.getLikedBy() != null && post.getLikedBy().contains(currentUserId);
        holder.btnLike.setEnabled(!isAlreadyLiked);
        holder.btnLike.setText(isAlreadyLiked ? "Liked" : "Like");

        // Load user profile image and username
        firestore.collection("users").document(post.getUserId()).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String username = userDoc.getString("name");
                        holder.tvUsername.setText(username);

                        String profileImageUrl = userDoc.getString("profileImageUrl");
                        if (profileImageUrl != null) {
                            Glide.with(holder.itemView.getContext())
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.icon)
                                    .error(R.drawable.icon)
                                    .into(holder.ivProfilePicture);
                        } else {
                            holder.ivProfilePicture.setImageResource(R.drawable.icon);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(holder.itemView.getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                });

        // Handle Like button click
        holder.btnLike.setOnClickListener(v -> {
            if (!isAlreadyLiked) {
                // Update local data
                if (post.getLikedBy() == null) {
                    post.setLikedBy(new ArrayList<>());
                }
                post.getLikedBy().add(currentUserId);
                post.setLikes(post.getLikes() + 1);

                // Update Firestore
                updatePostInFirestore(post, holder);
            }
        });


        holder.btnReport.setOnClickListener(v -> showReportDialog(post, holder.itemView.getContext()));



    }

    private void showReportDialog(Post post, Context context) {
        // Create the custom dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_report_post, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Initialize dialog components
        RadioGroup rgReportOptions = dialogView.findViewById(R.id.rgReportOptions);
        Button btnSubmitReport = dialogView.findViewById(R.id.btnSubmitReport);

        // Handle the submit button click
        btnSubmitReport.setOnClickListener(v -> {
            int selectedOptionId = rgReportOptions.getCheckedRadioButtonId();
            if (selectedOptionId == -1) {
                Toast.makeText(context, "Please select a reason to report", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get the selected report reason
            String reportReason = "";
            if (selectedOptionId == R.id.rbWrongInfo) {
                reportReason = "Inaccurate Information";
            } else if (selectedOptionId == R.id.rbInappropriateContent) {
                reportReason = "Inappropriate Content";
            } else if (selectedOptionId == R.id.rbUnrelatedContent) {
                reportReason = "Unrelated Content";
            }

            // Prepare the report data
            HashMap<String, Object> reportData = new HashMap<>();
            reportData.put("postId", post.getPostId());
            reportData.put("reportedBy", currentUserId);
            reportData.put("reason", reportReason);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedTimestamp = sdf.format(new Date());
            reportData.put("timestamp", formattedTimestamp);

            // Submit the report to Firestore
            firestore.collection("reports")
                    .add(reportData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Report submitted successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to submit report", Toast.LENGTH_SHORT).show();
                    });
        });

        // Show the dialog
        dialog.show();
    }


    private void updatePostInFirestore(Post post, PostViewHolder holder) {
        firestore.collection("posts").document(post.getPostId())
                .update(
                        "likes", FieldValue.increment(1), // Increment likes count atomically
                        "likedBy", FieldValue.arrayUnion(currentUserId) // Add current user to likedBy array atomically
                )
                .addOnSuccessListener(aVoid -> {
                    // Update UI after Firestore is updated
                    holder.tvLikes.setText("Likes: " + post.getLikes());
                    holder.btnLike.setEnabled(false);  // Disable Like button after liking
                    holder.btnLike.setText("Liked");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(holder.itemView.getContext(), "Failed to update post", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfilePicture;
        TextView tvUsername, tvPostContent, tvLikes;
        Button btnLike, btnReport;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfilePicture = itemView.findViewById(R.id.ivProfilePicture);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvPostContent = itemView.findViewById(R.id.tvPostContent);
            tvLikes = itemView.findViewById(R.id.tvLikes);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnReport = itemView.findViewById(R.id.btnReport);
        }
    }
}
