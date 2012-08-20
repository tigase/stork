package org.tigase.mobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;

public class FragmentWithUID extends Fragment {

	private static int idC = 10;

	private final IntentFilter filter = new IntentFilter(TigaseMobileMessengerActivity.CLIENT_FOCUS_MSG);

	private final BroadcastReceiver focusChangeReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			onPageChange();
		}
	};

	protected final int fragmentUID = (++idC);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActivity().registerReceiver(focusChangeReceiver, filter);
	}

	@Override
	public void onDestroy() {
		getActivity().unregisterReceiver(focusChangeReceiver);
		super.onDestroy();
	};

	protected void onPageChange() {
	}

}
