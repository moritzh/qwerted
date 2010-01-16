package org.momo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.momo.util.DisplaySet;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

/**
 * Provides a view of a keyboard. Is a magic entry point, because it also
 * computes ( if desired ) predictions and assigns keys the right values. and
 * now for the o3!
 * 
 * @author moritzhaarmann
 * 
 */
public class KeyboardView extends LinearLayout implements
		SurfaceHolder.Callback, View.OnTouchListener {

	// holders of thy surfaces, keyboards, canvas..
	SurfaceView mSurfaceView;
	Keyboard mKeyboard;
	Canvas mCanvas;
	boolean ready = false;
	HashMap<KeyboardButton, Rect> mHitRectangles;
	long oldtime, newtime;
	public KeyboardViewMessageHandler mHandler;
	private KeyboardButton lastHitButton;

	// maybe make this stuff public to improve perfomance. who knows.
	public PopupWindow mPreview;
	public Preview mPreviewContent;
	public ExtraKeyboard mExtraKeyboardView;
	public PopupWindow mExtraKeyboard;
	public Rect mExtraKeyboardRect;

	public static Typeface mTypeface;

	public static boolean mLayoutInitialized=  false;
	
	public static Drawable mButtonBackground;
	public static Drawable mAccentBackground;


	public static Paint mPreviewBackgroundPaint;

	/**
	 * constructor. bla.
	 * 
	 * @param kbd
	 */
	public KeyboardView(Keyboard k, Context c) {
		super(c);
		mHandler = new KeyboardViewMessageHandler(this);

		setKeyboard(k);
		// initialize to take up 30% of the screen.
		mSurfaceView = new SurfaceView(c);
		// the message handler
		// pull up the layout, the preview and the super small keyboard.
		initLayout();
		initPreview();
		initExtraKeyboard();
	}

	/**
	 * it's up the readers imagination to find out what the purpose of this method is.
	 * @param k the Keyboard.
	 */
	public void setKeyboard(Keyboard k){
		mKeyboard = k;
		mHitRectangles = new HashMap<KeyboardButton, Rect>();
		mHandler.reset();
		lastHitButton = null;
		KeyboardButton.resetMeasuredSizes();

	}
	
	
	/**
	 * sets up the initial layout and all according parameters.
	 */
	private void initLayout() {
		WindowManager w = (WindowManager) this.getContext().getSystemService(Context.WINDOW_SERVICE);
        Display d = w.getDefaultDisplay(); 
        int width = d.getWidth(); 
        int height = d.getHeight(); 
        Log.d("W", "Width:" + width + "  Height: "+ height);
        int designatedHeight;
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        	designatedHeight = Math.round(height*0.7f);
        else
        	designatedHeight = Math.round(height*0.5f);

        this.setPadding(0,0,0,0);
		LayoutParams foo = new LayoutParams(width, designatedHeight);
		foo.gravity = Gravity.BOTTOM | Gravity.LEFT;
		foo.leftMargin = 0;
		foo.bottomMargin = 0;
		mButtonBackground = getContext().getResources().getDrawable(R.drawable.bg);
		mAccentBackground = getContext().getResources().getDrawable(R.drawable.bg);

		this.addView(mSurfaceView, foo);
		KeyboardButton.onUpdateCanvasSize(width, designatedHeight);
		this.forceLayout();
		mTypeface = Typeface.createFromAsset(this.getContext().getAssets(),
				"arial.ttf");
		this.setOnTouchListener(this);
		KeyboardButton.mBackground = BitmapFactory.decodeResource(this
				.getResources(), R.drawable.bg);
		KeyboardButton.mBackgroundRect = new Rect(0, 0,
				KeyboardButton.mBackground.getWidth(),
				KeyboardButton.mBackground.getHeight());
	}

	private void initPreview() {
		this.mPreviewContent = new Preview(this);
		this.mPreview = new PopupWindow(mPreviewContent);
		this.mPreview.setWidth(30);
		this.mPreview.setHeight(30);
		KeyboardView.mPreviewBackgroundPaint = new Paint();
		KeyboardView.mPreviewBackgroundPaint.setColor(this.getContext().getResources().getColor(R.color.preview_background));
		KeyboardView.mPreviewBackgroundPaint.setAlpha(200);
		//this.mPreviewBackgroundPaint.setShadowLayer(5.00f, 0.0f, 0.0f,
			//	Color.BLACK);

	}

	private void initExtraKeyboard() {
		this.mExtraKeyboardView = new ExtraKeyboard(this);
		this.mExtraKeyboard = new PopupWindow(this.mExtraKeyboardView);		this.mExtraKeyboard.setTouchable(true);
		this.mExtraKeyboardRect = new Rect();
		mExtraKeyboard.setAnimationStyle(android.R.style.Animation_Toast);

	}

	public void show() {
		pullUpCanvas();

	}

	public int getInnerWidth() {
		return mSurfaceView.getWidth();
	}

	public int getInnerHeight() {
		return mSurfaceView.getHeight();
	}

	private void pullUpCanvas() {
		mSurfaceView.getHolder().addCallback(this);
	}

	// GRAPHICS CALLBACKS

	/**
	 * draws all buttons, regardless of their state.
	 */
	public void drawButtons() {
			mCanvas = mSurfaceView.getHolder().lockCanvas();
		
		mCanvas.drawColor(Color.rgb(110, 110, 110));

		KeyboardButton foo;
		for (int i = 0; i < mKeyboard.mButtons.length; i++) {
			foo = mKeyboard.mButtons[i];
			foo.draw(mCanvas);
			// improve, partition.
			this.mHitRectangles.put(foo, foo.currentHitRectangle());

		}
		mSurfaceView.getHolder().unlockCanvasAndPost(mCanvas);
		
	}

	/**
	 * the callback for the ontouch. basically, everything is dispatched here ot
	 * let the message handler do the dirtwork.
	 */
	public boolean onTouch(View v, MotionEvent event) {
		if ( this.mHitRectangles.size()==0)
			this.drawButtons();
		float tY = event.getY()<0  ?  -1.0f * event.getY()+this.getInnerHeight() : this.getInnerHeight()-event.getY();

		if ( this.mExtraKeyboard.isShowing() && mExtraKeyboardRect.contains((int)event.getX(),(int) tY)){
			// try to process it with the extra keyboard.
			
			if ( mExtraKeyboardView.onTouch(event))
				return true;
				
		}
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			Message m = mHandler.obtainMessage();
			m.arg1 = (int) event.getX();
			m.arg2 = (int) event.getY();
			m.what = KeyboardViewMessageHandler.DOWN;
			mHandler.sendMessage(m);
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			Message m = mHandler.obtainMessage();
			m.arg1 = (int) event.getX();
			m.arg2 = (int) event.getY();
			m.what = KeyboardViewMessageHandler.RELEASE;
			mHandler.sendMessage(m);
		} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			Message m = mHandler.obtainMessage();
			m.arg1 = (int) event.getX();
			m.arg2 = (int) event.getY();
			m.what = KeyboardViewMessageHandler.MOVE;
			mHandler.sendMessage(m);
		} 
		return true;
	}

	/**
	 * locks the canvas and draws a single button on it.
	 * 
	 * @param b
	 */
	private void drawSingleButton(KeyboardButton b) {
		if (b == null)
			return;
		mCanvas = mSurfaceView.getHolder().lockCanvas(b.currentHitRectangle());
		b.draw(mCanvas);
		mSurfaceView.getHolder().unlockCanvasAndPost(mCanvas);
	}

	/**
	 * highlights the button at the given x and y coordinates ( local to the
	 * view ).
	 * 
	 * @param x
	 * @param y
	 */
	public void highlightButton(final KeyboardButton foo) {
		if (foo == null) {
			hidePreview();
			return;
		}
		showPreview(foo);
		foo.highlight = true;
		Message m = mHandler.obtainMessage(KeyboardViewMessageHandler.LONG_PRESS, foo);
		
		mHandler.sendMessageDelayed(m,500);
		this.drawSingleButton(foo);
		this.drawButtons();

	}

	@Override
	protected void onDraw(Canvas c) {
		Paint p = new Paint();
		p.setColor(Color.BLUE);
		drawButtons();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		//KeyboardButton.onUpdateCanvasSize(w, h);
	}

	public KeyboardButton findButtonForPoint(int x, int y) {
		if (lastHitButton != null
				&& lastHitButton.currentHitRectangle().contains(x, y))
			return lastHitButton;
		Iterator<Entry<KeyboardButton, Rect>> it = this.mHitRectangles
				.entrySet().iterator();
		while (it.hasNext()) {
			Entry<KeyboardButton, Rect> ce = it.next();
			if (ce.getValue().contains(x, y)) {
				lastHitButton = ce.getKey();
				return ce.getKey();
			}

		}
		return null;
	}

	/**
	 * performs all actions after a button has been pressed ( confirmed press )
	 * 
	 * @param x
	 * @param y
	 */
	public void onButtonPressed(final KeyboardButton f) {

		if (f != null) {
			if ( f.mPunched != 0)
				mKeyboard.handleInput(f.mPunched);
			else
				mKeyboard.handleInput(f.sendKey());

		}

	}

	/**
	 * callback for a long button press.
	 * 
	 * @param b
	 */
	public void onButtonLongPressed(final KeyboardButton b) {

	}

	public void normalizeButton(final KeyboardButton f) {
		if (f == null)
			return;
		f.highlight = false;
		this.drawSingleButton(f);
		this.hidePreview();
	}

	/**
	 * callback that gets called after a structural change of the surface
	 * occured.
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

	public void showExtraKeyboard(final KeyboardButton b) {

		if (!mExtraKeyboard.isShowing())
			mExtraKeyboard.showAtLocation(this, Gravity.LEFT | Gravity.BOTTOM,
					10, 10);
		mExtraKeyboardView.buildKeyboardFor(b);
		AlphaAnimation bar = new AlphaAnimation(0.0f,255f);
		bar.setDuration(400);
		Rect f = mExtraKeyboardView.getDrawRect();
		// update to match the location of b.
		int wX  = Math.max(b.currentRect.centerX(), 60);
		wX = Math.min(wX, this.getInnerWidth()-60);
		mExtraKeyboardRect.left = wX - 60;
		mExtraKeyboardRect.top = this
		.getInnerHeight()
		- b.currentRect.top;
		mExtraKeyboardRect.right = mExtraKeyboardRect.left + 120;
		mExtraKeyboardRect.bottom = mExtraKeyboardRect.top + 130;

	
		mExtraKeyboard.update(wX-60, this
				.getInnerHeight()
				- b.currentRect.top, 120, 130);
		this.hidePreview();
	}

	public void hideExtraKeyboard() {
		this.mExtraKeyboard.dismiss();

	}

	/**
	 * Shows the Preview popup for a given button
	 */
	private void showPreview(final KeyboardButton b) {
		if (!mPreview.isShowing())
			mPreview.showAtLocation(this, Gravity.NO_GRAVITY, 0, 0);
		final PopupWindow w = this.mPreview;
		this.mPreviewContent.setButton(b);
		w.update(b.currentRect.left, b.currentRect.top - 100,
				b.currentRect.right - b.currentRect.left, 40);

	}

	/**
	 * hides the preview window
	 */
	private void hidePreview() {
		mPreview.dismiss();
		// mPreview.getContentView().setVisibility(View.INVISIBLE);
	}

	public void onSend(char mShiftKey) {
		this.mKeyboard.handleInput(mShiftKey);
		
	}

}

