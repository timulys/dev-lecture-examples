import java.time.LocalDate;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class TreeMapLesson {
    static LocalDate day(int month, int day) {
        return LocalDate.of(2026, month, day);
    }

    static NavigableMap<LocalDate, Integer> newRates() {
        // slide:setup:start
        NavigableMap<LocalDate, Integer> rates = new TreeMap<>();
        rates.put(LocalDate.of(2026, 6, 1), 15000);
        rates.put(LocalDate.of(2026, 1, 1), 10000);
        rates.put(LocalDate.of(2026, 3, 1), 12000);
        rates.put(LocalDate.of(2026, 9, 1), 18000);
        // slide:setup:end
        return rates;
    }

    static NavigableMap<Integer, Integer> newShipping() {
        NavigableMap<Integer, Integer> shipping = new TreeMap<>();
        shipping.put(1000, 3000);
        shipping.put(3000, 5000);
        shipping.put(5000, 7000);
        return shipping;
    }

    public static void rates() {
        var rates = newRates();
        System.out.println("keys=" + rates.keySet());
        System.out.println("rates=" + rates);
    }

    public static void floor() {
        var rates = newRates();
        // slide:floor:start
        LocalDate requested = LocalDate.of(2026, 4, 15);
        System.out.println(rates.get(requested));
        Map.Entry<LocalDate, Integer> rate = rates.floorEntry(requested);
        if (rate == null) {
            System.out.println("적용할 요금 없음");
        } else {
            System.out.println(rate.getKey() + " / " + rate.getValue());
        }
        // slide:floor:end
    }

    public static void neighbors() {
        var rates = newRates();
        LocalDate exact = day(3, 1);
        System.out.println("lower=" + rates.lowerEntry(exact));
        System.out.println("floor=" + rates.floorEntry(exact));
        System.out.println("ceiling=" + rates.ceilingEntry(exact));
        System.out.println("higher=" + rates.higherEntry(exact));
        System.out.println("before-first=" + rates.floorEntry(day(1, 1).minusDays(1)));
        System.out.println("empty=" + new TreeMap<LocalDate, Integer>().floorEntry(exact));
    }

    public static void shipping() {
        // slide:shipping:start
        NavigableMap<Integer, Integer> shipping = new TreeMap<>();
        shipping.put(1000, 3000);
        shipping.put(3000, 5000);
        shipping.put(5000, 7000);
        System.out.println(shipping.ceilingEntry(1500));
        System.out.println(shipping.ceilingEntry(3000));
        System.out.println(shipping.higherEntry(3000));
        System.out.println(shipping.ceilingEntry(6000));
        // slide:shipping:end
    }

    public static void range() {
        var rates = newRates();
        // slide:range:start
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 9, 1);
        NavigableMap<LocalDate, Integer> window =
                rates.subMap(from, true, to, false);
        System.out.println(window);
        // slide:range:end
        System.out.println("excluded-start=" + rates.subMap(from, false, to, false));
        System.out.println("included-end=" + rates.subMap(from, true, to, true));
    }

    public static void view() {
        var rates = newRates();
        var window = rates.subMap(day(3, 1), true, day(9, 1), false);
        // slide:view:start
        window.put(LocalDate.of(2026, 6, 1), 16000);
        System.out.println(rates.get(LocalDate.of(2026, 6, 1)));
        window.remove(LocalDate.of(2026, 3, 1));
        System.out.println(rates.containsKey(LocalDate.of(2026, 3, 1)));
        rates.put(LocalDate.of(2026, 4, 1), 14000);
        System.out.println(window);
        // slide:view:end
    }

    public static void copy() {
        var rates = newRates();
        var window = rates.subMap(day(3, 1), true, day(9, 1), false);
        try {
            window.put(day(9, 1), 19000);
            throw new AssertionError("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            System.out.println("out-of-range=" + expected.getClass().getSimpleName());
        }
        System.out.println("original-september=" + rates.get(day(9, 1)));
        var copied = new TreeMap<>(window);
        copied.remove(day(3, 1));
        System.out.println("copy=" + copied);
        System.out.println("view=" + window);
        System.out.println("original-march=" + rates.get(day(3, 1)));
    }

    public static void entry() {
        var rates = newRates();
        // slide:entry:start
        Map.Entry<LocalDate, Integer> found =
                rates.floorEntry(LocalDate.of(2026, 4, 15));
        rates.put(LocalDate.of(2026, 3, 1), 13000);
        System.out.println(found.getValue());
        System.out.println(rates.get(found.getKey()));
        // slide:entry:end
        try {
            found.setValue(14000);
            throw new AssertionError("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            System.out.println("setValue=" + expected.getClass().getSimpleName());
        }
        System.out.println("original-after-failure=" + rates.get(day(3, 1)));
    }

    public static void lengthOnly() {
        // slide:length:start
        Comparator<String> byLength = Comparator.comparingInt(String::length);
        NavigableMap<String, Integer> prices = new TreeMap<>(byLength);
        prices.put("pen", 2000);
        prices.put("tea", 3000);
        System.out.println(prices.size());
        System.out.println(prices.get("pen"));
        // slide:length:end
    }

    public static void tieBreak() {
        // slide:tie:start
        Comparator<String> byLengthThenName =
                Comparator.comparingInt(String::length)
                        .thenComparing(Comparator.naturalOrder());
        NavigableMap<String, Integer> prices = new TreeMap<>(byLengthThenName);
        prices.put("pen", 2000);
        prices.put("tea", 3000);
        prices.put("book", 5000);
        System.out.println(prices.keySet());
        System.out.println(prices.get("pen"));
        // slide:tie:end
    }

    static void run(String name) {
        switch (name) {
            case "rates" -> rates();
            case "floor" -> floor();
            case "neighbors" -> neighbors();
            case "shipping" -> shipping();
            case "range" -> range();
            case "view" -> view();
            case "copy" -> copy();
            case "entry" -> entry();
            case "length" -> lengthOnly();
            case "tie" -> tieBreak();
            default -> throw new IllegalArgumentException("Unknown example: " + name);
        }
    }

    public static void main(String[] args) {
        String requested = args.length == 0 ? "demo" : args[0];
        if (requested.equals("demo")) {
            for (String name : new String[]{"rates", "floor", "neighbors", "shipping",
                    "range", "view", "copy", "entry", "length", "tie"}) {
                System.out.println("[" + name + "]");
                run(name);
            }
        } else {
            run(requested);
        }
    }
}
