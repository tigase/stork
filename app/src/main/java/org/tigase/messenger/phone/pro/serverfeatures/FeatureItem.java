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

package org.tigase.messenger.phone.pro.serverfeatures;

import java.util.Objects;

public class FeatureItem {

	private final String xmlns;
	private String description;
	private String name;
	private String xep;

	FeatureItem(String xmlns) {
		this.description = description;
		this.name = name;
		this.xep = xep;
		this.xmlns = xmlns;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof FeatureItem)) {
			return false;
		}
		FeatureItem that = (FeatureItem) o;
		return xmlns.equals(that.xmlns) && Objects.equals(xep, that.xep);
	}

	@Override
	public int hashCode() {
		return Objects.hash(xmlns, xep);
	}

	public String getDescription() {
		return description;
	}

	void setDescription(String description) {
		this.description = description == null ? null : description.trim();
	}

	public String getName() {
		return name;
	}

	void setName(String name) {
		this.name = name == null ? null : name.trim();
	}

	public String getXep() {
		return xep;
	}

	void setXep(String xep) {
		this.xep = xep == null ? null : xep.trim();
	}

	public String getXmlns() {
		return xmlns;
	}

}
