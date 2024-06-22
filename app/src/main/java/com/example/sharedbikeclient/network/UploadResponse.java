package com.example.sharedbikeclient.network;

public class UploadResponse {
    private boolean success;
    private String imageUrl;

    public boolean isSuccess() {
        return success;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
