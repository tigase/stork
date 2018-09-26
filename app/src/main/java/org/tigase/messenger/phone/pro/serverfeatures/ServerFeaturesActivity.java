package org.tigase.messenger.phone.pro.serverfeatures;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import org.tigase.messenger.phone.pro.R;

public class ServerFeaturesActivity
		extends AppCompatActivity {

	public static final String ACCOUNT_JID = "account_name";
	public static String TAG = "ServerFeaturesActivity";
	private String accountJID;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.accountJID = getIntent().getStringExtra(ACCOUNT_JID);

		setContentView(R.layout.activity_serverfeatures);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

}
