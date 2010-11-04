/*
qwerted - virtual keyboard for android
Copyright (c) 2010 Moritz Haarmann. All Rights Reserved.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version
3 of the License, or (at your option) any later version.
 */
package com.qwerted;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

/**
 * Simple utility class to render buttons identicially. This is important
 * becuase buttons are used quite.. extensively.
 * 
 * @author moritzhaarmann
 * 
 */
public class NiceButtonRenderer implements ButtonRenderer {
    // we have that here to avoid allocation.
    static Rect mTempRect;
    static Rect mAnotherTempRect;
    static RectF mTempRectF;
    static Drawable mTempDrawable;
    static Drawable mHighlightBackground;
    static Drawable mDefaultBackground;
    static Drawable mPunchBackground;
    static Paint mTextPaint;

    static {
        mTempRect = new Rect();
        mAnotherTempRect = new Rect();
    }

    public void initializeEnvironment(final Context c) {

        mHighlightBackground = c.getResources().getDrawable(
                R.drawable.btn_keyboard_key_pressed);
        mDefaultBackground = c.getResources().getDrawable(R.drawable.qwertedbg);

        mPunchBackground = new GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP, new int[] {
                        Color.GREEN, Color.WHITE });
        ((GradientDrawable) mPunchBackground)
                .setGradientType(GradientDrawable.LINEAR_GRADIENT);
        mTextPaint = new Paint();
        mTextPaint.setColor(c.getResources().getColor(R.color.key_color));
        mTextPaint.setAntiAlias(true);
    }

    /**
     * looks ugly? right you are.
     */
    public final void drawButton(final KeyboardButton b, final Canvas canvas) {
        mTempDrawable = (b.highlight ? mHighlightBackground
                : mDefaultBackground);
        mTempDrawable.setBounds(b.getExpandedRect());
        mTempDrawable.draw(canvas);
        mTempRect.set(b.currentRect);
        mTempDrawable.getPadding(mAnotherTempRect);
        mTextPaint.setTextSize(KeyboardButton.baseButtonHeight * 0.8f);
        // we prefer graphics.
        if (b.getActiveSet().render == null) {
            canvas.drawText(b.getActiveSet().show + "", mTempRect.centerX()
                    - mTextPaint.measureText(b.getActiveSet().show + "") / 2,
                    mTempRect.centerY() + mTextPaint.getTextSize() / 2.0f,
                    mTextPaint);
        } else {
            final int bHeight = b.getActiveSet().render.getIntrinsicHeight();
            final int bWidth = b.getActiveSet().render.getIntrinsicWidth();
            mTempRect.top = mTempRect.centerY() - bHeight / 2;
            mTempRect.bottom = mTempRect.top + bHeight;
            mTempRect.left = mTempRect.centerX() - bWidth / 2;
            mTempRect.right = mTempRect.left + bWidth;
            b.getActiveSet().render.setBounds(mTempRect);

            b.getActiveSet().render.draw(canvas);
        }
    }

}
