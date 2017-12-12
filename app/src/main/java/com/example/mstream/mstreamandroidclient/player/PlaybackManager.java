package com.example.mstream.mstreamandroidclient.player;

import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.example.mstream.mstreamandroidclient.QueueManager;

import java.util.List;

/**
 * Менеджер, который обрабатывает активное воспроизведение, а также очередь, поэтому Services не нужен
 */

class PlaybackManager implements com.example.mstream.mstreamandroidclient.player.Playback.Callback {

    private QueueManager queueManager; //очередь воспроизведения
    private com.example.mstream.mstreamandroidclient.player.Playback playback; // интерфейс вопроизведения
    private PlaybackServiceCallback serviceCallback; // интерфейс описан ниже
    private MediaSessionCallback mediaSessionCallback; //класс описан в самом низу

    //конструктор класса
    PlaybackManager(PlaybackServiceCallback serviceCallback, QueueManager queueManager, com.example.mstream.mstreamandroidclient.player.Playback playback) {
        this.serviceCallback = serviceCallback;
        this.queueManager = queueManager;
        mediaSessionCallback = new MediaSessionCallback();
        this.playback = playback;
        this.playback.setCallback(this);
    }


    com.example.mstream.mstreamandroidclient.player.Playback getPlayback() {
        return playback;
    }

    MediaSessionCompat.Callback getMediaSessionCallback() {
        return mediaSessionCallback;
    }

    /**
     * Обработка запроса на воспроизведение музыки
     */
    void handlePlayRequest() {

        MediaSessionCompat.QueueItem currentMusic = queueManager.getCurrentMusic();
        if (currentMusic != null) {
            serviceCallback.onPlaybackStart();
            playback.play(currentMusic);
        }
    }

    /**
     * Обработка запроса на паузу
     */
    void handlePauseRequest() {
        if (playback.isPlaying()) {
            playback.pause();
            serviceCallback.onPlaybackStop();
        }
    }

    /**
     * Обработка запроса на стоп
     * @param withError Сообщение об ошибке в случае, если стоп имеет непредвиденную причину.
     * Сообщение об ошибке будет установлено в PlaybackState и будет видимым для клиентов MediaController.
     */
    void handleStopRequest(String withError) {
        playback.stop(true);
        serviceCallback.onPlaybackStop();
        updatePlaybackState(withError);
    }

    /**
     * Обновляет текущее состояние медиаплеера, дополнительно показывая сообщение об ошибке.
     * @param error если не null, сообщение об ошибке для представления пользователю.
     */
    void updatePlaybackState(String error) {
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        // Получение позиции воспроизведения, если воспроизводится элемент
        if (playback != null && playback.isConnected()) {
            position = playback.getCurrentStreamPosition();
        }

        //noinspection ResourceType
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder().setActions(getAvailableActions());
        int state = playback.getState();

        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(error);
            state = PlaybackStateCompat.STATE_ERROR;
        }
        //noinspection ResourceType
        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

        stateBuilder.setBufferedPosition(playback.getBufferedPosition());

        // Set the activeQueueItemId if the current index is valid.
        MediaSessionCompat.QueueItem currentMusic = queueManager.getCurrentMusic();
        if (currentMusic != null) {
            stateBuilder.setActiveQueueItemId(currentMusic.getQueueId());
        }

        serviceCallback.onPlaybackStateUpdated(stateBuilder.build());

