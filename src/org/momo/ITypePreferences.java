package org.momo;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;

public class ITypePreferences extends PreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
	/*	this.findPreference("dictionary.launcher").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
			public boolean onPreferenceClick(Preference p){
				launch();
				return false;
			}
		});*/
	}
	
	
	private void launch(){
		startActivity(new Intent(this,DictionaryManager.class));

	}
}
