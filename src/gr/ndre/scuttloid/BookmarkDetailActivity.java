package gr.ndre.scuttloid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
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
public class BookmarkDetailActivity extends Activity {

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
			((TextView) findViewById(R.id.title)).setText(item.title);
			this.setTextOrRemove(R.id.description, item.description);
			this.setTextOrRemove(R.id.tags, item.getCSVTags());
			((TextView) findViewById(R.id.url)).setText(item.url);
		}
	}
	
	protected void setTextOrRemove(int id, String value) {
		if (value != null && !value.isEmpty()) {
			((TextView) findViewById(id)).setText(value);
		}
		else {
			View view = findViewById(id);
			((LinearLayout)view.getParent()).removeView(view);
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
				intent.putExtra(ARG_ITEM,
						getIntent().getSerializableExtra(ARG_ITEM));
				startActivity(intent);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
