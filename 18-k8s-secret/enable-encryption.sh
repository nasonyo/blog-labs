#!/bin/bash
# kind control-plane 노드에서 etcd 저장 암호화(aescbc)를 켠다. 키는 실행할 때마다 새로 만들고 저장소에는 남기지 않는다.
set -e
NODE=secret-lab-control-plane
KEY=$(head -c 32 /dev/urandom | base64)
docker exec -i $NODE sh -c 'cat > /etc/kubernetes/enc.yaml' <<YAML
apiVersion: apiserver.config.k8s.io/v1
kind: EncryptionConfiguration
resources:
  - resources: ["secrets"]
    providers:
      - aescbc:
          keys:
            - name: key1
              secret: $KEY
      - identity: {}
YAML
# kube-apiserver 정적 파드 매니페스트에 플래그와 볼륨을 추가한다. kubelet 이 감지해서 apiserver 를 다시 띄운다.
docker exec $NODE sh -c '
M=/etc/kubernetes/manifests/kube-apiserver.yaml
grep -q encryption-provider-config $M && exit 0
sed -i "s#- kube-apiserver#- kube-apiserver\n    - --encryption-provider-config=/etc/kubernetes/enc.yaml#" $M
sed -i "s#^  volumes:#  volumes:\n  - hostPath: {path: /etc/kubernetes/enc.yaml, type: File}\n    name: enc#" $M
sed -i "s#^    volumeMounts:#    volumeMounts:\n    - {mountPath: /etc/kubernetes/enc.yaml, name: enc, readOnly: true}#" $M
'
echo "apiserver 재시작 대기..."
sleep 20
until kubectl get --raw /readyz >/dev/null 2>&1; do sleep 3; done
echo "완료"
