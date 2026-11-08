FROM jenkins/jenkins:2.541.3-lts-jdk17

USER root

# 호스트 Docker 데몬을 호출하기 위한 docker CLI 만 복사한다 (데몬은 설치하지 않는다)
COPY --from=docker:27.1.1-cli /usr/local/bin/docker /usr/local/bin/docker

# kubectl 과 kind CLI (CPU 아키텍처에 맞는 바이너리를 받는다)
ARG TARGETARCH
ARG KUBECTL_VERSION=v1.36.1
ARG KIND_VERSION=v0.33.0
RUN curl -fsSL -o /usr/local/bin/kubectl "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/${TARGETARCH}/kubectl" \
 && curl -fsSL -o /usr/local/bin/kind "https://kind.sigs.k8s.io/dl/${KIND_VERSION}/kind-linux-${TARGETARCH}" \
 && chmod +x /usr/local/bin/kubectl /usr/local/bin/kind

# [실습용] 마운트된 로컬 저장소의 소유자가 달라도 git 이 동작하도록 허용
RUN git config --system --add safe.directory '*'

RUN jenkins-plugin-cli --plugins configuration-as-code job-dsl workflow-aggregator git pipeline-stage-view

ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"
ENV CASC_JENKINS_CONFIG=/var/jenkins_home/casc.yaml
COPY casc.yaml /var/jenkins_home/casc.yaml

# [실습용] 마운트된 docker.sock 에 접근하려고 root 로 실행한다. 운영에서는 사용하지 않는다.
