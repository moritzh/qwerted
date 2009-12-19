package org.momo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.*;
import android.widget.LinearLayout;

/**
 * Provides a view of a keyboard. Is a magic entry point, because it also
 * computes ( if desired ) predictions and assigns keys the right values.
 * and now for the o3!
 * @author moritzhaarmann
 *
 */
public class KeyboardView extends LinearLayout implements SurfaceHolder.Callback, View.OnTouchListener {
	SurfaceView mSurfaceView;
	Keyboard mKeyboard;
	Canvas mCanvas;
	boolean ready = false;
	HashMap<KeyboardButton,Rect> hitRectangles;
	long oldtime,newtime;
	private int moveCounter = 0;
	private KeyboardButton lastHitButton; 
	
	Rect[] binaryPartition;
	
	/** 
	 * constructor
	 * @param kbd
	 */
	public KeyboardView(Keyboard k, Context c){
		super(c);
		mKeyboard = k;
		hitRectangles = new HashMap<KeyboardButton,Rect>();
		// initialize to take up 30% of the screen.
		mSurfaceView = new SurfaceView(c);
		
		LayoutParams foo = new LayoutParams(LayoutParams.FILL_PARENT, 224);
		foo.gravity = Gravity.BOTTOM;
		this.addView(mSurfaceView, foo);
		KeyboardButton.onUpdateCanvasSize(320, 224);
		mSurfaceView.setOnTouchListener(this);
		this.forceLayout();
		this.initPartition();
		oldtime = System.currentTimeMillis();
		
	}
	
	private void initPartition(){
		this.binaryPartition = new Rect[4];
		for(int i = 0;i<4;i++)
			this.binaryPartition[i] = new Rect();
		int height = getInnerHeight();
		int width = getInnerWidth();
		/*
		 * *****************
		 *    1   *    2
		 * *****************
		 *    3   *    4
		 * *****************
		 * left top right bottom
		 * above scheme shows the way the keyboard is partitioned.   
		 */
		binaryPartition[0] = new Rect(0,0,width/2,height/2);
		binaryPartition[1] = new Rect(width/2,0, width,height/2);
		binaryPartition[2] = new Rect(0,height/2,width/2,height);
		binaryPartition[3] = new Rect(width/2,height/2,width,height);
	}
	public void show(){
		pullUpCanvas();
	}
	
	public int getInnerWidth(){
		return mSurfaceView.getWidth();
	}
	
	public int getInnerHeight(){
		return mSurfaceView.getHeight();
	}
	
	
	private void pullUpCanvas(){
		mSurfaceView.getHolder().addCallback(this);
	}
	
	// GRAPHICS CALLBACKS
	
	/**
	 * callback that gets called after a structural change of the surface occured. 
	 * 
	 */
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		
	}

	public void surfaceCreated(SurfaceHolder holder) {
		ready = true;
		drawButtons();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		mCanvas = null;
		ready = false;
		
	}
	
	public void drawButtons(){
		mCanvas = mSurfaceView.getHolder().lockCanvas();
		
		mCanvas.drawColor(Color.WHITE);
		Paint t = new Paint();
		t.setColor(Color.BLACK);
		mCanvas.drawPaint(t);
		
		
		
		
		Iterator<KeyboardButton> it = this.mKeyboard.mButtons.iterator();
		while(it.hasNext()){
			KeyboardButton foo = it.next();
			foo.draw(mCanvas);
			// improve, partition.
			this.hitRectangles.put(foo,foo.currentHitRectangle());

		}
		// post
		mSurfaceView.getHolder().unlockCanvasAndPost(mCanvas);
	}

	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		int x,y;
		

		
		if ( event.getAction() == MotionEvent.ACTION_DOWN){
			oldtime = System.currentTimeMillis();

			// in case of over button: do something useful. else: do nothing.
			x = (int) event.getX();
			y = (int) event.getY();

			KeyboardButton b = findButtonForPoint(x,y);

			if ( b != null ){
					mKeyboard.handleInput(b.sendKey());
			
				b.highlight = false;
			
			drawButtons();
			Log.i("foo","Took" + (System.currentTimeMillis()-oldtime));

			}
		} /*else if( event.getAction() == MotionEvent.ACTION_DOWN ||event.getAction() == MotionEvent.ACTION_MOVE){
			if (MotionEvent.ACTION_MOVE == event.getAction()){
				if ( moveCounter++ < 2)
					return true;
				else
					moveCounter = 0;
			}
			x = (int) event.getX();
			y = (int) event.getY();
			KeyboardButton b = findButtonForPoint(x,y);
			
		}*/

		return true;
	}

	protected void onDraw(Canvas c){
		drawButtons();
	}
	
	protected void onSizeChanged (int w, int h, int oldw, int oldh){
		KeyboardButton.onUpdateCanvasSize(w, h);
	}
	
	private KeyboardButton findButtonForPoint(int x,int y){
		if (lastHitButton != null && lastHitButton.currentHitRectangle().contains(x, y))
			return lastHitButton;
		Iterator<Entry<KeyboardButton,Rect>> it = this.hitRectangles.entrySet().iterator();
		while(it.hasNext()){
			Entry<KeyboardButton,Rect> ce = it.next();
			if ( ce.getValue().contains(x, y) ){
				lastHitButton = ce.getKey();
				return ce.getKey();
			}
			
		}
		return null;
	}
	
	
	
}

