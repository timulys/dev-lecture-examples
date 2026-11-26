import java.util.ArrayList;
import java.util.List;

public final class CouponScreen {
    private final List<String> confirmedCodes = new ArrayList<>();

    public String message(ApplyResult result) {
        return switch (result) {
            case Applied a -> "적용 완료: " + a.code();
            case Expired e -> "사용 기간이 지났습니다";
            case AlreadyUsed u -> "이미 사용한 쿠폰입니다";
        };
    }

    public void confirm(ApplyResult result) {
        switch (result) {
            case Applied a -> confirmedCodes.add(a.code());
            case Expired e -> { }
            case AlreadyUsed u -> { }
        }
    }

    public List<String> confirmedCodes() {
        return List.copyOf(confirmedCodes);
    }
}
