package gr.ndre.scuttloid;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A fragment representing a single Bookmark detail screen.
 */
public class BookmarkDetailFragment extends Fragment {
	
	/**
	 * The fragment argument representing the item URL that this fragment
	 * represents.
	 */
	public static final String ARG_ITEM = "item";

	/**
	 * The bookmark content this fragment is presenting.
	 */
	private BookmarkContent.Item mItem;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public BookmarkDetailFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments().containsKey(ARG_ITEM)) {
			// Load the bookmark content specified by the fragment
			// arguments. In a real-world scenario, use a Loader
			// to load content from a content provider.
			mItem = (BookmarkContent.Item) getArguments().getSerializable(ARG_ITEM);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_bookmark_detail,
				container, false);

		// Show the bookmark content as text in a TextView.
		if (mItem != null) {
			((TextView) rootView.findViewById(R.id.bookmark_detail))
					.setText(mItem.title);
		}

		return rootView;
	}
}
