package org.tigase.mobile.bookmarks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.tigase.mobile.R;
import org.tigase.mobile.bookmarks.BookmarksActivity.Bookmark;

import tigase.jaxmpp.core.client.BareJID;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class BookmarksAdapter extends BaseExpandableListAdapter {

	private static final String TAG = "BookmarksAdapter";

	private final Activity context;
	private final List<BareJID> groups;
	private final Map<BareJID, List<Bookmark>> store;

	public BookmarksAdapter(Activity context) {
		this.context = context;
		groups = new ArrayList<BareJID>();
		store = Collections.synchronizedMap(new HashMap<BareJID, List<Bookmark>>());
	}

	public void add(Bookmark bookmark) {
		List<Bookmark> bookmarks = store.get(bookmark.accountJid);
		if (bookmarks == null) {
			bookmarks = new ArrayList<Bookmark>();
			store.put(bookmark.accountJid, bookmarks);
			if (!groups.contains(bookmark.accountJid)) {
				groups.add(bookmark.accountJid);
				Collections.sort(groups);
			}
		}
		bookmarks.add(bookmark);
		notifyDataSetChanged();
	}

	public void clear() {
		store.clear();
		notifyDataSetChanged();
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		BareJID group = groups.get(groupPosition);
		List<Bookmark> list = store.get(group);
		;
		if (list == null || list.size() <= childPosition)
			return null;
		return list.get(childPosition);
	}

	public Bookmark getChildById(long id) {
		for (BareJID group : groups) {
			List<Bookmark> bookmarks = store.get(group);
			for (Bookmark bookmark : bookmarks) {
				Log.v(TAG, "compare id = " + id + " with " + bookmark.hashCode());
				if (bookmark.hashCode() == id) {
					return bookmark;
				}
			}
		}
		return null;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		Bookmark child = (Bookmark) getChild(groupPosition, childPosition);
		if (child == null) {
			return 0;
		}
		return child.hashCode();
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		BareJID group = (BareJID) getGroup(groupPosition);
		List<Bookmark> list = store.get(group);
		;
		if (list == null)
			return 0;
		return list.size();
	}

	public List<Bookmark> getChildrenForGroup(BareJID group) {
		return store.get(group);
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		View v = convertView;

		if (v == null) {
			v = context.getLayoutInflater().inflate(R.layout.bookmarks_list_item, null);
		}
		TextView titleView = (TextView) v.findViewById(R.id.bookmark_title);
		TextView descriptionView = (TextView) v.findViewById(R.id.bookmark_description);

		Bookmark bookmark = (Bookmark) getChild(groupPosition, childPosition);
		if (bookmark != null) {
			titleView.setText(bookmark.name);
			descriptionView.setText("Join " + (bookmark.nick != null ? "as " + bookmark.nick : "") + " to "
				+ bookmark.jid.toString());
		}
		else {
			titleView.setText(null);
			descriptionView.setText(null);
		}

		return v;
	}

	@Override
	public Object getGroup(int groupPosition) {
		return groups.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return groups.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		BareJID group = (BareJID) getGroup(groupPosition);
		return group.hashCode();
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		View v = convertView;

		if (v == null) {
			v = context.getLayoutInflater().inflate(R.layout.bookmarks_group_item, null);
		}

		TextView group = (TextView) v.findViewById(R.id.bookmark_group);
		BareJID account = (BareJID) getGroup(groupPosition);
		group.setText(account == null ? null : account.toString());

		return v;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return (childPosition >= 0);
	}

}
