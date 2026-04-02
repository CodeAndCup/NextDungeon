package fr.perrier.dungeons.module.cinematic.clock;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.IntConsumer;

/**
 * Implémentation de l'horloge cinématique basée sur le temps réel.
 * <p>
 * Calcule les frames à 20 fps (50ms/frame) à partir du temps de lecture accumulé.
 * Tolère le lag serveur en sautant automatiquement des frames si nécessaire.
 */
public class CinematicClockImpl implements CinematicClock {

    private static final long MILLIS_PER_FRAME = 50; // 20 fps

    private Duration playTime = Duration.ZERO;
    private boolean paused = false;
    private int lastFrame = -1;
    private final List<IntConsumer> listeners = new CopyOnWriteArrayList<>();

    @Override
    public int getCurrentFrame() {
        return (int) (playTime.toMillis() / MILLIS_PER_FRAME);
    }

    @Override
    public void tick(Duration deltaTime) {
        if (paused) return;

        playTime = playTime.plus(deltaTime);
        int currentFrame = getCurrentFrame();

        if (currentFrame != lastFrame) {
            lastFrame = currentFrame;
            notifyListeners(currentFrame);
        }
    }

    @Override
    public void pause() {
        paused = true;
    }

    @Override
    public void resume() {
        paused = false;
    }

    @Override
    public void setFrame(int frame) {
        playTime = Duration.ofMillis(frame * MILLIS_PER_FRAME);
        lastFrame = frame;
        notifyListeners(frame);
    }

    @Override
    public void addFrameChangeListener(IntConsumer listener) {
        listeners.add(listener);
    }

    @Override
    public void removeFrameChangeListener(IntConsumer listener) {
        listeners.remove(listener);
    }

    @Override
    public boolean isRunning() {
        return !paused;
    }

    private void notifyListeners(int frame) {
        for (IntConsumer listener : listeners) {
            try {
                listener.accept(frame);
            } catch (Exception e) {
                // Continue notifying other listeners even if one fails
            }
        }
    }
}
