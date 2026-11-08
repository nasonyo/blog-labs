# blog-labs

기술 블로그(Tistory) 글에 사용한 실습 코드 모음. 각 디렉토리는 `docker compose up -d` 또는 `kubectl apply` 로 재현됩니다.

| 디렉토리 | 주제 | 블로그 글 |
|---|---|---|
| [05-prometheus](./05-prometheus) | Docker Compose로 메트릭 수집 시작하기 | [글 보기](https://journal52709.tistory.com/1) |
| [06-grafana](./06-grafana) | Prometheus 메트릭을 Grafana 대시보드로 (프로비저닝) | [글 보기](https://journal52709.tistory.com/3) |
| [07-elk](./07-elk) | Elasticsearch + Kibana 로 로그 삽입(Bulk)과 조회(Query DSL / KQL) | [글 보기](https://journal52709.tistory.com/2) |
| [08-jenkins](./08-jenkins) | Docker Compose 로 Jenkins CI/CD 파이프라인 (JCasC + Jenkinsfile) | [글 보기](https://journal52709.tistory.com/4) |
| [09-kubernetes](./09-kubernetes) | kind 로컬 클러스터에서 Deployment·Service 배포 (자가 복구, 롤링 업데이트) | TODO: Tistory URL |

## 규칙
- 시크릿은 커밋하지 않는다. 필요한 값은 `.env.example` 로 제공한다.
- 이미지 태그는 버전을 고정한다 (`latest` 금지).
