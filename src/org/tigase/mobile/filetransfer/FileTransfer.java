package org.tigase.mobile.filetransfer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.tigase.mobile.filetransfer.FileTransferModule.ActivateCallback;
import org.tigase.mobile.filetransfer.FileTransferModule.Host;
import org.tigase.mobile.net.SocketThread;

import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.util.Log;

public class FileTransfer {

	public static enum State {
		active,
		connecting,
		error,
		finished,
		negotiating
	}

	private static final String TAG = "FileTransfer";
	public final JID buddyJid;
	public final String buddyName;
	public String errorMessage = null;
	// private final Uri uri;
	public final String filename;
	public final FileTransferModule ftModule;
	// private FileChannel inputChannel = null;
	private InputStream inputStream = null;
	public final Jaxmpp jaxmpp;
	public final JID jid;
	public final boolean outgoing;

	private ByteBuffer outgoingBuffer = null;
	private JID proxyJid;
	private long read = 0;

	private Socks5IOService service;
	public String sid;

	private long size = 0;
	private State state = null;

	private long wrote = 0;

	public FileTransfer(Jaxmpp jaxmpp, JID buddyJid, String buddyName, String filename, InputStream is, long size) {
		this.jaxmpp = jaxmpp;
		this.jid = jaxmpp.getSessionObject().getProperty(ResourceBinderModule.BINDED_RESOURCE_JID);
		this.ftModule = jaxmpp.getModulesManager().getModule(FileTransferModule.class);
		this.buddyJid = buddyJid;
		this.buddyName = buddyName;
		// this.uri = uri;
		this.filename = filename;
		this.inputStream = is;
		this.outgoing = true;
		this.size = size;

		this.state = State.negotiating;
		this.updateProgress();
	}

	public void connectedToProxy() {
		final FileTransferModule ftModule = jaxmpp.getModulesManager().getModule(FileTransferModule.class);
		try {
			ftModule.requestActivate(proxyJid, sid, buddyJid.toString(), new ActivateCallback() {

				@Override
				public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
					Log.e(TAG, "activation for " + buddyJid.toString() + " resulted in error = " + error.getElementName());
					stop();
				}

				@Override
				public void onSuccess(Stanza responseStanza) throws JaxmppException {
					Log.v(TAG, "activation for " + buddyJid.toString() + " succeeded");
					state = State.active;
					updateProgress();
					try {
						// inputChannel = new
						// FileInputStream(uri.getPath()).getChannel();
						outgoingBuffer = readData();
						service.sendData(outgoingBuffer);
					} catch (Exception ex) {
						stop();
					}
				}

				@Override
				public void onTimeout() throws JaxmppException {
					Log.e(TAG, "stream activation for " + buddyJid.toString() + " timed out");
					stop();
				}
			});
		} catch (XMLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			stop();
		} catch (JaxmppException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			stop();
		}
	}

	public void connectToProxy(Host streamhost) throws IOException {
		state = State.connecting;
		SocketAddress address = new InetSocketAddress(streamhost.getAddress(), streamhost.getPort());
		final SocketChannel channel = SocketChannel.open(address);
		this.service = new Socks5IOService(channel, this);
		SocketThread.addSocketService(service);
	}

	public String getAuthString() {
		try {
			String data = sid + jid.toString() + buddyJid.toString();
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(data.getBytes());
			byte[] buff = md.digest();
			StringBuilder enc = new StringBuilder();
			for (byte b : buff) {
				char ch = Character.forDigit((b >> 4) & 0xF, 16);
				enc.append(ch);
				ch = Character.forDigit(b & 0xF, 16);
				enc.append(ch);
			} // end of for (b : digest)
			return enc.toString();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			return "";
		}
	}

	public int getProgress() {
		if (outgoing) {
			return (int) ((wrote * 100) / size);
		} else {
			return (int) ((read * 100) / size);
		}
	}

	public State getState() {
		return state;
	}

	private ByteBuffer readData() throws IOException {
		if (outgoingBuffer == null) {
			outgoingBuffer = ByteBuffer.allocate(64 * 1024);
		}

		outgoingBuffer.clear();

		// TODO - Read from file here!!
		// inputChannel.read(outgoingBuffer);
		byte[] data = new byte[64 * 1024];
		int read = inputStream.read(data);
		if (read != -1) {
			outgoingBuffer.put(data, 0, read);
		} else {
			inputStream.close();
		}

		outgoingBuffer.flip();

		return outgoingBuffer;
	}

	public void receivedData(ByteBuffer buf) {
		// TODO Auto-generated method stub
		read += buf.remaining();
		updateProgress();
	}

	public void sentData(int wrote) {
		this.wrote += wrote;
		updateProgress();
		if (!outgoingBuffer.hasRemaining()) {
			try {
				readData();
			} catch (IOException ex) {
				stop();
			}
		}
	}

	public void setProxyJid(JID jid) {
		this.proxyJid = jid;
	}

	public void setSid(String sid) {
		this.sid = sid;
	}

	public void stop() {
		Log.v(TAG, "stopped");
		if (service != null) {
			try {
				service.forceStop();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		updateProgress();
	}

	@Override
	public String toString() {
		if (outgoing) {
			return jid.toString() + " -> " + buddyJid.toString() + " file = " + filename;
		} else {
			return buddyJid.toString() + " -> " + jid.toString() + " file = " + filename;
		}
	}

	public void transferError(String errorMessage) {
		if (state != State.error && state != State.finished) {
			this.state = State.error;
			this.errorMessage = errorMessage;
			updateProgress();
		}
	}

	public void transferFinished() {
		// TODO Auto-generated method stub
		if (state != State.finished) {
			state = State.finished;
		}
		updateProgress();
	}

	private void updateProgress() {
		ftModule.fileTransferProgressUpdated(this);
	}

}
