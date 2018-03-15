package BrowserInteraction;

import java.io.Serializable;

public class Response implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String[] headerAttributes;
	private byte[] response;
	private int statusCode;
	public int getStatusCode() {
		return statusCode;
	}
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	public String[] getHeaderAttributes() {
		return headerAttributes;
	}
	public void setHeaderAttributes(String[] headerAttributes) {
		this.headerAttributes = headerAttributes;
	}
	public byte[] getResponse() {
		return response;
	}
	public void setResponse(byte[] response) {
		this.response = response;
	}
}
