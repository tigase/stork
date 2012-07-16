package org.tigase.mobile.service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class StanzaExecutor extends ThreadPoolExecutor {

	private static final int CORE_POOL_SIZE = 5;

	private static final int KEEP_ALIVE = 1;

	private static final int MAXIMUM_POOL_SIZE = 128;

	private static final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(100);

	public StanzaExecutor() {
		super(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, workQueue);
	}

}
