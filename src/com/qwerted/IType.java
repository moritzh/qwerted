/*
qwerted - virtual keyboard for android
Copyright (c) 2010 Moritz Haarmann. All Rights Reserved.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version
3 of the License, or (at your option) any later version.
 */
package com.qwerted;

import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;

/**
 * we need to move all our keyboard-switching related tasks here. And we should
 * handle prediction here in some way, at least the ability to switch it on and
 * off on demand.
 * 
 * @author Moritz
 * 
 */
public class IType extends InputMethodService {
    private Keyboard mStandardKeyboard; // normal keybosard.
    private Keyboard mNumericalKeyboard; // small, numerical board
    private Keyboard mSymbolKeyboard; // all the numerical, symbol, stuff

    private Keyboard mActiveKeyboard;

    private KeyboardView mKeyboardView;

    private boolean isSym = false;

    private boolean mSuppressNextMovement;

    private String mTypedText;

    @Override
    public void onBindInput() {

    }

    @Override
    public View onCreateInputView() {

        mStandardKeyboard = new Keyboard(this, this.getBaseContext()
                .getResources().getXml(R.xml.keyboard));
        mNumericalKeyboard = new Keyboard(this, this.getBaseContext()
                .getResources().getXml(R.xml.numerical_keyboard));
        mSymbolKeyboard = new Keyboard(this, this.getBaseContext()
                .getResources().getXml(R.xml.smybol_board));
        mNumericalKeyboard.setPrediction(false);
        mSymbolKeyboard.setPrediction(false);
        mStandardKeyboard.setPrediction(true);

        mKeyboardView = new KeyboardView(mStandardKeyboard,
                this.getBaseContext());
        mKeyboardView.show();

        if (this.getCurrentInputEditorInfo().inputType == InputType.TYPE_CLASS_NUMBER) {
            mActiveKeyboard = mNumericalKeyboard;
        } else {
            mActiveKeyboard = mStandardKeyboard;
        }
        mKeyboardView.setKeyboard(mActiveKeyboard);
        return mKeyboardView;
    }

    @Override
    public void onStartInputView(final EditorInfo attribute,
            final boolean restarting) {
        this.updateText();
        if ((attribute.inputType & InputType.TYPE_CLASS_NUMBER) == InputType.TYPE_CLASS_NUMBER) {
            mActiveKeyboard = mNumericalKeyboard;
        } else {
            mActiveKeyboard = mStandardKeyboard;
        }
        mActiveKeyboard.onStartInputView(attribute, restarting);
        if (restarting) {
            mKeyboardView.drawButtons();
        }
    }

    @Override
    public void onUpdateSelection(final int oldSelStart, final int oldSelEnd,
            final int newSelStart, final int newSelEnd,
            final int candidatesStart, final int candidatesEnd) {
        if (this.mSuppressNextMovement) {
            mSuppressNextMovement = false;
        } else {
            this.mActiveKeyboard.updatePredictionBase(this
                    .getCurrentStartedWord());
        }
    }

    /**
     * just switch from standard to symbolic.
     */
    public void switchSym() {
        if (isSym) {

            mKeyboardView.setKeyboard(mStandardKeyboard);
        } else {
            mSymbolKeyboard.onStartInput(this.getCurrentInputEditorInfo(),
                    false);

            mKeyboardView.setKeyboard(mSymbolKeyboard);
        }
        isSym = !isSym;
        mKeyboardView.drawButtons();
    }

    public void dispatchRedraw() {
        mKeyboardView.drawButtons();
    }

    /**
     * backspace handler.
     */
    public void back() {

    }

    /**
     * returns the currently started word.
     * 
     * @return
     */
    public String getCurrentStartedWord() {
        // this.updateText();
        final ExtractedText ex = this.getCurrentInputConnection()
                .getExtractedText(new ExtractedTextRequest(), 0);
        mTypedText = ex.text.toString();
        final String s = mTypedText.substring(0, ex.selectionStart);
        final int index = s.lastIndexOf(' ');
        if ((index >= 0) && (s.length() > index)) {
            return (s.substring(index));
        } else if (index == -1) {
            return s;
        } else {
            return "";
        }
    }

    private void updateText() {
        final ExtractedText t = this.getCurrentInputConnection()
                .getExtractedText(new ExtractedTextRequest(), 0);
        if (t != null) {
            mTypedText = t.text.toString();
        } else {
            mTypedText = "";
        }
    }

    public void inputText(final char c) {
        this.getCurrentInputConnection().beginBatchEdit();
        this.getCurrentInputConnection().commitText("" + c, 1);
        mTypedText += c;
        this.getCurrentInputConnection().endBatchEdit();
        this.mSuppressNextMovement = true;

    }

    public void inputText(final String text) {
        this.getCurrentInputConnection().beginBatchEdit();
        this.getCurrentInputConnection().commitText(text, 1);
        mTypedText += text;
        this.getCurrentInputConnection().endBatchEdit();
        this.mSuppressNextMovement = true;
    }

}
