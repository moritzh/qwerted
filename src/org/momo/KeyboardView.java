package org.momo;

import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

/**
 * Provides a view of a keyboard. Is a magic entry point, because it also
 * computes ( if desired ) predictions and assigns keys the right values.
 * and now for the o3!
 * @author moritzhaarmann
 *
 */
public class KeyboardView extends RelativeLayout  {
	private Keyboard mKeyboard;
	private String mTyped;
	private boolean predictionEnabled = false;
	
	public static final int SHIFT = 1;
	public static final int NUM = 2;
	
	public static final int MAX_KEYS = 100;
	
	// the keyboard state.
	private int mState = 0;
	HashMap<Integer,Float> mChildOrder;
	Integer[] mChildOrderSet;
	// we are using a hashmap to map our buttons to rows and columns.
	int mappingIndex = 0;
	
	String[] mStringMapping;
	KeyboardButton[] mButtonMapping;
	
	public KeyboardView(Keyboard k,Context context) {
		super(context,null,ViewGroup.FLAG_USE_CHILD_DRAWING_ORDER);
		mStringMapping = new String[MAX_KEYS];
		mButtonMapping = new KeyboardButton[MAX_KEYS];
		mKeyboard = k;
		mTyped = "";
		mChildOrder = new HashMap<Integer,Float>();
		// TODO externalize layout.
		this.setBackgroundColor(Color.DKGRAY);
		this.setVisibility(View.VISIBLE);
		// TODO Auto-generated constructor stub
	}
	
	// FIXME fix 
	public void onClickDelegate(String what){
		mTyped += what;
		mKeyboard.onClick(what);
		if ( what.compareTo(" ") == 0 ){
			mTyped = "";
			
		}
		// just update buttons if prediction enabled.
		if ( predictionEnabled )
			updateButtons();
		forceLayout();
	} 
	
	public void addKey(KeyboardButton b){
		mButtonMapping[mappingIndex] = b;
		mStringMapping[mappingIndex] = b.getPredictionIdentifier();	
		mappingIndex += 1;
		this.addView(b.getView());
	}
	
	// optimized
	public void changeState(int state){
		KeyboardButton[] localKeyboardButtons = mButtonMapping;
		int limit = mappingIndex + 1;
		for(int i=0;i<limit;i++){
			localKeyboardButtons[i].updateState(state);
		}
	}
	
	/**
	 * updates the prediction state.
	 * @param mPredictionState
	 */
	public void setPrediction(boolean mPredictionState){
		if (predictionEnabled){
			// reset buttons to default state;
			// TODO not use float
			updateButtons(1.0f);
			predictionEnabled = false;
		} else {
			predictionEnabled = true;
		}
	}
	
	// iterator-fail TODO
	private void updateButtons(float override){
		KeyboardButton[] localKeyboardButtons = mButtonMapping;
		int limit = mappingIndex + 1;
		float weightToUse;
		
		// get rid of all the override stuff.
		if (override!= 0.0f)
			 weightToUse = override;
		else
			weightToUse = 1.0f;
	
		for(int i=0;i<limit;i++){
			localKeyboardButtons[i].updateWeight(weightToUse);
		}
	}
	
	// TOO MUCH HARDCODING TODO
	private void updateButtons(){
		HashMap<String,Float> data = mKeyboard.getDictionary().returnByProbability(mTyped);
		float warpFactor = 0.0f;
		float staticFactor = 1.0f;
		float highest = staticFactor;
		if ( data.size()<21 && data.size()>0){
			staticFactor = 0.8f;
			warpFactor = 1.2f;
		} else if (data.size()>20  ) {
			staticFactor = 1.0f;
			warpFactor = 1.0f;
			data.clear();
		} else if (data.size() == 0 ) {
			staticFactor = 1.0f;
			warpFactor = 1.0f;
		}
		mChildOrder.clear();
		KeyboardButton[] localKeyboardButtons = mButtonMapping;
		int limit = mappingIndex + 1;
		for(int i=0;i<limit;i++){
			if ( mButtonMapping[i]!= null){
			if (data.containsKey(mStringMapping[i])){
				mButtonMapping[i].updateWeight((data.get(mStringMapping[i])+0.2f)*warpFactor);
				/*if ( data.get(mStringMapping[i])>highest || mChildOrder.size()==0){
					mChildOrder.put(i,data.get(mStringMapping[i]));
					highest = data.get(mStringMapping[i]);
				} else {
					for(int t=mChildOrder.size();t>-1;--t){
						if (mChildOrder.get(t) < data.get(mStringMapping[i])){
							mChildOrder.put(t, data.get(mStringMapping[i]));
						}
					}
				}*/
			} else {
				// ouch. find the first one that is higher and insert after. 
				
				mChildOrder.put(i,staticFactor);
				mButtonMapping[i].updateWeight(staticFactor);
			}
			}
			
		}
		//mChildOrderSet = (Integer[])mChildOrder.keySet().toArray();
		this.forceLayout();
		
	}
	
	
	protected int getChildDrawingOrder(int foo,int i){
		return i;
		//return mChildOrderSet != null ? mChildOrderSet[i] : i;
	}



}
