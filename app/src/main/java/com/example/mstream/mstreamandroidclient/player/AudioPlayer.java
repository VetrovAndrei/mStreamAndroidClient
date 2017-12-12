package com.example.mstream.mstreamandroidclient.player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;

import com.example.mstream.mstreamandroidclient.QueueManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLDecoder;

/**
 * Класс, который реализует локальное воспроизведение мультимедиа, используя {@link android.media.MediaPlayer}
 */
class AudioPlayer implements Playback, AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnBufferingUpdateListener {


    // Уровень звука который мы установили медиа-проигрывателем, когда теряем аудиофокус,
    // но им разрешено уменьшать громкость, а не останавливать воспроизведение.
    private static final float VOLUME_DUCK = 0.2f;
    // Уровень звука который мы установили медиа-проигрывателем, когда аудиофокус у нас
    private static final float VOLUME_NORMAL = 1.0f;

    // у нас нет фокуса звука, и он не может утихать (играть на малой громкости)
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    // у нас нет фокуса, но может утихать (игра на низком уровне)
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    // у нас есть полный аудио фокус
    private static final int AUDIO_FOCUSED = 2;

    private final Context context;
    private final WifiManager.WifiLock wifiLock;
    private int state;
    private boolean playOnFocusGain;
    private Callback callback;
    private volatile int currentPosition;
    private int currentBufferPosition = 0;
    private volatile String currentMediaId;

