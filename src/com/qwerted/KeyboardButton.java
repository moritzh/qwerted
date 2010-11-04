/*
qwerted - virtual keyboard for android
Copyright (c) 2010 Moritz Haarmann. All Rights Reserved.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version
3 of the License, or (at your option) any later version.
 */
package com.qwerted;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.qwerted.util.DisplaySet;

/**
 * basically, the button is some kind of information-holding entity. it is
 * capable of drawing itself onto a surface, handle some basic events ( like the
 * resizing of the environment ), knows pretty well about how it should look in
 * different situations, and is able to negotiate its size.
 * 
 * @author moritzhaarmann
 * 
 */

public class KeyboardButton {

    // background drawable, set by KeyboardView
    public static Bitmap mBackground;
    public static Rect mBackgroundRect;

    public char mPunched;

    // just for profiling and debugging
    public static long timeTaken;

    // the special key types.
    public static final String KEY_SHIFT = "SHIFT";
    public static final String KEY_SYM = "SYM";
    public static final String KEY_SPACE = "SPACE";
    public static final String KEY_GO = "GO";
    public static final String KEY_MAGIC = "MAGIC";
    public static final String KEY_BACK = "BACK";

    public static boolean isSpecial = false;

    // just some "defines" for orientations
    public static final short DIAG = 1;
    public static final short STRAIGHT = 2;

    // if this is set to true, any negotiation may not modify this buttons below
    // value.
    public boolean dontModifyBelow = false;

    public static short state = Keyboard.NORMAL;

    // the layout stuff
    public int row;
    public float column;
    public float columnSpan;
    // the weight of this button in this round, for the expansion strategy.
    // 0 means no demands, 1 means godlike, and 0.5 is just the value where all
    // buttons are equal in size.
    public float weight = 0.5f;
    public static int negCount = 0;
    // the relationed buttons, below.
    // if both are set, we have a symmetrical condition
    public KeyboardButton buttonRelStraightBelow;
    public KeyboardButton buttonRelDiagBelow;
    public KeyboardButton buttonRelRight;

    // the type of relation this button has to negotiate. is set in
    // keyboard.java
    // and used here.
    public short relType;

    // the inter-button gap.
    private static int gap = 4;

    Rect currentRect;

    // where the actual information about what is displayed etc is stored.
    // see the displayset below for how it works.
    public DisplaySet shiftSet, normSet;
    // wether this button is displayed highlighted.
    public boolean highlight = false;
    // the current drawable rectangle. this is, of course, also the

    // the rectangle used to store the negotiation results.
    private final RectF negotiationRect = new RectF(0.5f, 0.5f, 0.5f, 0.5f);

    // the screen width and height.
    // maybe those should be taken to keyboardview.
    private static int screenX;
    private static int screenY;

    // will be populated later.
    public static int baseButtonHeight = 0;
    public static int baseButtonWidth = 0;

    // the paints are stored here so they don't have to be recreated each draw.
    static Paint borderPaint;
    static Paint fillPaint;
    static Paint highlightPaint;
    public char currentSendChar;
    private final Keyboard keyboard;

    private ButtonRenderer mButtonRenderer;

    /**
     * public constructor
     * 
     * @param keys
     *            a list of characters this button is responsible for. the first
     *            four buttons are used to render either normal, shift, numeric
     *            and symbol state, all other buttons are displayed in advanced
     *            mode.
     * @param row
     *            the number of row.
     * @param rowSpan
     *            the vertical span of this button
     * @param column
     *            the column number
     * @param columnSpan
     *            the horizontal span of this button
     */
    public KeyboardButton(final DisplaySet norm, final DisplaySet shift,
            final float row, final float column, final float columnSpan,
            final Keyboard k) {
        this.keyboard = k;
        // init row and column setup, as well as the chars. no surprises here
        this.row = (int) row;
        this.normSet = norm;
        this.shiftSet = shift;
        this.mPunched = 0;
        this.column = column;
        this.columnSpan = columnSpan;
        if (mButtonRenderer == null) {
            mButtonRenderer = new NiceButtonRenderer();
            mButtonRenderer.initializeEnvironment(k.getContext());
        }
        if (this.row > keyboard.rowCount) {
            keyboard.rowCount = this.row;
        }
        if (this.column > keyboard.colCount) {
            keyboard.colCount = this.column;
            // init the paints
        }

    }

    /**
     * returns the current hit rectangle
     * 
     * @return the hit rectangle, as Rect
     */
    public Rect currentHitRectangle() {
        return currentRect;

    }

