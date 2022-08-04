package com.example.avrecorder;

import android.widget.ImageView;

public class DataObject {

/*    public String title;
    public String count(int count) {
        return "Titled as "+count;
    }*/

    public ImageView imageView;
    public String text;
    public DataObject(String string) {
        this.text = string;
    }

    public void setText(String text){
        this.text = text;
    }
    public String getText() {
        return text;
    }

    public void setImage(ImageView imageView) {
        this.imageView = imageView;
    }
    public ImageView getImageView(){
        return imageView;
    }
}
