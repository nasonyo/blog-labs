#!/bin/bash
# etcd 에 저장된 secret 의 원본 바이트를 읽는다. usage: ./etcd-get.sh <secret 이름>
kubectl -n kube-system exec etcd-secret-lab-control-plane -- etcdctl \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  get /registry/secrets/secret-lab/"$1" | strings | grep -E "registry|password|k8s:enc|demo-|aescbc|key1"