class Preview extends View {
	private KeyboardButton mKeyboardButton;
	private Drawable mDrawable;
	public Preview(KeyboardView k) {
		super(k.getContext());
		mDrawable = this.getResources().getDrawable(android.R.drawable.screen_background_dark);
		// TODO Auto-generated constructor stub
	}

	public void setButton(KeyboardButton b) {
		mKeyboardButton = b;
	}

	@Override
	public void onDraw(Canvas canvas) {
		Rect r = new Rect();
		
		this.getDrawingRect(r);
		ButtonRenderer.drawButton(canvas, mKeyboardButton.getActiveSet().show,
				mKeyboardButton.getActiveSet().render, r,
				KeyboardButton.borderPaint,
			mDrawable, KeyboardButton.textPaint);
	}
}

class ExtraKeyboard extends View {
	char[] mKeys;
	KeyboardButton mButton;

	Rect[] mCharKeys; // the accented keys
	Rect mPlay; // the play forward button
	Rect mShift; // shift/unshift possibility

	static Paint mPlayPaint;
	static Paint mShiftPaint;
	static Paint mKeyPaint;
	static Paint mDisabledPaint;

	KeyboardView mKeyboardView;
	private char mShiftKey;

	/**
	 * the real drawing is done here.
	 * 
	 * @author moritzhaarmann
	 * 
	 */

