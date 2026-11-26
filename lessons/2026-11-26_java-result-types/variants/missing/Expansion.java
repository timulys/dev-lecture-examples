public final class Expansion {
    sealed interface Result permits Applied, Expired, AlreadyUsed, Blocked { }
    record Applied(String code) implements Result { }
    record Expired() implements Result { }
    record AlreadyUsed() implements Result { }
    record Blocked() implements Result { }

    static String message(Result result) {
        return switch (result) {
            case Applied a -> "적용 완료: " + a.code();
            case Expired e -> "사용 기간이 지났습니다";
            case AlreadyUsed u -> "이미 사용한 쿠폰입니다";
        };
    }
}
