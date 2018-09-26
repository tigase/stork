package org.tigase.messenger.phone.pro.serverfeatures;

class FeatureItem {

	private final String xmlns;
	private String description;
	private String name;
	private String xep;

	public FeatureItem(String xmlns, String xep, String name, String description) {
		this.description = description;
		this.name = name;
		this.xep = xep;
		this.xmlns = xmlns;
	}

	public FeatureItem(String xmlns) {
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
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		FeatureItem that = (FeatureItem) o;

		return xmlns.equals(that.xmlns);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description == null ? null : description.trim();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name == null ? null : name.trim();
	}

	public String getXep() {
		return xep;
	}

	public void setXep(String xep) {
		this.xep = xep == null ? null : xep.trim();
	}

	public String getXmlns() {
		return xmlns;
	}

	@Override
	public int hashCode() {
		return xmlns.hashCode();
	}
}
