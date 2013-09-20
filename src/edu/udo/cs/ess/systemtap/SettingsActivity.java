package edu.udo.cs.ess.systemtap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import edu.udo.cs.ess.logging.Eventlog;

public class SettingsActivity extends PreferenceActivity {

	private static final String TAG = SettingsActivity.class.getSimpleName();

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		/* try to load preferences */
		try {
			addPreferencesFromResource(R.xml.preferences);
		} catch (ClassCastException e) {
			/* oh no, it did not worked :-( */
			Eventlog.e(TAG, "Shared preferences are corrupt! Resetting to default values.");
			/* reset the default preferences object ... */
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
			SharedPreferences.Editor editor = preferences.edit();
			editor.clear();
			editor.commit();
			/* ... and load the default values */
			PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
			editor = preferences.edit();
			editor.commit();
			/* now reload them! */
			addPreferencesFromResource(R.xml.preferences);
		}
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		//Intent intent = new Intent(this,MobiDACService.class);
		//intent.setAction(MobiDACService.RELOAD_PREFERENCES);
		//this.startService(intent);
	}
}
