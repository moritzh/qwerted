/*
qwerted - virtual keyboard for android
Copyright (c) 2010 Moritz Haarmann. All Rights Reserved.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version
3 of the License, or (at your option) any later version.
 */
package com.qwerted.util;

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.res.XmlResourceParser;

import com.qwerted.Keyboard;
import com.qwerted.KeyboardButton;
import com.qwerted.R;

public class KeyboardXMLParser {
    // for our xml-parser session
    public static final String TAG = "Keyboard";
    public static final String TAG_ROW = "Row";
    public static final String TAG_KEY = "Key";
    public static final String TAG_SPECIAL = "SpecialKey";
    public static final String TAG_SPAN = "Spacer";

    public static KeyboardButton[] loadKeyboardFromXml(final Keyboard k,
            final XmlResourceParser xml) {
        int tag;
        int row = 0;
        float span;
        final Vector<KeyboardButton> outputButtons = new Vector<KeyboardButton>();
        String drawable, normalKey, shiftKey;
        // String[] alternativeKeys;
        boolean isSpecial = false;
        float column = 0.0f;
        Vector<KeyboardButton> rowButtons = null, previousRowButtons = null;
        KeyboardButton lastButton = null;
        rowButtons = new Vector<KeyboardButton>();
        try {
            // state machine
            while ((tag = xml.next()) != XmlPullParser.END_DOCUMENT) {
                if (tag == XmlPullParser.START_TAG) {
                    if (TAG.compareTo(xml.getName()) == 0) {
                        // new keyboard, no surprise
                    } else if (TAG_ROW.compareTo(xml.getName()) == 0) {
                        row++;
                        previousRowButtons = rowButtons;
                        rowButtons = new Vector<KeyboardButton>();
                        lastButton = null;
                        column = 1.0f;
                    } else if (TAG_SPAN.compareTo(xml.getName()) == 0) {
                        column += xml.getAttributeFloatValue(null, "amount",
                                0.5f);
                    } else if ((TAG_KEY.compareTo(xml.getName()) == 0)
                            || (TAG_SPECIAL.compareTo(xml.getName()) == 0)) {

                        isSpecial = (TAG_KEY.compareTo(xml.getName()) == 0) ? false
                                : true;
                        span = xml.getAttributeFloatValue(null, "span", 1.0f);
                        normalKey = xml.getAttributeValue(null, "key");
                        shiftKey = xml.getAttributeValue(null, "key_shift");
                        drawable = xml.getAttributeValue(null, "drawable");
                        // instantiate new button.

                        final KeyboardButton b = createButton(new String[] {
                                normalKey, shiftKey }, drawable, row, column,
                                span, isSpecial, k);
                        outputButtons.add(b);
                        // add this button to the mapping of the current row,
                        // for further calculations.
                        rowButtons.add(b);
                        // set this as the button next to the previous one. this
                        // is the simple part.

                        if (lastButton != null) {
                            lastButton.buttonRelRight = b;
                        }
                        // calculate vertical relations.
                        if ((previousRowButtons != null)
                                && (previousRowButtons.size() > 0)) {
                            // iterate until an effective span+column is
                            // wider than this buttons span+column.
                            final Iterator<KeyboardButton> it = previousRowButtons
                                    .iterator();
                            while (it.hasNext()) {
                                final KeyboardButton prevRowButton = it.next();
                                // if the button above is really above us..
                                // where is this button?
                                final float rightEdge = prevRowButton.column
                                        + prevRowButton.columnSpan;
                                /*
                                 * a lot of distinct cases here: a button can
                                 * either be directly over another button, in
                                 * which case everything is clear. a button can
                                 * as well be, when columns are aligned, be
                                 * right next to the button above this one. in
                                 * this case, there is also a relation.
                                 */
                                if ((rightEdge > b.column)
                                        && (rightEdge < (b.column + b.columnSpan))) {
                                    prevRowButton.buttonRelDiagBelow = b;
                                    prevRowButton.relType = KeyboardButton.DIAG;

                                } else if ((rightEdge == b.column)
                                        && (prevRowButton.columnSpan == b.columnSpan)) {
                                    prevRowButton.buttonRelDiagBelow = b;
                                    prevRowButton.relType = KeyboardButton.STRAIGHT;
                                    // add a left-vertical relation
                                } else if (rightEdge == (b.column + b.columnSpan)) {
                                    prevRowButton.buttonRelStraightBelow = b;
                                    prevRowButton.relType = KeyboardButton.STRAIGHT;

                                }
                            }
                        }

                        column += span;
                        lastButton = b;
                    }
                }
            }
        } catch (final XmlPullParserException e) {
            e.printStackTrace();
            System.out.println("error");

        } catch (final IOException e) {
            e.printStackTrace();
            System.out.println("error");

        }
        final KeyboardButton[] output = new KeyboardButton[outputButtons.size()];
        return outputButtons.toArray(output);
    }

    private static KeyboardButton createButton(final String[] keys,
            final String drawable, final int row, final float column,
            final float span, final boolean special, final Keyboard k) {
        final DisplaySet normSet = new DisplaySet();
        final DisplaySet shiftSet = new DisplaySet();
        if (drawable != null) {
            final int res = k.getContext().getResources()
                    .getIdentifier(drawable, "drawable", "org.momo");
            if (res != 0) {
                normSet.render = k.getContext().getResources().getDrawable(res);
            }
        }
        // check for "special key"
        final String normalKey = keys[0];
        if (!special) {
            if (normalKey.length() > 0) {
                normSet.setKey(normalKey);
            }
            if (keys[1].length() > 0) {
                shiftSet.setKey(keys[1]);
            }

            return new KeyboardButton(normSet, shiftSet, row, column, span, k);
        } else {
            // special key.
            if (normalKey.compareTo(KeyboardButton.KEY_BACK) == 0) {
                normSet.render = k.getContext().getResources()
                        .getDrawable(R.drawable.sym_keyboard_delete);
                normSet.send = (char) 01;
            } else if (normalKey.compareTo(KeyboardButton.KEY_SHIFT) == 0) {
                normSet.render = k.getContext().getResources()
                        .getDrawable(R.drawable.sym_keyboard_shift);
                normSet.send = (char) 02;
            } else if (normalKey.compareTo(KeyboardButton.KEY_SYM) == 0) {
                normSet.render = k.getContext().getResources()
                        .getDrawable(R.drawable.sym_keyboard_done);
                normSet.send = (char) 03;
            } else if (normalKey.compareTo(KeyboardButton.KEY_GO) == 0) {
                normSet.render = k.getContext().getResources()
                        .getDrawable(R.drawable.sym_keyboard_return);
                normSet.send = (char) 04;
            } else if (normalKey.compareTo(KeyboardButton.KEY_SPACE) == 0) {
                normSet.render = k.getContext().getResources()
                        .getDrawable(R.drawable.sym_keyboard_space);
                normSet.send = (char) 05;
            }
            return new KeyboardButton(normSet, null, row, column, span, k);
        }
    }
}
