package org.momo;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class ITypePreferences extends PreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);
	}
}
