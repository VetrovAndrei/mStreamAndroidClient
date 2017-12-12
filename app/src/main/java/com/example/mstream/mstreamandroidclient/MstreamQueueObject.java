package com.example.mstream.mstreamandroidclient;

import android.net.Uri;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;

import java.io.File;

/**
 * Класс, который содержит метадату и класс элемента очереди
 */

public class MstreamQueueObject {

    private MetadataObject metadata;
    private MediaSessionCompat.QueueItem queueItem;

    public MetadataObject getMetadata(){
        return metadata;
    }
    public MediaSessionCompat.QueueItem getQueueItem(){
        return queueItem;
    }

    public MstreamQueueObject(MetadataObject mo) {
        if(mo != null){
            metadata = mo;
        }
    }

    public void setQueueItem(MediaSessionCompat.QueueItem q){
        this.queueItem = q;
    }
    public void setMetadata(MetadataObject metadata){
        this.metadata = metadata;
    }


    public void constructQueueItem(){
        String finalPath;
        Uri MediaURI;
        String mediaDescription;

        if(metadata.getLocalFile() != null && !metadata.getLocalFile().isEmpty()){
            finalPath = metadata.getLocalFile();
            MediaURI = Uri.fromFile(new File(finalPath));
            mediaDescription = "file";
        }else{
            finalPath = metadata.getUrl();
            MediaURI = Uri.parse(finalPath);
            mediaDescription = "network";
        }



        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaUri(MediaURI)
                .setMediaId(finalPath)
                .setDescription(mediaDescription)
                .setTitle(MediaUtils.titleFromFilename(metadata.getUrl()))
                .build();

        this.queueItem =  new MediaSessionCompat.QueueItem(description, 0);
    }
}
