package org.tigase.mobile.filetransfer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.tigase.mobile.filetransfer.FileTransferModule.ActivateCallback;
import org.tigase.mobile.filetransfer.FileTransferModule.Host;
import org.tigase.mobile.net.SocketThread;

import android.net.Uri;
import android.util.Log;

import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.j2se.Jaxmpp;

public class FileTransfer {

	private static final String TAG = "FileTransfer";
	
	public final Jaxmpp jaxmpp;
	public final FileTransferModule ftModule;
	public final JID jid;
	public final JID buddyJid;
	public final String buddyName;
	public String sid;
	private JID proxyJid;
	private long size = 0;	
//	private final Uri uri;
	public final String filename;
	public final boolean outgoing;
	
	private Socks5IOService service;
//	private FileChannel inputChannel = null;
	private InputStream inputStream = null;
	private ByteBuffer outgoingBuffer = null;
	
	private long read = 0;
	private long wrote = 0;	
	
	private State state = null;
	public String errorMessage = null;
	
	public static enum State {
		negotiating,
		connecting,
		active,
		finished,
		error
	}
	
	public FileTransfer(Jaxmpp jaxmpp, JID buddyJid, String buddyName, String filename, InputStream is, long size) {
		this.jaxmpp = jaxmpp;
		this.jid = jaxmpp.getSessionObject().getProperty(ResourceBinderModule.BINDED_RESOURCE_JID);
		this.ftModule = jaxmpp.getModulesManager().getModule(FileTransferModule.class);
		this.buddyJid = buddyJid;
		this.buddyName = buddyName;
		//this.uri = uri;
		this.filename = filename;
		this.inputStream = is;
		this.outgoing = true;
		this.size = size;
		
		this.state = State.negotiating;
		this.updateProgress();
	}

	public void setSid(String sid) {
		this.sid = sid;
	}
	
	public void setProxyJid(JID jid) {
		this.proxyJid = jid;
	}
	
	public void connectToProxy(Host streamhost) throws IOException {
		state = State.connecting;
        SocketAddress address = new InetSocketAddress(streamhost.getAddress(), streamhost.getPort());
        final SocketChannel channel = SocketChannel.open(address);
        this.service = new Socks5IOService(channel, this);
        SocketThread.addSocketService(service);
	}
	
	public void connectedToProxy() {
		final FileTransferModule ftModule = jaxmpp.getModulesManager().getModule(FileTransferModule.class);
        try {
			ftModule.requestActivate(proxyJid, sid, buddyJid.toString(), new ActivateCallback() {

			    public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
			             Log.e(TAG, "activation for " + buddyJid.toString() + " resulted in error = " + error.getElementName());
			             stop();
			    }

			    public void onSuccess(Stanza responseStanza) throws JaxmppException {
			    		Log.v(TAG, "activation for " + buddyJid.toString() + " succeeded");
			    		state = State.active;
			    		updateProgress();
			    		try {
//			    			inputChannel = new FileInputStream(uri.getPath()).getChannel();
			    			outgoingBuffer = readData();
			    			service.sendData(outgoingBuffer);
			    		}
			    		catch (Exception ex) {
			    			stop();
			    		}
			    }

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
	
	public void receivedData(ByteBuffer buf) {
		// TODO Auto-generated method stub
		read += buf.remaining();
		updateProgress();		
	}

	public String getAuthString() {
		try {
        	String data = sid+jid.toString()+buddyJid.toString();
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

	public State getState() {
		return state;
	}
	
	public void sentData(int wrote) {
		this.wrote += wrote;
		updateProgress();		
		if (!outgoingBuffer.hasRemaining()) {
			try {
				readData();
			}
			catch (IOException ex) {
				stop();
			}
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
	
	private ByteBuffer readData() throws IOException {
		if (outgoingBuffer == null) {
			outgoingBuffer = ByteBuffer.allocate(64 * 1024);
		}

		outgoingBuffer.clear();
			
		
		// TODO - Read from file here!!		
		//inputChannel.read(outgoingBuffer);
		byte[] data = new byte[64 *  1024];
		int read = inputStream.read(data);
		if (read != -1) {
			outgoingBuffer.put(data, 0, read);
		}
		else {
			inputStream.close();
		}
			
		outgoingBuffer.flip();
		
		return outgoingBuffer;
	}

	private void updateProgress() {
		ftModule.fileTransferProgressUpdated(this);
	}
	
	public int getProgress() {
		if (outgoing) {
			return (int) ((wrote * 100) / size);
		}
		else {
			return (int) ((read * 100) / size);
		}
	}
	
	@Override
	public String toString() {
		if (outgoing) { 
			return jid.toString() + " -> " + buddyJid.toString() + " file = " + filename;
		}
		else {
			return buddyJid.toString() + " -> " + jid.toString() + " file = " + filename;
		}
	}
	
}