    /**
     * draws this button onto canvas c
     * 
     * @param c
     *            the canvas to draw on .
     */
    public void draw(final Canvas c) {
        // thinking
        if (this.needsRenegotiation()) {
            this.renegotiateSize();
        }
        final long startTime = System.currentTimeMillis();

        currentRect = this.getExpandedRect();

        mButtonRenderer.drawButton(this, c);

        KeyboardButton.timeTaken += System.currentTimeMillis() - startTime;

    }

    public void updateState(final int state) {

    }

    /**
     * returns the current active send key for the active displayste.
     * 
     * @return
     */
    public char sendKey() {
        return this.getActiveSet().send;
    }

    /**
     * returns the active DisplaySet of this button considering the active state
     * of the keyboard ( saved in KeyboardButton.state ).
     * 
     * @return a DisplaySet representing the current Displayset
     */
    DisplaySet getActiveSet() {

        switch (KeyboardButton.state) {

        case (Keyboard.SHIFT):
            if (shiftSet != null) {
                return shiftSet;
            } else {
                return normSet;
            }

        default:
            return normSet;
        }
    }

    /**
     * returns the base button width of a button in the current keyboard. this
     * method caches the values in the fields KeyboardButton.baseButtonWidth but
     * one should always use this method to retrieve the value.
     * 
     * @return the base width of a button, in px, as int.
     */
    private int getBaseButtonWidth() {
        if (KeyboardButton.baseButtonWidth == 0) {
            // basic math: we have the total width of all gaps, the total screen
            // width
            // and want to know how wide a button can be. huh!
            final int roundedColCount = Math.round(keyboard.colCount);

            final int totalGapWidth = KeyboardButton.gap * roundedColCount;
            // take screen width, substract what we have and divide it by the
            // count.
            final int totalWidthAvailable = KeyboardButton.screenX
                    - totalGapWidth;
            final int expandedWidth = totalWidthAvailable / roundedColCount;
            // because the value above is only valid for expansion of 0.5 on
            // each side,
            // it's time to get the real value.
            KeyboardButton.baseButtonWidth = Math.round(expandedWidth * 0.5f);
            return KeyboardButton.baseButtonWidth;
        } else {
            return KeyboardButton.baseButtonWidth;
        }

    }

    /**
     * calculate expansion rectangle from local negotiation rectangle and the
     * base rectangle ( that is, the gap, the base button height and width ).
     * 
     * @return the expanded rectangle as Rect
     */
    Rect getExpandedRect() {
        final int width = this.getBaseButtonWidth();
        final int height = this.getBaseButtonHeight();
        // first calculate all extra widths and heights allowed.
        final int leftExtra = Math.round(this.negotiationRect.left * width);
        final int rightExtra = Math.round(this.negotiationRect.right * width);
        final int topExtra = Math.round(this.negotiationRect.top * height);
        final int bottomExtra = Math
                .round(this.negotiationRect.bottom * height);

        final int totalHeight = topExtra + bottomExtra + height;
        final int totalWidth = (int) (leftExtra + rightExtra
                + (width * this.columnSpan + ((this.columnSpan - 1) * width)) + KeyboardButton.gap
                * Math.abs(columnSpan - 1.0f));

        final int top = (this.row - 1) * (height * 2) + (this.row - 1) * gap
                + gap / 2 + (height / 2 - topExtra);
        final int left = (int) Math.abs((this.column - 1.0f) * width * 2
                + (this.column - 1) * gap + gap / 2)
                + (width / 2 - leftExtra);
        final int bottom = top + totalHeight;
        final int right = left + totalWidth;
        return new Rect(left, top, right, bottom);
    }

    /**
     * returns the base button height of a button in the current keyboard. this
     * method caches the values in the fields KeyboardButton.baseButtonHeight
     * but one should always use this method to retrieve the value.
     * 
     * @return the base height of a button, in px, as int.
     */
    private int getBaseButtonHeight() {
        if (KeyboardButton.baseButtonHeight == 0) {
            // basic math: we have the total width of all gaps, the total screen
            // width
            // and want to know how wide a button can be. huh!
            final int rowCount = keyboard.rowCount;

            final int totalGapWidth = KeyboardButton.gap * rowCount;
            // take screen width, substract what we have and divide it by the
            // count.
            final int totalHeightAvailable = KeyboardButton.screenY
                    - totalGapWidth;
            final int expandedHeight = totalHeightAvailable / rowCount;
            // because the value above is only valid for expansion of 0.5 on
            // each side,
            // it's time to get the real value.
            KeyboardButton.baseButtonHeight = (int) Math.abs(Math
                    .floor(expandedHeight * 0.5f));

            return KeyboardButton.baseButtonHeight;
        } else {
            return KeyboardButton.baseButtonHeight;
        }

    }

