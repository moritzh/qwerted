/*
qwerted - virtual keyboard for android
Copyright (c) 2010 Moritz Haarmann. All Rights Reserved.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version
3 of the License, or (at your option) any later version.
 */
package com.qwerted;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * nice little popup that opens after a long press to enabel selection of
 * special keys.
 * 
 * @author moritzhaarmann
 * 
 */
public class ExtraKeyboardPopup extends LinearLayout {
    char[] mKeys;
    KeyboardButton mButton;
    Button[] mViewButtons;
    Button closeButton;
    public final static int MAX_EXTRA_SIZE = 7;
    KeyboardView mKeyboardView;

    /**
     * the real drawing is done here.
     * 
     * @author moritzhaarmann
     * 
     */

    public ExtraKeyboardPopup(final KeyboardView k) {
        super(k.getContext());
        final LayoutParams layoutParams = new LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        this.setLayoutParams(layoutParams);
        this.setOrientation(LinearLayout.HORIZONTAL);
        // TODO Auto-generated constructor stub
        mKeyboardView = k;
        this.setBackgroundResource(android.R.drawable.alert_dark_frame);
        mViewButtons = new Button[MAX_EXTRA_SIZE];
        final View.OnClickListener extraButtonListener = new View.OnClickListener() {

            public void onClick(final View v) {
                final String f = (String) ((Button) v).getText();
                mKeyboardView.mKeyboard.handleInput(f.charAt(0));
                k.normalizeButton(mButton);
                k.hideExtraKeyboard();

            }
        };
        for (int i = 0; i < mViewButtons.length; i++) {
            mViewButtons[i] = new Button(this.getContext());
            mViewButtons[i].setOnClickListener(extraButtonListener);

            mViewButtons[i].setTextSize(22f);
            this.addView(mViewButtons[i]);
        }

        closeButton = new Button(this.getContext());
        closeButton.setBackgroundResource(android.R.drawable.btn_dialog);
        closeButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(final View v) {
                k.normalizeButton(mButton);
                k.hideExtraKeyboard();
            }
        });
        this.addView(closeButton);
        this.setVisibility(View.VISIBLE);
    }

    public void buildKeyboardFor(final KeyboardButton b) {
        this.mButton = b;
        this.mKeys = b.getActiveSet().extra;
        for (int i = 0; i < this.mKeys.length; i++) {
            this.mViewButtons[i].setText("" + mKeys[i]);
            this.mViewButtons[i].setVisibility(View.VISIBLE);
        }
        for (int t = mKeys.length; t < this.mViewButtons.length; t++) {
            mViewButtons[t].setVisibility(View.GONE);
        }
        this.forceLayout();

    }

}