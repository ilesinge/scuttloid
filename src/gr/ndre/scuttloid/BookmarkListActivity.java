package gr.ndre.scuttloid;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

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
public class BookmarkListActivity extends ScuttloidActivity implements
		BookmarkListFragment.Callback {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bookmark_list);
		// TODO: If exposing deep links into your app, handle intents here.
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
	
	/**
	 * Callback method from {@link BookmarkListFragment.Callback} indicating
	 * that the item was selected.
	 */
	@Override
	public void onItemSelected(BookmarkContent.Item item) {
		// Start the detail activity for the selected item url.
		Intent detailIntent = new Intent(this, BookmarkDetailActivity.class);
		Bundle extras = new Bundle();
		extras.putSerializable(BookmarkDetailFragment.ARG_ITEM, item);
		detailIntent.putExtra(BookmarkDetailFragment.ARG_ITEM, item);
		startActivity(detailIntent);
	}
	
}
