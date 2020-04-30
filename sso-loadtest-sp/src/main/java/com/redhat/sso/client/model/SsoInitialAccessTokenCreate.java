package com.redhat.sso.client.model;

public class SsoInitialAccessTokenCreate {
	private int count = 1;
	private int expiration;
	
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	public int getExpiration() {
		return expiration;
	}
	public void setExpiration(int expiration) {
		this.expiration = expiration;
	}
	
	
}
