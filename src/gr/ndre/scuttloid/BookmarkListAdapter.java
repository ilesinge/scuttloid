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

public class BookmarkListAdapter extends ArrayAdapter<BookmarkContent.Item>	implements Filterable {

	protected ArrayList<BookmarkContent.Item> bookmarks;
	protected ArrayList<BookmarkContent.Item> orig_bookmarks = new ArrayList<BookmarkContent.Item>();
	protected Filter filter;
	
	private final Object lock = new Object();

	public BookmarkListAdapter(Context context, int textViewResourceId, ArrayList<BookmarkContent.Item> bookmark_list) {
		super(context, textViewResourceId, bookmark_list);
		this.bookmarks = bookmark_list;
		this.orig_bookmarks = new ArrayList<BookmarkContent.Item>(bookmark_list);
	}

	@Override
	public int getCount() {
		return this.bookmarks.size();
	}

	@Override
	public BookmarkContent.Item getItem(int position) {
		return this.bookmarks.get(position);
	}

	@Override
	public int getPosition(BookmarkContent.Item item) {
		return this.bookmarks.indexOf(item);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public Filter getFilter() {
		if (this.filter == null) {
			this.filter = new BookmarkFilter();
		}
		return this.filter;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;

		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.bookmark_list_item, null);
		}

		BookmarkContent.Item item = this.getItem(position);

		if (item != null) {
			((TextView) view.findViewById(R.id.title)).setText(item.title);
			String tags = item.getCSVTags();
			TextView tags_view = (TextView) view.findViewById(R.id.tags);
			if (tags.isEmpty()) {
				tags_view.setVisibility(View.GONE);
			}
			else {
				tags_view.setText(item.getCSVTags());
				tags_view.setVisibility(View.VISIBLE);
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
			if (BookmarkListAdapter.this.orig_bookmarks == null) {
				synchronized (BookmarkListAdapter.this.lock) {
					BookmarkListAdapter.this.orig_bookmarks = new ArrayList<BookmarkContent.Item>(BookmarkListAdapter.this.bookmarks);
				}
			}

			if (prefix == null || prefix.length() == 0) {
				// No prefix is sent to filter by so we're going to send back
				// the original array
				ArrayList<BookmarkContent.Item> list;
				synchronized (BookmarkListAdapter.this.lock) {
					list = new ArrayList<BookmarkContent.Item>(BookmarkListAdapter.this.orig_bookmarks);
				}
				results.values = list;
				results.count = list.size();
			}
			else {
				// Compare lower case strings
				String prefixString = prefix.toString().toLowerCase(Locale.getDefault());

				ArrayList<BookmarkContent.Item> values;
				synchronized (BookmarkListAdapter.this.lock) {
					values = new ArrayList<BookmarkContent.Item>(BookmarkListAdapter.this.orig_bookmarks);
				}

				final int count = values.size();
				final ArrayList<BookmarkContent.Item> newValues = new ArrayList<BookmarkContent.Item>();

				for (int i = 0; i < count; i++) {
					final BookmarkContent.Item item = values.get(i);
					if (isIncluded(item, prefixString)) {
						newValues.add(item);
					}
				}
				// Set and return
				results.values = newValues;
				results.count = newValues.size();
			}
			return results;
		}

		protected boolean isIncluded(BookmarkContent.Item item, String prefix) {
			final String title = item.title.toLowerCase(Locale.getDefault());
			final String tags = item.tags.toLowerCase(Locale.getDefault());

			// First match against the whole, non-splitted value
			if (title.startsWith(prefix)) {
				return true;
			}

			// Match against each word in the title
			final String[] words = title.split(" ");
			final int wordCount = words.length;
			for (int k = 0; k < wordCount; k++) {
				if (words[k].startsWith(prefix)) {
					return true;
				}
			}

			// Match against each tag
			final String[] tag_list = tags.split(" ");
			final int tagCount = tag_list.length;
			for (int k = 0; k < tagCount; k++) {
				if (tag_list[k].startsWith(prefix)) {
					return true;
				}
			}

			return false;
		}

		@Override
		@SuppressWarnings("unchecked")
		protected void publishResults(CharSequence prefix, FilterResults results) {
			BookmarkListAdapter.this.bookmarks = (ArrayList<BookmarkContent.Item>) results.values;

			if (results.count > 0) {
				notifyDataSetChanged();
			}
			else {
				notifyDataSetInvalidated();
			}

			// Let the adapter know about the updated list
			notifyDataSetChanged();
		}

	}

}
