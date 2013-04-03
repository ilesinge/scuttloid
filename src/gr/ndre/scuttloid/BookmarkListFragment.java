package gr.ndre.scuttloid;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;


/**
 * A list fragment representing a list of Bookmarks.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callback}
 * interface.
 */
public class BookmarkListFragment extends ListFragment implements ScuttleAPI.BookmarksCallback {
	
	BookmarkContent bookmarks;
	
	/**
	 * The fragment's current callback object, which is notified of list item
	 * clicks.
	 */
	private Callback mCallback = sBookmarkCallback;

	/**
	 * A callback interface that all activities containing this fragment must
	 * implement. This mechanism allows activities to be notified of item
	 * selections.
	 */
	public interface Callback {
		/**
		 * Callback for when an item has been selected.
		 */
		public void onItemSelected(BookmarkContent.Item item);
	}

	/**
	 * A dummy implementation of the {@link Callback} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callback sBookmarkCallback = new Callback() {
		@Override
		public void onItemSelected(BookmarkContent.Item item) {
		}
	};

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public BookmarkListFragment() {
	}
	
	@Override
	public void onBookmarksReceived(BookmarkContent bookmarks) {
		this.bookmarks = bookmarks;
		setListAdapter(new ArrayAdapter<BookmarkContent.Item>(getActivity(),
				R.layout.bookmark_list_item,
				R.id.title, this.bookmarks.getItems()));
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		ScuttleAPI api = ((ScuttloidActivity) this.getActivity()).getAPI(this);
		api.getBookmarks();
		// TODO : only retrieve bookmarks if we don't have them yet !
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof Callback)) {
			throw new IllegalStateException(
					"Activity must implement fragment's callbacks.");
		}

		mCallback = (Callback) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();

		// Reset the active callbacks interface to the bookmark implementation.
		mCallback = sBookmarkCallback;
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position,
			long id) {
		super.onListItemClick(listView, view, position, id);

		// Notify the active callbacks interface (the activity, if the
		// fragment is attached to one) that an item has been selected.
		mCallback.onItemSelected(bookmarks.getItem(position));
	}
	
}
