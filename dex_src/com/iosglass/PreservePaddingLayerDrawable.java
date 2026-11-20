package com.iosglass;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

public class PreservePaddingLayerDrawable extends LayerDrawable {
    public PreservePaddingLayerDrawable(Drawable base, Drawable overlay) {
        super(new Drawable[]{ base, overlay });
    }

    @Override
    public boolean getPadding(Rect padding) {
        return getLayer(0).getPadding(padding);
    }
}
