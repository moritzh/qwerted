package org.momo;

import java.util.Date;

import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;

/**
 * we need to move all our keyboard-switching related tasks here. And we should handle prediction here in some way, 
 * at least the ability to switch it on and off on demand.
 * @author Moritz
 *
 */
public class IType extends InputMethodService {
	private Keyboard mStandardKeyboard; // normal keybosard.
	private Keyboard mNumericalKeyboard; // small, numerical board
	private Keyboard mSymbolKeyboard; // all the numerical, symbol, stuff
	
	private KeyboardView mKeyboardView;
	
	private boolean isSym = false;
	
	@Override
	public void onBindInput(){
		
	}
	 
	@Override
	public View onCreateInputView(){
		Date t = new Date();
			
		mStandardKeyboard = new Keyboard(this, this.getBaseContext().getResources().getXml(R.xml.keyboard));
		mNumericalKeyboard = new Keyboard(this, this.getBaseContext().getResources().getXml(R.xml.numerical_keyboard));
		mSymbolKeyboard = new Keyboard(this, this.getBaseContext().getResources().getXml(R.xml.smybol_board));
		mNumericalKeyboard.setPrediction(false);
		mSymbolKeyboard.setPrediction(false);
		if ( t.getYear() > 2010 || t.getYear() == 2010 && t.getMonth() > 5)
			mStandardKeyboard.setPrediction(false);
		mKeyboardView = new KeyboardView(mStandardKeyboard,this.getBaseContext());
		mKeyboardView.show();
		
		if ( this.getCurrentInputEditorInfo().inputType == InputType.TYPE_CLASS_NUMBER)
			mKeyboardView.setKeyboard(mNumericalKeyboard);
		else
			mKeyboardView.setKeyboard(mStandardKeyboard);
		return mKeyboardView;
	}
	
	@Override
	public void onStartInputView (EditorInfo attribute, boolean restarting){
		Log.d("RE","STARTING");
		if ( (attribute.inputType & InputType.TYPE_CLASS_NUMBER) == InputType.TYPE_CLASS_NUMBER)
			mKeyboardView.setKeyboard(mNumericalKeyboard);
		else
			mKeyboardView.setKeyboard(mStandardKeyboard);
		if ( restarting)
			mKeyboardView.drawButtons();
	}
	
	/**
	 * just switch from standard to symbolic.
	 */
	public void switchSym(){
		if ( isSym ){

			mKeyboardView.setKeyboard(mStandardKeyboard);
		}else{
			mSymbolKeyboard.onStartInput(this.getCurrentInputEditorInfo(), false);

			mKeyboardView.setKeyboard(mSymbolKeyboard);
		}
		isSym = !isSym;
		mKeyboardView.drawButtons();
	}
	
	public void dispatchRedraw(){
		mKeyboardView.drawButtons();
	}
}
