package org.tigase.messenger.phone.pro.account;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by bmalkow on 28.06.2017.
 */
public class PrioritiesEntityTest {

	@Test
	public void instance() throws Exception {
		PrioritiesEntity e = new PrioritiesEntity();
		Assert.assertEquals("5,4,3,2,1", e.toString());

		e.setChat(1001);
		e.setOnline(1000);
		e.setAway(999);
		e.setXa(998);
		e.setDnd(997);

		Assert.assertEquals("1001,1000,999,998,997", e.toString());
		Assert.assertEquals("1001,1000,999,998,997", PrioritiesEntity.instance(e.toString()).toString());
	}

}