	public ExtraKeyboard(KeyboardView k) {
		super(k.getContext());
		// TODO Auto-generated constructor stub
		mKeyboardView = k;
		paintBuilder();
		

	}

	public Rect getDrawRect() {
		buildBoxes();
		Rect ret = new Rect(mShift);
		// ret.union(mButton.currentRect);
		ret.union(mPlay);
		return mShift;
	}

	@Override
	public void onDraw(Canvas c) {
		/*
		 * we are not going to do too much if there is no button.
		 */
		if (mButton == null) {
			return;
		}
		buildBoxes();
		DisplaySet workingSet;
		Rect r = new Rect();
		this.getDrawingRect(r);
		//c.drawRect(r,mShiftPaint);
		// the keys
		if ( KeyboardButton.state == Keyboard.NORMAL){
			workingSet = mButton.normSet;
			mShiftKey = mButton.shiftSet.show;
		} else {
			mShiftKey = mButton.normSet.show;
			workingSet = mButton.shiftSet;
		}

		if ( workingSet.extra.length > 0)
		ButtonRenderer
				.drawButton(c, workingSet.extra[0], null, mCharKeys[0],
						KeyboardButton.borderPaint,KeyboardButton.mDefaultBackground,
						KeyboardButton.textPaint);
		
		if ( workingSet.extra.length > 1)
			ButtonRenderer
			.drawButton(c, workingSet.extra[1], null, mCharKeys[1],
					KeyboardButton.borderPaint,KeyboardButton.mDefaultBackground,
					KeyboardButton.textPaint);
		if ( workingSet.extra.length > 2)
			ButtonRenderer
			.drawButton(c, workingSet.extra[2], null, mCharKeys[2],
					KeyboardButton.borderPaint,KeyboardButton.mDefaultBackground,
					KeyboardButton.textPaint);
		if ( workingSet.extra.length > 3)
			ButtonRenderer
			.drawButton(c, workingSet.extra[3], null, mCharKeys[3],
					KeyboardButton.borderPaint,KeyboardButton.mDefaultBackground,
					KeyboardButton.textPaint);
		ButtonRenderer.drawButton(c, mShiftKey, null, mShift,
				KeyboardButton.borderPaint,KeyboardButton.mDefaultBackground,
				KeyboardButton.textPaint);

	}

