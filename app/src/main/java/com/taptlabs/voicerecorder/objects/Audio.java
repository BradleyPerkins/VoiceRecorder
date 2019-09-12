package com.taptlabs.voicerecorder.objects;

import java.io.Serializable;
import java.util.ArrayList;

public class Audio implements Serializable {

    private String title;
    private String size;
    private String date;
    private String filePath;
    private String clipLength;


    public Audio(String title, String size, String date, String filePath, String clipLength) {
        this.title = title;
        this.size = size;
        this.date = date;
        this.filePath = filePath;
        this.clipLength = clipLength;
    }

    public String getClipLength() {
        return clipLength;
    }

    public void setClipLength(String clipLength) {
        this.clipLength = clipLength;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
