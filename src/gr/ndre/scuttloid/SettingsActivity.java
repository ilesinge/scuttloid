/**
 * Scuttloid - Semantic Scuttle Android Client
 * Copyright (C) 2013 Alexandre Gravel-Raymond
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package gr.ndre.scuttloid;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.InputType;


/**
 * Settings screen
 */
public class SettingsActivity extends Activity {

    private static SharedPreferences preferences;

    /**
     * Updates the preference's summary to reflect its new value.
     */
    private static void updateSummaryToValue(Preference preference, Object value) {
        String stringValue = value.toString();
        if (stringValue.isEmpty()) {
            // Put back the default summary
            preference.setSummary((CharSequence) preference.getExtras().get("default_summary"));
        }
        else {
            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                CharSequence summary = null;
                // Set the summary to reflect the new value.
                if (index >= 0) {
                    summary = listPreference.getEntries()[index];
                }
                preference.setSummary(summary);
            }
            else if (preference instanceof EditTextPreference) {
                // For passwords, display dots as summary
                int type = ((EditTextPreference) preference).getEditText().getInputType();
                if (type == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                    int length = stringValue.length();
                    StringBuilder string_builder = new StringBuilder(length);
                    for (int i = 0; i < length; i++) {
                        string_builder.append("●");
                    }
                    preference.setSummary(string_builder.toString());
                }
                // For other types, just set the value as summary
                else {
                    preference.setSummary(stringValue);
                }
            }
            else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
        }
    }

    /**
	 * A preference value change listener
	 */
	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
            String prefKey = preference.getKey();

            // update summary
            if( !"acceptallcerts".equals(prefKey) ) {
                updateSummaryToValue(preference, value);
            }

            // schedule refresh (set flag in shared pref)
            if ("url".equals(prefKey) || "username".equals(prefKey) || "password".equals(prefKey) || "acceptallcerts".equals(prefKey)) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(BookmarkListActivity.LIST_PREFS_NEEDS_REFRESH, true);
                editor.apply();
            }

            return true;
		}
	};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

        preferences = getSharedPreferences(BookmarkListActivity.LIST_PREFS, 0);
    }
	
	/**
	 * Binds a preference's summary to its value. More specifically, when the
	 * preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also
	 * immediately updated upon calling this method. The exact display format is
	 * dependent on the type of preference.
	 * 
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	private static void bindPreferenceSummaryToValue(Preference preference) {
		// Save the default summary for later use if preference is cleared
		preference.getExtras().putCharSequence("default_summary", preference.getSummary());
		
		// Set the listener to watch for value changes.
		preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

		// update the preferences summary with the preference's
		// current value.
		updateSummaryToValue(
                preference,
                PreferenceManager.getDefaultSharedPreferences(
                        preference.getContext()).getString(preference.getKey(),
                        ""));
	}
	
    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.settings);
            
            // Bind the summaries of EditText/List/Dialog preferences
 			// to their values. When their values change, their summaries are
 			// updated to reflect the new value, per the Android Design
 			// guidelines.
            bindPreferenceSummaryToValue(findPreference("url"));
            bindPreferenceSummaryToValue(findPreference("username"));
            bindPreferenceSummaryToValue(findPreference("password"));
            findPreference("acceptallcerts").setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
            bindPreferenceSummaryToValue(findPreference("defaultstatus"));
        }
    }
    
}
