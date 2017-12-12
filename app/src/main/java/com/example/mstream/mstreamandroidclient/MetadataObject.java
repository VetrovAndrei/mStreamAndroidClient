package com.example.mstream.mstreamandroidclient;

import android.support.annotation.NonNull;


// Этот объект с метадатой может быть передан плееру

public class MetadataObject {

    private String album;
    private String artist;
    private String title;
    private int track;
    private int year;

    private String filename;
    private String filepath;

    private String url;
    private String localFile;

    private boolean isSyncing = false;
    private String downloadingToPath;

    private String sha256Hash;

    private String albumArtURL;


    public MetadataObject(Builder builder) {
        this.album = builder.album;
        this.artist = builder.artist;
        this.title = builder.title;
        this.track = builder.track;
        this.year = builder.year;
        this.url = builder.url;
        this.localFile = builder.localFile;
        this.sha256Hash = builder.sha256Hash;
        this.albumArtURL = builder.albumArtURL;
        this.filename = builder.filename;
        this.filepath = builder.filepath;
    }

    public String getAlbum() {
        return this.album;
    }
    public String getArtist() {
        return this.artist;
    }
    public String getTitle() {
        return this.title;
    }
    public int getTrackNumber() {
        return this.track;
    }
    public int getYear() {
        return this.year;
    }
    public String getUrl() {
        return this.url;
    }
    public String getLocalFile() {
        return this.localFile;
    }
    public String getSha256Hash() {
        return this.sha256Hash;
    }
    public String getAlbumArtURL() {
        return this.albumArtURL;
    }
    public String getFilename() {
        return this.filename;
    }
    public String getFilepath() {
        return this.filepath;
    }
    public boolean isSyncing() {
        return this.isSyncing;
    }
    public String getDownloadingToPath(){
        return this.downloadingToPath;
    }



    public void setAlbum(String album) {
        this.album = album;
    }
    public void setArtist(String artist) {
        this.artist = artist;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public void setTrack(int track) {
        this.track = track;
    }
    public void setYear(int year) {
        this.year = year;
    }
    public void setLocalFile(String localFile) {
        this.localFile = localFile;
    }
    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }
    public void setAlbumArtURL(String albumArtURL) {
        this.albumArtURL = albumArtURL;
    }
    public void setFilename(String filename) {
        this.filename = filename;
    }
    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }
    public void setSyncing(boolean syncStat){
        this.isSyncing = syncStat;
    }
    public void setAlbumArtUrlViaHash(String hash) {
        this.albumArtURL = "album-art/" + hash;
    }
    public void setDownloadingToPath(String path){
        this.downloadingToPath = path;
    }


    public static class Builder {
        private String album;
        private String title;
        private String artist;
        private int track;
        private int year;


        private String url;

        private String localFile;
        private String sha256Hash;
        private String albumArtURL;

        private String filename;
        private String filepath;



        public Builder(@NonNull String url) {
            this.url = url;
        }

        public Builder album(String album) {
            this.album = album;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder artist(String artist) {
            this.artist = artist;
            return this;
        }

        public Builder sha256Hash(String sha256Hash) {
            this.sha256Hash = sha256Hash;
            return this;
        }

        public Builder localFile(String localFile) {
            this.localFile = localFile;
            return this;
        }

        public Builder track(int track){
            this.track = track;
            return this;
        }

        public Builder year(int year){
            this.year = year;
            return this;
        }

        public Builder filename(String filename){
            this.filename = filename;
            return this;
        }

        public Builder filepath(String filepath){
            this.filepath = filepath;
            return this;
        }

        public Builder albumArtURL(String albumArtURL){
            this.albumArtURL = albumArtURL;
            return this;
        }


        public MetadataObject build() {
            return new MetadataObject(this);
        }
    }


}
