/*
qwerted - virtual keyboard for android
Copyright (c) 2010 Moritz Haarmann. All Rights Reserved.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version
3 of the License, or (at your option) any later version.
 */
package com.qwerted;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

/**
 * Provides a view of a keyboard. Is a magic entry point, because it also
 * computes ( if desired ) predictions and assigns keys the right values. and
 * now for the o3!
 * 
 * @author moritzhaarmann
 * 
 */
public class KeyboardView extends LinearLayout implements
        SurfaceHolder.Callback, View.OnTouchListener {

    // holders of thy surfaces, keyboards, canvas..
    SurfaceView mSurfaceView;
    Keyboard mKeyboard;
    Canvas mCanvas;
    boolean ready = false;
    HashMap<KeyboardButton, Rect> mHitRectangles;
    long oldtime, newtime;
    public KeyboardViewMessageHandler mHandler;
    private KeyboardButton lastHitButton;

    // maybe make this stuff public to improve perfomance. who knows.
    public PopupWindow mPreview;
    public Preview mPreviewContent;
    public ExtraKeyboardPopup mExtraKeyboardView;
    public PopupWindow mExtraKeyboard;
    public Rect mExtraKeyboardRect;

    public static Typeface mTypeface;

    public static boolean mLayoutInitialized = false;

    public boolean mExtraKeyboardIsShown = false;

    public static Drawable mButtonBackground;
    public static Drawable mAccentBackground;

    public static Paint mPreviewBackgroundPaint;

    /**
     * constructor. bla.
     * 
     * @param kbd
     */
    public KeyboardView(final Keyboard k, final Context c) {
        super(c);
        mHandler = new KeyboardViewMessageHandler(this);

        setKeyboard(k);
        // initialize to take up 30% of the screen.
        mSurfaceView = new SurfaceView(c);
        mSurfaceView.setWillNotDraw(false);
        // the message handler
        // pull up the layout, the preview and the super small keyboard.
        initLayout();
        initPreview();
        initExtraKeyboard();
    }

    /**
     * it's up the readers imagination to find out what the purpose of this
     * method is.
     * 
     * @param k
     *            the Keyboard.
     */
    public void setKeyboard(final Keyboard k) {
        mKeyboard = k;
        mHitRectangles = new HashMap<KeyboardButton, Rect>();
        mHandler.reset();
        lastHitButton = null;
        KeyboardButton.resetMeasuredSizes();

    }

    /**
     * sets up the initial layout and all according parameters.
     */
    private void initLayout() {
        final WindowManager w = (WindowManager) this.getContext()
                .getSystemService(Context.WINDOW_SERVICE);
        final Display d = w.getDefaultDisplay();
        final int width = d.getWidth();
        final int height = d.getHeight();
        int designatedHeight;
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            designatedHeight = Math.round(height * 0.7f);
        } else {
            designatedHeight = Math.round(height * 0.5f);
        }

        this.setPadding(0, 0, 0, 0);
        final LayoutParams foo = new LayoutParams(width, designatedHeight);
        foo.gravity = Gravity.BOTTOM | Gravity.LEFT;
        foo.leftMargin = 0;
        foo.bottomMargin = 0;
        mButtonBackground = getContext().getResources().getDrawable(
                R.drawable.bg);
        mAccentBackground = getContext().getResources().getDrawable(
                R.drawable.bg);

        this.addView(mSurfaceView, foo);
        KeyboardButton.onUpdateCanvasSize(width, designatedHeight);
        this.forceLayout();
        mTypeface = Typeface.createFromAsset(this.getContext().getAssets(),
                "arial.ttf");
        this.setOnTouchListener(this);
        KeyboardButton.mBackground = BitmapFactory.decodeResource(
                this.getResources(), R.drawable.bg);
        KeyboardButton.mBackgroundRect = new Rect(0, 0,
                KeyboardButton.mBackground.getWidth(),
                KeyboardButton.mBackground.getHeight());
    }

    private void initPreview() {
        this.mPreviewContent = new Preview(this);
        this.mPreview = new PopupWindow(mPreviewContent);
        this.mPreview.setWidth(30);
        this.mPreview.setHeight(30);
        KeyboardView.mPreviewBackgroundPaint = new Paint();
        KeyboardView.mPreviewBackgroundPaint.setColor(this.getContext()
                .getResources().getColor(R.color.preview_background));

    }

    private void initExtraKeyboard() {
        this.mExtraKeyboardView = new ExtraKeyboardPopup(this);
        this.mExtraKeyboard = new PopupWindow(this.mExtraKeyboardView);
        this.mExtraKeyboard.setTouchable(true);
        this.mExtraKeyboardRect = new Rect();
        // mExtraKeyboard.setAnimationStyle(android.R.style.Animation_Toast);

    }

    public void show() {
        pullUpCanvas();

    }

    public int getInnerWidth() {
        return mSurfaceView.getWidth();
    }

    public int getInnerHeight() {
        return mSurfaceView.getHeight();
    }

    private void pullUpCanvas() {
        mSurfaceView.getHolder().addCallback(this);
    }

    /**
     * draws all buttons, regardless of their state.
     */
    public void drawButtons() {

        mCanvas = mSurfaceView.getHolder().lockCanvas();
        if (mCanvas == null) {
            return;
        }
        mCanvas.drawColor(Color.rgb(0, 0, 0));

        KeyboardButton foo;
        for (int i = 0; i < mKeyboard.mButtons.length; i++) {
            foo = mKeyboard.mButtons[i];
            foo.draw(mCanvas);
            // improve, partition.
            this.mHitRectangles.put(foo, foo.currentHitRectangle());

        }
        if (this.mExtraKeyboardIsShown) {
            final Rect r = new Rect();
            mSurfaceView.getDrawingRect(r);
            final Paint p = new Paint();
            p.setColor(Color.BLACK);
            p.setAlpha(130);

            mCanvas.drawRect(r, p);
        }

        mSurfaceView.getHolder().unlockCanvasAndPost(mCanvas);
    }

    /**
     * the callback for the ontouch. basically, everything is dispatched here ot
     * let the message handler do the dirtwork.
     */
    public boolean onTouch(final View v, final MotionEvent event) {
        if (this.mHitRectangles.size() == 0) {
            this.drawButtons();
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            final Message m = mHandler.obtainMessage();
            m.arg1 = (int) event.getX();
            m.arg2 = (int) event.getY();
            m.what = KeyboardViewMessageHandler.DOWN;
            mHandler.sendMessage(m);
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {

            final Message m = mHandler.obtainMessage();
            m.arg1 = (int) event.getX();
            m.arg2 = (int) event.getY();
            m.what = KeyboardViewMessageHandler.MOVE;
            mHandler.sendMessage(m);
        } else {

            final Message m = mHandler.obtainMessage();
            m.arg1 = (int) event.getX();
            m.arg2 = (int) event.getY();
            m.what = KeyboardViewMessageHandler.RELEASE;
            mHandler.sendMessage(m);
        }
        return true;
    }

    /**
     * locks the canvas and draws a single button on it.
     * 
     * @param b
     */
    private void drawSingleButton(final KeyboardButton b) {
        if (b == null) {
            return;
        }
        mCanvas = mSurfaceView.getHolder().lockCanvas(b.currentHitRectangle());
        b.draw(mCanvas);
        mSurfaceView.getHolder().unlockCanvasAndPost(mCanvas);
    }

    /**
     * highlights the button at the given x and y coordinates ( local to the
     * view ).
     * 
     * @param x
     * @param y
     */
    public void highlightButton(final KeyboardButton foo) {
        if (foo == null) {
            hidePreview();
            return;
        }
        showPreview(foo);
        foo.highlight = true;
        Message m;
        if (foo.sendKey() == Keyboard.BUTTON_BACK) {
            m = mHandler.obtainMessage(KeyboardViewMessageHandler.REPEAT, foo);
            mHandler.sendMessageDelayed(m, 200);
        } else {
            m = mHandler.obtainMessage(KeyboardViewMessageHandler.LONG_PRESS,
                    foo);
            mHandler.sendMessageDelayed(m, 500);

        }
        this.drawSingleButton(foo);

    }

    @Override
    protected void onDraw(final Canvas c) {
        drawButtons();
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw,
            final int oldh) {
        KeyboardButton.onUpdateCanvasSize(w, h);
    }

    public KeyboardButton findButtonForPoint(final int x, final int y) {
        if ((lastHitButton != null)
                && lastHitButton.currentHitRectangle().contains(x, y)) {
            return lastHitButton;
        }
        final Iterator<Entry<KeyboardButton, Rect>> it = this.mHitRectangles
                .entrySet().iterator();
        while (it.hasNext()) {
            final Entry<KeyboardButton, Rect> ce = it.next();
            if (ce.getValue().contains(x, y)) {
                lastHitButton = ce.getKey();
                return ce.getKey();
            }

        }
        return null;
    }

    /**
     * performs all actions after a button has been pressed ( confirmed press )
     * 
     * @param x
     * @param y
     */
    public void onButtonPressed(final KeyboardButton f) {

        if (f != null) {
            if (f.mPunched != 0) {
                mKeyboard.handleInput(f.mPunched);
            } else {
                mKeyboard.handleInput(f.sendKey());
            }

        }

    }

    /**
     * callback for a long button press.
     * 
     * @param b
     */
    public void onButtonLongPressed(final KeyboardButton b) {

    }

    public void normalizeButton(final KeyboardButton f) {
        if (f == null) {
            return;
        }
        f.highlight = false;
        this.drawSingleButton(f);
        this.hidePreview();
    }

    public void surfaceCreated(final SurfaceHolder holder) {
        ready = true;
        drawButtons();
        drawButtons();
    }

    public void surfaceDestroyed(final SurfaceHolder holder) {
        mCanvas = null;
        ready = false;
    }

    public void showExtraKeyboard(final KeyboardButton b) {
        this.mExtraKeyboardIsShown = true;
        this.drawButtons();
        if (!mExtraKeyboard.isShowing()) {
            mExtraKeyboardView.buildKeyboardFor(b);
            // mExtraKeyboardView.forceLayout();
            mExtraKeyboardView.measure(MeasureSpec.makeMeasureSpec(getWidth(),
                    MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(
                    getHeight(), MeasureSpec.AT_MOST));
            Log.d("QWERTED", "Showing extra keyboard");
            mExtraKeyboard.showAtLocation(this, Gravity.NO_GRAVITY,
                    b.currentRect.left,
                    b.currentRect.top - mExtraKeyboardView.getMeasuredHeight());
        }
        mExtraKeyboardView.requestLayout();

        mExtraKeyboard.update(b.currentRect.left, b.currentRect.top
                - mExtraKeyboardView.getMeasuredHeight(),
                mExtraKeyboardView.getMeasuredWidth(),
                mExtraKeyboardView.getMeasuredHeight());

        this.hidePreview();
    }

    public void hideExtraKeyboard() {
        Log.d("QWERTED", "hidin extra keyboard");
        this.mExtraKeyboard.dismiss();
        this.mExtraKeyboardIsShown = false;
        this.drawButtons();
    }

    /**
     * Shows the Preview popup for a given button
     */
    private void showPreview(final KeyboardButton b) {
        if (!mPreview.isShowing()) {
            mPreview.showAtLocation(this, Gravity.NO_GRAVITY, 0, 0);
        }
        final PopupWindow w = this.mPreview;
        this.mPreviewContent.setButton(b);
        w.update(b.currentRect.left, b.currentRect.top - 100,
                b.currentRect.right - b.currentRect.left, 40);

    }

    /**
     * hides the preview window
     */
    private void hidePreview() {
        mPreview.dismiss();
    }

    /**
     * well hidden: the part where the input is redirected to the keyboard.
     * 
     * @param mShiftKey
     */
    public void onSend(final char mShiftKey) {
        this.mKeyboard.handleInput(mShiftKey);

    }

    public void surfaceChanged(final SurfaceHolder arg0, final int arg1,
            final int arg2, final int arg3) {
        // java's gone bitch around wildly if we don't implement it. so .. yes.

    }

}

class Preview extends View {
    private final Rect mTempRect;

    public Preview(final KeyboardView k) {
        super(k.getContext());
        mTempRect = new Rect();

        this.getResources().getDrawable(
                android.R.drawable.screen_background_dark);
    }

    public void setButton(final KeyboardButton b) {
    }

    @Override
    public void onDraw(final Canvas canvas) {

        this.getDrawingRect(mTempRect);
    }
}