    /**
     * yeah, lame-ass handler for updating the screenX and screenY fields.
     * 
     * @param x
     *            the new width of the canvas
     * @param y
     *            the new height-
     */
    public static void onUpdateCanvasSize(final int x, final int y) {
        KeyboardButton.screenX = x;
        KeyboardButton.screenY = y;
        KeyboardButton.baseButtonHeight = 0;
        KeyboardButton.baseButtonWidth = 0;
    }

    /**
     * updates this buttons weight and also changes the fillColor accordingly.
     * 
     * @param newWeight
     *            the new weight of this button.
     */
    public void updateWeight(final float newWeight) {
        // just for testing
        if (weight == newWeight) {
            // to avoid being recalc.
            return;
        }
        weight = newWeight;
        mPunched = 0;
        // int rgbVal = 150 + Math.round(newWeight * 100.0f);
        // fillPaint.setColor(Color.rgb(rgbVal, 255, 255));
    }

    /**
     * resets the negotiation rectangle. It only resets the bottom and the right
     * values, because the left and the top values may only be modified by
     * related buttons.
     */
    private void resetNegotiation() {
        if (this.dontModifyBelow == false) {
            negotiationRect.bottom = 0.5f;
            // negotiationRect.right = 0.5f;
        }

    }

    /**
     * returns whether this button is a special button, i.e. its send key in the
     * normal keyset is smaller than 10.
     * 
     * @return
     */
    public boolean isSpecialKey() {
        return (this.normSet.send < 10);
    }

    /**
     * renegotiates size based on the related buttons of this button. please
     * note that this method never changes the upper, and left values of the
     * expanded rectangle.
     */
    void renegotiateSize() {

        if (!this.needsRenegotiation()) {
            return;
        }

        resetNegotiation();
        KeyboardButton.negCount++;
        // store our temp values.
        // first case: straight.

        if ((this.relType == KeyboardButton.DIAG) && (this.row != 2)) {
            // second try, much simpler
            // just negotiate with the button right and the button below
            final float aToC = this.negotiate(this, this.buttonRelDiagBelow);
            final float aToB = this.negotiate(this, this.buttonRelRight);
            final float bToC = this.negotiate(this.buttonRelRight,
                    this.buttonRelDiagBelow);

            final float largerDownDistance = Math.max(aToC, bToC);

            if ((this.dontModifyBelow && (largerDownDistance < this.negotiationRect.bottom))
                    || (this.dontModifyBelow == false)) {
                this.negotiationRect.bottom = largerDownDistance;
            }
            // the right and left stuff is clear.
            this.negotiationRect.right = aToB;
            if (this.buttonRelRight != null) {
                this.buttonRelRight.dontModifyBelow = true;
                this.buttonRelRight.negotiationRect.left = 1.0f - aToB;
                this.buttonRelRight.negotiationRect.bottom = largerDownDistance;
            }

            if (this.buttonRelDiagBelow != null) {
                this.buttonRelDiagBelow.negotiationRect.top = 1.0f - largerDownDistance;
            }

        } else if (this.relType == KeyboardButton.STRAIGHT) {
            // find out the max growth for both right and below.
            float maxGrowRight;
            float maxGrowBelow;
            if (((this.buttonRelDiagBelow != null) && this.buttonRelDiagBelow
                    .isSpecialKey())
                    || ((this.buttonRelRight != null) && this.buttonRelRight
                            .isSpecialKey())) {
                maxGrowRight = 0.5f;
            } else if ((this.buttonRelStraightBelow == null)
                    || (this.weight > this.buttonRelStraightBelow.weight)) {
                final float growA = this.negotiate(this, this.buttonRelRight);
                final float growB = this.negotiate(this,
                        this.buttonRelDiagBelow);
                maxGrowRight = Math.min(growA, growB);
            } else {
                final float growA = this.negotiate(this.buttonRelStraightBelow,
                        this.buttonRelRight);
                final float growB = this.negotiate(this.buttonRelStraightBelow,
                        this.buttonRelDiagBelow);
                maxGrowRight = Math.min(growA, growB);
            }
            if (((this.buttonRelStraightBelow != null) && this.buttonRelStraightBelow
                    .isSpecialKey())
                    || ((this.buttonRelDiagBelow != null) && this.buttonRelDiagBelow
                            .isSpecialKey())) {
                maxGrowBelow = 0.5f;
            } else if ((this.buttonRelRight == null)
                    || (this.buttonRelRight.weight < this.weight)) {
                final float growC = this.negotiate(this,
                        this.buttonRelStraightBelow);
                final float growD = this.negotiate(this,
                        this.buttonRelDiagBelow);
                maxGrowBelow = Math.max(growC, growD);
            } else {
                final float growC = this.negotiate(this.buttonRelRight,
                        this.buttonRelStraightBelow);
                final float growD = this.negotiate(this.buttonRelRight,
                        this.buttonRelDiagBelow);
                maxGrowBelow = Math.max(growC, growD);
            }

            // set the buttons.
            this.negotiationRect.right = maxGrowRight;
            if (this.negotiationRect.bottom < maxGrowBelow) {
                this.negotiationRect.bottom = maxGrowBelow;
            }

            if (this.buttonRelRight != null) {
                this.buttonRelRight.negotiationRect.left = 1.0f - maxGrowRight;
                this.buttonRelRight.negotiationRect.bottom = maxGrowBelow;
            }

            if ((this.buttonRelStraightBelow != null)
                    && !this.buttonRelStraightBelow.isSpecialKey()) {
                if (this.buttonRelStraightBelow.negotiationRect.top > (1.0f - maxGrowBelow)) {
                    this.buttonRelStraightBelow.negotiationRect.top = 1.0f - maxGrowBelow;
                }
                if (this.buttonRelStraightBelow.negotiationRect.right > (1.0f - maxGrowRight)) {
                    this.buttonRelStraightBelow.negotiationRect.right = maxGrowRight;
                }
            }
            if ((this.buttonRelDiagBelow != null)
                    && !this.buttonRelDiagBelow.isSpecialKey()) {
                this.buttonRelDiagBelow.negotiationRect.left = 1.0f - maxGrowRight;
                this.buttonRelDiagBelow.negotiationRect.top = 1.0f - maxGrowBelow;
            }
        }

        // mark dirty
    }

