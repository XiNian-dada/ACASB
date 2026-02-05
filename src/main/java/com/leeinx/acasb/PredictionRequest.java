package com.leeinx.acasb;

public class PredictionRequest {
    private String image_path;

    public PredictionRequest() {
    }

    public PredictionRequest(String image_path) {
        this.image_path = image_path;
    }

    public String getImage_path() {
        return image_path;
    }

    public void setImage_path(String image_path) {
        this.image_path = image_path;
    }
}