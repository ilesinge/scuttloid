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

import gr.ndre.scuttloid.BookmarkContent.Item;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;
import android.support.v4.widget.SwipeRefreshLayout;

/**
 * An activity representing a list of Bookmarks. The activity
 * presents a list of items, which when touched, lead to a
 * {@link BookmarkDetailActivity} representing item details.
 */
public class BookmarkListActivity extends ListActivity implements BookmarkManager.BookmarksCallback,
        BookmarkManager.DeleteCallback, SwipeRefreshLayout.OnRefreshListener {

	/**
	 * Container for all bookmarks
	 */
	protected BookmarkContent bookmarks;
	
	/**
	 * Container for bookmark being deleted
	 */
	protected BookmarkContent.Item bookmark_to_delete;
	
	protected BookmarkListAdapter adapter;
	
	protected String search_query = "";

    private SwipeRefreshLayout swipeLayout;

    /**
     * preferences key for list prefs
     */
	public static final String LIST_PREFS = "list_prefs";
    public static final String LIST_PREFS_NEEDS_REFRESH = "needs_refresh";

    public SharedPreferences getSharedPreferences () {
        return this.getSharedPreferences("FILE", 0);
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_bookmark_list);
		ListView list = getListView();
		registerForContextMenu(list);
		list.setTextFilterEnabled(true);
		
		String pref_url = getURL();
		if ("".equals(pref_url)) {
			startActivity(new Intent(this, SettingsActivity.class));
		}
		
		handleIntent();

        //setup swipeLayout
        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);

        swipeLayout.setColorSchemeResources(android.R.color.holo_orange_light,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_green_light);
	}
	
	@Override
	public void onResume() {
		super.onResume();

        // skip loading bookmarks if server url is not set
        String pref_url = getURL();
        if (!"".equals(pref_url)) {
            // if a refresh is scheduled, refresh the bookmarks instead of loading them from the database
            SharedPreferences preferences = getSharedPreferences(BookmarkListActivity.LIST_PREFS, 0);
            Boolean refresh = preferences.getBoolean(LIST_PREFS_NEEDS_REFRESH, true);
            if (refresh) {
                this.refreshBookmarks();
                // reset scheduled refresh
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(BookmarkListActivity.LIST_PREFS_NEEDS_REFRESH, false);
                editor.apply();
            } else {
                // TODO : verify that the bookmarks are not reloaded on orientation change
                if (!(this.bookmarks instanceof BookmarkContent)) {
                    this.loadBookmarks();
                }
            }
        }
		// Reload bookmarks if we are not showing search results
		if (this.bookmarks instanceof BookmarkContent) {
			this.bookmarks = BookmarkContent.getShared();
			this.displayBookmarks();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		handleIntent();
	}
	
	protected void handleIntent() {
		if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
			// handles a search query
			this.search_query = getIntent().getStringExtra(SearchManager.QUERY);
		}
	}

    // refresh bookmarks on swipe
    @Override public void onRefresh() {
        this.refreshBookmarks();
    }

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.list_context_menu, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem menu_item) {
		int position = ((AdapterContextMenuInfo) menu_item.getMenuInfo()).position;
		final BookmarkContent.Item item;
		int shared_position = BookmarkContent.getShared().getPosition(this.adapter.getItem(position).url);
		Intent intent;
		switch (menu_item.getItemId()) {
			case R.id.edit:
				intent = new Intent(this, BookmarkEditActivity.class);
				intent.putExtra(BookmarkDetailActivity.ARG_ITEM_POS, shared_position);
				startActivity(intent);
				return true;
			case R.id.details:
				intent = new Intent(this, BookmarkDetailActivity.class);
				intent.putExtra(BookmarkDetailActivity.ARG_ITEM_POS, shared_position);
				startActivity(intent);
				return true;
			case R.id.open:
				item = this.bookmarks.getItem(position);
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.url));
	    		startActivity(intent);
	    		return true;
			case R.id.share:
				item = this.bookmarks.getItem(position);
				intent = new Intent(Intent.ACTION_SEND);
				intent.setType("text/plain");
				intent.putExtra(Intent.EXTRA_SUBJECT, item.title);
				intent.putExtra(Intent.EXTRA_TEXT, item.url);
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
        BookmarkManager manager = new BookmarkManager(this.getGlobalPreferences(), this);
		manager.deleteBookmark(item);
	}
	
	/**
	 * Display option menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_menu, menu);
	    
	    // Get the SearchView and set the searchable configuration
	    SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
	    SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
	    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
	    // Add live search capability
	    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextChange(String newText) {
				if (BookmarkListActivity.this.adapter != null) {
					BookmarkListActivity.this.adapter.getFilter().filter(newText);
				}
				BookmarkListActivity.this.search_query = newText;
				return true;
			}
			@Override
			public boolean onQueryTextSubmit(String query) {
				return false;
			}
	    });
	    return true;
	}
	
	/**
	 * Option menu clicks
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.refresh:
				this.refreshBookmarks();
				return true;
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
	public void onListItemClick(ListView listView, View view, int position, long item_id) {
		super.onListItemClick(listView, view, position, item_id);

		// Start the detail activity for the selected item.
		Intent detail_intent = new Intent(this, BookmarkDetailActivity.class);
		int shared_position = BookmarkContent.getShared().getPosition(this.adapter.getItem(position).url);
		detail_intent.putExtra(BookmarkDetailActivity.ARG_ITEM_POS, shared_position);
		startActivity(detail_intent);
	}
	
	protected void loadBookmarks() {
		// Ensure list is invisible
		View list = findViewById(android.R.id.list);
		list.setVisibility(View.GONE);

		// Get the bookmarks
        BookmarkManager manager = new BookmarkManager(this.getGlobalPreferences(), this);
		manager.getBookmarks();
	}

    protected void refreshBookmarks() {
        // start 'refresh' animation
        swipeLayout.setRefreshing(true);

        // refresh the bookmarks
        BookmarkManager manager = new BookmarkManager(this.getGlobalPreferences(), this);
        manager.refreshBookmarks();
    }
	
	protected void displayBookmarks() {
		// Set the list adapter
		this.adapter = new BookmarkListAdapter(
				this,
				R.id.title,
				this.bookmarks.getItems()
		);
		
		// Display now if there is no search term
		if (this.search_query.isEmpty()) {
			setListAdapter(this.adapter);
		}
		// Delay the display otherwise
		else {
			this.adapter.getFilter().filter(this.search_query);
			this.adapter.registerDataSetObserver(new DataSetObserver() {
				public void onChanged() {
					setListAdapter(BookmarkListActivity.this.adapter);
				}
			});
		}
	}

    /**
     * Display the received bookmarks and hide loaders
     * @param new_bookmarks: a list of all bookmarks or null if unchanged
     */
	@Override
	public void onBookmarksReceived(BookmarkContent new_bookmarks) {
        if( new_bookmarks != null ) {
            this.bookmarks = new_bookmarks;
            BookmarkContent.setShared(new_bookmarks);
        }

        // stop 'refresh' animation
        swipeLayout.setRefreshing(false);

		// Display list
		View list = findViewById(android.R.id.list);
		list.setVisibility(View.VISIBLE);
		
		this.displayBookmarks();
	}
	
	@Override
	public void onBookmarkDeleted() {
		BookmarkContent.getShared().removeItem(this.bookmark_to_delete.url);
		Toast.makeText(this, getString(R.string.bookmark_deleted), Toast.LENGTH_SHORT).show();
		this.bookmarks = BookmarkContent.getShared();
		this.displayBookmarks();
	}
	
	@Override
	public void onManagerError(String message) {
		AlertDialog alert = new AlertDialog.Builder(this).create();
		alert.setMessage(message);  
		alert.show();

        // stop 'refresh' animation
        swipeLayout.setRefreshing(false);

		// Display list
		View list = findViewById(android.R.id.list);
		list.setVisibility(View.VISIBLE);
	}

	@Override
	public Context getContext() {
		return this;
	}
	
	public String getURL() {
		return this.getGlobalPreferences().getString("url", "");
	}
	
	protected SharedPreferences getGlobalPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
	}
	
}