    /**
     * checks if this button, and/or related buttons are dirty, and if one is
     * true, returns true, otherwise false. IMPORTANT: Left here for historical
     * reasons, but it turns out that it's actually faster to calculate all
     * relations than to figure out which ones could be omitted.
     * 
     * @return if this button needs to renegotiate its size.
     */
    boolean needsRenegotiation() {

        /*
         * if (this.dirty) { return true; } else { if ((this.buttonRelRight !=
         * null && this.buttonRelRight.dirty) || (this.buttonRelDiagBelow !=
         * null && this.buttonRelDiagBelow .dirty) ||
         * (this.buttonRelStraightBelow != null && this.buttonRelStraightBelow
         * .dirty) ) return true; else return false; }
         */
        return true;

    }

    /**
     * returns all characters this key is able to display.
     * 
     * @return
     */
    public char[] getAllChars() {
        if (this.isSpecialKey()) {
            return new char[0];
        }
        final char[] normSet = this.normSet.getAllCharacters();
        final char[] cShiftSet = this.shiftSet.getAllCharacters();
        final char returnSet[] = new char[normSet.length + cShiftSet.length];
        System.arraycopy(normSet, 0, returnSet, 0, normSet.length);
        System.arraycopy(cShiftSet, 0, returnSet, normSet.length,
                cShiftSet.length);
        return returnSet;
    }

    /**
     * returns the negotiated weight between two buttons. there may also be null
     * values passed in, which results in 0.5f always.
     * 
     * @param from
     *            the button for which the result is valid
     * @param to
     *            the other button to negotiate with
     * @return a value between 0.0 and 1.0 as float
     */
    private float negotiate(final KeyboardButton from, final KeyboardButton to) {

        if ((from == null) || (to == null)) {
            return 0.5f;

        }
        final float fromWeight = from.weight;
        final float toWeight = to.weight;
        if ((fromWeight == toWeight) || from.isSpecialKey()
                || to.isSpecialKey()) {
            return 0.5f;
        } else if (fromWeight == 0.0) {
            return 0.0f;
        } else if (toWeight == 0.0) {
            return 1.0f;
        } else {
            final float scale = 1.0f / ((fromWeight) + (toWeight));
            return fromWeight * scale;
        }
    }

    public static void setButtonGap(final int gap) {
        KeyboardButton.gap = gap;
        KeyboardButton.baseButtonHeight = 0;
        KeyboardButton.baseButtonWidth = 0;

    }

    /**
     * no one can really tell what it does.
     */
    public static void resetMeasuredSizes() {
        baseButtonHeight = 0;
        baseButtonWidth = 0;

    }

}
