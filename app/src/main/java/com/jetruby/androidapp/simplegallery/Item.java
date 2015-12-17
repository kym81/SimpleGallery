package com.jetruby.androidapp.simplegallery;

/**
 * Created by Administrator on 17.12.2015.
 */
public class Item {
    public String file;
    public boolean isFile;
    public int icon;

    public Item(String file, Integer icon, boolean isFile) {
        this.file = file;
        this.icon = icon;
        this.isFile = isFile;
    }

    @Override
    public String toString() {
        return file;
    }
}