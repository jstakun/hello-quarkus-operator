package com.redhat.waw.operator.hello.quarkus;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.config.ConfigurationService;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class HelloQuarkusOperator implements QuarkusApplication {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Inject Operator operator;
	
	public static void main(String... args) {
		//TODO check if crd exists and if not create it
		//TODO check if operator runs in openshift or kubernetes mode and initiate appropriate KubernetesClient 
		Quarkus.run(HelloQuarkusOperator.class, args);
	}
	  
	@Override
	public int run(String... args) throws Exception {
		operator.start();
		Quarkus.waitForExit();
	    return 0;
	}
}
