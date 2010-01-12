package org.momo;

import android.inputmethodservice.InputMethodService;
import android.text.InputType;
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
	
	private boolean isSym = false;
	
	@Override
	public void onBindInput(){
		
	}
	 
	@Override
	public View onCreateInputView(){
		mStandardKeyboard = new Keyboard(this, this.getBaseContext().getResources().getXml(R.xml.keyboard));
		mNumericalKeyboard = new Keyboard(this, this.getBaseContext().getResources().getXml(R.xml.numerical_keyboard));
		mSymbolKeyboard = new Keyboard(this, this.getBaseContext().getResources().getXml(R.xml.smybol_board));
		mNumericalKeyboard.setPrediction(false);
		mSymbolKeyboard.setPrediction(false);
		if ( this.getCurrentInputEditorInfo().inputType == InputType.TYPE_CLASS_NUMBER)
			return mNumericalKeyboard.getKeyboard();
		else
			return mStandardKeyboard.getKeyboard();
	}
	
	@Override
	public void onStartInputView (EditorInfo attribute, boolean restarting){
		if ( (attribute.inputType & InputType.TYPE_CLASS_NUMBER) == InputType.TYPE_CLASS_NUMBER){
			mNumericalKeyboard.onStartInput(attribute, restarting);
			this.setInputView(mNumericalKeyboard.getKeyboard());
		}
		else {
			mStandardKeyboard.onStartInput(attribute, restarting);
			this.setInputView(mStandardKeyboard.getKeyboard());
		}

	}
	
	/**
	 * just switch from standard to symbolic.
	 */
	public void switchSym(){
		if ( isSym ){
			mStandardKeyboard.onStartInput(this.getCurrentInputEditorInfo(), false);

			this.setInputView(mStandardKeyboard.getKeyboard());
		}else{
			mSymbolKeyboard.onStartInput(this.getCurrentInputEditorInfo(), false);

			this.setInputView(mSymbolKeyboard.getKeyboard());
		}
		isSym = !isSym;
	}
}
