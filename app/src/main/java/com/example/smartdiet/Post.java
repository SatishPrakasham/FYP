package com.example.smartdiet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Post {
    private String postId; // Unique identifier for the post
    private String userId; // ID of the user who created the post
    private String username; // Username of the post creator
    private String content; // Content of the post
    private int likes; // Number of likes
    private List<String> likedBy; // List of user IDs who liked the post

    // No-argument constructor for Firestore deserialization
    public Post() {}

    // Parameterized constructor
    public Post(String postId, String userId, String username, String content, int likes, List<String> likedBy) {
        this.postId = postId;
        this.userId = userId;
        this.username = username;
        this.content = content;
        this.likes = likes >= 0 ? likes : 0; // Default to 0 if invalid value
        this.likedBy = likedBy != null ? likedBy : new ArrayList<>(); // Handle null likedBy
    }

    // Getters and Setters
    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes >= 0 ? likes : 0; // Prevent negative likes
    }

    public List<String> getLikedBy() {
        return likedBy;
    }

    public void setLikedBy(List<String> likedBy) {
        this.likedBy = likedBy != null ? likedBy : new ArrayList<>();
    }

    // Helper methods
    public void addLike(String userId) {
        if (!likedBy.contains(userId)) {
            likedBy.add(userId);
            likes++;
        }
    }

    public void removeLike(String userId) {
        if (likedBy.contains(userId)) {
            likedBy.remove(userId);
            likes = Math.max(0, likes - 1); // Prevent negative likes
        }
    }

    // Convert to Firestore map format
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> postMap = new HashMap<>();
        postMap.put("userId", userId);
        postMap.put("username", username);
        postMap.put("content", content);
        postMap.put("likes", likes);
        postMap.put("likedBy", likedBy);
        return postMap;
    }
}
