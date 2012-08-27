package org.tigase.mobile.net;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.annotation.SuppressLint;
import android.os.Build;

@SuppressLint("NewApi")
public class SocketThread extends Thread {

	private class IOServiceComparator implements Comparator<IOService<?>> {

		/**
		 * Method description
		 * 
		 * 
		 * @param o1
		 * @param o2
		 * 
		 * @return
		 */
		@Override
		public int compare(IOService<?> o1, IOService<?> o2) {
			return o1.getUniqueId().compareTo(o2.getUniqueId());
		}
	}

	// @TODO(note = "ExecutionException is poorly implemented.")
	protected class ResultsListener extends Thread {

		private boolean shutdown = false;

		/**
		 * Constructs ...
		 * 
		 * 
		 * @param name
		 */
		public ResultsListener(String name) {
			super();
			setName(name);
		}

		// ~--- methods
		// ------------------------------------------------------------

		/**
		 * Method description
		 * 
		 */
		@Override
		public void run() {
			while (!shutdown) {
				try {
					IOService<?> service = completionService.take().get();

					if (service != null) {
						if (service.isConnected()) {
							if (log.isLoggable(Level.FINEST)) {
								log.log(Level.FINEST, "COMPLETED: {0}", service.getUniqueId());
							}

							addSocketService(service);
						} else {
							if (log.isLoggable(Level.FINEST)) {
								log.log(Level.FINEST, "REMOVED: {0}", service.getUniqueId());
							}
						} // end of else
					}
				} catch (ExecutionException e) {
					log.log(Level.WARNING, "Protocol execution exception.", e);

					// TODO: Do something with this
				} // end of catch
				catch (InterruptedException e) {
					log.log(Level.WARNING, "Protocol execution interrupted.", e);
				} // end of try-catch
				catch (Exception e) {
					log.log(Level.WARNING, "Protocol execution unknown exception.", e);
				} // end of catch
			} // end of for ()
		}

		public void shutdown() {
			shutdown = true;
			interrupt();
		}
	}

	/**
	 * Variable <code>completionService</code> keeps reference to server thread
	 * pool. There is only one thread pool used by all server modules. Each
	 * module requiring separate threads for tasks processing must have access
	 * to server thread pool.
	 */
	private static CompletionService<IOService<?>> completionService = null;
	private static int cpus = Runtime.getRuntime().availableProcessors();
	public static final int DEF_MAX_THREADS_PER_CPU = 2;
	private static ThreadPoolExecutor executor = null;
	private static final Logger log = Logger.getLogger(SocketThread.class.getCanonicalName());

	private static final int MAX_EMPTY_SELECTIONS = 10;

	private static SocketThread[] socketReadThread = null;

	private static SocketThread[] socketWriteThread = null;

	/**
	 * Method description
	 * 
	 * 
	 * @param s
	 */
	public static void addSocketService(IOService<?> s) {

		// Due to a delayed SelectionKey cancelling deregistering
		// nature this distribution doesn't work well, it leads to
		// dead-lock. Let's make sure the service is always processed
		// by the same thread thus the same Selector.
		// socketReadThread[incrementAndGet()].addSocketServicePriv(s);
		if (s.waitingToRead()) {
			socketReadThread[s.hashCode() % socketReadThread.length].addSocketServicePriv(s);
		}

		if (s.waitingToSend()) {
			socketWriteThread[s.hashCode() % socketWriteThread.length].addSocketServicePriv(s);
		}
	}

	private static boolean hasConcurrency() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param s
	 */
	public static void removeSocketService(IOService<Object> s) {
		Selector clientsSel = socketReadThread[s.hashCode() % socketReadThread.length].clientsSel;
		SelectionKey key = s.getSocketChannel().keyFor(clientsSel);

		if ((key != null) && (key.attachment() == s)) {
			key.cancel();
		} // end of if (key != null)

		clientsSel = socketWriteThread[s.hashCode() % socketWriteThread.length].clientsSel;
		key = s.getSocketChannel().keyFor(clientsSel);

		if ((key != null) && (key.attachment() == s)) {
			key.cancel();
		} // end of if (key != null)
	}

