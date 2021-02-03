package com.redhat.waw.operator.hello.quarkus;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;

@Controller
public class HelloQuarkusController implements ResourceController<HelloQuarkus> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final KubernetesClient kubernetesClient;

	public HelloQuarkusController(KubernetesClient kubernetesClient) {
	    this.kubernetesClient = kubernetesClient;
	}
	
	@Override
	public UpdateControl<HelloQuarkus> createOrUpdateResource(HelloQuarkus helloQuarkusInst, Context<HelloQuarkus> context) {
		//validate input crd instance parmeters 
		final String ns = helloQuarkusInst.getMetadata().getNamespace();
		
		final String greetingSuffix = helloQuarkusInst.getSpec().getGreetingSuffix();
		
		String version = helloQuarkusInst.getSpec().getVersion();
	    if (StringUtils.isEmpty(version)) {
    		version = "latest";
	    }
	    
	    int replicas = helloQuarkusInst.getSpec().getReplicas();
        if (replicas < 0) {
        	log.warn("Min replicas is 0. Changing to 0.");
        	replicas = 0;
        } else if (replicas > 10) {
        	log.warn("Max replicas is 10. Changing to 10.");
        	replicas = 10;
        }
	    
        final String ingressHost = helloQuarkusInst.getSpec().getIngressHost(); 
        if (StringUtils.isEmpty(ingressHost)) {
        	 log.warn("No ingress host specified. Manual ingress configuration will be required!");
        }
        
		//init kubernetes/openshift objects
        
		Deployment deployment = loadYaml(Deployment.class, "deployment.yaml");
	    deployment.getMetadata().setName(deploymentName(helloQuarkusInst));
	    deployment.getMetadata().setNamespace(ns);
	    deployment.getMetadata().getLabels().put("app", deploymentName(helloQuarkusInst));
	    
	    deployment.getSpec().setReplicas(replicas); 
	    
	    deployment
	        .getSpec()
	        .getTemplate()
	        .getMetadata()
	        .getLabels()
	        .put("app", deploymentName(helloQuarkusInst));
	    
    	List<Container> containers = deployment.getSpec().getTemplate().getSpec().getContainers();
	    if (!containers.isEmpty()) {
	    	if (StringUtils.isNotEmpty(greetingSuffix)) {
		    	List<EnvVar> envs = new ArrayList<EnvVar>();
		    	EnvVar helloSuffix = new EnvVar();
		    	helloSuffix.setName("HELLO_SUFFIX");
		    	helloSuffix.setValue(greetingSuffix);
		    	envs.add(helloSuffix);
		    	containers.get(0).setEnv(envs);
	    	}
		    containers.get(0).setImage("quay.io/jstakun/hello-quarkus:" + version);
	    }  else {
	    	throw new RuntimeException("Deployment has no container definition! Please add one...");
	    }
	    
	    Service service = loadYaml(Service.class, "service.yaml");
	    service.getMetadata().setName(serviceName(helloQuarkusInst));
	    service.getMetadata().setNamespace(ns);
	    service.getSpec().setSelector(deployment.getSpec().getTemplate().getMetadata().getLabels());
        service.getMetadata().getLabels().put("app", deploymentName(helloQuarkusInst));
	    
	    Ingress ingress = null;
	    Route route = null;
	    boolean newHost = false;
    	
	    if (StringUtils.isNotEmpty(ingressHost)) {	
	    	if (kubernetesClient instanceof DefaultOpenShiftClient) {
	    		route = loadYaml(Route.class, "route.yaml");
	    		route.getMetadata().setName(ingressName(helloQuarkusInst));
	    		route.getMetadata().setNamespace(ns);   
	    		route.getMetadata().getLabels().put("app", deploymentName(helloQuarkusInst));
	    		if (!StringUtils.equals(route.getSpec().getHost(), ingressHost)) {
	    			route.getSpec().setHost(ingressHost);
	    			newHost = true;
	    		}
	    	} else {
	    		ingress = loadYaml(Ingress.class, "ingress.yaml");
	    		ingress.getMetadata().setName(ingressName(helloQuarkusInst));
	    		ingress.getMetadata().setNamespace(ns);
	    		ingress.getMetadata().getLabels().put("app", deploymentName(helloQuarkusInst));
	    		
	    		ingress.getSpec().
	    			getRules().get(0).
	    			getHttp().
	    			getPaths().get(0).
	    			getBackend().
	    			setServiceName(service.getMetadata().getName());
	    	
	    		if (!StringUtils.equals(ingress.getSpec().getRules().get(0).getHost(), ingressHost)) {
	    			ingress.getSpec().
    					getRules().get(0).
    					setHost(ingressHost);
	    			newHost = true;
	    		}
	    	}
	    }
	    
	    //create kubernetes/openshift objects

	    log.info("Creating or updating Deployment {} in {}", deployment.getMetadata().getName(), ns);
	    kubernetesClient.apps().deployments().inNamespace(ns).createOrReplace(deployment);

	    if (kubernetesClient.services().inNamespace(ns).withName(service.getMetadata().getName()).get() == null) {
	    	log.info("Creating Service {} in {}", service.getMetadata().getName(), ns);
	    	kubernetesClient.services().inNamespace(ns).createOrReplace(service);
	    }
	    
	    if (kubernetesClient instanceof DefaultOpenShiftClient) {
	    	DefaultOpenShiftClient openshiftClient = (DefaultOpenShiftClient) kubernetesClient;
	    	if (newHost) {
	    		log.info("Creating or updating Route {} in {}", route.getMetadata().getName(), ns);
	    		openshiftClient.routes().inNamespace(ns).createOrReplace(route);
	    	}
	    } else {
	    	if (newHost) {
	    		log.info("Creating or updating Ingress {} in {}", ingress.getMetadata().getName(), ns);
	    		kubernetesClient.extensions().ingresses().inNamespace(ns).createOrReplace(ingress);
	    	}
	    }
	    
	    //set crd instance status
	    
	    HelloQuarkusStatus status = new HelloQuarkusStatus();
	    status.setAreWeGood("Yes!");
	    if (ingressHost != null) {
	    	status.setIngressHost(ingressHost);
	    }
	    helloQuarkusInst.setStatus(status);
	    
	    return UpdateControl.updateCustomResource(helloQuarkusInst);
	}

	@Override
	public DeleteControl deleteResource(HelloQuarkus helloQuarkusInst, Context<HelloQuarkus> context) {
		 //delete all kubernetes/openshift objects 
		
		log.info("Execution deleteResource for: {}", helloQuarkusInst.getMetadata().getName());
		 final String ns = helloQuarkusInst.getMetadata().getNamespace();
		 
		 log.info("Deleting Deployment {}", deploymentName(helloQuarkusInst));
		   
		 RollableScalableResource<Deployment> deployment =
		        kubernetesClient
		            .apps()
		            .deployments()
		            .inNamespace(ns)
		            .withName(deploymentName(helloQuarkusInst));
		 if (deployment.get() != null) {
			  try {
				  deployment.withPropagationPolicy(DeletionPropagation.FOREGROUND).delete();
			  } catch (Exception e) {
				   log.error("Error deleting deployment " + deploymentName(helloQuarkusInst), e);
			  }
		 }

		 log.info("Deleting Service {}", serviceName(helloQuarkusInst));
		 ServiceResource<Service> service =
		        kubernetesClient
		            .services()
		            .inNamespace(ns)
		            .withName(serviceName(helloQuarkusInst));
		 if (service.get() != null) {
		     try {
		    	 service.delete();
		     } catch (Exception e) {
				   log.error("Error deleting service " + serviceName(helloQuarkusInst), e);
			 }
		 }
		 
		 if (kubernetesClient instanceof DefaultOpenShiftClient) {
			log.info("Deleting Route {}", ingressName(helloQuarkusInst));
			DefaultOpenShiftClient openshiftClient = (DefaultOpenShiftClient) kubernetesClient;
		    Resource<Route> route =
		        openshiftClient
		            .routes()
		            .inNamespace(ns)
		            .withName(ingressName(helloQuarkusInst));
		    if (route.get() != null) {
		    	try {
		    		route.delete();
		    	} catch (Exception e) {
					log.error("Error deleting route " + ingressName(helloQuarkusInst), e);
		    	}
		    }
		 } else {
			 log.info("Deleting Ingress {}", ingressName(helloQuarkusInst));
			 Resource<Ingress> ingress =
			        kubernetesClient
			            .extensions()
			            .ingresses()
			            .inNamespace(ns)
			            .withName(ingressName(helloQuarkusInst));
			 if (ingress.get() != null) {
			     try {
			    	 ingress.delete();
			     } catch (Exception e) {
					 log.error("Error deleting ingress " + ingressName(helloQuarkusInst), e);
			     }
			 } 
		 }
		    
		 return DeleteControl.DEFAULT_DELETE;
	}

	private static String deploymentName(HelloQuarkus helloQuarkusInst) {
	    return helloQuarkusInst.getMetadata().getName();
	}

	private static String serviceName(HelloQuarkus helloQuarkusInst) {
	    return helloQuarkusInst.getMetadata().getName();
	}
	
	private static String ingressName(HelloQuarkus helloQuarkusInst) {
	    return helloQuarkusInst.getMetadata().getName();
	}

	private <T> T loadYaml(Class<T> clazz, String yaml) {
	    try (InputStream is = getClass().getResourceAsStream(yaml)) {
	    	return Serialization.unmarshal(is, clazz);
	    } catch (IOException ex) {
	    	throw new IllegalStateException("Cannot find yaml on classpath: " + yaml);
	    }
	}
}
