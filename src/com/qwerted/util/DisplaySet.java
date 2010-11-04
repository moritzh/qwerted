/*
qwerted - virtual keyboard for android
Copyright (c) 2010 Moritz Haarmann. All Rights Reserved.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version
3 of the License, or (at your option) any later version.
 */
package com.qwerted.util;

import android.graphics.drawable.Drawable;

/**
 * holds a set of characters that are represented by one key.
 * 
 * @author moritzhaarmann
 * 
 */
public class DisplaySet {
    public char send;
    public char extra[];
    public char show;
    public Drawable render;

    public void setKey(final char c) {

        this.send = c;
        this.show = c;
    }

    public void setKey(final String c) {
        String[] helper;
        helper = c.split(" ");
        if (helper.length > 0) {
            this.send = helper[0].charAt(0);
            this.show = helper[0].charAt(0);
        }
        // also assign extra field
        if (helper.length > 1) {
            final int extraLength = helper.length - 1;
            extra = new char[extraLength];
            for (int i = 0; i < extraLength; i++) {
                extra[i] = helper[i + 1].charAt(0);
            }
        } else {
            extra = new char[0];
        }
    }

    /**
     * nifty helper.
     * 
     * @return a list of all chars this set holds.
     */
    public char[] getAllCharacters() {
        final char[] returnValue = new char[1 + extra.length];
        returnValue[0] = this.send;
        System.arraycopy(extra, 0, returnValue, 1, extra.length);
        return returnValue;
    }
}
