import java.util.List;
import java.util.Objects;

public final class Checks {
    private static int assertions;
    private static void equal(Object expected, Object actual, String label) {
        assertions++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": expected=" + expected + ", actual=" + actual);
        }
    }
    private static <T extends Throwable> void raises(Class<T> type, Runnable action, String label) {
        assertions++;
        try { action.run(); }
        catch (Throwable e) {
            if (type.isInstance(e)) return;
            throw new AssertionError(label + ": unexpected exception", e);
        }
        throw new AssertionError(label + ": exception missing");
    }
    public static void main(String[] args) {
        CouponService service = new CouponService();
        CouponScreen screen = new CouponScreen();
        equal(new Applied("WELCOME"), service.apply("WELCOME", false, false), "성공과 코드 보존");
        equal(new Expired(), service.apply("WELCOME", true, false), "만료");
        equal(new AlreadyUsed(), service.apply("WELCOME", false, true), "사용 완료");
        equal(new Expired(), service.apply("WELCOME", true, true), "만료 우선");
        equal("적용 완료: WELCOME", screen.message(new Applied("WELCOME")), "성공 안내");
        equal("사용 기간이 지났습니다", screen.message(new Expired()), "만료 안내");
        equal("이미 사용한 쿠폰입니다", screen.message(new AlreadyUsed()), "사용 완료 안내");
        screen.confirm(service.apply("WELCOME", true, false));
        equal(List.of(), screen.confirmedCodes(), "만료 후 상태");
        screen.confirm(service.apply("WELCOME", false, true));
        equal(List.of(), screen.confirmedCodes(), "사용 완료 후 상태");
        screen.confirm(service.apply("WELCOME", false, false));
        equal(List.of("WELCOME"), screen.confirmedCodes(), "성공 후 상태");
        int[] reached = {0};
        service.apply("WELCOME", true, false);
        reached[0]++;
        equal(1, reached[0], "실패 값 반환 후 다음 문장");
        try {
            service.applyOrThrow("WELCOME", true, false);
            reached[0]++;
        } catch (IllegalStateException e) {
            equal("만료된 쿠폰", e.getMessage(), "예외 안내");
        }
        equal(1, reached[0], "throw 뒤 try 내부 문장 건너뜀");
        equal("WELCOME", service.applyOrThrow("WELCOME", false, false), "예외 방식 성공");
        raises(IllegalStateException.class, () -> service.applyOrThrow("WELCOME", false, true), "사용 완료 예외");
        raises(IllegalArgumentException.class, () -> service.apply("", false, false), "빈 코드");
        raises(IllegalArgumentException.class, () -> service.apply("  ", false, false), "공백 코드");
        raises(IllegalArgumentException.class, () -> service.apply(null, false, false), "null 코드");
        raises(IllegalArgumentException.class, () -> service.apply("", true, true), "입력 검사 우선");
        raises(NullPointerException.class, () -> screen.message(null), "null은 결과 종류가 아님");
        equal(List.of("WELCOME"), screen.confirmedCodes(), "예외 후 기존 상태 유지");
        java.util.List<String> records = new java.util.ArrayList<>();
        records.add("WELCOME");
        service.apply("WELCOME", true, false);
        equal(List.of("WELCOME"), records, "실패 반환 전 변경 보존");
        raises(IllegalStateException.class, () -> service.applyOrThrow("WELCOME", true, false), "상태 변경 후 예외");
        equal(List.of("WELCOME"), records, "예외 전 변경 보존");
        System.out.println("PASS: " + assertions + " assertions");
    }
}
