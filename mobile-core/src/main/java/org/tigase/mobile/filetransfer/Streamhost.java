package org.tigase.mobile.filetransfer;

public class Streamhost {

	private String address;
	private String jid;
	private Integer port;

	public Streamhost(String jid, String address, Integer port) {
		this.jid = jid;
		this.address = address;
		this.port = port;
	}

	public String getAddress() {
		return address;
	}

	public String getJid() {
		return jid;
	}

	public Integer getPort() {
		return port;
	}

}