package org.tigase.mobile.muc;

import org.tigase.mobile.R;
import org.tigase.mobile.chat.ChatView;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MucRoomFragment extends Fragment {

	private static final boolean DEBUG = false;

	private static final String TAG = "MUC";

	public static Fragment newInstance(String account, long roomId, int pageIndex) {
		MucRoomFragment f = new MucRoomFragment();

		// Bundle args = new Bundle();
		// args.putLong("roomId", roomId);
		// args.putString("account", account);
		// args.putInt("page", pageIndex);
		// f.setArguments(args);

		if (DEBUG)
			Log.d(TAG, "Creating MucRoomFragment id=" + roomId);

		return f;
	}

	private ChatView layout;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.muc_conversation, container, false);
	}
}
