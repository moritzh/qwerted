package org.momo;

import android.inputmethodservice.InputMethodService;
import android.view.View;

public class SnapBoard extends InputMethodService {
	private Keyboard keyboard;
	public void onBindInput(){
		
	}
	
	public View onCreateInputView(){
		keyboard = new Keyboard(this);
		
		return keyboard.getKeyboard();
	}
}
