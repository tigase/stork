package org.tigase.mobile;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class RosterFragment extends Fragment {

	public static RosterFragment newInstance(RosterAdapter adapter, OnItemClickListener listener) {
		RosterFragment f = new RosterFragment();
		f.adapter = adapter;
		f.clickListener = listener;
		return f;
	}

	private RosterAdapter adapter;

	private OnItemClickListener clickListener;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View layout = inflater.inflate(R.layout.roster_list, null);

		ListView lv = (ListView) layout.findViewById(R.id.rosterList);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(clickListener);

		return layout;
	}

}
