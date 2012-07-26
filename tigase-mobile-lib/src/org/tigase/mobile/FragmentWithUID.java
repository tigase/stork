package org.tigase.mobile;

import android.support.v4.app.Fragment;

public class FragmentWithUID extends Fragment {

	private static int idC = 10;

	protected final int fragmentUID = (++idC);

}
