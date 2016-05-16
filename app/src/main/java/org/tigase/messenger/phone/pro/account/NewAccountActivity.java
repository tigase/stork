package org.tigase.messenger.phone.pro.account;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import org.tigase.messenger.phone.pro.R;

public class NewAccountActivity extends AppCompatActivity {

	public static final int LOGIN_REQUEST = 1;

	@Bind(R.id.textView3)
	TextView tv;

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i("NewAccountActivity", "onActivityResult(" + requestCode + ", " + resultCode + ", " + data + ")");
		// Check which request we're responding to
		if (requestCode == LOGIN_REQUEST) {
			finish();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_account);
		ButterKnife.bind(this);

		try {
			PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			String versionName = pinfo.versionName;
			tv.setText(getString(R.string.about_version_name, versionName));
		} catch (Exception e) {
		}
	}

	@OnClick(value = R.id.createNewAccountButton)
	void onCreateNewAccountButtonClick() {

	}

	@OnClick(value = R.id.useExistingAccountButton)
	void onUseExistingAccountButtonClick() {
		Intent intent = new Intent(this, LoginActivity.class);
		startActivityForResult(intent, LOGIN_REQUEST);
	}

}
