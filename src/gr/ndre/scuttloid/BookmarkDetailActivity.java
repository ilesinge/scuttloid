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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * An activity representing a single Bookmark detail screen. This activity is
 * only used on handset devices. On tablet-size devices, item details are
 * presented side-by-side with a list of items in a {@link BookmarkListActivity}
 * .
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing more than
 * a {@link BookmarkDetailFragment}.
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
		
		position = getIntent().getIntExtra(ARG_ITEM_POS, 0);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		displayBookmark();
	}
	
	protected void displayBookmark() {
		this.item = BookmarkContent.getShared().getItem(position);
		if (this.item != null) {
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
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.url));
	    		startActivity(intent);
	    		return true;
			case R.id.share:
				intent = new Intent(android.content.Intent.ACTION_SEND);
				intent.setType("text/plain");
				intent.putExtra(android.content.Intent.EXTRA_SUBJECT, item.title);
				intent.putExtra(android.content.Intent.EXTRA_TEXT, item.url);
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
				builder.setMessage(getString(R.string.delete_confirm, item.title));
				builder.setPositiveButton(android.R.string.yes, dialogClickListener);
				builder.setNegativeButton(android.R.string.no, dialogClickListener);
				builder.show();
				return true;
		}
		return super.onOptionsItemSelected(menu_item);
	}

	protected void onDeleteConfirmed() {
		ScuttleAPI api = new ScuttleAPI(this.getGlobalPreferences(), this);
		api.deleteBookmark(item);
	}
	
	@Override
	public void onBookmarkDeleted() {
		BookmarkContent.getShared().removeItem(item.url);
		Toast.makeText(this, getString(R.string.bookmark_deleted), Toast.LENGTH_SHORT).show();
		finish();
	}
	
	protected SharedPreferences getGlobalPreferences() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
		return preferences;
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
