package com.redhat.waw.operator.hello.quarkus;

public class HelloQuarkusSpec {
	
	private int replicas;
	private String greetingSuffix;
	private String version;
	private String ingressHost;
	
	public int getReplicas() {
		return replicas;
	}
	
	public void setReplicas(int replicas) {
		this.replicas = replicas;
	}

	public String getGreetingSuffix() {
		return greetingSuffix;
	}

	public void setGreetingSuffix(String greetingSuffix) {
		this.greetingSuffix = greetingSuffix;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getIngressHost() {
		return ingressHost;
	}

	public void setIngressHost(String ingressHost) {
		this.ingressHost = ingressHost;
	}
}
