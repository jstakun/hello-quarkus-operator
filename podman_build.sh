export VERSION=0.2
podman build -f ./Dockerfile -t dev.local/jstakun/hello-quarkus-operator:$VERSION --build-arg JAR_FILE=hello-quarkus-operator-$VERSION-runner.jar --force-rm .
podman tag dev.local/jstakun/hello-quarkus-operator:$VERSION quay.io/jstakun/hello-quarkus-operator:$VERSION
podman login quay.io
podman push quay.io/jstakun/hello-quarkus-operator:$VERSION

podman rmi quay.io/jstakun/hello-quarkus-operator:$VERSION
podman rmi dev.local/jstakun/hello-quarkus-operator:$VERSION