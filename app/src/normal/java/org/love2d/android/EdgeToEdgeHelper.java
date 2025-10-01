package org.love2d.android;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class EdgeToEdgeHelper {
    private EdgeToEdgeHelper() {
    }

    public static void applySystemBarsPadding(View view) {
        if (view == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
