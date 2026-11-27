# 스트랭글러 피그 패턴

상품 조회를 새 서비스로 옮기고 재고 변경은 기존 서비스에서 처리하는 강의 예제입니다. Java 프로세스 세 개가 실제 HTTP로 통신합니다. 영상만 보셔도 설명을 따라갈 수 있으며, 직접 실행하실 때 아래 순서를 사용하시면 됩니다.

## 실행 환경

- JDK 17 이상: 강의 검증 환경은 Eclipse Temurin 17.0.17입니다.
- Spring Boot 3.5.8: `pom.xml`에 버전을 고정했습니다.
- Maven Wrapper 3.3.4가 Maven 3.9.9를 받습니다. 최초 실행에는 인터넷 연결이 필요합니다.
- macOS·Linux의 셸 또는 Windows의 PowerShell을 사용합니다. Windows에서는 `./mvnw` 대신 `.\mvnw.cmd`를 실행합니다.
- 수동 시연에서는 로컬 포트 19080, 19081, 19082를 사용합니다. 통합 테스트는 별도의 빈 포트를 선택합니다.

이 프로젝트 폴더에서 다음 명령으로 빌드하고 통합 테스트를 실행합니다.

```sh
./mvnw clean verify
```

`BUILD SUCCESS`와 실패 없는 두 통합 테스트가 나옵니다. 테스트는 실제 Spring Boot 프로세스를 띄워 응답 본문, 상태 코드, 처리 경로, 재고 변경, 실패 후 상태와 이전 경로 종료를 확인합니다. 테스트 종료 시 자신이 시작한 프로세스를 종료합니다. 실행 로그는 `target/scenario-logs/`, 테스트 보고서는 `target/failsafe-reports/`에 생성됩니다.

## 앱 세 개 시작하기

각각 다른 터미널에서 실행합니다. 명령은 모두 프로젝트 폴더 기준입니다.

터미널 1: 기존 서비스

```sh
java -jar target/strangler-fig-demo.jar --spring.profiles.active=legacy --server.port=19081
```

터미널 2: 새 상품 조회 서비스

```sh
java -jar target/strangler-fig-demo.jar --spring.profiles.active=modern --server.port=19082
```

터미널 3: 요청을 전달하는 진입 앱

```sh
java -jar target/strangler-fig-demo.jar --spring.profiles.active=gateway --server.port=19080
```

각 앱은 `127.0.0.1`에만 바인딩됩니다. `/lesson/catalog-route/{mode}`는 로컬 시연용 설정 변경 주소이며 인증 기능을 포함하지 않습니다. 진입 앱도 강의에 필요한 두 업무 요청을 전달하는 코드로 구성했습니다. 운영용 범용 프록시, 인증 서버나 부하 분산기 구현은 포함하지 않습니다.

## 상품 조회 전환과 재고 변경

네 번째 터미널에서 순서대로 실행합니다. `Demo.java`는 JDK의 HTTP 클라이언트로 실제 요청을 보내고 예상 상태 코드와 다르면 실패합니다. 응답 필드와 값의 상세 검증은 앞의 통합 테스트에 있습니다.

```sh
java tools/Demo.java baseline
java tools/Demo.java migrate
java tools/Demo.java update
java tools/Demo.java rollback
java tools/Demo.java missing
```

| 명령 인수 | 처리 경로와 결과 |
|---|---|
| `baseline` | LEGACY, `{"id":"P100","name":"노트","stock":10}` |
| `migrate` | MODERN, 같은 상품 본문과 재고 10 |
| `update` | 기존 서비스에서 재고를 7로 바꾸고 MODERN으로 조회, 재고 7 |
| `rollback` | LEGACY로 다시 조회, 재고는 변경된 7 유지 |
| `missing` | 두 상품 조회 경로 모두 없는 상품에 404 반환 |

각 명령은 필요한 조회 경로를 먼저 설정합니다. 첫 재고는 10이며, `update`를 실행한 뒤에는 7이 유지됩니다. `X-Lesson-Catalog` 응답 헤더는 이번 조회에 선택한 서비스 이름을 표시합니다. 기존·새 서비스 터미널의 `LEGACY catalog`, `MODERN catalog`, `LEGACY inventory` 로그로 실제 호출도 확인할 수 있습니다.

`PUT /inventory/P100`은 수량을 지정한 값으로 바꿉니다. 음수 수량은 400으로 거절하고 기존 수량을 유지합니다. 없는 상품은 404를 반환합니다.

## 새 서비스 종료와 조회 경로 원복

