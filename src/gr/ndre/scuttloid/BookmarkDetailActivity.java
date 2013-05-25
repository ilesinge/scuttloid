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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * An activity representing a single Bookmark detail screen. This activity is
 * only used on handset devices. On tablet-size devices, item details are
 * presented side-by-side with a list of items in a {@link BookmarkListActivity}
 */
public class BookmarkDetailActivity extends Activity implements ScuttleAPI.DeleteCallback {
	
	/**
	 * The bundle extra representing the position of the item in the shared content list.
	 */
	public static final String ARG_ITEM_POS = "item_pos";
	
	/**
	 * The bookmark's position in the shared content list
	 */
	private int position;
	
	/**
	 * The bookmark content this activity is presenting.
	 */
	private BookmarkContent.Item item;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bookmark_detail);

		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		this.position = getIntent().getIntExtra(ARG_ITEM_POS, 0);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		displayBookmark();
	}
	
	protected void displayBookmark() {
		this.item = BookmarkContent.getShared().getItem(this.position);
		if (this.item != null) {
			((TextView) findViewById(R.id.title)).setText(this.item.title);
			this.setTextOrRemove(R.id.description, this.item.description);
			this.setTextOrRemove(R.id.tags, this.item.getCSVTags());
			((TextView) findViewById(R.id.url)).setText(this.item.url);
		}
	}
	
	protected void setTextOrRemove(int view_id, String value) {
		TextView view = (TextView) findViewById(view_id);
		if (value == null || value.isEmpty()) {
			view.setVisibility(View.GONE);
		}
		else {
			view.setText(value);
			view.setVisibility(View.VISIBLE);
		}
	}
	
	/**
	 * Display option menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.detail_menu, menu);
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menu_item) {
		switch (menu_item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
			case R.id.edit:
				Intent intent = new Intent(this, BookmarkEditActivity.class);
				intent.putExtra(ARG_ITEM_POS, getIntent().getIntExtra(ARG_ITEM_POS, 0));
				startActivity(intent);
				return true;
			case R.id.open:
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse(this.item.url));
	    		startActivity(intent);
	    		return true;
			case R.id.share:
				intent = new Intent(Intent.ACTION_SEND);
				intent.setType("text/plain");
				intent.putExtra(Intent.EXTRA_SUBJECT, this.item.title);
				intent.putExtra(Intent.EXTRA_TEXT, this.item.url);
	    		startActivity(Intent.createChooser(intent, getString(R.string.share_via)));
    	    	return true;
			case R.id.delete:
				DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				        if (which == DialogInterface.BUTTON_POSITIVE) {
				            BookmarkDetailActivity.this.onDeleteConfirmed();
				        }
				    }
				};
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.delete_bookmark);
				builder.setMessage(getString(R.string.delete_confirm, this.item.title));
				builder.setPositiveButton(android.R.string.yes, dialogClickListener);
				builder.setNegativeButton(android.R.string.no, dialogClickListener);
				builder.show();
				return true;
			default:
				return super.onOptionsItemSelected(menu_item);
		}
	}

	protected void onDeleteConfirmed() {
		ScuttleAPI api = new ScuttleAPI(this.getGlobalPreferences(), this);
		api.deleteBookmark(this.item);
	}
	
	@Override
	public void onBookmarkDeleted() {
		BookmarkContent.getShared().removeItem(this.item.url);
		Toast.makeText(this, getString(R.string.bookmark_deleted), Toast.LENGTH_SHORT).show();
		finish();
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
}
