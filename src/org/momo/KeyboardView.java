package org.momo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
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
	private GestureDetector mListener;
	private KeyboardButton lastHitButton; 
	
	public static Typeface mTypeface;
	
	Rect[] binaryPartition;
	private long mLastEventTime;
	
	/** 
	 * constructor
	 * @param kbd
	 */
	public KeyboardView(Keyboard k, Context c){
		super(c);
		mKeyboard = k;
		mListener = new GestureDetector(c, new KeyboardViewTouchListener(this));
		hitRectangles = new HashMap<KeyboardButton,Rect>();
		// initialize to take up 30% of the screen.
		mSurfaceView = new SurfaceView(c);
		this.setOnTouchListener(this);
		LayoutParams foo = new LayoutParams(LayoutParams.FILL_PARENT, 224);
		foo.gravity = Gravity.BOTTOM;
		this.addView(mSurfaceView, foo);
		KeyboardButton.onUpdateCanvasSize(320, 224);
		this.forceLayout();
		this.initPartition();
		mTypeface = Typeface.createFromAsset(c.getAssets(), "arial.ttf");
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
		KeyboardButton.timeTaken = 0;
		mCanvas = mSurfaceView.getHolder().lockCanvas();

		
		mCanvas.drawColor(Color.rgb(110, 110,110));
		
		Iterator<KeyboardButton> it = this.mKeyboard.mButtons.iterator();
		while(it.hasNext()){
			KeyboardButton foo = it.next();
			foo.draw(mCanvas);
			// improve, partition.
			this.hitRectangles.put(foo,foo.currentHitRectangle());

		}
		// post
		mSurfaceView.getHolder().unlockCanvasAndPost(mCanvas);
		Log.d("NEGTIME", "Time taken for negotiations:" + KeyboardButton.timeTaken);
	}

	public boolean onTouch(View v, MotionEvent event) {
		if ( event.getAction() == MotionEvent.ACTION_DOWN){
			//Log.d("UP", "Time: " + System.currentTimeMillis());
			this.highlightButton((int)event.getX(), (int)event.getY());

			//mLastEventTime = event.getEventTime();
			return true;
		} else if (event.getAction() == MotionEvent.ACTION_UP){
			Log.d("UP", "Time: " + System.currentTimeMillis());
			//long timeTaken = event.getEventTime()-mLastEventTime;
			//if (timeTaken<150){
			this.highlightButton(-1, -1);

			this.onButtonPressed((int)event.getX(), (int)event.getY());

			//} else {
			//}
			return true;
		
		} else if (event.getAction() == MotionEvent.ACTION_MOVE){
			this.highlightButton((int)event.getX(), (int)event.getY());
			
			return true;
		}
		return true;
	}
	
	public void onButtonPressed(int x, int y){
		long oldtime = System.currentTimeMillis();
		KeyboardButton b = findButtonForPoint(x,y);

		if ( b != null ){
				mKeyboard.handleInput(b.sendKey());
		
			b.highlight = false;
		
		drawButtons();
		}
		Log.d("BC", "Time taken: " + (System.currentTimeMillis() - oldtime));

	}
	
	private void drawButton(KeyboardButton foo,Canvas mCanvas){
		if (foo.needsRenegotiation())
			foo.renegotiateSize();

		foo.currentRect = foo.getExpandedRect();
		if ( foo.highlight){
			Paint hPaint = new Paint();
			hPaint.setColor(Color.RED);
			foo.drawRectWithBorder(mCanvas, foo.currentRect, 2, foo.borderPaint, hPaint);
		} else {
			foo.drawRectWithBorder(mCanvas, foo.currentRect, 2, foo.borderPaint, foo.fillPaint);

		}
		
		// we prefer graphics.
		if (foo.getActiveSet().render == null) {
			mCanvas.drawText(foo.getActiveSet().show + "", foo.currentRect.centerX(),
					foo.currentRect.centerY() + 8, foo.textPaint);
			foo.currentSendChar = foo.getActiveSet().send;
		} else {

			Rect nBounds = new Rect();
			int bHeight = foo.normSet.render.getIntrinsicHeight();
			int bWidth = foo.normSet.render.getIntrinsicWidth();
			nBounds.top = foo.currentRect.centerY() - bHeight / 2;
			nBounds.bottom = nBounds.top + bHeight;
			nBounds.left = foo.currentRect.centerX() - bWidth / 2;
			nBounds.right = nBounds.left + bWidth;
			foo.normSet.render.setBounds(nBounds);

			foo.normSet.render.draw(mCanvas);
		}
	}

	public void highlightButton(int x, int y){
		if ( lastHitButton != null ){
			mCanvas = mSurfaceView.getHolder().lockCanvas(lastHitButton.currentHitRectangle());

			lastHitButton.highlight = false;
			lastHitButton.draw(mCanvas);
			mSurfaceView.getHolder().unlockCanvasAndPost(mCanvas);

		}

		if ( lastHitButton != null && lastHitButton.currentHitRectangle().contains(x, y)){
			lastHitButton.highlight = true;
		//	lastHitButton.draw(mCanvas);

			return;
		} else {
			KeyboardButton b = findButtonForPoint(x,y);
			
			if ( b != null ){
				b.highlight = true;
			}
			lastHitButton = b;

		}
		if (lastHitButton!=null){
			//lastHitButton.draw(mCanvas);
		}
	}
	
	protected void onDraw(Canvas c){

		drawButtons();
	}
	
	protected void onSizeChanged (int w, int h, int oldw, int oldh){
		Log.i("S", "Size changes, ");
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

