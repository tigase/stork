package org.tigase.mobile.pubsub;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.tigase.mobile.db.GeolocationTableMetaData;
import org.tigase.mobile.db.providers.MessengerDatabaseHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.util.Log;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.XmppModule;
import tigase.jaxmpp.core.client.criteria.Criteria;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubModule;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubModule.PubSubEvent;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;

public class GeolocationModule implements XmppModule {

	private static final String TAG = "GeolocationModule";
	
	public static final String XMLNS = "http://jabber.org/protocol/geoloc";

	public static final String[] FEATURES = { XMLNS + "+notify" };

	private Listener<PubSubEvent> listener;

	private MessengerDatabaseHelper dbHelper;

	public GeolocationModule(Context context) {
		dbHelper = new MessengerDatabaseHelper(context);
	}

	@Override
	public Criteria getCriteria() {
		return null;
	}

	@Override
	public String[] getFeatures() {
		return FEATURES;
	}

	@Override
	public void process(Element element) throws XMPPException, XMLException, JaxmppException {
	}

	public void init(final JaxmppCore jaxmpp) {
		if (listener != null) return;
		
		listener = new Listener<PubSubEvent>() {

			@Override
			public void handleEvent(PubSubEvent be) throws JaxmppException {
				String node = be.getNodeName();
				Log.v(TAG, "got event for " + node);
				if (!XMLNS.equals(node))
					return;

				Element geoloc = be.getPayload();
				Log.v(TAG, "got payload = " + geoloc);
				if (geoloc == null || !"geoloc".equals(geoloc.getName()))
					return;

				List<Element> children = geoloc.getChildren();
				if (children == null || children.isEmpty() || (be.getMessage() != null && be.getMessage().getType() == StanzaType.error)) {
					removeLocalityForJid(be.getPubSubJID().getBareJid());
				}
				else {
					ContentValues locality = new ContentValues();
					locality.put(GeolocationTableMetaData.FIELD_LON, (Double) null);
					locality.put(GeolocationTableMetaData.FIELD_LAT, (Double) null);
					locality.put(GeolocationTableMetaData.FIELD_ALT, (Double) null);
					locality.put(GeolocationTableMetaData.FIELD_COUNTRY, (String) null);
					locality.put(GeolocationTableMetaData.FIELD_LOCALITY, (String) null);
					locality.put(GeolocationTableMetaData.FIELD_STREET, (String) null);
					for (Element el : children) {
						if ("lon".equals(el.getName())) {
							String val = el.getValue();
							if (val != null && !val.isEmpty()) {
								locality.put(GeolocationTableMetaData.FIELD_LON, Double.parseDouble(val));
							}
						}
						else if ("lat".equals(el.getName())) {
							String val = el.getValue();
							if (val != null && !val.isEmpty()) {
								locality.put(GeolocationTableMetaData.FIELD_LAT, Double.parseDouble(val));
							}
						}
						else if ("alt".equals(el.getName())) {
							String val = el.getValue();
							if (val != null && !val.isEmpty()) {
								locality.put(GeolocationTableMetaData.FIELD_ALT, Double.parseDouble(val));
							}
						}
						else if ("country".equals(el.getName())) {
							String val = el.getValue();
							if (val != null && !val.isEmpty()) {
								locality.put(GeolocationTableMetaData.FIELD_COUNTRY, val);
							}
						}
						else if ("locality".equals(el.getName())) {
							String val = el.getValue();
							if (val != null && !val.isEmpty()) {
								locality.put(GeolocationTableMetaData.FIELD_LOCALITY, val);
							}
						}
						else if ("street".equals(el.getName())) {
							String val = el.getValue();
							if (val != null && !val.isEmpty()) {
								locality.put(GeolocationTableMetaData.FIELD_STREET, val);
							}
						}
					}
					setLocalityForJid(be.getPubSubJID().getBareJid(), locality);
				}
			}

		};
		jaxmpp.addListener(PubSubModule.NotificationReceived, listener);
	}
	
	public void deinit(JaxmppCore jaxmpp) {
		if (listener == null) return;
		jaxmpp.removeListener(PubSubModule.NotificationReceived, listener);
		listener = null;
	}

	private static final String WHERE_BY_JID = GeolocationTableMetaData.FIELD_JID + " = ? ";
	
	public void setLocalityForJid(BareJID jid, ContentValues locality) {
		final SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.beginTransaction();
		locality.put(GeolocationTableMetaData.FIELD_JID, jid.toString());
		try {
			int updated = db.update(GeolocationTableMetaData.TABLE_NAME, locality, WHERE_BY_JID, new String[] { jid.toString() });
			if (updated == 0) {
				db.insert(GeolocationTableMetaData.TABLE_NAME, null, locality);
			}
			db.setTransactionSuccessful();
		}
		finally {
			db.endTransaction();
		}
	}
	
	public void removeLocalityForJid(BareJID jid) {
		final SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			db.delete(GeolocationTableMetaData.TABLE_NAME, WHERE_BY_JID, new String[] { jid.toString() });
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}
	
	public ContentValues getLocationForJid(BareJID jid) {
		final SQLiteDatabase db = dbHelper.getWritableDatabase();
		ContentValues location = null;
		final Cursor c = db.query(GeolocationTableMetaData.TABLE_NAME, new String[] { GeolocationTableMetaData.FIELD_LON, GeolocationTableMetaData.FIELD_LAT, GeolocationTableMetaData.FIELD_ALT,
				GeolocationTableMetaData.FIELD_COUNTRY, GeolocationTableMetaData.FIELD_LOCALITY, GeolocationTableMetaData.FIELD_STREET}, WHERE_BY_JID, new String[] { jid.toString() }, null, null, null); 
		try {
			while(c.moveToNext()) {
				location = new ContentValues();
				
				int col = c.getColumnIndex(GeolocationTableMetaData.FIELD_LON); 
				if (!c.isNull(col)) {
					location.put(GeolocationTableMetaData.FIELD_LON, c.getDouble(col));
				}
				col = c.getColumnIndex(GeolocationTableMetaData.FIELD_LAT); 
				if (!c.isNull(col)) {
					location.put(GeolocationTableMetaData.FIELD_LAT, c.getDouble(col));
				}
				col = c.getColumnIndex(GeolocationTableMetaData.FIELD_ALT); 
				if (!c.isNull(col)) {
					location.put(GeolocationTableMetaData.FIELD_ALT, c.getDouble(col));
				}
				col = c.getColumnIndex(GeolocationTableMetaData.FIELD_COUNTRY);
				location.put(GeolocationTableMetaData.FIELD_COUNTRY, c.getString(col));
				col = c.getColumnIndex(GeolocationTableMetaData.FIELD_LOCALITY);
				location.put(GeolocationTableMetaData.FIELD_LOCALITY, c.getString(col));
				col = c.getColumnIndex(GeolocationTableMetaData.FIELD_STREET);
				location.put(GeolocationTableMetaData.FIELD_STREET, c.getString(col));
			}
		}		
		finally {
			c.close();
		}
		return location;
	}
}
