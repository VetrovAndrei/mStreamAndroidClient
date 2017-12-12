package com.example.mstream.mstreamandroidclient.player;

import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;

// Воровано у гугл плеера

/**
 * Интерфейс, представляющий собой локальное или удаленное воспроизведение.
 * Служба работает непосредственно с экземпляром объекта воспроизведения для
 * совершения различных вызовов, таких как воспроизведение, пауза и т.д.
 */
public interface Playback {
    /**
     * Запустите / настройте воспроизведение.
     * Ресурсы / слушатели будут распределены реализацией.
     */
    void start();

    /**
     * Остановка воспроизведения. Реализации здесь можно разделить на все ресурсы.
     * @param notifyListeners, если true, и callback был установлен как setCallback,
     * callback.onPlaybackStatusChanged будет вызываться после изменения состояния.
     */
    void stop(boolean notifyListeners);

    /**
     * Установить последнее состояние воспроизведения.
     */
    void setState(int state);

    /**
     * Получить текущее состояние {@link android.media.session.PlaybackState#getState()}
     */
    int getState();

    /**
     * @return boolean, который указывает, что он готов к использованию.
     */
    boolean isConnected();

    /**
     * @return boolean указывает, играет ли плеер или должен играть,
     * когда мы получаем фокус звука (focus gain).
     */
    boolean isPlaying();

    /**
     * @return позицию, если в данный момент воспроизводится элемент
     */
    int getCurrentStreamPosition();

    /**
     * Установите текущую позицию. Обычно используется при переключении игроков,
     * находящихся в состоянии паузы.
     * Позиция @param pos в потоке
     */
    void setCurrentStreamPosition(int pos);

    /**
     * Запросите базовый поток и обновите внутреннюю последнюю известную позицию потока.
     */
    void updateLastKnownStreamPosition();

    /**
     * @param item для воспроизведения
     */
    void play(MediaSessionCompat.QueueItem item);

    /**
     * Приостановить текущий элемент воспроизведения
     */
    void pause();

    /**
     * Искать в данной позиции
     */
    void seekTo(int position);

    /**
     * Установить текущий mediaId. Это используется только при переключении
     * с одного воспроизведения на другое.
     * @param mediaId будет установлен как текущий.
     */
    void setCurrentMediaId(String mediaId);

    /**
     * @return текущий mediaId обрабатывается в любом состоянии или null.
     */
    String getCurrentMediaId();

    int getBufferedPosition();

    interface Callback {
        /**
         * По текущей музыке завершена.
         */
        void onCompletion();

        /**
         * Статус воспроизведения изменен
         * Реализации могут использовать этот callback для обновления
         * состояния воспроизведения на сеансах мультимедиа.
         */
        void onPlaybackStatusChanged(int state);

        /**
         * @param error для добавления в PlaybackState
         */
        void onError(String error);

        /**
         * @param mediaId играет в настоящее время
         */
        void setCurrentMediaId(String mediaId);

        void onExtrasChanged( Bundle extras);

        void onDur(int dur);
    }

    /**
     * @param callback вызывается
     */
    void setCallback(Callback callback);
}
