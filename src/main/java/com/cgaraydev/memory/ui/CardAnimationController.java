package com.cgaraydev.memory.ui;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.control.Button;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Animaciones visuales de cartas. Nunca modifica el estado del motor.
 * Debe utilizarse únicamente desde el hilo de aplicación de JavaFX.
 */
final class CardAnimationController {

    private record ActiveAnimation(Button button, Animation animation) {}

    private final Map<Integer, ActiveAnimation> active = new HashMap<>();
    private boolean paused;

    boolean isAnimating(int cardId) {
        return active.containsKey(cardId);
    }

    void flip(int cardId, Button button, Runnable swapFace, Runnable onFinished) {
        Objects.requireNonNull(button);
        Objects.requireNonNull(swapFace);
        Objects.requireNonNull(onFinished);

        cancel(cardId);
        resetTransforms(button);
        button.setRotationAxis(Rotate.Y_AXIS);
        button.setDisable(true);

        // Primera mitad: la cara actual se estrecha hasta quedar de canto.
        RotateTransition firstHalf = new RotateTransition(Duration.millis(145), button);
        firstHalf.setAxis(Rotate.Y_AXIS);
        firstHalf.setFromAngle(0);
        firstHalf.setToAngle(90);
        firstHalf.setInterpolator(Interpolator.EASE_IN);

        // Segunda mitad: aparece la nueva cara sin mostrar el texto invertido.
        RotateTransition secondHalf = new RotateTransition(Duration.millis(155), button);
        secondHalf.setAxis(Rotate.Y_AXIS);
        secondHalf.setFromAngle(-90);
        secondHalf.setToAngle(0);
        secondHalf.setInterpolator(Interpolator.EASE_OUT);

        SequentialTransition flip = new SequentialTransition(firstHalf, secondHalf);
        firstHalf.setOnFinished(event -> {
            if (isCurrent(cardId, flip)) {
                swapFace.run();
                button.setRotate(-90);
            }
        });
        flip.setOnFinished(event -> finish(cardId, flip, onFinished));
        active.put(cardId, new ActiveAnimation(button, flip));
        flip.play();
        if (paused) {
            flip.pause();
        }
    }

    void match(int cardId, Button button, Runnable onFinished) {
        Objects.requireNonNull(button);
        Objects.requireNonNull(onFinished);

        cancel(cardId);
        resetTransforms(button);

        // Una pequeña pulsación sin desplazar ni superponer otras cartas.
        ScaleTransition pulse = new ScaleTransition(Duration.millis(165), button);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.07);
        pulse.setToY(1.07);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        pulse.setInterpolator(Interpolator.EASE_BOTH);
        pulse.setOnFinished(event -> finish(cardId, pulse, onFinished));

        active.put(cardId, new ActiveAnimation(button, pulse));
        pulse.play();
        if (paused) {
            pulse.pause();
        }
    }

    void setPaused(boolean value) {
        if (paused == value) {
            return;
        }
        paused = value;

        for (ActiveAnimation current : active.values()) {
            if (paused && current.animation().getStatus() == Animation.Status.RUNNING) {
                current.animation().pause();
            } else if (!paused && current.animation().getStatus() == Animation.Status.PAUSED) {
                current.animation().play();
            }
        }
    }

    void cancelAll() {
        for (Integer cardId : java.util.List.copyOf(active.keySet())) {
            cancel(cardId);
        }
    }

    private void cancel(int cardId) {
        ActiveAnimation previous = active.remove(cardId);
        if (previous != null) {
            previous.animation().stop();
            resetTransforms(previous.button());
        }
    }

    private boolean isCurrent(int cardId, Animation animation) {
        ActiveAnimation current = active.get(cardId);
        return current != null && current.animation() == animation;
    }

    private void finish(int cardId, Animation animation, Runnable callback) {
        if (!isCurrent(cardId, animation)) {
            return;
        }
        ActiveAnimation completed = active.remove(cardId);
        resetTransforms(completed.button());
        callback.run();
    }

    private static void resetTransforms(Button button) {
        button.setRotate(0.0);
        button.setScaleX(1.0);
        button.setScaleY(1.0);
    }
}