        if (state == PlaybackStateCompat.STATE_PLAYING ||
                state == PlaybackStateCompat.STATE_PAUSED) {
            serviceCallback.onNotificationRequired();
        }
    }

    // Функция для получения текущей длительности песни
    void updatePlaybackStateHack(int dur) {
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        if (playback != null && playback.isConnected()) {
            position = playback.getCurrentStreamPosition();
        }

        //noinspection ResourceType
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder().setActions(getAvailableActions());
        int state = dur;


        //noinspection ResourceType
        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

        stateBuilder.setBufferedPosition(playback.getBufferedPosition());

        // Устанавливает activeQueueItemId, если текущий индекс действителен.
        MediaSessionCompat.QueueItem currentMusic = queueManager.getCurrentMusic();
        if (currentMusic != null) {
            stateBuilder.setActiveQueueItemId(currentMusic.getQueueId());
        }

        serviceCallback.onPlaybackStateUpdated(stateBuilder.build());

        if (state == PlaybackStateCompat.STATE_PLAYING ||
                state == PlaybackStateCompat.STATE_PAUSED) {
            serviceCallback.onNotificationRequired();
        }
    }
    //функция получения доступных действий
    private long getAvailableActions() {
        long actions =
                PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID |
                        PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        if (playback.isPlaying()) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        }
        return actions;
    }

    /**
     * Реализация интерфейса Playback.Callback
     */
    @Override
    public void onCompletion() {
        // Медиа-проигрыватель закончил играть текущую песню,
        // поэтому мы идем вперед и начинаем играть следующую.
        if (queueManager.skipQueuePosition(1)) {
            handlePlayRequest();
            queueManager.updateMetadata();
        } else {
            // Если пропуск невозможен, мы останавливаем и освобождаем ресурсы:
            // handleStopRequest(null);
        }
    }

    @Override
    public void onPlaybackStatusChanged(int state) {
        updatePlaybackState(null);
    }

    @Override
    public void onError(String error) {
        updatePlaybackState(error);
    }

    @Override
    public void onExtrasChanged( Bundle extras){
        serviceCallback.onExtrasChanged(extras);
    }

    @Override
    public void onDur(int dur){
        updatePlaybackStateHack(dur);
    }

    @Override
    // Так надо, потому что если удалить, то не запустится
    public void setCurrentMediaId(String mediaId) {
        // queueManager.setQueueFromMusic(mediaId);
    }

    List<MediaBrowserCompat.MediaItem> getQueueAsMediaItems() {
        return queueManager.getQueueAsMediaItems();
    }

    /**
     * Этот callback позволяет нам обрабатывать разобранные
     * события, которые MediaSession испускает из действия MEDIA_BUTTON.
     */
    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            handlePlayRequest();
        }

        @Override
        public void onSkipToQueueItem(long queueId) {
//            queueManager.setCurrentQueueItem(queueId);
            queueManager.updateMetadata();
        }

        @Override
        public void onSeekTo(long position) {
            playback.seekTo((int) position);
        }

        @Override
        public void onCustomAction(String command, Bundle extras) {
            if(command.equals("addToQueue")){
                String q =  extras.getString("lol");
                String tempTitle = Uri.decode(q.substring(q.lastIndexOf('/') + 1, q.lastIndexOf('.')));
                MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                        .setMediaUri(Uri.parse(q))
                        .setMediaId(q)
                        // TODO: something a bit less hacky, maybe a Utils method
                        .setTitle(tempTitle)
                        .build();

                MediaSessionCompat.QueueItem q2 =  new MediaSessionCompat.QueueItem(description, 0);
                queueManager.addToQueue2(q2);
            }

            if(command.equals("pingQueueListener")){
                queueManager.callListener();
            }
//            if( COMMAND_EXAMPLE.equalsIgnoreCase(command) ) {
//                //Custom command here
//            }
        }



//        @Override
//        public void onPlayFromMediaId(String mediaId, Bundle extras) {
//            Log.d(TAG, "playFromMediaId mediaId:" + mediaId + " extras=" + extras);
//            // TODO: problems?
//            queueManager.setQueueFromMusic(mediaId);
//            handlePlayRequest();
//        }

        @Override
        public void onPause() {
            handlePauseRequest();
        }

        @Override
        public void onStop() {
            handleStopRequest(null);
        }

        @Override
        public void onSkipToNext() {
            if (queueManager.skipQueuePosition(1)) {
                handlePlayRequest();
            } else {
                // handleStopRequest("Cannot skip");
                return;
            }
            queueManager.updateMetadata();
        }

        @Override
        public void onSkipToPrevious() {
            if (queueManager.skipQueuePosition(-1)) {
                handlePlayRequest();
            } else {
                // handleStopRequest("Cannot skip");
                return;
            }
            queueManager.updateMetadata();
        }
    }

    interface PlaybackServiceCallback {
        void onPlaybackStart();

        void onNotificationRequired();

        void onPlaybackStop();

        void onPlaybackStateUpdated(PlaybackStateCompat newState);

        void onExtrasChanged( Bundle extras);

        void onDur(int dur);
    }
}
