package gr.ndre.scuttloid;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.InputType;


/**
 * Settings screen
 */
public class SettingsActivity extends PreferenceActivity {
	

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
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
        }
    }
    
    /**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value.toString();
			if (!stringValue.isEmpty()) {
				if (preference instanceof ListPreference) {
					// For list preferences, look up the correct display value in
					// the preference's 'entries' list.
					ListPreference listPreference = (ListPreference) preference;
					int index = listPreference.findIndexOfValue(stringValue);
	
					// Set the summary to reflect the new value.
					preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
				}
				else if (preference instanceof EditTextPreference) {
					// For passwords, display dots as summary
					int type = ((EditTextPreference) preference).getEditText().getInputType();
					if (type == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
						int length = stringValue.length();
						StringBuilder sb = new StringBuilder(length);
					    for (int i=0; i<length; i++ ) {
					        sb.append("●"); 
					    }
						preference.setSummary(sb.toString());
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
			else {
				// Put back the default summary
				preference.setSummary((CharSequence)preference.getExtras().get("default_summary"));
			}
			return true;
		}
	};
	
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

		// Trigger the listener immediately with the preference's
		// current value.
		sBindPreferenceSummaryToValueListener.onPreferenceChange(
				preference,
				PreferenceManager.getDefaultSharedPreferences(
						preference.getContext()).getString(preference.getKey(),
						""));
	}
    
}
