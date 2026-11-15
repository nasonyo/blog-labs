# 19. Helm 차트로 환경별 배포하기

nginx 로 한 줄짜리 페이지를 보여 주는 작은 차트(`memo-web`)를 만들고, 같은 차트에 값(values)만 바꿔 배포(install), 업그레이드(upgrade), 실패 시 되돌리기(rollback)를 해 본다.

## 환경
- Helm v4.3.2, kubectl v1.36.1, kind 클러스터 `blog` (쿠버네티스 v1.37.0)
- 이미지 `nginx:1.27-alpine`

## 구성
| 파일 | 설명 |
|---|---|
| `memo-web/Chart.yaml` | 차트 이름과 버전 |
| `memo-web/values.yaml` | 기본값(개발 환경) |
| `values-prod.yaml` | 운영 환경에서 덮어쓸 값 |
| `memo-web/templates/` | ConfigMap, Deployment, Service 템플릿 |
| `logs/` | 실제 실행 출력 |

## 실행
```bash
helm lint memo-web
helm template web ./memo-web                       # 클러스터에 적용하지 않고 결과만 본다
helm install web ./memo-web -n helm-lab --create-namespace --wait
helm upgrade web ./memo-web -n helm-lab -f values-prod.yaml --wait
helm history web -n helm-lab
helm upgrade web ./memo-web -n helm-lab --set image.tag=does-not-exist --rollback-on-failure --timeout 45s   # 일부러 실패
helm rollback web 1 -n helm-lab --wait
```

## 정리
```bash
helm uninstall web -n helm-lab
kubectl delete namespace helm-lab
```
