# 자바 TreeMap과 NavigableMap의 동작 원리와 범위 검색

날짜별 요금표와 무게별 배송비로 키의 앞뒤 조회, 범위 뷰, 맵 복사, 비교 기준을 설명하는 강의 코드입니다. 모든 시연은 새 맵을 만들어 시작하므로 실행 순서에 영향을 받지 않습니다. 영상만으로 설명을 이해할 수 있으며, 코드 실행은 선택 사항입니다.

## 실행 환경

- JDK 21 이상과 Bash가 필요합니다. `java`와 `javac`가 같은 JDK를 가리키도록 설정합니다.
- 실행 확인 환경은 macOS의 Zulu JDK 21.0.7입니다. 컴파일 대상은 Java 21이며 외부 라이브러리·서버·데이터베이스는 사용하지 않습니다.
- `JAVA_HOME`이 설정되어 있으면 실행 스크립트가 그 JDK를 사용합니다. 설정되어 있지 않으면 PATH의 `java`와 `javac`를 사용합니다.

프로젝트 폴더에서 실행합니다.

```sh
./run.sh demo
./run.sh test
```

`demo`는 열 가지 시연을 순서대로 출력합니다. `test`는 경계 조건과 실패 후 상태, 시연의 실제 출력을 검사합니다. 성공하면 `PASS: 60 checks`가 나옵니다. 컴파일 결과는 `build/classes`에 생성됩니다.

스크립트를 사용하지 않을 때의 같은 명령은 다음과 같습니다.

```sh
mkdir -p build/classes
javac -encoding UTF-8 --release 21 -d build/classes src/*.java
java -cp build/classes TreeMapLesson demo
java -cp build/classes LessonChecks
```

## 예제별 실행과 결과

| 명령 | 설명과 확인할 결과 |
|---|---|
| `./run.sh rates` | 6·1·3·9월 순서로 저장해도 키 목록은 1·3·6·9월 순서입니다. |
| `./run.sh floor` | 4월 15일의 `get`은 `null`, `floorEntry`는 `2026-03-01 / 12000`입니다. |
| `./run.sh neighbors` | 3월 1일 기준 lower는 1월, floor·ceiling은 3월, higher는 6월입니다. 첫 키 이전과 빈 맵의 floor 결과는 `null`입니다. |
| `./run.sh shipping` | 양수 무게를 대상으로 합니다. 1,500g의 ceiling은 `3000=5000`, 3,000g의 ceiling도 `3000=5000`, 같은 무게의 higher는 `5000=7000`, 6,000g의 ceiling은 `null`입니다. |
| `./run.sh range` | 3월 포함·9월 제외 범위에는 3월과 6월 항목이 들어갑니다. 시작 제외·끝 포함으로 바꾼 결과도 출력합니다. |
| `./run.sh view` | 뷰에서 6월을 수정하면 원본도 16,000원입니다. 뷰에서 3월을 지우면 원본에도 없으며, 원본에 넣은 4월은 뷰에 나타납니다. |
| `./run.sh copy` | 뷰 범위 밖인 9월 저장은 `IllegalArgumentException`이며 기존 18,000원이 유지됩니다. 복사한 맵에서 3월을 지워도 원본과 뷰에는 3월이 남습니다. |
| `./run.sh entry` | 반환 항목의 값은 12,000원, 갱신한 원본 값은 13,000원입니다. 반환 항목의 `setValue`는 `UnsupportedOperationException`이며 원본은 그대로입니다. |
| `./run.sh length` | 길이만 비교하면 `pen`과 `tea`는 같은 키로 처리됩니다. 항목 수는 1, `pen`의 조회값은 3,000입니다. |
| `./run.sh tie` | 길이가 같으면 문자열을 비교합니다. 키 순서는 `[pen, tea, book]`, `pen`의 값은 2,000입니다. |

요금표의 날짜는 모두 2026년이며, 1월 1일 10,000원, 3월 1일 12,000원, 6월 1일 15,000원, 9월 1일 18,000원입니다. 각 요금은 다음 시행일 전까지 적용됩니다. 배송비 표는 양수 무게에 대해 상한 1,000g·3,000g·5,000g을 사용합니다. 무게 입력 검증이나 실제 배송 정책 전체를 구현한 프로그램은 아닙니다.

## 코드 구성

- [TreeMapLesson.java](src/TreeMapLesson.java): 영상에서 설명하는 코드와 개별 시연입니다. `slide` 주석 사이의 문장이 슬라이드에 사용됩니다.
- [LessonChecks.java](src/LessonChecks.java): 값·키 목록·예외·실패 후 원본 상태와 시연 출력을 검사합니다. 새 맵과 반환 항목이 수정 가능한 값 객체를 공유할 수 있다는 조건도 확인합니다.
- [run.sh](run.sh): 컴파일과 실행을 묶은 스크립트입니다.

범위 뷰는 원본과 연결되며, 새 맵으로 복사해도 키·값 객체의 내부 상태까지 깊게 복사하지는 않습니다. 예제의 날짜·정수·문자열은 불변 객체입니다. 비교 기준은 항목의 순서와 같은 키 판정을 함께 결정하므로, 서로 다른 키를 구별하는 기준인지 확인합니다.

이 프로젝트는 한 프로세스의 순차 실행을 다룹니다. 동시 수정이나 성능 수치는 검증 대상에 포함하지 않았습니다. 맵의 내부 트리 모양은 공개 API의 보장 대상이 아니므로 특정 모양이나 비교 횟수에 의존하지 않습니다.

## 공식 자료

- [Java 21 TreeMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeMap.html)
- [Java 21 NavigableMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/NavigableMap.html)
- [Java 21 Comparator](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Comparator.html)
