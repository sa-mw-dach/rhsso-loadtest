package com.redhat.sso.metrics;

public class SSOMetrics {
	
	private int loginSuccessCount;
	private int loginErrorCount;
	
	public int incrementLoginSuccessCount() {
		return ++ loginSuccessCount;
	}
	public int incrementLoginErrorCount() {
		return ++ loginErrorCount;
	}
	public int getLoginSuccessCount() {
		return loginSuccessCount;
	}
	public void setLoginSuccessCount(int loginSuccessCount) {
		this.loginSuccessCount = loginSuccessCount;
	}
	public int getLoginErrorCount() {
		return loginErrorCount;
	}
	public void setLoginErrorCount(int loginErrorCount) {
		this.loginErrorCount = loginErrorCount;
	}
	
}
