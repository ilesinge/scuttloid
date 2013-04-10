package gr.ndre.scuttloid;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

/**
 * An activity representing a single Bookmark detail screen. This activity is
 * only used on handset devices. On tablet-size devices, item details are
 * presented side-by-side with a list of items in a {@link BookmarkListActivity}
 * .
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing more than
 * a {@link BookmarkDetailFragment}.
 */
public class BookmarkDetailActivity extends ScuttloidActivity {

	/**
	 * The bundle extra representing the item that this activity must display.
	 */
	public static final String ARG_ITEM = "item";
	
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

		item = (BookmarkContent.Item) getIntent().getSerializableExtra(ARG_ITEM);
		
		if (item != null) {
			((TextView) findViewById(R.id.bookmark_title)).setText(item.title);
			((TextView) findViewById(R.id.bookmark_summary)).setText(item.summary);
			((TextView) findViewById(R.id.bookmark_tags)).setText(item.getCommaSeparatedTags());
			((TextView) findViewById(R.id.bookmark_url)).setText(item.url);
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
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
			case R.id.edit:
				Intent intent = new Intent(this, BookmarkEditActivity.class);
				startActivity(intent);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
