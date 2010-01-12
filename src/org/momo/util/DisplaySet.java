package org.momo.util;

import android.graphics.drawable.Drawable;
import android.util.Log;

public class DisplaySet {
	public char send;
	public char extra[];
	public char show;
	public Drawable render;
	
	public void setKey(char c){
		
		this.send = c;
		this.show = c;
	}
	
	public void setKey(String c){
		String[] helper;
		helper = c.split(" ");
		if (helper.length > 0){
			this.send = helper[0].charAt(0);
			this.show = helper[0].charAt(0);
		}
		// also assign extra field
		if (helper.length>1){
			int extraLength = helper.length-1;
			extra = new char[extraLength];
			for ( int i=0;i<extraLength;i++){
				extra[i] = helper[i+1].charAt(0);
			}
		} else
			extra = new char[0];
	}
	
	
	/**
	 * nifty helper.
	 * @return a list of all chars this set holds.
	 */
	public char[] getAllCharacters(){
		char[] returnValue = new char[1+extra.length];
		returnValue[0] = this.send;
		System.arraycopy(extra, 0, returnValue, 1, extra.length);
		return returnValue;
	}
}
