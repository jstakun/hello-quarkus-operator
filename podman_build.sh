podman build -f ./Dockerfile -t dev.local/jstakun/hello-quarkus-operator:0.1 --build-arg JAR_FILE=hello-quarkus-operator-0.1-runner.jar --force-rm .
podman tag dev.local/jstakun/hello-quarkus-operator:0.1 quay.io/jstakun/hello-quarkus-operator:0.1
podman login quay.io
podman push quay.io/jstakun/hello-quarkus-operator:0.1

podman rmi quay.io/jstakun/hello-quarkus-operator:0.1
podman rmi dev.local/jstakun/hello-quarkus-operator:0.1