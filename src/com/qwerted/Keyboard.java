/*
qwerted - virtual keyboard for android
Copyright (c) 2010 Moritz Haarmann. All Rights Reserved.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version
3 of the License, or (at your option) any later version.
 */
package com.qwerted;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.inputmethod.EditorInfo;

import com.qwerted.dict.Dictionary;
import com.qwerted.dict.DictionaryItem;
import com.qwerted.util.DisplaySet;
import com.qwerted.util.KeyboardXMLParser;

/**
 * The keyboard.
 * 
 * @author moritzhaarmann
 * 
 */
public class Keyboard implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    public final static int BUTTON_BACK = 01;
    public final static int BUTTON_SHIFT = 02;
    public final static int BUTTON_SYM = 03;
    public final static int BUTTON_GO = 04;
    public final static int BUTTON_SPACE = 05;

    public final static int SHIFT = 1;
    public final static int NORMAL = 0;
    // the maximums of rows and columns, needed to calculate button width and
    // height.
    public int rowCount = 0;
    public float colCount = 0;

    private final Context mContext;
    private final IType mInputMethodService;
    private final Vibrator mVibrator;
    // the stuff, the stuff.

    private static Dictionary mDictionary;
    private DictionaryItem mDictionaryItem;
    private String word;

    private boolean mPredictionEnabled = true;
    private boolean mVibrateOnKey = false;

    public KeyboardButton[] mButtons;

    public HashMap<Character, KeyboardButton> mPunchMapping;

    // the cached tables for the chars/keyboardbuttns.
    HashMap<Character, KeyboardButton> mNormKeyMapping;
    HashMap<Character, KeyboardButton> mNumKeyMapping;
    HashMap<Character, KeyboardButton> mShiftKeyMapping;
    HashMap<Character, KeyboardButton> mSymKeyMapping;

    // the preference manager.
    SharedPreferences mPreferences;

    public Keyboard(final IType ss, final XmlResourceParser s) {
        // we need the context to create views.
        mContext = ss.getApplicationContext();
        mInputMethodService = ss;

        // some bootstrapping.
        mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mPreferences.registerOnSharedPreferenceChangeListener(this);
        mButtons = KeyboardXMLParser.loadKeyboardFromXml(this, s);
        mVibrator = (Vibrator) mContext.getSystemService("vibrator");

        mPunchMapping = new HashMap<Character, KeyboardButton>();
        this.createCharTables();
        getPreferences();
        word = "";

    }

    /**
     * maybe one could pull this out.
     * 
     * @param c
     */
    public void handleInput(final char c) {
        System.currentTimeMillis();
        if (this.mVibrateOnKey && (mVibrator != null)) {
            mVibrator.vibrate(50);
        }
        String text = "";
        if (c < 10) {
            switch (c) {
            case (01):
                if (mInputMethodService.getCurrentInputStarted()) {
                    mInputMethodService.getCurrentInputConnection()
                            .deleteSurroundingText(1, 0);
                }
                if (mPredictionEnabled) {
                    if ((word.length() > 1) && (mDictionaryItem != null)) {
                        mDictionaryItem = mDictionaryItem.getParent();
                        if (mDictionaryItem != null) {
                            word = mDictionaryItem.fullWord;
                        }
                        updatePredictions();
                    } else if (word.length() == 1) {
                        mDictionaryItem = null;
                        word = "";
                        updatePredictions();
                    } else {
                        this.updatePredictionBase(this.mInputMethodService
                                .getCurrentStartedWord());
                    }
                }
                this.mInputMethodService.dispatchRedraw();
                break;
            case (02):
                if (KeyboardButton.state == Keyboard.SHIFT) {
                    KeyboardButton.state = Keyboard.NORMAL;
                } else {
                    KeyboardButton.state = Keyboard.SHIFT;
                }
                unpunchButtons();
                updatePredictions();
                this.mInputMethodService.dispatchRedraw();

                break;
            case (03):
                this.mInputMethodService.switchSym();
                break;
            case (04):
                if ((this.mInputMethodService.getCurrentInputEditorInfo().inputType & EditorInfo.IME_ACTION_UNSPECIFIED) == EditorInfo.IME_ACTION_UNSPECIFIED) {
                    text = "\n";
                } else {
                    this.mInputMethodService.sendDefaultEditorAction(true);
                }

                this.mInputMethodService.dispatchRedraw();
                break;
            case (05):
                final CharSequence textBefore = mInputMethodService
                        .getCurrentInputConnection().getTextBeforeCursor(1, 0);
                if ((textBefore.length() > 0) && (textBefore.charAt(0) == ' ')) {
                    mInputMethodService.getCurrentInputConnection()
                            .deleteSurroundingText(1, 0);
                    mInputMethodService.getCurrentInputConnection().commitText(
                            ". ", 1);
                    word = "";
                    text = "";
                } else {
                    if (mPredictionEnabled) {
                        if (mDictionaryItem == null) {
                            try {
                                if ((word != null) && (word.length() > 0)) {
                                    DictionaryItem.create(word);
                                }
                            } catch (final Exception e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                mDictionaryItem.increase();
                            } catch (final Exception e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                    text = " ";
                    mDictionaryItem = null;
                    word = "";
                }
                updatePredictions();
                this.mInputMethodService.dispatchRedraw();
            }
        } else {
            text += c;
            word += c;
            if (mPredictionEnabled) {
                if (mDictionaryItem != null) {
                }
                if (word.length() == 1) {
                    try {
                        mDictionaryItem = DictionaryItem.lookup(word, true);

                    } catch (final Exception e) {
                        // TODO Auto-generated catch block

                        mDictionaryItem = null;
                    }
                } else if (mDictionaryItem != null) {
                    mDictionaryItem = mDictionaryItem.childContainingChar(c);
                }
                if ((mDictionaryItem == null) && (word.length() == 0)) {
                    try {
                        mDictionaryItem = DictionaryItem.lookup(word, true);
                    } catch (final Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }
                updatePredictions();
            }
            // reset everything to normal after .. it wasn't :-)
            if (KeyboardButton.state != Keyboard.NORMAL) {
                KeyboardButton.state = Keyboard.NORMAL;
            }
            this.mInputMethodService.dispatchRedraw();
        }

        if (mInputMethodService.getCurrentInputStarted() && (text.length() > 0)) {
            mInputMethodService.inputText(text);
            // update prediction state?
        }
    }

    private void unpunchButtons() {
        // TODO Auto-generated method stub
        final Iterator<KeyboardButton> it = this.getCurrentMapping().values()
                .iterator();
        while (it.hasNext()) {
            it.next().mPunched = 0;
        }
    }

    public Dictionary getDictionary() {

        return mDictionary;
    }

    private void bunchUpdateAllButtons(final float weight) {
        final Iterator<KeyboardButton> k = this.getCurrentMapping().values()
                .iterator();
        while (k.hasNext()) {
            k.next().updateWeight(weight);
        }

        return;
    }

    private void updatePredictions() {
        if (!mPredictionEnabled) {
            if (((mDictionaryItem == null) && (this.word.trim().length() > 0))
                    || !mPredictionEnabled) {
                bunchUpdateAllButtons(0.5f);

                return;
            }
        }
        final HashMap<Character, Float> weights = DictionaryItem
                .childrenWithWeights(mDictionaryItem);
        // temporary mapping
        // update all to be even sized if no weights present.
        if (weights.size() == 0) {
            bunchUpdateAllButtons(0.5f);

            return;
        }
        // get current active map.
        final HashMap<KeyboardButton, Float> tempMapping = new HashMap<KeyboardButton, Float>();

        final Iterator<Character> it = weights.keySet().iterator();
        KeyboardButton tButton;
        while (it.hasNext()) {
            final char c = it.next();
            /*
             * if (KeyboardButton.state == Keyboard.NORMAL) tButton =
             * mPunchMapping.get(c); else
             */
            tButton = this.getCurrentMapping().get(c);
            // null : do nothing, else: do a lot.
            if (tButton == null) {
                // nothing.
            } else {
                if (!tempMapping.containsKey(tButton)
                        || (tempMapping.containsKey(tButton) && (tempMapping
                                .get(tButton) < weights.get(c)))) {
                    tButton.updateWeight(weights.get(c));
                    tempMapping.put(tButton, weights.get(c));
                    tButton.mPunched = c;
                }
            }
        }

        // reset the buttons not in the set.
        final Iterator<KeyboardButton> kt = this.getCurrentMapping().values()
                .iterator();
        while (kt.hasNext()) {
            tButton = kt.next();
            if (!tempMapping.containsKey(tButton)) {
                tButton.updateWeight(0.0f);
            }

        }
    }

    /**
     * this method creates the tables in which mappings from chars to
     * keyboardbuttons are stored. these mappings are important because we need
     * the mto perform very fast lookups.
     */
    private void createCharTables() {
        mNormKeyMapping = new HashMap<Character, KeyboardButton>();
        mShiftKeyMapping = new HashMap<Character, KeyboardButton>();
        for (int i = 0; i < mButtons.length; i++) {
            final KeyboardButton cButton = mButtons[i];
            final DisplaySet norm = cButton.normSet;
            // check if this button is special. or not.
            if (norm.send > 10) {
                mNormKeyMapping.put(norm.send, cButton);
                if (cButton.shiftSet != null) {
                    mShiftKeyMapping.put(cButton.shiftSet.send, cButton);
                }
            }
        }
        // same for the punch.
        for (int i = 0; i < mButtons.length; i++) {
            final char[] chars = mButtons[i].getAllChars();
            for (int t = 0; t < chars.length; t++) {
                mPunchMapping.put(chars[t], mButtons[i]);
            }
        }
    }

    /**
     * gets the current char->button mapping for this keyboard.
     * 
     * @return
     */
    public HashMap<Character, KeyboardButton> getCurrentMapping() {
        if (KeyboardButton.state == Keyboard.SHIFT) {
            return mShiftKeyMapping;
        } else {
            return mNormKeyMapping;
        }
    }

    public void onFinishInput() {
        mDictionaryItem = null;
        updatePredictions();
    }

    public void onStartInput(final EditorInfo attribute,
            final boolean restarting) {
        mDictionaryItem = null;
        updatePredictions();
    }

    public void onStartInputView(final EditorInfo info, final boolean restarting) {
        mDictionaryItem = null;
        updatePredictions();
    }

    public Context getContext() {
        return mContext;
    }

    /**
     * retrieves the shared prefernce values and sets everything accordingly.
     * keys are: view.button.gap: inter-button gap, in pixels.
     * view.button.border : if there is a border view.button.round: if the
     * buttons are to be round keyboard.vibrate : if vibrate on keystroke is one
     * keyboard.prediction : if to predict.
     */
    private void getPreferences() {
        final String gap = mPreferences.getString("view.button.gap", "4");
        try {
            final int gapI = Integer.parseInt(gap);
            KeyboardButton.setButtonGap(gapI);
        } catch (final NumberFormatException e) {
            // TODO Auto-generated catch block
            KeyboardButton.setButtonGap(2);
        }

        final boolean predict = mPreferences.getBoolean("keyboard.predict",
                true);
        final boolean vibrate = mPreferences.getBoolean("keyboard.vibrate",
                false);

        final String dict = mPreferences.getString("dict.dict", null);
        if (dict != null) {
            try {
                mDictionary = new Dictionary(mContext.getFilesDir() + "/"
                        + dict.trim() + ".dict");
                DictionaryItem.dict = mDictionary;
            } catch (final IOException e) {
            }

        }

        this.mVibrateOnKey = vibrate;
        this.mPredictionEnabled = predict;
    }

    // guess what, the preference change bla.
    public void onSharedPreferenceChanged(
            final SharedPreferences sharedPreferences, final String key) {
        // TODO Auto-generated method stub
        getPreferences();
    }

    public void setPrediction(final boolean b) {
        this.mPredictionEnabled = b;
    }

    /**
     * external string to update prediction bla.
     * 
     * @param currentStartedWord
     */
    public void updatePredictionBase(final String currentStartedWord) {
        word = currentStartedWord;
        try {
            mDictionaryItem = DictionaryItem.lookup(word, true);
        } catch (final Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        this.updatePredictions();
        this.mInputMethodService.dispatchRedraw();
    }
}
