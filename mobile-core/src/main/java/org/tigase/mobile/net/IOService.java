package org.tigase.mobile.net;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author andrzej
 */
public abstract class IOService<T> implements Callable {

	private static final Logger log = Logger.getLogger(IOService.class.getCanonicalName());

	private SocketChannel channel;
	private int emptyReadCount = 0;
	private final String id;
	private final ReentrantLock inProgress = new ReentrantLock();

	private final String local_address;

	private final String remote_address;
	private boolean stopping = false;

	public IOService(SocketChannel ch) {
		channel = ch;

		Socket sock = ch.socket();

		local_address = sock.getLocalAddress().getHostAddress();
		remote_address = sock.getInetAddress().getHostAddress();
		id = local_address + "_" + sock.getLocalPort() + "_" + remote_address + "_" + sock.getPort();
	}

	@Override
	public IOService<T> call() throws Exception {
		boolean readLock = true;
		if (stopping) {
			forceStop();
		} else {
			readLock = inProgress.tryLock();

			if (readLock) {
				try {

					processSocketData();
				} catch (IOException ex) {
					log.log(Level.WARNING, "processing exception", ex);
					forceStop();
					return null;
				} finally {
					inProgress.unlock();
				}
			}
		}
		return readLock ? this : null;
	}

	public void forceStop() throws IOException {
		stopping = true;
		if (channel.isConnected()) {
			log.finest("closing connection");
			channel.close();
		}
	}

	public SocketChannel getSocketChannel() {
		return channel;
	}

	public String getUniqueId() {
		return id;
	}

	public boolean isConnected() {
		return channel.isConnected();
	}

	protected abstract void processSocketData() throws IOException;

	public ByteBuffer read(ByteBuffer buf) throws IOException {
		int read = channel.read(buf);

		if (read == 0) {
			emptyReadCount++;
			if (emptyReadCount > 1000) {
				throw new IOException("Closing channel - too many empty reads!");
			}
		} else {
			emptyReadCount = 0;
		}

		if (read > 0) {
			buf.flip();
			return buf;
		}

		if (read == -1) {
			throw new EOFException("Channel has been closed.");
		}

		return null;
	}

	public boolean waitingToRead() {
		return true;
	}

	public boolean waitingToSend() {
		return false;
	}

	public int write(final ByteBuffer buff) throws IOException {
		int wrote = channel.write(buff);

		emptyReadCount = 0;

		if (wrote == -1) {
			throw new EOFException("Channel has been closed.");
		}

		return wrote;
	}
}
