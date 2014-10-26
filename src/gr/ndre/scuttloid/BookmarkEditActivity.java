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

public class BookmarkEditActivity extends Activity implements OnClickListener, BookmarkManager.UpdateCallback {

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
		this.item = BookmarkContent.getShared().getItem(position);
		if (this.item != null) {
			((TextView) findViewById(R.id.url)).setText(this.item.url);
			((TextView) findViewById(R.id.title)).setText(this.item.title);
			((TextView) findViewById(R.id.description)).setText(this.item.description);
			((TextView) findViewById(R.id.tags)).setText(this.item.getTags());
		}
		
		// Setup the Privacy (status) spinner.
		Spinner spinner = (Spinner) findViewById(R.id.status);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
				R.array.status_options,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		int default_status = Integer.parseInt(this.getGlobalPreferences().getString("defaultstatus", "0"));
		spinner.setSelection(default_status);
		
		// Handle when the user presses the save button.
		Button btnSave = (Button) findViewById(R.id.save_button);
		btnSave.setOnClickListener(this);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem menu_item) {
		switch (menu_item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
		}
		return super.onOptionsItemSelected(menu_item);
	}

	@Override
	public void onClick(View view) {
		EditText field_title = (EditText) findViewById(R.id.title); 
		String title = field_title.getText().toString();
		String description = ((EditText) findViewById(R.id.description)).getText().toString(); 
		String tags = ((EditText) findViewById(R.id.tags)).getText().toString();
		String status = String.valueOf(((Spinner) findViewById(R.id.status)).getSelectedItemPosition());
		
		if ("".equals(title.trim())) {
			field_title.setError(getString(R.string.error_titlerequired));
		}
		else {
			this.item.title = title;
			this.item.description = description;
			this.item.tags = tags;
			this.item.status = status;
			
			// Update the bookmark
            BookmarkManager api = new BookmarkManager(this.getGlobalPreferences(), this);
			api.updateBookmark(this.item);
		}
	}
	
	protected SharedPreferences getGlobalPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
	}

	@Override
	public void onManagerError(String message) {
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
		BookmarkContent.getShared().updateItem(this.item);
		Toast.makeText(this, getString(R.string.bookmark_updated), Toast.LENGTH_SHORT).show();
		finish();
	}

}
