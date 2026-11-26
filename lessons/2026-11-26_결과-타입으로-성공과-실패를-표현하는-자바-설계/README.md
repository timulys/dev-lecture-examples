# 결과 타입으로 성공과 실패를 표현하는 자바 설계

쿠폰 적용 판정을 성공, 만료, 사용 완료라는 객체로 반환하고 호출자가 안내문과 후속 작업을 정하는 Java 예제입니다. 영상만 보셔도 흐름을 이해할 수 있으며, 직접 실행은 선택 사항입니다.

## 환경

- JDK 21 이상이 필요합니다. `java`와 `javac`가 같은 JDK를 가리키도록 설정합니다.
- 검증 환경: Zulu OpenJDK 21.0.7, macOS. 소스는 Java 21 기준으로 컴파일합니다.
- 외부 라이브러리, 데이터베이스, 네트워크 연결은 사용하지 않습니다.
- `run.sh`는 macOS, Linux 또는 Windows의 Git Bash에서 실행할 수 있습니다. Windows 기본 터미널용 명령도 아래에 있습니다.

## 실행

터미널에서 이 README가 있는 강의 폴더를 연 뒤 실행합니다.

```sh
./run.sh demo outcomes
./run.sh demo confirm
./run.sh demo flow
./run.sh demo invalid
./run.sh demo ignored
./run.sh demo state
./run.sh test
./run.sh compile-demo
```

다른 JDK를 직접 지정하려면 `LECTURE_JAVA_HOME`에 해당 JDK의 홈 디렉터리를 지정합니다. 지정하지 않으면 터미널의 `java`, `javac`를 사용합니다.

Windows PowerShell에서는 다음 명령으로 주요 예제와 검증을 실행할 수 있습니다.

```powershell
New-Item -ItemType Directory -Force build/classes
javac -encoding UTF-8 --release 21 -d build/classes src/*.java test/Checks.java
java -cp build/classes Demo outcomes
java -cp build/classes Checks
```

컴파일 비교는 `run.sh compile-demo`에서 의도적으로 실패하는 파일과 수정한 파일을 별도로 컴파일합니다. `variants/missing`과 `variants/unpermitted`는 오류를 보여주는 자료이므로 정상 소스와 한 번에 컴파일하지 않습니다. VS Code가 해당 파일의 오류를 표시하는 것도 예상된 동작입니다.

## 결과와 소스

| 실행 | 확인하는 동작 | 예상 결과 |
|---|---|---|
| `demo outcomes` | 성공, 만료, 사용 완료, 두 조건 중복 | 아래 네 안내문 |
| `demo confirm` | 성공한 경우에만 목록에 추가 | 실패 후 각각 0개, 성공 후 `[WELCOME]` |
| `demo flow` | 값 반환과 예외의 다음 실행 위치 | 반환 뒤 출력 실행, 예외 뒤 try 내부 출력 생략 |
| `demo invalid` | 빈 코드 검사 | `IllegalArgumentException: 쿠폰 코드가 필요합니다` |
| `demo ignored` | 반환값을 사용하지 않는 호출 | 컴파일 성공, 다음 줄 실행 안내 |
| `demo state` | 실패 전에 바꾼 상태 | 반환 뒤와 예외 뒤 모두 `[WELCOME]` |
| `test` | 반환값, 메시지, 실패 후 상태와 예외 | `PASS: 24 assertions` |
| `compile-demo` | 빠진 분기와 허용하지 않은 타입, 수정한 분기 | 예상 컴파일 실패 확인 후 두 수정본 실행, 마지막 PASS |

`demo outcomes`의 출력입니다.

```text
적용 완료: WELCOME
사용 기간이 지났습니다
이미 사용한 쿠폰입니다
조건 중복: 사용 기간이 지났습니다
```

`demo flow`의 출력입니다.

```text
결과 반환 방식
사용 기간이 지났습니다
호출 다음 줄 실행
예외 방식
catch: 만료된 쿠폰
try-catch 다음 줄 실행
```

소스는 다음 순서로 읽으시면 흐름을 따라갈 수 있습니다.

- [ApplyResult.java](src/ApplyResult.java): 구현을 허용하는 결과 목록
- [Applied.java](src/Applied.java), [Expired.java](src/Expired.java), [AlreadyUsed.java](src/AlreadyUsed.java): 각 결과 객체
- [CouponService.java](src/CouponService.java): 입력 검사, 결과 반환, 예외 방식 비교
- [CouponScreen.java](src/CouponScreen.java): 안내문 선택과 성공 코드 기록
- [Demo.java](src/Demo.java): 입력과 실행 경로
- [Checks.java](test/Checks.java): 예상값과 실패 후 상태 검증
- [분기가 빠진 코드](variants/missing/Expansion.java), [분기를 보완한 코드](variants/fixed/Expansion.java), [default를 사용한 코드](variants/default/Expansion.java): 새 결과 종류 추가 비교
- [허용하지 않은 구현](variants/unpermitted/Unknown.java): sealed 제한 확인

`expired`와 `used`는 입력으로 전달한 고정 상태입니다. 실제 쿠폰 사용 이력을 조회하거나 변경하지 않으며, 성공한 코드의 기록도 메모리의 문자열 목록입니다. 두 상태가 모두 참이면 먼저 검사하는 만료를 반환합니다. 쿠폰 코드가 `null`, 빈 문자열 또는 공백 문자열이면 입력 조건 위반 예외를 던집니다.

이 구조는 Java 문법으로 직접 만든 결과 타입입니다. 반환 타입 선언이 예외를 자동으로 변환하거나 변경한 상태를 되돌리지는 않습니다. `record`의 참조 필드가 `final`이어도 참조한 가변 객체 내부까지 불변으로 바뀌지는 않습니다. 이 예제의 성공 데이터는 `String`입니다.