	/**
	 * to get a bit of users love and admiration, we'll make this skinnable some
	 * time.
	 */
	private void paintBuilder() {  
		// green seems to be nice for the play..
		mShiftPaint = new Paint();
		mShiftPaint.setAlpha(200);
		mShiftPaint.setColor(mKeyboardView.getContext().getResources().getColor(R.color.shift_background));
		mKeyPaint = new Paint();
		mKeyPaint.setColor(mKeyboardView.getContext().getResources().getColor(R.color.accent_background));
		mKeyPaint.setAlpha(200);
		mDisabledPaint = new Paint();
		mDisabledPaint.setColor(Color.GRAY);
	}

	public void buildKeyboardFor(final KeyboardButton b) {
		mButton = b;
		mShift = new Rect();
		mPlay = new Rect();
		mCharKeys = new Rect[4];
		this.mKeys = b.getActiveSet().extra;
	}

	
	private void buildBoxes() {
		// for convenience, we'll pull in the basic button sizes here.
		// we need to build at least 2 boxes, for play and for modified
		// character.

		mShift.bottom = 130;
		mShift.top = 0; // 1.5
		mShift.left = 40;
		mShift.right = 80;

		mCharKeys[0] = new Rect();
		mCharKeys[0].left = 0;
		mCharKeys[0].bottom = 65;
		mCharKeys[0].top = 0;
		mCharKeys[0].right = 40;

		mCharKeys[1] = new Rect();
		mCharKeys[1].left = 0;
		mCharKeys[1].bottom = 130;
		mCharKeys[1].top = 65;
		mCharKeys[1].right = 40;

		mCharKeys[2] = new Rect();
		mCharKeys[2].left = 80;
		mCharKeys[2].bottom = 65;
		mCharKeys[2].top = 0;
		mCharKeys[2].right = 120;

		mCharKeys[3] = new Rect();
		mCharKeys[3].left = 80;
		mCharKeys[3].bottom = 130;
		mCharKeys[3].top = 65;
		mCharKeys[3].right = 120;

		// the boxes for the obvious accent keys :-)

	}

	public boolean onTouch(MotionEvent event) {
		// TODO Auto-generated method stub
		// puh. 
		// translate event.
		float tY = event.getY()<0  ?  -1.0f * event.getY()+mKeyboardView.getInnerHeight() : mKeyboardView.getInnerHeight()-event.getY();

		int x = (int)event.getX()-mKeyboardView.mExtraKeyboardRect.left;
		int y = mKeyboardView.mExtraKeyboardRect.height() - (int) (tY-mKeyboardView.mExtraKeyboardRect.top);

		if ( event.getAction()==MotionEvent.ACTION_UP){
			// press key.
			if ( mShift.contains(x, y)){
				mKeyboardView.onSend(this.mShiftKey);
				deactiv‰te();

				return true;
			} else if ( this.mKeys.length > 0 && this.mCharKeys[0].contains(x, y)){
				mKeyboardView.onSend(this.mKeys[0]);
				deactiv‰te();

				return true;
			}
			else if ( this.mKeys.length >1 && this.mCharKeys[1].contains(x, y)){
				mKeyboardView.onSend(this.mKeys[1]);
				deactiv‰te();

				return true;
			}else if ( this.mKeys.length > 2 && this.mCharKeys[2].contains(x, y)){
				mKeyboardView.onSend(this.mKeys[2]);
				deactiv‰te();
				return true;
			}else if ( this.mKeys.length > 3 && this.mCharKeys[3].contains(x, y)){
				mKeyboardView.onSend(this.mKeys[3]);
				deactiv‰te();

				return true;
			}
			
		} else if ( event.getAction() == MotionEvent.ACTION_MOVE){
			return true;
		}
		
		return false;
	}
	
	private void deactiv‰te(){
		mKeyboardView.hideExtraKeyboard();

		mKeyboardView.normalizeButton(mButton);
		mKeyboardView.drawButtons();
	}

}
