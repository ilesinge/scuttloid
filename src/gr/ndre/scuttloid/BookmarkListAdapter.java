package gr.ndre.scuttloid;

import java.util.ArrayList;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

public class BookmarkListAdapter extends ArrayAdapter<BookmarkContent.Item> implements Filterable {

	protected ArrayList<BookmarkContent.Item> bookmarks;
	protected ArrayList<BookmarkContent.Item> orig_bookmarks = new ArrayList<BookmarkContent.Item>();
	private final Object lock = new Object();
	protected Filter filter;
	
	public BookmarkListAdapter(Context context, int textViewResourceId, ArrayList<BookmarkContent.Item> bookmarks) {
		super(context, textViewResourceId, bookmarks);
		this.bookmarks = bookmarks;
		this.orig_bookmarks = new ArrayList<BookmarkContent.Item>(bookmarks);
	}
	
	@Override
	public int getCount() {
		return bookmarks.size();
	}
	@Override
	public BookmarkContent.Item getItem(int position) {
		return bookmarks.get(position);
	}
	@Override
	public int getPosition(BookmarkContent.Item item) {
		return bookmarks.indexOf(item);
	}
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public Filter getFilter() {
		if (filter == null) {
			filter = new BookmarkFilter();
		}
		return filter;
	}
	
	public View getView(int position, View convertView, ViewGroup parent){
		View view = convertView;

		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.bookmark_list_item, null);
		}

		BookmarkContent.Item item = this.getItem(position);

		if (item != null) {
			((TextView)view.findViewById(R.id.title)).setText(item.title);
			String tags = item.getCSVTags();
			TextView tags_view = (TextView) view.findViewById(R.id.tags);
			if (!tags.isEmpty()) {
				tags_view.setText(item.getCSVTags());
				tags_view.setVisibility(View.VISIBLE);
			}
			else {
				tags_view.setVisibility(View.GONE);
			}
		}

		return view;

	}
	
    protected class BookmarkFilter extends Filter {

    	@Override
        @SuppressLint("DefaultLocale")
		protected FilterResults performFiltering(CharSequence prefix) {
            // Initiate our results object
            FilterResults results = new FilterResults();
            
            // If the adapter array is empty, check the actual items array and use it
            if (orig_bookmarks == null) {
                synchronized (lock) {
                	orig_bookmarks = new ArrayList<BookmarkContent.Item>(bookmarks);
                }
            }
            
            if (prefix == null || prefix.length() == 0) {
            	// No prefix is sent to filter by so we're going to send back the original array
            	ArrayList<BookmarkContent.Item> list;
            	synchronized(lock) {
            		list =  new ArrayList<BookmarkContent.Item>(orig_bookmarks);
            	}
            	results.values = list;
                results.count = list.size();
            }
            else {
            	// Compare lower case strings
                String prefixString = prefix.toString().toLowerCase(Locale.getDefault());
                
                ArrayList<BookmarkContent.Item> values;
                synchronized (lock) {
                    values = new ArrayList<BookmarkContent.Item>(orig_bookmarks);
                }
                
                final int count = values.size();
                final ArrayList<BookmarkContent.Item> newValues = new ArrayList<BookmarkContent.Item>();

                for (int i = 0; i < count; i++) {
                    final BookmarkContent.Item item = values.get(i);
                    final String title = item.title.toLowerCase(Locale.getDefault());
                    //String description = item.description.toLowerCase(Locale.getDefault());
                    //String tags = item.tags.toLowerCase(Locale.getDefault());
                    
                    // First match against the whole, non-splitted value
                    if (title.startsWith(prefixString)) {
                    	newValues.add(item);
                    } else {} /* This is option and taken from the source of ArrayAdapter
                        final String[] words = itemName.split(" ");
                        final int wordCount = words.length;
                        for (int k = 0; k < wordCount; k++) {
                            if (words[k].startsWith(prefixString)) {
                                newItems.add(item);
                                break;
                            }
                        }
                    } */
                }
                // Set and return
	            results.values = newValues;
	            results.count = newValues.size();
            }
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
		protected void publishResults(CharSequence prefix, FilterResults results) {
        	bookmarks = (ArrayList<BookmarkContent.Item>) results.values;

        	if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        	
        	// Let the adapter know about the updated list
            notifyDataSetChanged();
        }

    }
    
}
