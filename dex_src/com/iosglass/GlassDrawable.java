package com.iosglass;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

public class GlassDrawable extends Drawable {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float radius;
    private final boolean isDark;
    private final float density;
    private final float borderWidth;

    public GlassDrawable(boolean isDark, float radiusPx, float density) {
        this.isDark = isDark;
        this.radius = radiusPx;
        this.density = density;
        this.borderWidth = 1.2f * density;
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(borderWidth);
    }

    @Override
    public void draw(Canvas canvas) {
        rect.set(getBounds());
        float inset = borderWidth / 2f;
        int topFill, botFill;
        if (isDark) {
            topFill = 0x33FFFFFF;
            botFill = 0x10FFFFFF;
        } else {
            topFill = 0x66FFFFFF;
            botFill = 0x2EFFFFFF;
        }
        fill.setShader(new LinearGradient(0, rect.top, 0, rect.bottom, topFill, botFill, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, radius, radius, fill);
        fill.setShader(null);

        int bTL, bBR;
        if (isDark) {
            bTL = 0x8CFFFFFF;
            bBR = 0x1AFFFFFF;
        } else {
            bTL = 0xCCFFFFFF;
            bBR = 0x55FFFFFF;
        }
        RectF inner = new RectF(rect);
        inner.inset(inset, inset);
        border.setShader(new LinearGradient(rect.left, rect.top, rect.right, rect.bottom, bTL, bBR, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(inner, radius - inset, radius - inset, border);
        border.setShader(null);

        Paint spec = new Paint(Paint.ANTI_ALIAS_FLAG);
        spec.setStyle(Paint.Style.STROKE);
        spec.setStrokeWidth(borderWidth * 1.1f);
        spec.setShader(new LinearGradient(rect.left, rect.top, rect.left, rect.top + rect.height() * 0.4f, isDark ? 0x55FFFFFF : 0xB3FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        RectF specRect = new RectF(rect);
        specRect.inset(inset, inset);
        canvas.drawArc(specRect, 180f, 180f, false, spec);
    }

    @Override public void setAlpha(int alpha) { fill.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter cf) { fill.setColorFilter(cf); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
