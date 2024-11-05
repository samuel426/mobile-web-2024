package com.example.myapplication;

import android.graphics.Bitmap;

public class Post {
    private String title;
    private String text;
    private Bitmap image;

    public Post(String title, String text, Bitmap image) {
        this.title = title;
        this.text = text;
        this.image = image;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public Bitmap getImage() {
        return image;
    }
}
