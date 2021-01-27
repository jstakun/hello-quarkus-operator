package com.redhat.waw.operator.hello.quarkus;

public class HelloQuarkusStatus {
	  
	private String areWeGood;

	private String ingressHost;

	public String getAreWeGood() {
		return areWeGood;
	}

	public void setAreWeGood(String areWeGood) {
		this.areWeGood = areWeGood;
	}

	public String getIngressHost() {
		return ingressHost;
	}

	public void setIngressHost(String ingressUrl) {
		this.ingressHost = ingressUrl;
	}
}
