package gr.ndre.scuttloid;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.ProgressBar;

/**
 * An activity representing a list of Bookmarks. The activity
 * presents a list of items, which when touched, lead to a
 * {@link BookmarkDetailActivity} representing item details.
 * <p>
 * The list of items is a {@link BookmarkListFragment}.
 * <p>
 * This activity also implements the required
 * {@link BookmarkListFragment.Callback} interface to listen for item
 * selections.
 */
public class BookmarkListActivity extends ListActivity implements ScuttleAPI.BookmarksCallback {

	/**
	 * Container for all bookmarks
	 */
	protected BookmarkContent bookmarks;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bookmark_list);
		ListView list = (ListView)findViewById(android.R.id.list);
		registerForContextMenu(list);
		
		String pref_url = getURL();
		if (pref_url.equals("")) {
			startActivity(new Intent(this, SettingsActivity.class));
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.list_context_menu, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		int position = ((AdapterContextMenuInfo) item.getMenuInfo()).position;
		switch (item.getItemId()) {
			case R.id.edit:
				Intent intent = new Intent(this, BookmarkEditActivity.class);
				intent.putExtra(BookmarkDetailActivity.ARG_ITEM_POS, position);
				startActivity(intent);
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// TODO : verify that the bookmarks are not reloaded on orientation change
		String pref_url = getURL();
		if (!pref_url.equals("") && !(bookmarks instanceof BookmarkContent)) {
			loadBookmarks();
		}
		if (bookmarks instanceof BookmarkContent) {
			bookmarks = BookmarkContent.getShared();
			displayBookmarks();
		}
	}

	/**
	 * Display option menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_menu, menu);
	    return true;
	}
	
	/**
	 * Option menu clicks
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.settings:
				Intent intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onListItemClick(ListView listView, View view, int position,
			long id) {
		super.onListItemClick(listView, view, position, id);

		// Start the detail activity for the selected item.
		Intent detailIntent = new Intent(this, BookmarkDetailActivity.class);
		detailIntent.putExtra(BookmarkDetailActivity.ARG_ITEM_POS, position);
		startActivity(detailIntent);
	}
	
	protected void loadBookmarks() {
		// Ensure the progress bar is visible
		ProgressBar progress_bar = (ProgressBar) findViewById(R.id.progress_bar);
		progress_bar.setVisibility(View.VISIBLE);
		
		// Get the bookmarks
		ScuttleAPI api = new ScuttleAPI(this.getGlobalPreferences(), this);
		api.getBookmarks();
	}
	
	protected void displayBookmarks() {
		// Set the list adapter
		BookmarkListAdapter adapter = new BookmarkListAdapter(
				this,
				R.id.title,
				this.bookmarks.getItems()
		);
		setListAdapter(adapter);
	}
	
	@Override
	public void onBookmarksReceived(BookmarkContent bookmarks) {
		this.bookmarks = bookmarks;
		BookmarkContent.setShared(bookmarks);
		
		// Remove the progress bar
		ProgressBar progress_bar = (ProgressBar) findViewById(R.id.progress_bar);
		progress_bar.setVisibility(View.GONE);
		
		displayBookmarks();
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
	
	public String getURL() {
		return this.getGlobalPreferences().getString("url", "");
	}
	
	protected SharedPreferences getGlobalPreferences() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
		return preferences;
	}
	
}
