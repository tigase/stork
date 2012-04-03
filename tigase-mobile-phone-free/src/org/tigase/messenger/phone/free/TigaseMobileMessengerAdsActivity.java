package org.tigase.messenger.phone.free;

import android.os.Bundle;
import android.widget.LinearLayout;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

public class TigaseMobileMessengerAdsActivity extends org.tigase.mobile.TigaseMobileMessengerActivity {

	// @Override
	// protected RosterFragment createRosterFragment(String string) {
	// return RosterAdsFragment.newInstance(string);
	// }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AdView adView = new AdView(this, AdSize.BANNER, "a14f7a9fd28f3ed");

		LinearLayout layout = (LinearLayout) findViewById(R.id.XlinearLayout);

		// Add the adView to it
		layout.addView(adView);

		// Initiate a generic request to load it with an ad
		AdRequest request = new AdRequest();
		request.addTestDevice(AdRequest.TEST_EMULATOR);

		// request.addTestDevice(testDevice)
		adView.loadAd(request);

	}

}
