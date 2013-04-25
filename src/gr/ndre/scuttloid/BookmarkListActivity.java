package gr.ndre.scuttloid;

import gr.ndre.scuttloid.BookmarkContent.Item;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
import android.widget.Toast;

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
public class BookmarkListActivity extends ListActivity implements ScuttleAPI.BookmarksCallback,
	ScuttleAPI.DeleteCallback {

	/**
	 * Container for all bookmarks
	 */
	protected BookmarkContent bookmarks;
	
	/**
	 * Container for bookmark being deleted
	 */
	protected BookmarkContent.Item bookmark_to_delete;
	
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
	public boolean onContextItemSelected(MenuItem menu_item) {
		int position = ((AdapterContextMenuInfo) menu_item.getMenuInfo()).position;
		final BookmarkContent.Item item;
		Intent intent;
		switch (menu_item.getItemId()) {
			case R.id.edit:
				intent = new Intent(this, BookmarkEditActivity.class);
				intent.putExtra(BookmarkDetailActivity.ARG_ITEM_POS, position);
				startActivity(intent);
				return true;
			case R.id.details:
				intent = new Intent(this, BookmarkDetailActivity.class);
				intent.putExtra(BookmarkDetailActivity.ARG_ITEM_POS, position);
				startActivity(intent);
				return true;
			case R.id.open:
				item = this.bookmarks.getItem(position);
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.url));
	    		startActivity(intent);
	    		return true;
			case R.id.share:
				item = this.bookmarks.getItem(position);
				intent = new Intent(android.content.Intent.ACTION_SEND);
				intent.setType("text/plain");
				intent.putExtra(android.content.Intent.EXTRA_SUBJECT, item.title);
				intent.putExtra(android.content.Intent.EXTRA_TEXT, item.url);
	    		startActivity(Intent.createChooser(intent, getString(R.string.share_via)));
    	    	return true;
			case R.id.delete:
				item = this.bookmarks.getItem(position);
				DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				        if (which == DialogInterface.BUTTON_POSITIVE) {
				            BookmarkListActivity.this.onDeleteConfirmed(item);
				        }
				    }
				};
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.delete_bookmark);
				builder.setMessage(getString(R.string.delete_confirm, item.title));
				builder.setPositiveButton(android.R.string.yes, dialogClickListener);
				builder.setNegativeButton(android.R.string.no, dialogClickListener);
				builder.show();
				return true;
			default:
				return super.onContextItemSelected(menu_item);
		}
	}
	
	protected void onDeleteConfirmed(Item item) {
		this.bookmark_to_delete = item;
		ScuttleAPI api = new ScuttleAPI(this.getGlobalPreferences(), this);
		api.deleteBookmark(item);
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
				Intent settings_intent = new Intent(this, SettingsActivity.class);
				startActivity(settings_intent);
				return true;
			case R.id.add:
				Intent add_intent = new Intent(this, BookmarkAddActivity.class);
				startActivity(add_intent);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onListItemClick(ListView listView, View view, int position,
			long id) {
		super.onListItemClick(listView, view, position, id);

		// Start the detail activity for the selected item.
		Intent detail_intent = new Intent(this, BookmarkDetailActivity.class);
		detail_intent.putExtra(BookmarkDetailActivity.ARG_ITEM_POS, position);
		startActivity(detail_intent);
	}
	
	protected void loadBookmarks() {
		// Ensure the progress bar is visible
		View progress_bar = findViewById(R.id.progress_bar);
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
		View progress_bar = findViewById(R.id.progress_bar);
		progress_bar.setVisibility(View.GONE);
		
		displayBookmarks();
	}
	
	@Override
	public void onBookmarkDeleted() {
		BookmarkContent.getShared().removeItem(this.bookmark_to_delete.url);
		Toast.makeText(this, getString(R.string.bookmark_deleted), Toast.LENGTH_SHORT).show();
		bookmarks = BookmarkContent.getShared();
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
