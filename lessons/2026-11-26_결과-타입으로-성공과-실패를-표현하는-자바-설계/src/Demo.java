public final class Demo {
    public static void main(String[] args) {
        String mode = args.length == 0 ? "outcomes" : args[0];
        CouponService service = new CouponService();
        CouponScreen screen = new CouponScreen();
        switch (mode) {
            case "outcomes" -> {
                System.out.println(screen.message(service.apply("WELCOME", false, false)));
                System.out.println(screen.message(service.apply("WELCOME", true, false)));
                System.out.println(screen.message(service.apply("WELCOME", false, true)));
                System.out.println("조건 중복: " + screen.message(service.apply("WELCOME", true, true)));
            }
            case "flow" -> {
                System.out.println("결과 반환 방식");
                ApplyResult result = service.apply("WELCOME", true, false);
                System.out.println(screen.message(result));
                System.out.println("호출 다음 줄 실행");
                System.out.println("예외 방식");
                try {
                    service.applyOrThrow("WELCOME", true, false);
                    System.out.println("호출 다음 줄 실행");
                } catch (IllegalStateException e) {
                    System.out.println("catch: " + e.getMessage());
                }
                System.out.println("try-catch 다음 줄 실행");
            }
            case "confirm" -> {
                screen.confirm(service.apply("WELCOME", true, false));
                System.out.println("만료 후 기록 수: " + screen.confirmedCodes().size());
                screen.confirm(service.apply("WELCOME", false, true));
                System.out.println("사용 완료 후 기록 수: " + screen.confirmedCodes().size());
                screen.confirm(service.apply("WELCOME", false, false));
                System.out.println("성공 후 기록: " + screen.confirmedCodes());
            }
            case "state" -> {
                java.util.List<String> records = new java.util.ArrayList<>();
                records.add("WELCOME");
                service.apply("WELCOME", true, false);
                System.out.println("실패 반환 뒤 기록: " + records);
                try {
                    service.applyOrThrow("WELCOME", true, false);
                } catch (IllegalStateException e) {
                    System.out.println("예외 처리 뒤 기록: " + records);
                }
            }
            case "invalid" -> {
                try {
                    service.apply("", false, false);
                    throw new AssertionError("예외가 발생해야 합니다");
                } catch (IllegalArgumentException e) {
                    System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
            case "ignored" -> {
                service.apply("WELCOME", true, false);
                System.out.println("결과를 사용하지 않아도 컴파일되고 다음 줄이 실행됩니다");
            }
            default -> throw new IllegalArgumentException("알 수 없는 실행 모드: " + mode);
        }
    }
}
