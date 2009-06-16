package org.momo;

import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ImageView.ScaleType;
public class KeyboardButton implements OnClickListener {
	public static final int WIDTH  = 30;
	public static final int HEIGHT = 60;
	
	private float span  =1.0f; // default keyspan
	private boolean isSpecialKey = false;
	
	// to not allocate memory every time we update weights.
	private RelativeLayout.LayoutParams layoutParams;
	// will be used to keep track of this row and column. float to be able to span.
	private PointF position;	
	
	// define some special keys here. obviously needed. 
	enum specialKeyTypes  {SPACE,SHIFT,NUM, BACKSPACE};
	// and the instance to keep track of it.
	private int specialKey; 
	// for the toggle stuff
	private boolean active = false;
	
	// the label.
	private String key;
	private String symbolKey;
	private String numKey;
	private String shiftKey;
	// the returnCode
	private String sendKey;
	private KeyboardView kv;
	
	private View mView;
	
	// getter setter for span
	public float getSpan() {
		return span;
	}

	public void setSpan(float span) {
		this.span = span;
	}

	// nocomment
	public PointF getPosition() {
		return position;
	}

	public void setPosition(PointF position) {
		this.position = position;
	}

	public View getView(){
		return mView;
	}
	
	private void initLayout(){
		if ( this.isSpecialKey ){
			
			ImageButton btn = new ImageButton(kv.getContext());
			switch(this.specialKey){
			case KeyboardView.BACK:
				btn.setImageResource(R.drawable.sym_keyboard_delete);
				break;
			case KeyboardView.SPACE:
				btn.setImageResource(R.drawable.sym_keyboard_space);
				break;				
			case KeyboardView.NUM:
				btn.setImageResource(R.drawable.sym_keyboard_delete);
				break;			
			case KeyboardView.SHIFT:
				btn.setImageResource(R.drawable.sym_keyboard_shift);
				break;				
			case KeyboardView.RETURN:
				btn.setImageResource(R.drawable.sym_keyboard_return);
				break;	
			}
			btn.setScaleType(ImageView.ScaleType.CENTER);
			this.mView = btn;
		} else {
			Button btn = new Button(kv.getContext());
			btn.setText(key.toString());
			btn.setTextColor(Color.WHITE);
			
			this.mView = btn;
		}
		//mView.setBackgroundResource(R.drawable.bg);
		// just for now, doesn't change too much here.
		mView.setBackgroundColor(Color.BLACK);
		mView.setPadding(0, 0, 0, 0);
		// are we special?
		setBoundaries(Math.round((position.x-1.0f)*WIDTH+2.5f),Math.round((position.y-1.0f)*HEIGHT+2.5f),Math.round(WIDTH*span),HEIGHT);
		this.mView.setVisibility(View.VISIBLE);

	}

	private void setBoundaries(int x, int y, int width, int height){
		// check for existence
		if (layoutParams==null){
			layoutParams = new RelativeLayout.LayoutParams(WIDTH,HEIGHT);
		}
		layoutParams.leftMargin = x;
		layoutParams.topMargin = y;
		layoutParams.width = width;
		layoutParams.height = height;
		this.mView.setLayoutParams(layoutParams);
		
	}
	
	public KeyboardButton(KeyboardView k, String key, String numKey, String symKey, String shiftKey, int row, float column, float span) {
		
		kv = k;
		
		this.key = key;
		this.numKey = numKey;
		this.symbolKey = symKey;
		this.shiftKey = shiftKey;
		this.span = span;
		this.position = new PointF(0.0f,0.0f);
		this.position.x = column ;
		this.position.y = row * 1.0f;

		this.isSpecialKey = true;
		if ( this.key.compareTo("SPACE")==0){
			this.specialKey = KeyboardView.SPACE;
			this.sendKey = " ";
			
		} else if (this.key.compareTo("SHIFT")==0){
			this.specialKey = KeyboardView.SHIFT;
			
		}  else if (this.key.compareTo("NUM")==0){
			this.specialKey = KeyboardView.NUM;
			
			
		}  else if (this.key.compareTo("RETURN")==0){
			this.specialKey = KeyboardView.BACK;
			this.sendKey = Character.toString((char)KeyEvent.KEYCODE_BACK);
			
		} else {
			this.sendKey = key;
			this.isSpecialKey = false;
		}
		initLayout();
		this.mView.setOnClickListener(this);

	}
	
	// this should range from 0..1, where 1 resembles 50 px and 0 20.
	public void updateWeight(float weight){
		if ( !isSpecialKey ){
			// update width to be the weight * the static width
			int newWidth = Math.round(WIDTH*weight*1.1f);
			int newLeftMargin = Math.round((WIDTH * (position.x-.5f)+(position.x*1.4f))- (newWidth/2));
			int newHeight = Math.round(HEIGHT*weight*1.0f);
			int newTopMargin = Math.round((HEIGHT*(position.y-0.5f)+(position.y*1.4f)) - (newHeight/2));
			// set Text Size accordingly.
			((Button)this.mView).setTextSize(newWidth - 15);
			// update my Layout.
			setBoundaries(newLeftMargin,newTopMargin,newWidth,newHeight);
			this.mView.forceLayout();

		}
	}
	
	// just delegate.
	public void onClick(View v) {
		if ( this.isSpecialKey ){
			if ( this.specialKey == KeyboardView.SHIFT )
				 if (this.active==true) {
					 kv.changeState(0);
					 ((ImageButton)this.mView).setAlpha(255);
					 ((ImageButton)this.mView).setColorFilter(Color.GREEN,Mode.DST_ATOP);
				 } else
					 kv.changeState(KeyboardView.SHIFT);
			else
				kv.onClickDelegate(this.sendKey);
		} else {
			kv.onClickDelegate(this.sendKey);

		}
	}

	public void updateState(int state) {
		if ( !isSpecialKey ){
		if ( state == 0 ){
			((Button)this.mView).setText(this.key);
			
		
		} else if (state == KeyboardView.SHIFT){
			String temp = this.shiftKey;
			this.shiftKey = this.key;
			this.key = temp;
			((Button)this.mView).setText(this.key);
			this.sendKey = (this.key);
		}
		}
	}
	
	public String getPredictionIdentifier(){
		return this.key;
	}


}
