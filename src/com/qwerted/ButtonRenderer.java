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

public interface ButtonRenderer {
    public void initializeEnvironment(Context c);

    public void drawButton(KeyboardButton b, Canvas c);
}
