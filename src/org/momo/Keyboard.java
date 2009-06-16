package org.momo;

import java.io.IOException;
import java.util.logging.Logger;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.inputmethodservice.InputMethodService;
import android.view.View;


/**
 * The keyboard.
 * @author moritzhaarmann
 *
 */
public class Keyboard {
	// for our xml-parser session
	public static final String TAG = "Keyboard";
	public static final String TAG_ROW = "Row";
	public static final String TAG_KEY = "Key";
	
	// one keyboardview to rule them all.
	private KeyboardView mKeyboardView;
	
	private Context mContext;
	private InputMethodService mInputMethodService;
	private Dictionary mDictionary;

	public Keyboard(InputMethodService ss){
		// we need the context to create views.
		mContext = ss.getApplicationContext();
		mInputMethodService = ss;
		mDictionary = new Dictionary();
		Logger.getAnonymousLogger().info("Loading dictionary now.");
		try {
			mDictionary.readTrainingFile(mContext.getAssets().open("training_german_DE.txt"), false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Couldn't read input file.");
			e.printStackTrace();
		}

		// some bootstrapping.
		mKeyboardView = new KeyboardView(this,mContext);
		mKeyboardView.setPrediction(true);
		loadKeyboardFromXml(mContext.getResources().getXml(R.xml.keyboard));
		
	}

	
	private void loadKeyboardFromXml(XmlResourceParser xml) {
		int tag;
		int row= 0;
		float span;
		
		String numericKey,symbolicKey,normalKey,shiftKey;
	//	String[] alternativeKeys;
		
		float column=0.0f;
		System.out.println("starting to parse.");

		try {
			// state machine
			while((tag=xml.next())!=XmlPullParser.END_DOCUMENT){
				if (tag == XmlResourceParser.START_TAG) {
					if(TAG.compareTo(xml.getName())==0){
						// new keyboard, no surprise 
					} else if (TAG_ROW.compareTo(xml.getName())==0){
						row++;
						column = 1.0f;
					} else if (TAG_KEY.compareTo(xml.getName())==0){
						span = xml.getAttributeFloatValue(null,"span",1.0f);
						numericKey = xml.getAttributeValue(null,"key_num");
						symbolicKey = xml.getAttributeValue(null,"key_sim");
						normalKey = xml.getAttributeValue(null,"key");
						shiftKey = xml.getAttributeValue(null,"key_shift");
						// instantiate new button.
						KeyboardButton b = new KeyboardButton(mKeyboardView, normalKey,numericKey, symbolicKey, shiftKey, row, column, span);
						mKeyboardView.addKey(b);
						column +=  span;
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
		System.out.println("should be loaded.");
		
	}
	
	public void onClick(String foo){
		//v.vibrate(30);
		if (mInputMethodService.getCurrentInputStarted())
			mInputMethodService.getCurrentInputConnection().commitText(foo,1);
		
	}
	
	public Dictionary getDictionary(){
		return mDictionary;
	}


	public View getKeyboard() {
		// TODO Auto-generated method stub
		return mKeyboardView;
	}
	
}
