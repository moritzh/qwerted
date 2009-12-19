package org.momo;

import java.util.Vector;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.NinePatch;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.util.Log;
import org.momo.util.*;

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
	// the special key types.
	public static final String KEY_SHIFT = "SHIFT";
	public static final String KEY_SYM = "SYM";
	public static final String KEY_SPACE = "SPACE";
	public static final String KEY_GO = "GO";
	public static final String KEY_MAGIC = "MAGIC";
	public static final String KEY_BACK = "BACK";

	// just some "defines" for orientations
	public static final short DIAG = 1;
	public static final short STRAIGHT = 2;

	public boolean dontModifyBelow = false;
	public boolean dontModifyRight = false;

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

	public short relType;

	// the maximums of rows and columns, needed to calculate button width and
	// height.
	private static int rowCount = 0;
	private static float colCount = 0;

	// the inter-button gap.
	private static final int gap = 2;

	private Rect currentRect;

	// where the actual information about what is displayed etc is stored.
	// see the displayset below for how it works.
	public DisplaySet numSet, symSet, shiftSet, normSet;
	public static NinePatch mBackgroundDrbl;
	// wether this button is displayed highlighted.
	public boolean highlight = false;
	// the current drawable rectangle. this is, of course, also the
	// hit-rectangle.
	private Rect mRect;
	// the base-rectangle. the
	private Rect baseRect;
	// the rectangle used to store the negotiation results.
	private RectF negotiationRect = new RectF(0.5f, 0.5f, 0.5f, 0.5f);
	// the screen width and height.
	private static int screenX;
	private static int screenY;

	private static int baseButtonHeight = 0;
	private static int baseButtonWidth = 0;

	// the paints are stored here so they don't have to be recreated each draw.
	private Paint borderPaint, fillPaint, textPaint;
	public char currentSendChar;

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
	public KeyboardButton(DisplaySet norm, DisplaySet num, DisplaySet shift,
			DisplaySet sym, float row, float column, float columnSpan) {
		// init row and column setup, as well as the chars. no surprises here
		this.row = (int) row;
		this.normSet = norm;
		this.numSet = num;
		this.shiftSet = shift;
		this.symSet = sym;
		this.column = column;
		this.columnSpan = columnSpan;
		if (this.row > KeyboardButton.rowCount)
			KeyboardButton.rowCount = this.row;
		if (this.column > KeyboardButton.colCount)
			KeyboardButton.colCount = this.column;
		// init the paints
		borderPaint = new Paint();
		borderPaint.setColor(Color.rgb(60, 60, 60));
		fillPaint = new Paint();
		fillPaint.setColor(Color.rgb(90, 90, 90));
		textPaint = new Paint();
		textPaint.setAntiAlias(true);
		textPaint.setColor(Color.WHITE);
		textPaint.setTypeface(Typeface.DEFAULT_BOLD);
		textPaint.setTextSize(18f);
		textPaint.setTextAlign(Align.CENTER);
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
		if (this.needsRenegotiation())
			this.renegotiateSize();
		currentRect = this.getExpandedRect();

		// drawing

		// c.drawRoundRect(new RectF(currentRect), 5f, 5f, fillPaint);
		drawRectWithBorder(c, currentRect, 1, borderPaint, fillPaint);
		// we prefer graphics.
		if (getActiveSet().render == null) {
			c.drawText(getActiveSet().show + "", currentRect.centerX(),
					currentRect.centerY() + 4, textPaint);
			this.currentSendChar = getActiveSet().send;
		} else {

			Rect nBounds = new Rect();
			int bHeight = normSet.render.getIntrinsicHeight();
			int bWidth = normSet.render.getIntrinsicWidth();
			nBounds.top = currentRect.centerY() - bHeight / 2;
			nBounds.bottom = nBounds.top + bHeight;
			nBounds.left = currentRect.centerX() - bWidth / 2;
			nBounds.right = nBounds.left + bWidth;
			normSet.render.setBounds(nBounds);

			normSet.render.draw(c);
		}
	}

	/**
	 * helper to draw a rectangle with a border, rounded.
	 * 
	 * @param c
	 *            the canvas
	 * @param r
	 *            the rectangle to draw
	 * @param bWidth
	 *            the borderwidth, in px.
	 * @param border
	 *            the border color.
	 * @param fill
	 *            the fill-color.
	 */
	private void drawRectWithBorder(Canvas c, Rect r, int bWidth, Paint border,
			Paint fill) {
		Rect fillRect = new Rect(r.left + bWidth, r.top + bWidth, r.right
				- bWidth, r.bottom - bWidth);
		c.drawRoundRect(new RectF(r), 2, 2, border);
		c.drawRoundRect(new RectF(fillRect), 2, 2, fill);
	}

	public void updateState(int state) {

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
	private DisplaySet getActiveSet() {

		switch (KeyboardButton.state) {

		case (Keyboard.SHIFT):
			if (shiftSet != null)
				return shiftSet;
			else
				return normSet;
		case (Keyboard.NUMERICAL):
			if (numSet != null)
				return numSet;
			else
				return normSet;
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
			int roundedColCount = Math.round(KeyboardButton.colCount);

			int totalGapWidth = KeyboardButton.gap * roundedColCount;
			// take screen width, substract what we have and divide it by the
			// count.
			int totalWidthAvailable = KeyboardButton.screenX - totalGapWidth;
			int expandedWidth = totalWidthAvailable / roundedColCount;
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
	private Rect getExpandedRect() {
		int width = this.getBaseButtonWidth();
		int height = this.getBaseButtonHeight();
		// first calculate all extra widths and heights allowed.
		int leftExtra = Math.round(this.negotiationRect.left * width);
		int rightExtra = Math.round(this.negotiationRect.right * width);
		int topExtra = Math.round(this.negotiationRect.top * height);
		int bottomExtra = Math.round(this.negotiationRect.bottom * height);

		int totalHeight = topExtra + bottomExtra + height;
		int totalWidth = (int) (leftExtra + rightExtra
				+ (width * this.columnSpan + ((this.columnSpan - 1) * width)) + KeyboardButton.gap
				* Math.abs(columnSpan - 1.0f));

		int top = (this.row - 1) * (height * 2) + (this.row - 1) * gap + gap
				/ 2 + (height / 2 - topExtra);
		int left = (int) Math.abs((this.column - 1.0f) * width * 2
				+ (this.column - 1) * gap + gap / 2)
				+ (width / 2 - leftExtra);
		int bottom = top + totalHeight;
		int right = left + totalWidth;
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
			int rowCount = KeyboardButton.rowCount;

			int totalGapWidth = KeyboardButton.gap * rowCount;
			// take screen width, substract what we have and divide it by the
			// count.
			int totalHeightAvailable = KeyboardButton.screenY - totalGapWidth;
			int expandedHeight = totalHeightAvailable / rowCount;
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
	public static void onUpdateCanvasSize(int x, int y) {
		KeyboardButton.screenX = x;
		KeyboardButton.screenY = y;
	}

	/**
	 * updates this buttons weight and also changes the fillColor accordingly.
	 * 
	 * @param newWeight
	 *            the new weight of this button.
	 */
	public void updateWeight(float newWeight) {
		// just for testing
		if (weight == newWeight) {
			// to avoid being recalc.
			this.dirty = false;
			return;
		}
		dirty = true;
		weight = newWeight;
		int rgbVal = Math.round(newWeight * 160.0f);
		fillPaint.setColor(Color.rgb(rgbVal, 0, 0));
	}

	/**
	 * resets the negotiation rectangle. It only resets the bottom and the right
	 * values, because the left and the top values may only be modified by
	 * related buttons.
	 */
	private void resetNegotiation() {
		if (this.dontModifyBelow == false)
			negotiationRect.bottom = 0.5f;
		// negotiationRect.right = 0.5f;

	}

	/**
	 * dirty is always used directly
	 */
	private boolean dirty = false;


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
	private void renegotiateSize() {
		if (!this.needsRenegotiation()) {
			return;
		}

		resetNegotiation();
		KeyboardButton.negCount++;
		// store our temp values.
		// first case: straight.

		if (this.relType == KeyboardButton.DIAG) {
			// all variables here use the notation a to d as base, where a is
			// "this", b is the button right next to this, c is the button below
			// a and b and d is the button on the lower-right

			float aRight = this.negotiate(this, this.buttonRelRight);

			float aLowerLimit = this.negotiate(this, this.buttonRelDiagBelow);
			float bLowerLimit = this.negotiate(this.buttonRelRight, this.buttonRelDiagBelow);
			// because b is dependant on d to know it's exact allowed stretch,
			// this value is also negotiated if there is d.
			if (this.buttonRelRight != null && this.buttonRelRight.buttonRelDiagBelow != null && this.buttonRelRight.buttonRelDiagBelow.weight > this.buttonRelRight.weight) {
				bLowerLimit = this.negotiate(this.buttonRelRight, this.buttonRelRight.buttonRelDiagBelow);
			} else if (this.buttonRelRight!= null && this.buttonRelRight.buttonRelDiagBelow == null) {
				bLowerLimit = (bLowerLimit > 0.5f ? 0.5f : bLowerLimit);
			}

			aLowerLimit = (aLowerLimit > bLowerLimit ? aLowerLimit
					: bLowerLimit);

			if (this.dontModifyBelow == false)
				this.negotiationRect.bottom = aLowerLimit;
			this.negotiationRect.right = aRight;
			if (this.buttonRelRight != null) {
				this.buttonRelRight.negotiationRect.left = 1.0f - aRight;
				this.buttonRelRight.negotiationRect.bottom = bLowerLimit;
				this.buttonRelRight.dontModifyBelow = true;
				if (this.buttonRelRight.buttonRelDiagBelow != null) {

				}
			}
			if (this.buttonRelDiagBelow != null) {
				this.buttonRelDiagBelow.negotiationRect.top = 1.0f - aLowerLimit;
			}
		} else if (this.relType == KeyboardButton.STRAIGHT) {
			// a relation type straight means that we need to set up four keys 
			// now. a bit expensive, but necessary.
			float wRight = this.negotiate(this,this.buttonRelRight);
			float wBelow = this.negotiate(this,this.buttonRelStraightBelow);	
			float wDiagLeft = this.negotiate(this,this.buttonRelDiagBelow);
			float wDiagTop = wDiagLeft;
			// only perform _anything_ if one of the buttons is really "unstandard"
			float wDownMax = 0.5f;
			float wRightMax = 0.5f;
			float wBelowRightGrowth = 0.5f;
			if ( wDiagTop != 0.5f || wBelow != 0.5f || wRight != 0.5f){
				// determine the limiting factor for the downside growth.
				// we grow down as much as allowed
				wDownMax = ( wDiagTop > wBelow ? wDiagTop : wBelow);
				// we also grow right as much as possible
				wRightMax = ( wDiagTop > wRight ? wDiagTop : wRight );
				// we have to check that we don't grow down further as we already are. 
				// to not interfer with the 'other' instance.
				wDownMax = ( wDiagTop > this.negotiationRect.bottom ? this.negotiationRect.bottom : wDiagTop);
				// now we set everything accordingly.
				
				
				wBelowRightGrowth = this.negotiate(this.buttonRelStraightBelow,this.buttonRelDiagBelow);

					
				// ... same for the right growth.
			
			} 	

				if ( this.buttonRelStraightBelow != null){
					this.buttonRelStraightBelow.negotiationRect.right = wBelowRightGrowth;					this.buttonRelStraightBelow.negotiationRect.top = 0.5f;
					this.buttonRelStraightBelow.negotiationRect.top = 1.0f - wDownMax;

				}
				if ( this.buttonRelDiagBelow != null){
					this.buttonRelDiagBelow.negotiationRect.left = 1.0f-wBelowRightGrowth;
					this.buttonRelDiagBelow.negotiationRect.top = 1.0f - wDownMax;
				}
				if ( this.buttonRelRight != null){
					this.buttonRelRight.negotiationRect.left = 1.0f-wRightMax;
					this.buttonRelRight.negotiationRect.bottom = wDownMax;
				}
				
				this.negotiationRect.right = wRightMax;
				this.negotiationRect.bottom = wDownMax;
		}

		// mark dirty
		this.dirty = false;
	}

	/**
	 * checks if this button, and/or related buttons are dirty, and if one is
	 * true, returns true, otherwise false.
	 * 
	 * @return if this button needs to renegotiate its size.
	 */
	private boolean needsRenegotiation() {

		if (this.dirty) {
			return true;
		} else {
			if ((this.buttonRelRight != null && this.buttonRelRight.dirty)
					|| (this.buttonRelDiagBelow != null && this.buttonRelDiagBelow
							.dirty)
					|| (this.buttonRelStraightBelow != null && this.buttonRelStraightBelow
							.dirty) /*|| this.buttonRelRight != null
					&& this.buttonRelRight.buttonRelDiagBelow != null
					&& this.buttonRelRight.buttonRelDiagBelow.dirty*/)
				return true;
			else
				return false;
		}
		 
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
	private float negotiate(KeyboardButton from, KeyboardButton to) {

		if (from == null || to == null) {
			return 0.5f;

		}
		float fromWeight = from.weight;
		float toWeight = to.weight;
		if (fromWeight == toWeight || from.isSpecialKey() || to.isSpecialKey()) {
			return 0.5f;
		} else if (fromWeight == 0.0) {
			return 0.0f;
		} else if (toWeight == 0.0) {
			return 1.0f;
		} else {
			float scale = 1.0f / ((fromWeight) + (toWeight));
			return fromWeight * scale;
		}
	}
}
