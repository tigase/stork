package org.tigase.messenger.phone.pro.conversations;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.selectionview.MultiSelectFragment;
import org.tigase.messenger.phone.pro.service.MessageSender;

public class ViewHolderImg
		extends ViewHolderMsg {

	private final Context context;
	ImageView mImage;

	public ViewHolderImg(Context contex, View itemView, MultiSelectFragment fragment) {
		super(itemView, fragment);
		this.context = contex;
		mImage = (ImageView) itemView.findViewById(R.id.image);
	}

	@Override
	public void bind(Context context, Cursor cursor) {
		super.bind(context, cursor);
		final String contentUri = cursor.getString(
				cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_INTERNAL_CONTENT_URI));
		setImageContent(contentUri);

		final String body = cursor.getString(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_BODY));
		final String data = cursor.getString(cursor.getColumnIndex(DatabaseContract.ChatHistory.FIELD_DATA));
		if (body != null && data != null && data.equalsIgnoreCase("<url>" + body + "</url>")) {
			mContentView.setVisibility(View.GONE);
		}

	}

	protected void setImageContent(final String datUri) {
		if (mImage != null) {
			mImage.setOnClickListener(v -> context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(datUri))));
			mImage.post(() -> {
				if (datUri == null) {
					mImage.setImageResource(R.drawable.image_placeholder);
					return;
				}
				try {
					Bitmap bmp = MessageSender.getBitmapFromUri(context, Uri.parse(datUri));
					mImage.setImageBitmap(bmp);

				} catch (Throwable e) {
					e.printStackTrace();
					mImage.setImageResource(R.drawable.image_placeholder);
				}
			});
		}
	}

}
