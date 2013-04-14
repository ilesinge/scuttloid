package gr.ndre.scuttloid;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BookmarkListAdapter extends ArrayAdapter<BookmarkContent.Item> {

	protected ArrayList<BookmarkContent.Item> bookmarks;
	
	public BookmarkListAdapter(Context context, int textViewResourceId, ArrayList<BookmarkContent.Item> bookmarks) {
		super(context, textViewResourceId, bookmarks);
		this.bookmarks = bookmarks;
	}
	
	public View getView(int position, View convertView, ViewGroup parent){
		View view = convertView;

		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.bookmark_list_item, null);
		}

		BookmarkContent.Item item = this.bookmarks.get(position);

		if (item != null) {
			((TextView)view.findViewById(R.id.title)).setText(item.title);
			String tags = item.getCSVTags();
			if (!tags.isEmpty()) {
				((TextView)view.findViewById(R.id.tags)).setText(item.getCSVTags());
			}
			else {
				View tags_view = view.findViewById(R.id.tags);
				((LinearLayout)tags_view.getParent()).removeView(tags_view);
			}
		}

		return view;

	}
	
}
