public final class CouponService {
    public ApplyResult apply(String code, boolean expired, boolean used) {
        requireCode(code);
        if (expired) return new Expired();
        if (used) return new AlreadyUsed();
        return new Applied(code);
    }

    public String applyOrThrow(String code, boolean expired, boolean used) {
        requireCode(code);
        if (expired) throw new IllegalStateException("만료된 쿠폰");
        if (used) throw new IllegalStateException("이미 사용한 쿠폰");
        return code;
    }

    private void requireCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("쿠폰 코드가 필요합니다");
        }
    }
}