기존 서비스와 진입 앱은 유지하고, 새 서비스를 실행한 터미널 2에서 Ctrl+C를 눌러 새 서비스만 종료합니다.

```sh
java tools/Demo.java outage
```

MODERN 경로의 상품 조회는 502로 실패합니다. 이어서 도구가 조회 경로를 LEGACY로 바꾸면 기존 상품 조회가 200으로 응답합니다. 앞 순서대로 실행했다면 재고는 7입니다. 프로그램은 새 서비스 실패 시 자동으로 기존 서비스로 우회하도록 구현하지 않았습니다. 각 HTTP 클라이언트에 연결·읽기 제한을 2초로 설정했습니다. 여러 통신을 거치는 전체 요청의 완료 시간을 항상 2초로 보장하는 설정은 아닙니다.

다음 시연 전에 터미널 2의 시작 명령으로 새 서비스를 다시 실행합니다.

## 기존 상품 조회 주소만 닫기

터미널 1에서 기존 서비스를 종료하고 아래 명령으로 다시 실행합니다.

```sh
java -jar target/strangler-fig-demo.jar --spring.profiles.active=legacy --server.port=19081 --legacy.catalog-enabled=false
```

기존 상품 조회 컨트롤러의 등록을 끈 상태입니다. 소스 파일은 이전 구현을 비교할 수 있도록 프로젝트에 남겨 두었습니다. 설정은 앱 시작 시 적용됩니다. 시연 중 설정 파일만 고쳐서는 이미 실행 중인 앱의 컨트롤러가 사라지지 않습니다.

```sh
java tools/Demo.java retired
```

MODERN 상품 조회는 200으로 응답하고, LEGACY 상품 조회는 404로 실패합니다. 기존 서비스의 재고 기능은 계속 동작합니다. 이 명령은 마지막에 MODERN으로 설정을 돌려놓습니다.

재고는 기존 서비스 메모리에 있으므로 기존 서비스를 다시 시작하면 10으로 초기화됩니다. 데이터베이스의 데이터 이전이나 재시작 후 영속성은 구현하지 않았습니다. 새 상품 조회는 기존 재고 기능을 호출하므로, 기존 서비스 전체를 종료하면 새 상품 조회도 502로 실패합니다. 이 동작은 통합 테스트의 두 번째 시나리오에서도 확인합니다.

시연이 끝나면 시작한 각 터미널에서 Ctrl+C로 앱을 종료합니다. 다음 실행은 초기 재고 10, 기본 상품 조회 경로 LEGACY로 시작합니다.

## 코드 위치

| 파일 | 역할 |
|---|---|
| [GatewayController.java](src/main/java/dev/tim/strangler/GatewayController.java) | 상품 조회 목적지 선택과 기존 재고 변경 요청 전달 |
| [CatalogRoutes.java](src/main/java/dev/tim/strangler/CatalogRoutes.java) | 현재 상품 조회 목적지 보관 |
| [LessonRouteController.java](src/main/java/dev/tim/strangler/LessonRouteController.java) | 로컬 시연용 경로 설정 |
| [LegacyCatalogController.java](src/main/java/dev/tim/strangler/LegacyCatalogController.java) | 기존 상품 조회와 조건부 등록 |
| [ModernCatalogController.java](src/main/java/dev/tim/strangler/ModernCatalogController.java) | 기존 재고를 읽어 새 상품 응답 구성 |
| [LegacyInventory.java](src/main/java/dev/tim/strangler/LegacyInventory.java) | 재고 보관·검사·변경 |
| [LegacyStockController.java](src/main/java/dev/tim/strangler/LegacyStockController.java) | 기존 재고의 조회·변경 HTTP 주소 |
| [BackendClients.java](src/main/java/dev/tim/strangler/BackendClients.java) | 서비스 주소와 연결·읽기 시간 제한 |
| [UpstreamErrors.java](src/main/java/dev/tim/strangler/UpstreamErrors.java) | 뒤쪽 서비스의 오류 상태와 연결 실패 응답 |
| [ProductView.java](src/main/java/dev/tim/strangler/ProductView.java) | 상품 응답의 필드 |
| [Demo.java](tools/Demo.java) | 시연 순서별 HTTP 요청 |
| [MigrationIT.java](src/test/java/dev/tim/strangler/MigrationIT.java) | 실제 프로세스·HTTP·상태 검증 |

상품은 P100 한 개, 이름은 노트라는 고정 데이터입니다. 두 상품 구현의 응답과 재고 의존성을 비교하는 구성입니다. 여러 서버 사이의 데이터 복제나 결제, 로그인, 데이터베이스 트랜잭션을 실행하는 예제는 아닙니다.
