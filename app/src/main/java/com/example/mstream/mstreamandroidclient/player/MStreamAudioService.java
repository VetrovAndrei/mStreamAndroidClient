package com.example.mstream.mstreamandroidclient.player;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.view.KeyEvent;

import com.example.mstream.mstreamandroidclient.MetadataObject;
import com.example.mstream.mstreamandroidclient.QueueManager;
import com.example.mstream.mstreamandroidclient.R;
import com.example.mstream.mstreamandroidclient.ui.PlayerActivity;

import java.lang.ref.WeakReference;
import java.util.List;

public class MStreamAudioService extends MediaBrowserServiceCompat implements com.example.mstream.mstreamandroidclient.player.PlaybackManager.PlaybackServiceCallback {
    private static final String TAG = "MStreamAudioService";
    private static final int NOTIFICATION_ID = 6689;

    // Медиа-сессия позволяет системе узнать, что у нас происходит
    private MediaSessionCompat mediaSession;
    // playbackManager
    private com.example.mstream.mstreamandroidclient.player.PlaybackManager playbackManager;

    // Задержка устанавливается с помощью обработчика.
    private final DelayedStopHandler delayedStopHandler = new DelayedStopHandler(this);
    private static final int STOP_DELAY = 30000;

    @Override
    public void onCreate() {
        super.onCreate();

        // Создание менеджера очереди для обработки плейлиста
        QueueManager queueManager = new QueueManager(
                new QueueManager.MetadataUpdateListener() {
                    @Override
                    public void onMetadataChanged(MediaMetadataCompat metadata) {
                        mediaSession.setMetadata(metadata);
                    }

                    @Override
                    public void onMetadataRetrieveError() {
                        playbackManager.updatePlaybackState(getString(R.string.error_no_metadata));
                    }

                    @Override
                    public void onCurrentQueueIndexUpdated(int queueIndex) {
                        playbackManager.handlePlayRequest();
                    }

                    @Override
                    public void onQueueUpdated(String title, List<MediaSessionCompat.QueueItem> newQueue) {
                        mediaSession.setQueue(newQueue);
                        mediaSession.setQueueTitle(title);
                    }
                });
        // Создание класса воспроизведения, который будет играть музыку
        com.example.mstream.mstreamandroidclient.player.AudioPlayer playback = new com.example.mstream.mstreamandroidclient.player.AudioPlayer(this);
        // Создаем менеджер воспроизведения, который будет обрабатывать плеер и очередь воспроизведения
        playbackManager = new com.example.mstream.mstreamandroidclient.player.PlaybackManager(this, queueManager, playback);

        // установим медиа сессию
        mediaSession = new MediaSessionCompat(this, TAG);
        // Call to super to set up the session
        setSessionToken(mediaSession.getSessionToken());
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PAUSED, 0, 0)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
                .build());
        // Зададим активити, к которому привязан сеанс мультимедиа, - возможно, только PlayerActivity.
        // Это запустится, когда пользователь удалит наше уведомление.
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 667,
                new Intent(this, PlayerActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        mediaSession.setSessionActivity(pendingIntent);
        // установим callbacks - они будут вызваны через регистрацию onStartCommand MediaButtonReceiver
        mediaSession.setCallback(playbackManager.getMediaSessionCallback());

        startService(new Intent(getApplicationContext(), MStreamAudioService.class));
    }

    @Override
    public int onStartCommand(@NonNull Intent intent, int flags, int startId) {
        // Handle the Media Button Receiver automatic intents
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        // Сбросить обработчик задержки, чтобы обнулить сообщение,
        // чтобы остановить службу, если ничего не воспроизводится.
        delayedStopHandler.removeCallbacksAndMessages(null);
        delayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
        return START_STICKY;
    }

    // Overrides for MediaBrowser
    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        // Returning null == no one can connect, so we’ll return something
        return new BrowserRoot(getString(R.string.app_name), null);
    }

    // Дает список всех элементов, которые можно просмотреть. охватывает воспроизводимые и неиграемые элементы (файлы и папки).
    // Должен использовать это для заполнения пользовательского интерфейса!
    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        // Получить текущую очередь
        result.sendResult(playbackManager.getQueueAsMediaItems());
    }

