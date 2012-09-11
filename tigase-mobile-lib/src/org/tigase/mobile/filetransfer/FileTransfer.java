package org.tigase.mobile.filetransfer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.tigase.mobile.filetransfer.FileTransferModule.ActivateCallback;
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
	public final File destination;
	public String errorMessage = null;
	// private final Uri uri;
	public final String filename;
	public final FileTransferModule ftModule;
	private InputStream inputStream = null;
	public final Jaxmpp jaxmpp;
	public final JID jid;
	public String mimetype;
	public final boolean outgoing;
	private ByteBuffer outgoingBuffer = null;

	private OutputStream outputStream = null;
	private JID proxyJid;
	private long read = 0;

	private String responseId = null;
	private Socks5IOService service;
	public String sid;
	private long size = 0;

	private State state = null;
	private Streamhost streamhost = null;

	private long wrote = 0;

	public FileTransfer(Jaxmpp jaxmpp, JID buddyJid, String buddyName, String filename, InputStream is, long size) {
		this.jaxmpp = jaxmpp;
		this.jid = jaxmpp.getSessionObject().getProperty(ResourceBinderModule.BINDED_RESOURCE_JID);
		this.ftModule = jaxmpp.getModule(FileTransferModule.class);
		this.buddyJid = buddyJid;
		this.buddyName = buddyName;
		this.filename = filename;
		this.inputStream = is;
		this.outgoing = true;
		this.destination = null;
		this.size = size;

		this.state = State.negotiating;
		this.updateProgress();
	}

	public FileTransfer(Jaxmpp jaxmpp, JID buddyJid, String buddyName, String filename, long size, File destination) {
		this.jaxmpp = jaxmpp;
		this.jid = jaxmpp.getSessionObject().getProperty(ResourceBinderModule.BINDED_RESOURCE_JID);
		this.ftModule = jaxmpp.getModule(FileTransferModule.class);
		this.buddyJid = buddyJid;
		this.buddyName = buddyName;
		this.filename = filename;
		this.outgoing = false;
		this.size = size;
		this.destination = destination;

		this.state = State.negotiating;
		this.updateProgress();
	}

	public void connectedToProxy() {
		final FileTransferModule ftModule = jaxmpp.getModule(FileTransferModule.class);
		try {
			if (outgoing) {
				outgoingConnectedToProxy(ftModule);
			} else {
				Log.v(TAG, "connection to host " + streamhost.getJid() + " for " + buddyJid.toString() + " succeeded");
				state = State.active;
				updateProgress();
				ftModule.sendStreamhostUsed(buddyJid, responseId, sid, streamhost);
			}
		} catch (XMLException e) {
			e.printStackTrace();
			stop();
		} catch (JaxmppException e) {
			e.printStackTrace();
			stop();
		}
	}

	public void connectToProxy(Streamhost streamhost, String responseId) throws IOException {
		state = State.connecting;
		this.responseId = responseId;
		this.streamhost = streamhost;
		SocketAddress address = new InetSocketAddress(streamhost.getAddress(), streamhost.getPort());
		final SocketChannel channel = SocketChannel.open(address);
		this.service = new Socks5IOService(channel, this);
		SocketThread.addSocketService(service);
	}

	public String getAuthString() {
		try {
			String data = outgoing ? sid + jid.toString() + buddyJid.toString() : sid + buddyJid.toString() + jid.toString();
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

	public void outgoingConnected() throws JaxmppException {
		try {
			Log.v(TAG, "activation for " + buddyJid.toString() + " succeeded");
			state = State.active;
			updateProgress();
			outgoingBuffer = readData();
			service.sendData(outgoingBuffer);
		} catch (Exception ex) {
			Log.e(TAG, "stopping channel due to exception", ex);
			transferError("not able to send file");
			stop();
		}
	}

	private void outgoingConnectedToProxy(final FileTransferModule ftModule) throws JaxmppException {
		ftModule.requestActivate(proxyJid, sid, buddyJid.toString(), new ActivateCallback() {

			@Override
			public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
				Log.e(TAG, "activation for " + buddyJid.toString() + " resulted in error = " + error.getElementName());
				stop();
			}

			@Override
			public void onSuccess(Stanza responseStanza) throws JaxmppException {
				outgoingConnected();
			}

			@Override
			public void onTimeout() throws JaxmppException {
				Log.e(TAG, "stream activation for " + buddyJid.toString() + " timed out");
				stop();
			}
		});
	}

	private ByteBuffer readData() throws IOException {
		if (outgoingBuffer == null) {
			outgoingBuffer = ByteBuffer.allocate(64 * 1024);
		}

		outgoingBuffer.clear();

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
		read += buf.remaining();

		if (outputStream != null) {
			if (read == size) {
				this.state = State.finished;
				updateProgress();
			}
			byte[] data = new byte[buf.remaining()];
			buf.get(data);
			try {
				outputStream.write(data, 0, data.length);
			} catch (IOException ex) {
				stop();
			}
		}

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

	public void setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
	}

	public void setProxyJid(JID jid) {
		this.proxyJid = jid;
	}

	public void setService(Socks5IOService serv) {
		this.service = serv;
	}

	public void setSid(String sid) {
		this.sid = sid;
	}

	public void stop() {
		Log.v(TAG, "stopped");
		try {
			if (outgoing) {
				if (inputStream != null) {
					inputStream.close();
				}
			} else {
				if (outputStream != null) {
					outputStream.close();
				}
			}
		} catch (Exception ex) {
			Log.v(TAG, "exception while closing local io stream", ex);
		}

		if (service != null) {
			try {
				service.forceStop();
			} catch (IOException e) {
				Log.v(TAG, "WTF?", e);
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
		if (state != State.finished) {
			state = State.finished;
		}
		updateProgress();
	}

	private void updateProgress() {
		ftModule.fileTransferProgressUpdated(this);
	}

}
