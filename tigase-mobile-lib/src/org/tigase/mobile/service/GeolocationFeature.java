package org.tigase.mobile.service;

import java.io.IOException;
import java.util.List;

import org.tigase.mobile.Preferences;
import org.tigase.mobile.pubsub.GeolocationModule;

import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.DefaultElement;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence.Show;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class GeolocationFeature {

	public static final String GEOLOCATION_LISTEN_ENABLED = "geolocation#listen_enabled";

	public static final String GEOLOCATION_PUBLISH_ENABLED = "geolocation#publish_enabled";

	public static final String GEOLOCATION_PUBLISH_PRECISION = "geolocation#publish_precision";

	public static void sendCurrentLocation(JaxmppCore jaxmpp, Context context) {
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		try {
			GeolocationFeature.updateLocation(jaxmpp, location, context);
		} catch (JaxmppException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method used to update geolocation settings on connected or disconnected
	 * instance of JaxmppCore with proper handling of this situation
	 * 
	 * @param account
	 * @param jaxmpp
	 * @param context
	 */
	public static void updateGeolocationSettings(final Account account, final JaxmppCore jaxmpp, final Context context) {
		new Thread() {
			@Override
			public void run() {
				AccountManager accountManager = AccountManager.get(context);

				// update of geolocation listen setting
				Boolean listenOld = jaxmpp.getSessionObject().getProperty(GEOLOCATION_LISTEN_ENABLED);
				if (listenOld == null)
					listenOld = false;
				String valueStr = accountManager.getUserData(account, GEOLOCATION_LISTEN_ENABLED);
				boolean listenNew = (valueStr != null && Boolean.parseBoolean(valueStr));

				jaxmpp.getSessionObject().setUserProperty(GEOLOCATION_LISTEN_ENABLED, listenNew);
				if (listenNew != listenOld) {
					if (listenNew) {
						GeolocationModule geolocationModule = new GeolocationModule(context);
						jaxmpp.getModulesManager().register(geolocationModule);
						geolocationModule.init(jaxmpp);
					} else {
						GeolocationModule module = jaxmpp.getModule(GeolocationModule.class);
						module.deinit(jaxmpp);
						jaxmpp.getModulesManager().unregister(module);
					}
					jaxmpp.getSessionObject().setProperty(CapabilitiesModule.VERIFICATION_STRING_KEY, null);
					if (jaxmpp.isConnected()) {
						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
						PresenceModule presenceModule = jaxmpp.getModule(PresenceModule.class);
						try {
							if (JaxmppService.focused) {
								int pr = prefs.getInt(Preferences.DEFAULT_PRIORITY_KEY, 5);
								presenceModule.setPresence(JaxmppService.userStatusShow, JaxmppService.userStatusMessage, pr);
							} else {
								int pr = prefs.getInt(Preferences.AWAY_PRIORITY_KEY, 0);
								presenceModule.setPresence(Show.away, "Auto away", pr);
							}
						} catch (JaxmppException ex) {
							ex.printStackTrace();
						}
					}
				}

				// update of geolocation publish setting
				Boolean publishOld = jaxmpp.getSessionObject().getProperty(GEOLOCATION_PUBLISH_ENABLED);
				if (publishOld == null) {
					publishOld = false;
				}
				valueStr = accountManager.getUserData(account, GEOLOCATION_PUBLISH_ENABLED);
				boolean publishNew = (valueStr != null && Boolean.parseBoolean(valueStr));

				if (jaxmpp.isConnected() && publishNew != publishOld) {
					if (publishNew) {
						jaxmpp.getSessionObject().setUserProperty(GEOLOCATION_PUBLISH_ENABLED, publishNew);
						sendCurrentLocation(jaxmpp, context);
					} else {
						try {
							updateLocation(jaxmpp, null, context);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
						jaxmpp.getSessionObject().setUserProperty(GEOLOCATION_PUBLISH_ENABLED, publishNew);
					}
				}
				jaxmpp.getSessionObject().setUserProperty(GEOLOCATION_PUBLISH_ENABLED, publishNew);

				// update of geolocation publish precision
				Integer precisionOld = jaxmpp.getSessionObject().getProperty(GEOLOCATION_PUBLISH_PRECISION);
				if (precisionOld == null)
					precisionOld = 0;
				valueStr = accountManager.getUserData(account, GEOLOCATION_PUBLISH_PRECISION);
				int precisionNew = (valueStr != null) ? Integer.parseInt(valueStr) : 0;

				jaxmpp.getSessionObject().setUserProperty(GEOLOCATION_PUBLISH_PRECISION, precisionNew);
				if (jaxmpp.isConnected() && precisionOld != precisionNew) {
					sendCurrentLocation(jaxmpp, context);
				}
			}
		}.start();
	}

	public static void updateLocation(final JaxmppCore jaxmpp, Location location, Context context) throws JaxmppException {
		if (!jaxmpp.isConnected())
			return;
		Boolean enabled = (Boolean) jaxmpp.getSessionObject().getProperty(GEOLOCATION_PUBLISH_ENABLED);
		if (enabled == null || !enabled.booleanValue())
			return;

		int precision = jaxmpp.getSessionObject().getProperty(GEOLOCATION_PUBLISH_PRECISION);

		final IQ iq = IQ.create();
		iq.setType(StanzaType.set);
		iq.setId("pubsub1");
		Element pubsub = new DefaultElement("pubsub");
		pubsub.setXMLNS("http://jabber.org/protocol/pubsub");
		iq.addChild(pubsub);
		Element publish = new DefaultElement("publish");
		publish.setAttribute("node", "http://jabber.org/protocol/geoloc");
		pubsub.addChild(publish);
		Element item = new DefaultElement("item");
		publish.addChild(item);
		Element geoloc = new DefaultElement("geoloc");
		geoloc.setXMLNS("http://jabber.org/protocol/geoloc");
		item.addChild(geoloc);
		if (location != null) {
			if (precision > 2) {
				Element lat = new DefaultElement("lat");
				lat.setValue(String.valueOf(location.getLatitude()));
				geoloc.addChild(lat);
				Element lon = new DefaultElement("lon");
				lon.setValue(String.valueOf(location.getLongitude()));
				geoloc.addChild(lon);
				Element alt = new DefaultElement("alt");
				alt.setValue(String.valueOf(location.getAltitude()));
				geoloc.addChild(alt);
			}

			if (context != null) {
				try {
					Geocoder geocoder = new Geocoder(context);
					List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
					if (!addresses.isEmpty()) {
						Address address = addresses.get(0);
						// precision == 0
						if (address.getCountryName() != null) {
							Element country = new DefaultElement("country");
							country.setValue(address.getCountryName());
							geoloc.addChild(country);
						}
						// precision == 1
						if (precision >= 1) {
							if (address.getLocality() != null) {
								Element locality = new DefaultElement("locality");
								locality.setValue(address.getLocality());
								geoloc.addChild(locality);
							}
							// precision == 1
							if (address.getPostalCode() != null) {
								Element postalcode = new DefaultElement("postalcode");
								postalcode.setValue(address.getPostalCode());
								geoloc.addChild(postalcode);
							}
						}
						// precision == 2
						if (precision >= 2) {
							if (address.getThoroughfare() != null) {
								Element street = new DefaultElement("street");
								street.setValue(address.getThoroughfare());
								geoloc.addChild(street);
							}
						}
					} else if (precision < 3) {
						// nothing to send - exiting
						return;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if (precision < 3) {
				// nothing to send - exiting
				return;
			}
		}

		new Thread() {
			@Override
			public void run() {
				try {
					jaxmpp.send(iq);
				} catch (XMLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JaxmppException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();
	}

	private final JaxmppService jaxmppService;

	private Criteria locationCriteria = new Criteria();

	private long locationInterval = 5 * 60 * 1000;

	private final LocationListener locationListener;

	public GeolocationFeature(JaxmppService service) {
		jaxmppService = service;

		locationListener = new LocationListener() {

			@Override
			public void onLocationChanged(Location location) {
				updateLocation(location);
			}

			@Override
			public void onProviderDisabled(String provider) {
			}

			@Override
			public void onProviderEnabled(String provider) {
			}

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
			}

		};
	}

	public void registerLocationListener() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			registerLocationListenerGingerbread();
		} else {
			LocationManager locationManager = (LocationManager) jaxmppService.getSystemService(Context.LOCATION_SERVICE);
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, locationInterval, 100, locationListener);
		}
	}

	@TargetApi(9)
	public void registerLocationListenerGingerbread() {
		LocationManager locationManager = (LocationManager) jaxmppService.getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(locationInterval, 100, locationCriteria, locationListener, null);
	}

	public void sendCurrentLocation() {
		LocationManager locationManager = (LocationManager) jaxmppService.getSystemService(Context.LOCATION_SERVICE);
		Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		for (JaxmppCore jaxmpp : jaxmppService.getMulti().get()) {
			try {
				updateLocation(jaxmpp, location, jaxmppService);
			} catch (JaxmppException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void unregisterLocationListener() {
		LocationManager locationManager = (LocationManager) jaxmppService.getSystemService(Context.LOCATION_SERVICE);
		locationManager.removeUpdates(locationListener);
	}

	public void updateLocation(Location location) {
		for (JaxmppCore jaxmpp : jaxmppService.getMulti().get()) {
			try {
				updateLocation(jaxmpp, location, jaxmppService);
			} catch (JaxmppException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
