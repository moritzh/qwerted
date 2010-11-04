/*
qwerted - virtual keyboard for android
Copyright (c) 2010 Moritz Haarmann. All Rights Reserved.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version
3 of the License, or (at your option) any later version.
 */
package com.qwerted;

import android.os.Handler;
import android.os.Message;

public class KeyboardViewMessageHandler extends Handler {
    public static final int LONG_PRESS = 1;
    public static final int RELEASE = 2;
    public static final int DOWN = 3;
    public static final int MOVE = 5;

    public static final int LONG_PRESS_CANCELLED = 4;
    public static final int REPEAT = 0;

    private final KeyboardView mKeyboardView;

    private KeyboardButton mCurrentButton;

    public KeyboardViewMessageHandler(final KeyboardView v) {
        mKeyboardView = v;
    }

    public void reset() {
        mCurrentButton = null;
    }

    @Override
    public void handleMessage(final Message msg) {
        if (mKeyboardView.mExtraKeyboardIsShown) {
            return;
        }
        switch (msg.what) {
        case DOWN:

            // highlight button, set longpressabort to false.
            mCurrentButton = mKeyboardView.findButtonForPoint(msg.arg1,
                    msg.arg2);
            if (mCurrentButton != null) {
                mKeyboardView.highlightButton(mCurrentButton);
                System.currentTimeMillis();
            }
            break;
        case RELEASE:
            if (mCurrentButton == null) {
                mCurrentButton = mKeyboardView.findButtonForPoint(msg.arg1,
                        msg.arg2);
            }

            if (mCurrentButton != null) {

                mKeyboardView.normalizeButton(mCurrentButton);
                mKeyboardView.onButtonPressed(mCurrentButton);
            } else {

            }
            mKeyboardView.hideExtraKeyboard();
            mCurrentButton = null;
            System.currentTimeMillis();
            break;
        case MOVE:
            if (mCurrentButton != null) {
                if (!mCurrentButton.currentRect.contains(msg.arg1, msg.arg2)) {
                    mKeyboardView.normalizeButton(mCurrentButton);
                } else {
                    break;
                    // nothing?
                }
            }
            mCurrentButton = mKeyboardView.findButtonForPoint(msg.arg1,
                    msg.arg2);
            mKeyboardView.highlightButton(mCurrentButton);
            break;
        case LONG_PRESS:

            if ((mCurrentButton == msg.obj) && !mCurrentButton.isSpecialKey()) {

                mKeyboardView.showExtraKeyboard(mCurrentButton);
            }
            break;
        case LONG_PRESS_CANCELLED:
        case REPEAT:
            if (mCurrentButton == msg.obj) {
                mKeyboardView.onButtonPressed(mCurrentButton);
                this.sendMessageDelayed(this.obtainMessage(REPEAT, msg.obj),
                        400);

            }
        }
    }

}
