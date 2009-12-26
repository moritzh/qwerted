package org.momo;

import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.inputmethod.EditorInfo;

/**
 * we need to move all our keyboard-switching related tasks here. And we should handle prediction here in some way, 
 * at least the ability to switch it on and off on demand.
 * @author Moritz
 *
 */
public class IType extends InputMethodService {
	private Keyboard mStandardKeyboard;
	private Keyboard mNumericalKeyboard;
	public void onBindInput(){
		
	}
	
	public View onCreateInputView(){
		mStandardKeyboard = new Keyboard(this, this.getBaseContext().getResources().getXml(R.xml.keyboard));
		mNumericalKeyboard = new Keyboard(this, this.getBaseContext().getResources().getXml(R.xml.numerical_keyboard));
		if ( this.getCurrentInputEditorInfo().inputType == EditorInfo.TYPE_CLASS_NUMBER)
			return mNumericalKeyboard.getKeyboard();
		else
			return mStandardKeyboard.getKeyboard();
	}
	
	public void onStartInputView (EditorInfo attribute, boolean restarting){
		if ( (attribute.inputType & EditorInfo.TYPE_CLASS_NUMBER) == EditorInfo.TYPE_CLASS_NUMBER){
			mNumericalKeyboard.onStartInput(attribute, restarting);
			this.setInputView(mNumericalKeyboard.getKeyboard());
		}
		else {
			mStandardKeyboard.onStartInput(attribute, restarting);
			this.setInputView(mStandardKeyboard.getKeyboard());
		}
	}
}
