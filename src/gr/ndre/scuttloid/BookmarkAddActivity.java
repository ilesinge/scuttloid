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
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class BookmarkAddActivity extends Activity
	implements OnClickListener, ScuttleAPI.CreateCallback, ScuttleAPI.BookmarksCallback {

	/**
	 * The bookmark content this activity is editing.
	 */
	private BookmarkContent.Item item;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bookmark_add);
		
		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		// Setup the Privacy (status) spinner.
		Spinner spinner = (Spinner) findViewById(R.id.status);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
				R.array.status_options,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		int default_status = Integer.parseInt(this.getGlobalPreferences().getString("defaultstatus", "0"));
		spinner.setSelection(default_status);
		
		// Get the extras data passed in with the intent.
		Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			// Set the text fields to the values of the passed in data.
			EditText url_field = (EditText) findViewById(R.id.url);
			url_field.setText(extras.getCharSequence(Intent.EXTRA_TEXT));
			EditText title_field = (EditText) findViewById(R.id.title);
			title_field.setText(extras.getString(Intent.EXTRA_SUBJECT));
		}
		
		// Handle when the user presses the save button.
		Button btnSave = (Button) findViewById(R.id.save_button);
		btnSave.setOnClickListener(this);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem menu_item) {
		if (menu_item.getItemId() == android.R.id.home) {
				finish();
				return true;
		}
		return super.onOptionsItemSelected(menu_item);
	}

	@Override
	public void onClick(View view) {
		EditText field_url = (EditText) findViewById(R.id.url);
		EditText field_title = (EditText) findViewById(R.id.title);
		String url = field_url.getText().toString();
		String title = field_title.getText().toString();
		String description = ((EditText) findViewById(R.id.description)).getText().toString(); 
		String tags = ((EditText) findViewById(R.id.tags)).getText().toString();
		String status = String.valueOf(((Spinner) findViewById(R.id.status)).getSelectedItemPosition());
		
		boolean error = false;
		if ("".equals(title.trim())) {
			field_title.setError(getString(R.string.error_titlerequired));
			error = true;
		}
		if ("".equals(url.trim())) {
			field_url.setError(getString(R.string.error_urlrequired));
			error = true;
		}
		if (!error) {
			this.item = new BookmarkContent.Item();
			String fixed_url = URLUtil.guessUrl(url);
			if (fixed_url.endsWith("/")) {
				fixed_url = fixed_url.substring(0, fixed_url.length() - 1);
			}
			this.item.url = fixed_url;
			this.item.title = title;
			this.item.description = description;
			this.item.tags = tags;
			this.item.status = status;
			
			// Save the bookmark
			ScuttleAPI api = new ScuttleAPI(this.getGlobalPreferences(), this);
			api.createBookmark(this.item);
		}
	}
	
	protected SharedPreferences getGlobalPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
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
	public void onBookmarkCreated() {
        BookmarkContent bookmarks = BookmarkContent.getShared();
        if (bookmarks != null) {
            // Otherwise we come from a SHARE intent and there is no need to put the item on top of the list.
            bookmarks.addItemToTop(this.item);
        }
        Toast.makeText(this, getString(R.string.bookmark_created), Toast.LENGTH_SHORT).show();
        finish();
	}

	@Override
	public void onBookmarkExists() {
		BookmarkContent bookmarks = BookmarkContent.getShared();
		if (bookmarks == null) {
			// No bookmarks are available, probably coming from SHARE intent.
			this.retrieveBookmarks();
		}
		else {
			Toast.makeText(this, getString(R.string.error_bookmarkexists), Toast.LENGTH_SHORT).show();
			this.sendToEdit();
		}
	}

	protected void sendToEdit() {
		Integer position = BookmarkContent.getShared().getPosition(this.item.url);
		if (position != -1) {
			Intent intent = new Intent(this, BookmarkEditActivity.class);
			intent.putExtra(BookmarkDetailActivity.ARG_ITEM_POS, position);
			startActivity(intent);
			finish();
		}
	}

	protected void retrieveBookmarks() {
		// Display the progress bar
		View form = findViewById(R.id.form);
		form.setVisibility(View.GONE);
		View progress_bar = findViewById(R.id.progress_bar);
		progress_bar.setVisibility(View.VISIBLE);
		
		Toast.makeText(this, getString(R.string.error_bookmarkexists_retrieving), Toast.LENGTH_LONG).show();
		
		ScuttleAPI api = new ScuttleAPI(this.getGlobalPreferences(), this);
		api.getBookmarks();
	}
	
	@Override
	public void onBookmarksReceived(BookmarkContent bookmarks) {
		BookmarkContent.setShared(bookmarks);
		this.sendToEdit();
	}

}
