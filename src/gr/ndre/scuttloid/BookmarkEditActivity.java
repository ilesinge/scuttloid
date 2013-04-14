package gr.ndre.scuttloid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class BookmarkEditActivity extends Activity implements OnClickListener, ScuttleAPI.UpdateCallback {

	/**
	 * The bookmark content this activity is editing.
	 */
	private BookmarkContent.Item item;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bookmark_edit);
		
		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		// Fill the form with actual bookmark data
		int position = getIntent().getIntExtra(BookmarkDetailActivity.ARG_ITEM_POS, 0);
		item = (BookmarkContent.Item) BookmarkContent.getShared().getItem(position);
		if (item != null) {
			((TextView) findViewById(R.id.url)).setText(item.url);
			((TextView) findViewById(R.id.title)).setText(item.title);
			((TextView) findViewById(R.id.description)).setText(item.description);
			((TextView) findViewById(R.id.tags)).setText(item.getTags());
		}
		
		// Setup the Privacy (status) spinner.
		Spinner spinner = (Spinner) findViewById(R.id.status);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
				R.array.status_options,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		
		// Handle when the user presses the save button.
		Button btnSave = (Button)findViewById(R.id.save_button);
		btnSave.setOnClickListener(this);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		EditText field_title = (EditText)findViewById(R.id.title); 
		String title = field_title.getText().toString();
		String description = ((EditText)findViewById(R.id.description)).getText().toString(); 
		String tags = ((EditText)findViewById(R.id.tags)).getText().toString();
		String status = String.valueOf(((Spinner)findViewById(R.id.status)).getSelectedItemPosition());
		
		if (title.trim().equals("")) {
			field_title.setError(getString(R.string.error_titlerequired));
		}
		else {
			item.title = title;
			item.description = description;
			item.tags = tags;
			item.status = status;
			
			// Update the bookmark
			ScuttleAPI api = new ScuttleAPI(this.getGlobalPreferences(), this);
			api.updateBookmark(item);
		}
	}
	
	protected SharedPreferences getGlobalPreferences() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
		return preferences;
	}

	@Override
	public void onAPIError(String message) {
	    AlertDialog alert = new AlertDialog.Builder(this).create();
	    alert.setMessage(message);  
	    alert.show();
	}

	@Override
	public Context getContext() {
		return this;
	}

	@Override
	public void onBookmarkUpdated() {
		BookmarkContent.getShared().addItem(item);
		Toast.makeText(this, getString(R.string.bookmark_updated), Toast.LENGTH_SHORT).show();
		finish();
	}

}
