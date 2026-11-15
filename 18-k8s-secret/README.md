# 18. 쿠버네티스 Secret 은 정말 암호화일까?

Secret 이 base64 일 뿐이라는 것을 직접 확인하고, RBAC 으로 읽기 권한을 막고, etcd 저장 암호화(aescbc)를 켜서 저장된 바이트가 어떻게 바뀌는지 본다.

## 환경
- kind v0.33.0, 노드 이미지 `kindest/node:v1.37.0` (단일 control-plane), kubectl v1.36.1
- Docker Desktop 실행 필요

## 실행 순서
```bash
kind create cluster --name secret-lab --config kind-config.yaml
kubectl create namespace secret-lab
kubectl -n secret-lab create secret generic db-pass --from-literal=password='demo-Passw0rd!'   # 실습용 가짜 값
kubectl -n secret-lab get secret db-pass -o jsonpath='{.data.password}' | base64 -d           # 1. 디코딩
kubectl apply -f rbac.yaml                                                                    # 2. RBAC
kubectl -n secret-lab get secret db-pass --as=system:serviceaccount:secret-lab:nobody
./etcd-get.sh db-pass                                                                          # 3. etcd 원본 (평문)
./enable-encryption.sh                                                                         # 4. 암호화 켜기
kubectl get secrets -A -o json | kubectl replace -f -                                          # 기존 secret 다시 쓰기
./etcd-get.sh db-pass                                                                          # 암호문 확인
```

## 파일
| 파일 | 설명 |
|---|---|
| `kind-config.yaml` | 단일 노드 클러스터 |
| `rbac.yaml` | 읽을 수 있는 계정 reader, 읽을 수 없는 계정 nobody |
| `etcd-get.sh` | etcd 에 저장된 원본 바이트 읽기 |
| `enable-encryption.sh` | apiserver 에 aescbc 암호화 설정. 키는 실행 때마다 새로 만들고 저장소에 남기지 않는다 |
| `logs/` | 실제 실행 출력 (암호문 바이트는 `·` 로 치환) |

## 정리
```bash
kind delete cluster --name secret-lab
```

## 주의
- 이 실습의 비밀번호는 가짜 값이다. 실제 값으로 따라 하지 않는다.
- `enable-encryption.sh` 는 control-plane 노드의 apiserver 매니페스트를 직접 고친다. 실습용 kind 클러스터에서만 실행한다.