//    public void testPlay() {
//        // Set the queue?
//        playbackManager.setCurrentMediaId("https://paulserver.mstre.am:5050/6553fd58-c032-401c-ae9d-68eb5d394c26/Feed%20Me/Feed%20Me%20-%20Calamari%20Tuesday%20[V0]/04.%20Ebb%20&%20Flow.mp3?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6InBhdWwiLCJpYXQiOjE0OTc5NzU5Mzl9.Y4B3kHhExuq0nCPMxZoxfbSibb7HbQ6S2ZDPD8ep6xA");
//
//        // Finally, play the item with the metadata specified above.
//        playbackManager.handlePlayRequest();
//    }

    @Override
    public void onDestroy() {
        playbackManager.handleStopRequest(null);
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);

        delayedStopHandler.removeCallbacksAndMessages(null);
        mediaSession.release();
        super.onDestroy();
    }

    private Notification buildNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        // MetadataObject moo = QueueManager.getIt().get(QueueManager.getIndex()).getMetadata();
        MetadataObject moo = QueueManager.getCurrentSong().getMetadata();

        String p1;
        String p2 = "";

        String artist = moo.getArtist();
        String title = moo.getTitle();
        String filename = moo.getFilename();

        if(artist != null && !artist.isEmpty()){
            p1 = artist;
            if(title != null && !title.isEmpty()){
                p2 = title;
            }else{
                p2 = filename;
            }
        }else{
            p1 = filename;
        }

        builder.setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(null)
                .setContentTitle(p1)
                .setContentText(p2)
//                .setSubText("LOL3")
                // when tapped, launch the mstream activity (have to set this elsewhere)
                .setContentIntent(mediaSession.getController().getSessionActivity())
                // Media controls should be publicly visible
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                // When swiped away, stop playback.
                .setDeleteIntent(getActionIntent(KeyEvent.KEYCODE_MEDIA_STOP))
                // TODO: test out the coloration
                .setColor(getResources().getColor(R.color.colorPrimaryDark));
        // Add some actions
        // ...
        // TODO: Need to update queueAdapter so the correct song gets highlighted
        builder.addAction(new NotificationCompat.Action(R.drawable.ic_skip_previous_white_36dp, "Previous", getActionIntent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)));
        // Then add a play/pause action
        addPlayPauseAction(builder);
        builder.addAction(new NotificationCompat.Action(R.drawable.ic_skip_next_white_36dp, "Previous", getActionIntent(KeyEvent.KEYCODE_MEDIA_NEXT)));
        // Set the style and configure the action buttons
        builder.setStyle(new NotificationCompat.MediaStyle()
                // Show the first button we added, in this cause, pause
                .setShowActionsInCompactView(0)
                .setMediaSession(mediaSession.getSessionToken())
                // Add a little 'x' to allow users to tap it to exit playback, in addition to swiping away
                .setShowCancelButton(true)
                .setCancelButtonIntent(getActionIntent(KeyEvent.KEYCODE_MEDIA_STOP)));

        return builder.build();
    }

    private void addPlayPauseAction(NotificationCompat.Builder builder) {
        if (isPlaying()) {
            builder.addAction(new NotificationCompat.Action(R.drawable.ic_pause_white_36dp, getString(R.string.pause),
                    getActionIntent(KeyEvent.KEYCODE_MEDIA_PAUSE)));
        } else {
            builder.addAction(new NotificationCompat.Action(R.drawable.ic_play_arrow_white_36dp, getString(R.string.play),
                    getActionIntent(KeyEvent.KEYCODE_MEDIA_PLAY)));
        }
    }

    /**
     * A helper method to get a PendingIntent based on a media key function.
     */
    private PendingIntent getActionIntent(int mediaKeyEvent) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.setPackage(this.getPackageName());
        intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, mediaKeyEvent));
        return PendingIntent.getBroadcast(this, mediaKeyEvent, intent, 0);
    }

    public boolean isPlaying() {
        return mediaSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING;
    }

    /**
     * Callback method called from PlaybackManager whenever the music is about to play.
     */
    @Override
    public void onPlaybackStart() {
        if (!mediaSession.isActive()) {
            mediaSession.setActive(true);
        }
        delayedStopHandler.removeCallbacksAndMessages(null);

        // The service needs to continue running even after the bound client (usually a
        // MediaController) disconnects, otherwise the music playback will stop.
        // Calling startService(Intent) will keep the service running until it is explicitly killed.
        startService(new Intent(getApplicationContext(), MStreamAudioService.class));
    }

    /**
     * Callback method called from PlaybackManager whenever the music stops playing.
     */
    @Override
    public void onPlaybackStop() {
        // Reset the delayed stop handler, so after STOP_DELAY it will be executed again,
        // potentially stopping the service.
        delayedStopHandler.removeCallbacksAndMessages(null);
        delayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
        stopForeground(false);
    }

    @Override
    public void onNotificationRequired() {
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    @Override
    public void onPlaybackStateUpdated(PlaybackStateCompat newState) {
        mediaSession.setPlaybackState(newState);
    }

    @Override
    public void onExtrasChanged(Bundle extras){

    }

    @Override
    public void onDur(int dur){

    }

    /**
     * A simple handler that stops the service if playback is not active (playing)
     */
    private static class DelayedStopHandler extends Handler {
        private final WeakReference<MStreamAudioService> mWeakReference;

        private DelayedStopHandler(MStreamAudioService service) {
            mWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            MStreamAudioService service = mWeakReference.get();
            if (service != null && service.playbackManager.getPlayback() != null) {
                if (service.playbackManager.getPlayback().isPlaying()) {
                    return;
                }
                service.stopSelf();
            }
        }
    }
}