    private volatile boolean audioNoisyReceiverRegistered;
    private final IntentFilter audioNoisyIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private final BroadcastReceiver audioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                if (isPlaying()) {
                    pause();
                }
            }
        }
    };

    // Тип звукового фокуса у нас:
    private int audioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    private final AudioManager audioManager;
    private MediaPlayer mediaPlayer;

    AudioPlayer(Context context) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        // Создайте блокировку Wifi (это не вызывает блокировку, это просто создает ее)
        this.wifiLock = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mStream_lock");
        this.state = PlaybackStateCompat.STATE_NONE;
    }

    @Override
    public void start() {
    }

    //отсановка воспроизведения
    @Override
    public void stop(boolean notifyListeners) {
        state = PlaybackStateCompat.STATE_STOPPED;
        if (notifyListeners && callback != null) {
            callback.onPlaybackStatusChanged(state);
        }
        currentPosition = getCurrentStreamPosition();
        // Give up Audio focus
        giveUpAudioFocus();
        unregisterAudioNoisyReceiver();
        // Relax all resources
        relaxResources(true);
    }

    //установить состояние: играет не играет
    @Override
    public void setState(int state) {
        this.state = state;
    }

    @Override
    public int getState() {
        return state;
    }

    @Override
    public boolean isConnected() {
        return true;
    }
    // проверка играет или не играет
    @Override
    public boolean isPlaying() {
        return playOnFocusGain || (mediaPlayer != null && mediaPlayer.isPlaying());
    }
    // показать время текущее воспроизведение
    @Override
    public int getCurrentStreamPosition() {
        return mediaPlayer != null ?
                mediaPlayer.getCurrentPosition() : currentPosition;
    }
    // показать сколько скачалось
    @Override
    public int getBufferedPosition() {
        return currentBufferPosition;
    }
    // устаонвить новое теущее расположение игры
    @Override
    public void updateLastKnownStreamPosition() {
        if (mediaPlayer != null) {
            currentPosition = mediaPlayer.getCurrentPosition();
        }
    }

    // играть элемент из очереди
    @Override
    public void play(MediaSessionCompat.QueueItem item) {
        playOnFocusGain = true;
        tryToGetAudioFocus();
        registerAudioNoisyReceiver();
        // если текущая песня отличается то начать вопроизведение сначала
        String mediaId = item.getDescription().getMediaId();
        boolean mediaHasChanged = !TextUtils.equals(mediaId, currentMediaId);
        if (mediaHasChanged) {
            currentPosition = 0;
            currentMediaId = mediaId;
        }
        // если стоит на паузе и не изменилость и что-то играло, то продолжит воспроизведение
        if (state == PlaybackStateCompat.STATE_PAUSED && !mediaHasChanged && mediaPlayer != null) {
            configMediaPlayerState();
        } else {
            //иначе остановить текущее воспроизведение
            state = PlaybackStateCompat.STATE_STOPPED;
            relaxResources(false); // освободить все, кроме MediaPlayer
            // Это может привести к NPE, но мы хотим, чтобы приложение вышло из строя, если это произойдет. что-то не так!
            String source = item.getDescription().getMediaUri().toString();

            try {
                createMediaPlayerIfNeeded();
                state = PlaybackStateCompat.STATE_BUFFERING;
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

                // Проверьте, есть ли у нас файл или URL-адрес
                String type = item.getDescription().getDescription().toString();
                if(type.equals("file")){
                    // Android заставляет нас кодировать это как URI,
                    // так что теперь нам нужно сделать какую-то взломанную ерунду, чтобы декодировать эту
                    source = URLDecoder.decode(source, "UTF-8");
                    File file = new File(source.substring(6));

                    // Файл подтверждения доступен
                    if(!file.exists()){
                        //  TODO: Log error and update DB
                        // Использовать URL-адрес
                        mediaPlayer.setDataSource(item.getDescription().getMediaUri().toString());
                    }

                    FileInputStream inputStream = new FileInputStream(file);
                    mediaPlayer.setDataSource(inputStream.getFD());
                    inputStream.close();
                }else{
                    mediaPlayer.setDataSource(source);
                }

                QueueManager.setCurrentSong();


                // Запускаем медиа-плеер в фоновом режиме. Когда это будет сделано,
                // он вызовет наш OnPreparedListener (то есть метод onPrepared ()
                // в этом классе, поскольку мы установили для этого слушателя значение «this»).
                // Пока медиапроигрыватель не подготовлен, мы *не можем* вызвать start () на нем!
                mediaPlayer.prepareAsync();

                // Если мы выходим из Интернета, мы хотим провести блокировку Wifi,
                // которая предотвращает переключение Wi-Fi во время воспроизведения песни.
                wifiLock.acquire();

                if (callback != null) {
                    callback.onPlaybackStatusChanged(state);
                }

            } catch (IOException ex) {
                if (callback != null) {
                    callback.onError(ex.getMessage());
                }
            }
        }
    }

    // пауза
    @Override
    public void pause() {
        // есди сейчас плеер играет
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            // Приостановите медиаплеер и отмените состояние «переднего плана».
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                currentPosition = mediaPlayer.getCurrentPosition();
            }
            // в то время как пауза, сохраните MediaPlayer, но отказаться от фокуса аудио
            relaxResources(false);
            giveUpAudioFocus();
        }
        state = PlaybackStateCompat.STATE_PAUSED;
        if (callback != null) {
            callback.onPlaybackStatusChanged(state);
        }
        unregisterAudioNoisyReceiver();
    }

    @Override
    public void seekTo(int position) {

        if (mediaPlayer == null) {
            // Если у нас нет текущего медиаплеера, просто обновите текущую позицию
            currentPosition = position;
        } else {
            if (mediaPlayer.isPlaying()) {
                state = PlaybackStateCompat.STATE_BUFFERING;
            }
            mediaPlayer.seekTo(position);
            if (callback != null) {
                callback.onPlaybackStatusChanged(state);
            }
        }
    }

    @Override
    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void setCurrentStreamPosition(int pos) {
        this.currentPosition = pos;
    }

    @Override
    public void setCurrentMediaId(String mediaId) {
        this.currentMediaId = mediaId;
    }

    @Override
    public String getCurrentMediaId() {
        return currentMediaId;
    }

    /**
     * Попытка получить системный аудио фокус.
     */
    private void tryToGetAudioFocus() {
        if (audioFocus != AUDIO_FOCUSED) {
            int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioFocus = AUDIO_FOCUSED;
            }
        }
    }

    /**
     * Откажитесь от фокуса аудио.
     */
    private void giveUpAudioFocus() {
        if (audioFocus == AUDIO_FOCUSED) {
            if (audioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioFocus = AUDIO_NO_FOCUS_NO_DUCK;
            }
        }
    }

    /**
     * Reconfigures MediaPlayer в соответствии с настройками аудио фокуса
     * и запускает / перезапускает его. Этот метод запускает / перезапускает
     * MediaPlayer в соответствии с текущим состоянием аудиофокусировки.
     * Поэтому, если у нас есть фокус, он будет играть нормально;
     * если у нас нет фокуса, он либо покинет MediaPlayer приостановил
     * или установил его на низкую громкость, в зависимости от того,
     * что разрешено текущими настройками фокуса. Этот метод предполагает
     * mPlayer! = Null, поэтому, если вы его вызываете, вы должны
     * сделать это из контекста, в котором вы уверены, что это так.
     */
    private void configMediaPlayerState() {
        if (audioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            // Если у нас нет фокуса звука и мы не можем уменьшить громкость, мы должны сделать паузу,
            if (state == PlaybackStateCompat.STATE_PLAYING) {
                pause();
            }
        } else {  // у нас есть аудио фокус и мы можем уменьшить громкость
            if (audioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
                mediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK);
            } else {
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL); // мы снова можем повысить громкость
                } // иначе сделайте что-нибудь для удаленного клиента.
            }
            //Если бы песня проигрывалась, когда теряли фокус, нам нужно было возобновить игру.
            if (playOnFocusGain) {
                if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    if (currentPosition == mediaPlayer.getCurrentPosition()) {
                        mediaPlayer.start();
                        state = PlaybackStateCompat.STATE_PLAYING;
                    } else {
                        mediaPlayer.seekTo(currentPosition);
                        state = PlaybackStateCompat.STATE_BUFFERING;
                    }
                }
                playOnFocusGain = false;
            }
        }
        if (callback != null) {
            callback.onPlaybackStatusChanged(state);
        }
    }

    /**
     * Вызывается AudioManager при изменении аудио фокуса
     * реализация {@link android.media.AudioManager.OnAudioFocusChangeListener}
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // у нас есть аудио фокус
            audioFocus = AUDIO_FOCUSED;

        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // Мы потеряли фокус. Если мы можем понизить громкость, мы можем продолжать играть.
            // Иначе останавливаем воспроизведениеи.
            boolean canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
            audioFocus = canDuck ? AUDIO_NO_FOCUS_CAN_DUCK : AUDIO_NO_FOCUS_NO_DUCK;

            // Если мы играем, нам нужно сбросить медиаплеер, вызвав
            // configMediaPlayerState с правильными настройками audioFocus.
            if (state == PlaybackStateCompat.STATE_PLAYING && !canDuck) {
                // Если у нас нет фокуса звука и мы не можем уменьшить громкость
                // , мы сохраняем информацию, которую мы играем, чтобы мы могли
                // возобновить воспроизведение, как только получим фокус.
                playOnFocusGain = true;
            }
        } else {
        }
        configMediaPlayerState();
    }

    /**
     * Вызывается, когда MediaPlayer завершил поиск
     * @see MediaPlayer.OnSeekCompleteListener
     */
    @Override
    public void onSeekComplete(MediaPlayer mp) {
        currentPosition = mp.getCurrentPosition();
        if (state == PlaybackStateCompat.STATE_BUFFERING) {
            mediaPlayer.start();
            state = PlaybackStateCompat.STATE_PLAYING;
        }
        if (callback != null) {
            callback.onPlaybackStatusChanged(state);
        }
    }

    /**
     * Вызывается, когда проигрыватель проигрывает до конца текущую песню.
     * @see MediaPlayer.OnCompletionListener
     */
    @Override
    public void onCompletion(MediaPlayer player) {
        // Медиа-проигрыватель закончил играть текущую песню, поэтому мы идем вперед и начинаем дальше.
        if (callback != null) {
            callback.onCompletion();
        }
    }

    /**
     * Вызывается при обновлении буфера.
     * @see MediaPlayer.OnBufferingUpdateListener
     */
    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        if (mp.getDuration() != -1) {
            currentBufferPosition = mp.getDuration() / 100 * percent;
        }
    }

    /**
     * Вызывается, когда медиаплеер готов к подготовке.
     * @see MediaPlayer.OnPreparedListener
     */
    @Override
    public void onPrepared(MediaPlayer player) {
        // Медиа-плеер готовится. Это означает, что мы можем
        // начать играть, если у нас есть фокус звука.

        // Time for some hacky bullshit lol
        // Since the state does not allow us to duration (becasue who needs that, right?)
        // We just send the duration over as a negative number
        // Since no state has a negative quantity less than -1
        // We just check for that on the other side, and carry out a special action if so
        // Fucking hell google, everyone of your frameworks suckssssssss
        int duration = player.getDuration();

        if (callback != null) {
            callback.onDur(duration * -1);
        }

        configMediaPlayerState();
    }

    /**
     * Вызывается при появлении ошибки при воспроизведении носителя.
     * Когда это произойдет, медиаплеер переходит в состояние ошибки.
     * Мы предупреждаем пользователя об ошибке и сбросили медиаплеер.
     * @see MediaPlayer.OnErrorListener
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (callback != null) {
            callback.onError("MediaPlayer error " + what + " (" + extra + ")");
        }
        return true; // true указывает, что мы обработали ошибку
    }

    /**
     * Функция создаст медиаплеер если потребуется, или
     * перезагрузит существующий медиаплеер, если он уже существует.
     */
    private void createMediaPlayerIfNeeded() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();

            //Убедитесь, что медиаплеер будет играть во время воспроизведения.
            // Если мы этого не сделаем, процессор может заснуть во время воспроизведения песни,
            // что приведет к остановке воспроизведения.
            mediaPlayer.setWakeMode(context.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            // мы хотим, чтобы медиаплеер уведомлял нас, когда он готов к подготовке, и когда это делается:
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.setOnSeekCompleteListener(this);
            mediaPlayer.setOnBufferingUpdateListener(this);
        } else {
            mediaPlayer.reset();
        }
    }

    /**
     * Освобождает ресурсы, используемые службой для воспроизведения.
     * Это включает в себя статус «foreground service», блокировки слежения и, возможно, MediaPlayer.
     * @param releaseMediaPlayer Указывает, должен ли Media Player также быть освобожен или нет
     */
    private void relaxResources(boolean releaseMediaPlayer) {

        // остановить и освободить Media Player, если это необходимо и он существует
        if (releaseMediaPlayer && mediaPlayer != null) {
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // мы также можем освободить Wifi-lock, если мы его держим
        if (wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    private void registerAudioNoisyReceiver() {
        if (!audioNoisyReceiverRegistered) {
            context.registerReceiver(audioNoisyReceiver, audioNoisyIntentFilter);
            audioNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver() {
        if (audioNoisyReceiverRegistered) {
            context.unregisterReceiver(audioNoisyReceiver);
            audioNoisyReceiverRegistered = false;
        }
    }
}
