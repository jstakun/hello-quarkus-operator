package com.redhat.waw.operator.hello.quarkus;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("operator.waw.redhat.pl")
@Version("v1")
public class HelloQuarkus extends CustomResource<HelloQuarkusSpec, HelloQuarkusStatus> implements Namespaced {
	private static final long serialVersionUID = -4664755001008436550L;
}
