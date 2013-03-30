package gr.ndre.scuttloid;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
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
 * {@link BookmarkListFragment.Callbacks} interface to listen for item
 * selections.
 */
public class BookmarkListActivity extends FragmentActivity implements
		BookmarkListFragment.Callbacks {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bookmark_list);
		// TODO: If exposing deep links into your app, handle intents here.
	}

	/**
	 * Callback method from {@link BookmarkListFragment.Callbacks} indicating
	 * that the item with the given ID was selected.
	 */
	@Override
	public void onItemSelected(String id) {
		// Start the detail activity for the selected item ID.
		Intent detailIntent = new Intent(this, BookmarkDetailActivity.class);
		detailIntent.putExtra(BookmarkDetailFragment.ARG_ITEM_ID, id);
		startActivity(detailIntent);
	}
}
