package org.momo;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * wants to extended to indeed handle all relevant events occuring in {@link KeyboardView}
 * @see KeyboardView
 * @author moritzhaarmann
 *
 */
public class KeyboardViewTouchListener extends GestureDetector.SimpleOnGestureListener{
	KeyboardView mKeyboardView;
	private  long lastClickTime;
	public KeyboardViewTouchListener(KeyboardView v){
		super();
		mKeyboardView = v;
	}
	
	
	
	public boolean onDown(MotionEvent e){
		lastClickTime = e.getEventTime();
		return true;
	}
	
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY){
		return true;
	}
	
	public boolean onUp(MotionEvent e){
		return true;
	}
}
