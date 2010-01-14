package org.momo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.momo.dict.Dictionary;
import org.momo.dict.DictionaryItem;
import org.momo.util.DisplaySet;
import org.momo.util.KeyboardXMLParser;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;

/**
 * The keyboard.
 * 
 * @author moritzhaarmann
 * 
 */
public class Keyboard implements
		SharedPreferences.OnSharedPreferenceChangeListener {

	public final static int SHIFT = 1;
	public final static int NORMAL = 0;
	private static final int PREDICTION_LIMIT = 10;


	// the maximums of rows and columns, needed to calculate button width and
	// height.
	public int rowCount = 0;
	public float colCount = 0;

	private Context mContext;
	private IType mInputMethodService;
	private Vibrator mVibrator;
	// the stuff, the stuff.

	private Dictionary mDictionary;
	private DictionaryItem mDictionaryItem;
	private String word;

	private boolean mPredictionEnabled = true;
	private boolean mVibrateOnKey = false;

	// to give the back button some use
	private int mAdvanceLastPrediction;
	private DictionaryItem mLastPrediction;

	public KeyboardButton[] mButtons;
	
	public HashMap<Character, KeyboardButton> mPunchMapping;

	// the cached tables for the chars/keyboardbuttns.
	HashMap<Character, KeyboardButton> mNormKeyMapping;
	HashMap<Character, KeyboardButton> mNumKeyMapping;
	HashMap<Character, KeyboardButton> mShiftKeyMapping;
	HashMap<Character, KeyboardButton> mSymKeyMapping;

	// the preference manager.
	SharedPreferences mPreferences;

	public Keyboard(IType ss, XmlResourceParser s) {
		// we need the context to create views.
		mContext = ss.getApplicationContext();
		mInputMethodService = ss;

		try {
			mDictionary = new Dictionary("/sdcard/data.dict");
			DictionaryItem.dict = mDictionary;
		} catch (IOException e) {
			try {
				mDictionary = new Dictionary("/sdcard/data.dict");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			DictionaryItem.dict = mDictionary;
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// some bootstrapping.
		mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		mPreferences.registerOnSharedPreferenceChangeListener(this);
		mButtons = KeyboardXMLParser.loadKeyboardFromXml(this, s);
		mVibrator = (Vibrator) mContext.getSystemService("vibrator");
		
		mPunchMapping = new HashMap<Character, KeyboardButton>();
		this.createCharTables();
		getPreferences();
		word = "";

	}

	/**
	 * maybe one could pull this out.
	 * 
	 * @param c
	 */
	public void handleInput(char c) {
		long oldtime = System.currentTimeMillis();
		if (this.mVibrateOnKey && mVibrator != null) {
			mVibrator.vibrate(50);
		}
		String text = "";
		if (c < 10) {
			switch (c) {
			case (01):
				if (mInputMethodService.getCurrentInputStarted())
					mInputMethodService.getCurrentInputConnection()
							.deleteSurroundingText(1, 0);
				if (mPredictionEnabled) {
					if ( word.length() > 1 && mDictionaryItem != null){
						mDictionaryItem = mDictionaryItem.getParent();
						word = mDictionaryItem.fullWord;
						updatePredictions();
					} else if ( word.length() == 1 ){
						mDictionaryItem = null;
						word = "";
						updatePredictions();
					}   
				}
				this.mInputMethodService.dispatchRedraw();
				break;
			case (02):
				if (KeyboardButton.state == Keyboard.SHIFT)
					KeyboardButton.state = Keyboard.NORMAL;
				else
					KeyboardButton.state = Keyboard.SHIFT;
				unpunchButtons();
				updatePredictions();
				this.mInputMethodService.dispatchRedraw();

				break;
			case (03):
				this.mInputMethodService.switchSym();
				break;
			case (04):
				this.mInputMethodService.getCurrentInputConnection().performEditorAction(EditorInfo.IME_ACTION_GO);
				break;
			case (05):
				if (mPredictionEnabled) {
					if (mDictionaryItem == null) {
						try {
							if (word != null && word.length() > 0)
								DictionaryItem.create(word);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						try {
							mDictionaryItem.increase();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				text = " ";
				mDictionaryItem = null;
				mLastPrediction = null;
				mAdvanceLastPrediction = -1;
				word = "";

				updatePredictions();
				this.mInputMethodService.dispatchRedraw();
			}
		} else {
			text += c;
			word += c;
			if (mPredictionEnabled) {
				if (mDictionaryItem != null)
					mLastPrediction = mDictionaryItem;
				if (word.length() == 1) {
					try {
						mDictionaryItem = DictionaryItem.lookup(word, true);

					} catch (Exception e) {
						// TODO Auto-generated catch block

						mDictionaryItem = null;
					}
				} else if (mDictionaryItem != null) {
					mDictionaryItem = mDictionaryItem.childContainingChar(c);
				}
				if (mDictionaryItem == null) {
					mAdvanceLastPrediction = 1;

				}
				updatePredictions();
			}
			// reset everything to normal after .. it wasn't :-)
			if (KeyboardButton.state != Keyboard.NORMAL) {
				KeyboardButton.state = Keyboard.NORMAL;
			}
			this.mInputMethodService.dispatchRedraw();
		}
		if (mInputMethodService.getCurrentInputStarted() && text.length() > 0)
			mInputMethodService.getCurrentInputConnection().commitText(text, 1);
		// update prediction state?
		Log.d("TT","Time taken: " + (oldtime-System.currentTimeMillis()));
	}

	private void unpunchButtons() {
		// TODO Auto-generated method stub
		Iterator<KeyboardButton> it = this.getCurrentMapping().values().iterator();
		while (it.hasNext())
			it.next().mPunched = 0;
	}

	public Dictionary getDictionary() {

		return mDictionary;
	}

	private void bunchUpdateAllButtons(float weight) {
		Iterator<KeyboardButton> k = this.getCurrentMapping().values()
				.iterator();
		while (k.hasNext())
			k.next().updateWeight(weight);

		return;
	}

	private void updatePredictions() {
		if (!mPredictionEnabled)
			Log.d("P","prediction disabled");
		if ((mDictionaryItem == null && this.word.trim().length() > 0)
				|| !mPredictionEnabled) {
			Log.d("NOT","Predicting anything");
			bunchUpdateAllButtons(0.5f);

			return;
		}
		HashMap<Character, Float> weights = DictionaryItem
				.childrenWithWeights(mDictionaryItem);
		// temporary mapping
		
		// update all to be even sized if no weights present.
		if (weights.size() == 0 ) {
			bunchUpdateAllButtons(0.5f);

			return;
		}
		// get current active map.
		HashMap<KeyboardButton, Float> tempMapping = new HashMap<KeyboardButton,Float>();
		
		Iterator<Character> it = weights.keySet().iterator();
		KeyboardButton tButton;
		while ( it.hasNext()){
			char c = it.next();
			if (KeyboardButton.state == Keyboard.NORMAL)
				tButton = mPunchMapping.get(c);
			else
				tButton = this.getCurrentMapping().get(c);
			// null : do nothing, else: do a lot.
			if ( tButton == null){
				// nothing.
			} else {
				if (!tempMapping.containsKey(tButton) || tempMapping.containsKey(tButton) && tempMapping.get(tButton) < weights.get(c)){
					tButton.updateWeight(weights.get(c));
					tempMapping.put(tButton, weights.get(c));
					tButton.mPunched = c;
				} 
			}
		}
		
		// reset the buttons not in the set.
		Iterator<KeyboardButton> kt = this.getCurrentMapping().values().iterator();
		while ( kt.hasNext()){
			tButton = kt.next();
			if ( !tempMapping.containsKey(tButton))
				tButton.updateWeight(0.0f);
			
		}
		Log.d("ASDF", tempMapping.size()+ "adsf");
		
		
	}

	/**
	 * this method creates the tables in which mappings from chars to
	 * keyboardbuttons are stored. these mappings are important because we need
	 * the mto perform very fast lookups.
	 */
	private void createCharTables() {
		mNormKeyMapping = new HashMap<Character, KeyboardButton>();
		mShiftKeyMapping = new HashMap<Character, KeyboardButton>();
		for (int i = 0; i < mButtons.length; i++) {
			KeyboardButton cButton = mButtons[i];
			DisplaySet norm = cButton.normSet;
			// check if this button is special. or not.
			if (norm.send > 10) {
				mNormKeyMapping.put(norm.send, cButton);
				if (cButton.shiftSet != null)
					mShiftKeyMapping.put(cButton.shiftSet.send, cButton);
			}
		}
		// same for the punch.
		for ( int i=0;i<mButtons.length;i++){
			char[] chars = mButtons[i].getAllChars();
			for ( int t=0;t<chars.length;t++)
				mPunchMapping.put(chars[t], mButtons[i]);
		}
	}

	/**
	 * gets the current char->button mapping for this keyboard.
	 * 
	 * @return
	 */
	public HashMap<Character, KeyboardButton> getCurrentMapping() {
		if (KeyboardButton.state == Keyboard.SHIFT)
			return mShiftKeyMapping;
		else
			return mNormKeyMapping;
	}

	public void onFinishInput() {
		mDictionaryItem = null;
		updatePredictions();
	}

	public void onStartInput(EditorInfo attribute, boolean restarting) {
		mDictionaryItem = null;
		updatePredictions();
	}

	public void onStartInputView(EditorInfo info, boolean restarting) {
		mDictionaryItem = null;
		updatePredictions();
	}

	public Context getContext() {
		return mContext;
	}

	/**
	 * retrieves the shared prefernce values and sets everything accordingly.
	 * keys are: view.button.gap: inter-button gap, in pixels.
	 * view.button.border : if there is a border view.button.round: if the
	 * buttons are to be round keyboard.vibrate : if vibrate on keystroke is one
	 * keyboard.prediction : if to predict.
	 */
	private void getPreferences() {
		String gap = mPreferences.getString("view.button.gap", "2");
		try {
			int gapI = Integer.parseInt(gap);
			KeyboardButton.setButtonGap(gapI);
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			KeyboardButton.setButtonGap(2);
		}

		boolean predict = mPreferences.getBoolean("keyboard.predict", true);
		boolean vibrate = mPreferences.getBoolean("keyboard.vibrate", true);
		this.mVibrateOnKey = vibrate;
		this.mPredictionEnabled = predict;
	}

	// guess what, the preference change bla.
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// TODO Auto-generated method stub
		getPreferences();
	}

	public void setPrediction(boolean b) {
		this.mPredictionEnabled = b;
	}
}
