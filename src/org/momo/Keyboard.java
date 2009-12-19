package org.momo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import org.momo.dict.Dictionary;
import org.momo.dict.DictionaryItem;
import org.momo.util.DisplaySet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.BitmapDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Toast;


/**
 * The keyboard.
 * @author moritzhaarmann
 *
 */
public class Keyboard {
	// for the types of keyboards
	public static final short NUMERICAL = 1;
	public static final short SYMBOLIC = 2;
	public static final short SHIFT = 4;
	public static final short NORMAL = 0;
	
	// for our xml-parser session
	public static final String TAG = "Keyboard";
	public static final String TAG_ROW = "Row";
	public static final String TAG_KEY = "Key";
	public static final String TAG_SPAN = "Spacer";
	
	// one keyboardview to rule them all.
	private KeyboardView mKeyboardView;
	
	private Context mContext;
	private InputMethodService mInputMethodService;
	private Vibrator mVibrator;
	// the stuff, the stuff.
	
	private Dictionary mDictionary;
	private DictionaryItem mDictionaryItem;
	private String word;
	
	// to give the back button some use
	private int mAdvanceLastPrediction;
	private DictionaryItem mLastPrediction;
	
	public Vector<KeyboardButton> mButtons;

	
	// the cached tables for the chars/keyboardbuttns.
	HashMap<Character,KeyboardButton> mNormKeyMapping;
	HashMap<Character,KeyboardButton> mNumKeyMapping;
	HashMap<Character,KeyboardButton> mShiftKeyMapping;
	HashMap<Character,KeyboardButton> mSymKeyMapping;

