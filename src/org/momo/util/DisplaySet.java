package org.momo.util;

import android.graphics.drawable.Drawable;

public class DisplaySet {
	public char send;
	public char show;
	public Drawable render;
	
	public void setKey(char c){
		this.send = c;
		this.show = c;
	}
}
