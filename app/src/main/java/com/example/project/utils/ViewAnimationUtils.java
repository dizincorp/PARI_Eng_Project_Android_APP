package com.example.project.utils;

import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Utility helper providing tactile, responsive button click animations (press-bounce and 360° rotation).
 */
public final class ViewAnimationUtils {

    private ViewAnimationUtils() {
        // Utility class
    }

    /**
     * Plays a tactile press-bounce animation (scales down to 0.85f and springs back to 1.0f).
     *
     * @param view  The target view to animate
     * @param onEnd Optional callback to execute after the animation completes
     */
    public static void playBounceAnimation(@Nullable View view, @Nullable Runnable onEnd) {
        if (view == null) {
            if (onEnd != null) onEnd.run();
            return;
        }

        view.animate().cancel();
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);

        view.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(90)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(110)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .withEndAction(() -> {
                                if (onEnd != null) {
                                    onEnd.run();
                                }
                            })
                            .start();
                })
                .start();
    }

    /**
     * Plays an interactive 360° spin rotation animation accompanied by a tactile bounce.
     *
     * @param view  The target view to animate
     * @param onEnd Optional callback to execute after the animation completes
     */
    public static void playRotateAnimation(@Nullable View view, @Nullable Runnable onEnd) {
        if (view == null) {
            if (onEnd != null) onEnd.run();
            return;
        }

        view.animate().cancel();
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);

        view.animate()
                .rotationBy(360f)
                .scaleX(0.86f)
                .scaleY(0.86f)
                .setDuration(160)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(160)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .withEndAction(() -> {
                                if (onEnd != null) {
                                    onEnd.run();
                                }
                            })
                            .start();
                })
                .start();
    }
}