	public Keyboard(InputMethodService ss){
		// we need the context to create views.
		mContext = ss.getApplicationContext();
		mButtons = new Vector<KeyboardButton>();
		mInputMethodService = ss;
		try {
			mDictionary = new Dictionary("/data/german.dict");
			DictionaryItem.dict = mDictionary;
		} catch (IOException e) {
			try {
				mDictionary = new Dictionary("/sdcard/german.dict");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			DictionaryItem.dict = mDictionary;
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Logger.getAnonymousLogger().info("Loading dictionary now.");
		
		mVibrator = (Vibrator)mContext.getSystemService("vibrator");
		// some bootstrapping.
		mKeyboardView = new KeyboardView(this,mContext);
		loadKeyboardFromXml(mContext.getResources().getXml(R.xml.keyboard));
		this.createCharTables();
		word = "";
		mKeyboardView.show();

	}

	
	private void loadKeyboardFromXml(XmlResourceParser xml) {
		int tag;
		int row= 0;
		float span;
		
		String numericKey,symbolicKey,normalKey,shiftKey;
	//	String[] alternativeKeys;
		
		float column=0.0f;
		System.out.println("starting to parse.");
		Vector<KeyboardButton> rowButtons=null,previousRowButtons=null;
		KeyboardButton lastButton = null;
		rowButtons = new Vector<KeyboardButton>();
		try {
			// state machine
			while((tag=xml.next())!=XmlPullParser.END_DOCUMENT){
				if (tag == XmlResourceParser.START_TAG) {
					if(TAG.compareTo(xml.getName())==0){
						// new keyboard, no surprise 
					} else if (TAG_ROW.compareTo(xml.getName())==0){
						row++;
						previousRowButtons = rowButtons;
						rowButtons = new Vector<KeyboardButton>();
						lastButton = null;
						column = 1.0f;
					} else if (TAG_SPAN.compareTo(xml.getName())==0){
						column += xml.getAttributeFloatValue(null,"amount",0.5f);
					} else if (TAG_KEY.compareTo(xml.getName())==0){
						span = xml.getAttributeFloatValue(null,"span",1.0f);
						numericKey = xml.getAttributeValue(null,"key_num");
						symbolicKey = xml.getAttributeValue(null,"key_sim");
						normalKey = xml.getAttributeValue(null,"key");
						shiftKey = xml.getAttributeValue(null,"key_shift");
						// instantiate new button.
						
						KeyboardButton b = this.createButton(new String[]{normalKey,numericKey,symbolicKey,shiftKey}, row,column,span);
						this.mButtons.add(b);
						// add this button to the mapping of the current row, for further calculations.
						rowButtons.add(b);
						// set this as the button next to the previous one. this is the simple part.
						
							if (lastButton != null) {
								lastButton.buttonRelRight = b;
							}
							// calculate vertical relations.
							if (previousRowButtons != null
									&& previousRowButtons.size() > 0) {
								// iterate until an effective span+column is
								// wider than this buttons span+column.
								Iterator<KeyboardButton> it = previousRowButtons
										.iterator();
								while (it.hasNext()) {
									KeyboardButton prevRowButton = it.next();
									// if the button above is really above us..
									// where is this button?
									float rightEdge = prevRowButton.column
											+ prevRowButton.columnSpan;
									float leftEdge = prevRowButton.column;
									/*
									 * a lot of distinct cases here: a button
									 * can either be directly over another
									 * button, in which case everything is
									 * clear. a button can as well be, when
									 * columns are aligned, be right next to the
									 * button above this one. in this case,
									 * there is also a relation.
									 */
									if (rightEdge > b.column
											&& rightEdge < (b.column + b.columnSpan)) {
										Log.i("XC", "right-edge between "
												+ prevRowButton.normSet.send
												+ " and " + b.normSet.send);
										prevRowButton.buttonRelDiagBelow = b;
										prevRowButton.relType = KeyboardButton.DIAG;

									} else if (rightEdge == b.column
											&& prevRowButton.columnSpan == b.columnSpan) {
										prevRowButton.buttonRelDiagBelow = b;
										prevRowButton.relType = KeyboardButton.STRAIGHT;
										// add a left-vertical relation
									} else if (rightEdge == (b.column + b.columnSpan)) {
										prevRowButton.buttonRelStraightBelow = b;
										prevRowButton.relType = KeyboardButton.STRAIGHT;

									}
								}
							}
						
						column +=  span;
						lastButton = b;
					}
				} 
			}
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("error");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("error");

		}
		// wnat to test toasts
	
	}
	
	/**
	 * maybe one could pull this out.
	 * @param c
	 */
	public void handleInput(char c){
		String text = "";
		if ( c < 10){
			switch(c){
				case(01):
					if (mInputMethodService.getCurrentInputStarted())
						mInputMethodService.getCurrentInputConnection().deleteSurroundingText(1, 0);
					
					if (mAdvanceLastPrediction==1 && mLastPrediction != null){
						mAdvanceLastPrediction = -1;
						mDictionaryItem = mLastPrediction;
						updatePredictions();
					} else if (mDictionaryItem !=null ){
						if ( mDictionaryItem.getParent() != null )
							mDictionaryItem = mDictionaryItem.getParent();
						else
							this.bunchUpdateAllButtons(0.5f);
						updatePredictions();
					}
						mAdvanceLastPrediction -= 1;
					break;
				case(02):
					if ( KeyboardButton.state == Keyboard.SHIFT)
						KeyboardButton.state = Keyboard.NORMAL;
					else 
						KeyboardButton.state = Keyboard.SHIFT;
					break;
				case(03):
					if ( KeyboardButton.state == Keyboard.NUMERICAL)
						KeyboardButton.state = Keyboard.NORMAL;
					else 
						KeyboardButton.state = Keyboard.NUMERICAL;
					break;
				case(05):
					text = " ";
					mDictionaryItem = null;
					mLastPrediction = null;
					mAdvanceLastPrediction = -1;
					updatePredictions();
					word ="";
			}
		} else {
			text += c;
			word += c;
			if ( mDictionaryItem != null )
			mLastPrediction = mDictionaryItem;
			if ( word.length() == 1 ){
				try {
					mDictionaryItem = DictionaryItem.lookup(word, true);

				} catch (Exception e) {
					// TODO Auto-generated catch block
					
					mDictionaryItem = null;
				}
			} else if ( mDictionaryItem != null ){
				mDictionaryItem = mDictionaryItem.childContainingChar(c);
			}
			if ( mDictionaryItem == null){
				mAdvanceLastPrediction = 1;
				
			}
			updatePredictions();

			// reset everything to normal after .. it wasn't :-)
			if ( KeyboardButton.state != Keyboard.NORMAL){
				KeyboardButton.state = Keyboard.NORMAL;
			}
			
			
		}
		if (mInputMethodService.getCurrentInputStarted() && text.length() > 0)
			mInputMethodService.getCurrentInputConnection().commitText(text,1);
		// update prediction state?
	}
	
	
	public Dictionary getDictionary(){
		
		return mDictionary;
	}
	
	private void bunchUpdateAllButtons(float weight){
		Iterator<KeyboardButton> k = this.getCurrentMapping().values().iterator();
		while (k.hasNext())
			k.next().updateWeight(weight);

		return;
	}
	
	private void updatePredictions(){
		if (mDictionaryItem == null ){
			bunchUpdateAllButtons(0.5f);

			return;
		}
		HashMap<Character,Float> weights = mDictionaryItem.childrenWithWeights();
		// update all to be even sized if no weights present.
		if ( weights.size() == 0){
			bunchUpdateAllButtons(0.5f);

			return;
		}
		// get current active map.
		HashMap<Character,KeyboardButton> mapping = this.getCurrentMapping();
		Iterator<Character> it = mapping.keySet().iterator();
		while (it.hasNext()){
			char c = it.next();
			
			if (weights.containsKey(c)){
				// adjust this.
				mapping.get(c).updateWeight(weights.get(c));
			} else {
				mapping.get(c).updateWeight(0f);
			}
			
		}
	}
	
	/**
	 * this method creates the tables in which mappings from chars to keyboardbuttons
	 * are stored. these mappings are important because we need the mto perform 
	 * very fast lookups.
	 */
	private void createCharTables(){
		mNormKeyMapping = new HashMap<Character,KeyboardButton>();
		mNumKeyMapping = new HashMap<Character,KeyboardButton>();
		mSymKeyMapping = new HashMap<Character,KeyboardButton>();
		mShiftKeyMapping = new HashMap<Character,KeyboardButton>();
		Iterator<KeyboardButton> it = mButtons.iterator();
		while(it.hasNext()){
			KeyboardButton cButton = it.next();
			DisplaySet norm = cButton.normSet;
			// check if this button is special. or not.
			if (norm.send > 10){
				mNormKeyMapping.put(norm.send, cButton);
				if(cButton.numSet != null)
					mNumKeyMapping.put(cButton.numSet.send, cButton);
				if(cButton.symSet != null)
					mSymKeyMapping.put(cButton.symSet.send, cButton);
				if(cButton.shiftSet != null)
					mShiftKeyMapping.put(cButton.shiftSet.send, cButton);
			}
		}
	}
	
	/**
	 * gets the current char->button mapping for this keyboard.
	 * @return
	 */
	public HashMap<Character,KeyboardButton> getCurrentMapping(){
		if ( KeyboardButton.state == Keyboard.SHIFT)
			return mShiftKeyMapping;
		else if (KeyboardButton.state == Keyboard.SYMBOLIC)
			return mSymKeyMapping;
		else
			return mNormKeyMapping;
	}


	public View getKeyboard() {
		// TODO Auto-generated method stub
		return mKeyboardView;
	}
	
	public  KeyboardButton createButton(String[] keys, int row, float column, float span){
		DisplaySet normSet = new DisplaySet();
		DisplaySet numSet = new DisplaySet();
		DisplaySet symSet = new DisplaySet();
		DisplaySet shiftSet = new DisplaySet();
		// check for "special key"
		String normalKey = keys[0];
		if ( normalKey.length() == 1){
			if (normalKey.length()>0)
				normSet.setKey(normalKey.charAt(0));
			if (keys[1].length()>0)
				numSet.setKey(keys[1].charAt(0));
			if (keys[2].length()>0)
				symSet.setKey(keys[2].charAt(0));
			if (keys[3].length()>0)
				shiftSet.setKey(keys[3].charAt(0));
			return new KeyboardButton(normSet,numSet,shiftSet,symSet, row,column,span);
		} else {
			// special key.
			if ( normalKey.compareTo(KeyboardButton.KEY_BACK)==0){
				normSet.render= mContext.getResources().getDrawable(R.drawable.sym_keyboard_delete);
				normSet.send = (char)01;
			} else if ( normalKey.compareTo(KeyboardButton.KEY_SHIFT)==0){
				normSet.render = mContext.getResources().getDrawable(R.drawable.sym_keyboard_shift);
				normSet.send = (char)02;
			}else if ( normalKey.compareTo(KeyboardButton.KEY_SYM)==0){
				normSet.render = mContext.getResources().getDrawable(R.drawable.sym_keyboard_done);
				normSet.send = (char)03;
			}else if ( normalKey.compareTo(KeyboardButton.KEY_GO)==0){
				normSet.render = mContext.getResources().getDrawable(R.drawable.sym_keyboard_return);
				normSet.send = (char)04;
			}
			else if ( normalKey.compareTo(KeyboardButton.KEY_SPACE)==0){
				normSet.render = mContext.getResources().getDrawable(R.drawable.sym_keyboard_space);
				normSet.send = (char)05;
			}
			return new KeyboardButton(normSet,null,null,null, row,column,span);
		}
	}
}
