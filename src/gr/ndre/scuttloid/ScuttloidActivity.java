package gr.ndre.scuttloid;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;

/**
 * Base activity
 */
abstract class ScuttloidActivity extends FragmentActivity {

	protected SharedPreferences getGlobalPreferences() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
		return preferences;
	}
	
	public String getURL() {
		return this.getGlobalPreferences().getString("url", "");
	}
	
	public String getUsername() {
		return this.getGlobalPreferences().getString("username", "");
	}
	
	public String getPassword() {
		return this.getGlobalPreferences().getString("password", "");
	}
	
	public ScuttleAPI getAPI(ScuttleAPI.Callback callback) {
		ScuttleAPI api = new ScuttleAPI(this.getURL(), this.getUsername(), this.getPassword(), callback);
		return api;
	}

}
