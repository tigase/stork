/*
 * Stork
 * Copyright (C) 2019 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */

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