	public static void startTreads() {
		if (socketReadThread == null) {
			if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD) {
				java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
				java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
			}
			int nThreads = (cpus * DEF_MAX_THREADS_PER_CPU) / 2 + 1;

			executor = new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
					new LinkedBlockingQueue<Runnable>());
			completionService = new ExecutorCompletionService<IOService<?>>(executor);
			socketReadThread = new SocketThread[nThreads];
			socketWriteThread = new SocketThread[nThreads];

			for (int i = 0; i < socketReadThread.length; i++) {
				socketReadThread[i] = new SocketThread("socketReadThread-" + i);
				socketReadThread[i].reading = true;
				socketReadThread[i].setName("socketReadThread-" + i);
				socketReadThread[i].setDaemon(true);
				socketReadThread[i].start();
			}

			log.log(Level.WARNING, "{0} socketReadThreads started.", socketReadThread.length);

			for (int i = 0; i < socketWriteThread.length; i++) {
				socketWriteThread[i] = new SocketThread("socketWriteThread-" + i);
				socketWriteThread[i].writing = true;

				socketWriteThread[i].setName("socketWriteThread-" + i);
				socketWriteThread[i].setDaemon(true);
				socketWriteThread[i].start();
			}

			log.log(Level.WARNING, "{0} socketWriteThreads started.", socketWriteThread.length);
		} // end of if (acceptThread == null)
	}

	public static void stopThreads() {
		for (int i = 0; i < socketReadThread.length; i++) {
			socketReadThread[i].stopping = true;
		}
		socketReadThread = null;
		for (int i = 0; i < socketWriteThread.length; i++) {
			socketWriteThread[i].stopping = true;
		}
		executor.shutdown();
	}

	private Selector clientsSel;
	private int empty_selections = 0;

	// IOServices must be added to thread pool after they are removed from
	// the selector and the selector and key is cleared, otherwise we have
	// dead-lock somewhere down in the:
	// java.nio.channels.spi.AbstractSelectableChannel.removeKey(AbstractSelectableChannel.java:111)

	// FIX - Android 2.3.1 do not have implementation of ConcurrentSkipListSet
	private Set<IOService<?>> forCompletion = hasConcurrency() ? new ConcurrentSkipListSet<IOService<?>>(
			new IOServiceComparator()) : Collections.synchronizedSet(new TreeSet<IOService<?>>(new IOServiceComparator()));
	// new ConcurrentSkipListSet<IOService<?>>(new IOServiceComparator());

	private final String name;

	private boolean reading = false;

	private ResultsListener resultListener = null;
	private boolean stopping = false;

	// FIX - Android 2.3.1 do not have implementation of ConcurrentSkipListSet
	private Set<IOService<?>> waiting = hasConcurrency() ? new ConcurrentSkipListSet<IOService<?>>(new IOServiceComparator())
			: Collections.synchronizedSet(new TreeSet<IOService<?>>(new IOServiceComparator()));

	private boolean writing = false;

	public SocketThread(String name) {
		this.name = name;

		try {
			clientsSel = Selector.open();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Server I/O error, can't continue my work.", e);
			stopping = true;
		} // end of try-catch

		resultListener = new ResultsListener("ResultsListener-" + name);
		resultListener.setDaemon(true);
		resultListener.start();
	}

	// ~--- methods
	// --------------------------------------------------------------

	private void addAllWaiting() throws IOException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "waiting.size(): {0}", waiting.size());
		}

		IOService s = null;

		// boolean added = false;
		while ((s = pollFirst(waiting)) != null) {
			SocketChannel sc = s.getSocketChannel();

			try {
				if (sc.isConnected()) {
					if (reading) {
						sc.register(clientsSel, SelectionKey.OP_READ, s);

						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "ADDED OP_READ: {0}", s.getUniqueId());
						}
					}

					if (writing) {
						sc.register(clientsSel, SelectionKey.OP_WRITE, s);

						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "ADDED OP_WRITE: {0}", s.getUniqueId());
						}
					}

					// added = true;
				} else {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Socket not connected: {0}", s.getUniqueId());
					}

					try {
						if (log.isLoggable(Level.FINER)) {
							log.log(Level.FINER, "Forcing stopping the service: {0}", s.getUniqueId());
						}

						s.forceStop();
					} catch (Exception e) {
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "Exception while stopping service: " + s.getUniqueId(), e);
						}
					}
				}
			} catch (Exception e) {
				if (log.isLoggable(Level.FINER)) {
					log.log(Level.FINER, "Forcing stopping the service: " + s.getUniqueId(), e);
				}

				try {
					s.forceStop();
				} catch (Exception ez) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Exception while stopping service: " + s.getUniqueId(), ez);
					}
				}
			} // end of try-catch
		} // end of for ()

		// if (added) {
		// clientsSel.wakeup();
		// }
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param s
	 */
	@SuppressWarnings("unchecked")
	public void addSocketServicePriv(IOService<?> s) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Adding to waiting: {0}", s);
		}

		waiting.add(s);

		// Calling lazy wakeup to avoid multiple wakeup calls
		// when lots of new services are added....
		clientsSel.wakeup();

		// wakeupHelper.wakeup();
	}

	// FIX - Android below 2.3.1 do not have implementation for
	// ConcurrentSkipListSet
	// Simple implementation of missing needed method
	private IOService<?> pollFirst(Set<IOService<?>> set) {
		if (hasConcurrency()) {
			return ((ConcurrentSkipListSet<IOService<?>>) set).pollFirst();
		} else {
			if (set.isEmpty())
				return null;

			Iterator<IOService<?>> it = set.iterator();
			IOService<?> service = it.next();
			it.remove();
			return service;
		}
	}

	// ~--- inner classes
	// --------------------------------------------------------

	// Implementation of java.lang.Runnable
	private synchronized void recreateSelector() throws IOException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Recreating selector, opened channels: {0}", clientsSel.keys().size());
		}

		empty_selections = 0;

		// Handling a bug or not a bug described in the
		// last comment to this issue:
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4850373
		// and
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6403933
		// Recreating the selector and registering all channles with
		// the new selector
		// Selector tempSel = clientsSel;
		// clientsSel = Selector.open();
		boolean cancelled = false;

		// Sometimes this is just a broken connection which causes
		// selector spin... this is the cheaper solution....
		for (SelectionKey sk : clientsSel.keys()) {
			IOService<?> serv = (IOService<?>) sk.attachment();
			SocketChannel sc = serv.getSocketChannel();

			if ((sc == null) || !sc.isConnected()) {
				cancelled = true;
				sk.cancel();

				try {
					log.log(Level.INFO, "Forcing stopping the service: {0}", serv.getUniqueId());
					serv.forceStop();
				} catch (Exception e) {
				}
			}

			// waiting.offer(serv);
		}

		if (cancelled) {
			clientsSel.selectNow();
		} else {

			// Unfortunately must be something wrong with the selector
			// itself, now more expensive calculations...
			Selector tempSel = clientsSel;

			clientsSel = Selector.open();

			for (SelectionKey sk : tempSel.keys()) {
				IOService<?> serv = (IOService<?>) sk.attachment();

				sk.cancel();
				waiting.add(serv);
			}

			tempSel.close();
		}
	}

	/**
	 * Describe <code>run</code> method here.
	 * 
	 */
	@SuppressWarnings({ "unchecked" })
	@Override
	public void run() {
		while (!stopping) {
			try {
				clientsSel.select();

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Selector AWAKE: {0}", clientsSel);
				}

				Set<SelectionKey> selected = clientsSel.selectedKeys();
				int selectedKeys = selected.size();

				if ((selectedKeys == 0) && (waiting.size() == 0)) {
					if (log.isLoggable(Level.FINEST)) {
						log.finest("Selected keys = 0!!! a bug again?");
					}

					if ((++empty_selections) > MAX_EMPTY_SELECTIONS) {
						recreateSelector();
					}
				} else {
					empty_selections = 0;

					if (selectedKeys > 0) {

						// This is dirty but selectNow() causes concurrent
						// modification exception
						// and the selectNow() is needed because of a bug in JVM
						// mentioned below
						for (SelectionKey sk : selected) {

							// According to most guides we should use below code
							// removing SelectionKey from iterator, however a
							// little later
							// we do cancel() on this key so removing is somehow
							// redundant
							// and causes concurrency exception if a few calls
							// are performed
							// at the same time.
							// selected_keys.remove(sk);
							IOService s = (IOService) sk.attachment();

							try {
								if (log.isLoggable(Level.FINEST)) {
									StringBuilder sb = new StringBuilder("AWAKEN: " + s.getUniqueId());

									if (sk.isWritable()) {
										sb.append(", ready for WRITING");
									}

									if (sk.isReadable()) {
										sb.append(", ready for READING");
									}

									sb.append(", readyOps() = ").append(sk.readyOps());
									log.finest(sb.toString());
								}

								// Set<SelectionKey> selected_keys =
								// clientsSel.selectedKeys();
								// for (SelectionKey sk : selected_keys) {
								// Handling a bug or not a bug described in the
								// last comment to this issue:
								// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4850373
								// and
								// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6403933
								sk.cancel();
								forCompletion.add(s);

								// IOServices must be added to thread pool after
								// they are removed from
								// the selector and the selector and key is
								// cleared, otherwise we have
								// dead-lock somewhere down in the:
								// java.nio.channels.spi.AbstractSelectableChannel.
								// removeKey(AbstractSelectableChannel.java:111)
								// completionService.submit(s);
							} catch (CancelledKeyException e) {
								if (log.isLoggable(Level.FINEST)) {
									log.log(Level.FINEST, "CancelledKeyException, stopping the connection: {0}",
											s.getUniqueId());
								}

								try {
									s.forceStop();
								} catch (Exception ex2) {
									if (log.isLoggable(Level.WARNING)) {
										log.log(Level.WARNING, "got exception during forceStop: {0}", e);
									}
								}
							}
						}
					}

					// Clean-up cancelled keys...
					clientsSel.selectNow();
				}

				addAllWaiting();

				IOService serv = null;

				while ((serv = pollFirst(forCompletion)) != null) {
					completionService.submit(serv);
				}

				// clientsSel.selectNow();
			} catch (CancelledKeyException brokene) {

				// According to Java API that should not happen.
				// I think it happens only on the broken Java implementation
				// from Apple.
				log.log(Level.WARNING, "Ups, broken JDK, Apple? ", brokene);

				try {
					recreateSelector();
				} catch (Exception e) {
					log.log(Level.SEVERE, "Serious problem, can't recreate selector: ", e);

					// stopping = true;
				}
			} catch (IOException ioe) {

				// According to Java API that should not happen.
				// I think it happens only on the broken Java implementation
				// from Apple
				// and due to a bug:
				// http://bugs.sun.com/view_bug.do?bug_id=6693490
				log.log(Level.WARNING, "Problem with the network connection: ", ioe);

				try {
					recreateSelector();
				} catch (Exception e) {
					log.log(Level.SEVERE, "Serious problem, can't recreate selector: ", e);

					// stopping = true;
				}
			} catch (Exception exe) {
				log.log(Level.SEVERE, "Server I/O error: ", exe);

				try {
					recreateSelector();
				} catch (Exception e) {
					log.log(Level.SEVERE, "Serious problem, can't recreate selector: ", e);

					// stopping = true;
				}

				// stopping = true;
			}
		}
	}

	public void shutdown() {
		resultListener.shutdown();

		stopping = true;
		clientsSel.wakeup();
		interrupt();
	}
}
