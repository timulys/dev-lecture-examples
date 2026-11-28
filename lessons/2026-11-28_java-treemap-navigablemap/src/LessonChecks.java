import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

public class LessonChecks {
    private static int checks;
    static void equal(Object expected, Object actual) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }
    static void throwsType(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("Unexpected exception", error);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
    static void output(String example, String expected) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            TreeMapLesson.run(example);
        } finally { System.setOut(original); }
        equal(expected.strip(), buffer.toString(StandardCharsets.UTF_8).replace("\r\n", "\n").strip());
    }
    public static void main(String[] args) {
        var rates = TreeMapLesson.newRates();
        var jan = TreeMapLesson.day(1, 1);
        var mar = TreeMapLesson.day(3, 1);
        var jun = TreeMapLesson.day(6, 1);
        var sep = TreeMapLesson.day(9, 1);
        equal(List.of(jan, mar, jun, sep), new ArrayList<>(rates.keySet()));
        equal(null, rates.get(TreeMapLesson.day(4, 15)));
        equal(mar, rates.floorKey(TreeMapLesson.day(4, 15)));
        equal(jan, rates.lowerKey(mar));
        equal(mar, rates.floorKey(mar));
        equal(mar, rates.ceilingKey(mar));
        equal(jun, rates.higherKey(mar));
        equal(null, rates.floorEntry(jan.minusDays(1)));
        equal(null, rates.higherEntry(sep));
        equal(null, new TreeMap<LocalDate, Integer>().floorEntry(mar));
        var window = rates.subMap(mar, true, sep, false);
        equal(List.of(mar, jun), new ArrayList<>(window.keySet()));
        equal(List.of(jun), new ArrayList<>(rates.subMap(mar, false, sep, false).keySet()));
        equal(List.of(mar, jun, sep), new ArrayList<>(rates.subMap(mar, true, sep, true).keySet()));
        equal(0, rates.subMap(mar, true, mar, false).size());
        throwsType(IllegalArgumentException.class, () -> rates.subMap(sep, true, mar, false));
        var original = new TreeMap<>(rates);
        throwsType(IllegalArgumentException.class, () -> window.put(sep, 19000));
        equal(original, rates);
        equal(null, window.remove(sep));
        equal(18000, rates.get(sep));
        window.put(jun, 16000);
        equal(16000, rates.get(jun));
        window.remove(mar);
        equal(false, rates.containsKey(mar));
        rates.put(TreeMapLesson.day(4, 1), 14000);
        equal(14000, window.get(TreeMapLesson.day(4, 1)));
        var copy = new TreeMap<>(window);
        copy.clear();
        equal(2, window.size());
        equal(4, rates.size());
        var found = rates.floorEntry(TreeMapLesson.day(4, 15));
        rates.put(found.getKey(), 17000);
        equal(14000, found.getValue());
        throwsType(UnsupportedOperationException.class, () -> found.setValue(18000));
        equal(17000, rates.get(found.getKey()));
        // New map/entry objects retain references to mutable values.
        var objects = new TreeMap<Integer, ArrayList<String>>();
        objects.put(1, new ArrayList<>(List.of("before")));
        var objectCopy = new TreeMap<>(objects);
        var entrySnapshot = objects.floorEntry(1);
        objects.get(1).add("after");
        equal(List.of("before", "after"), objectCopy.get(1));
        equal(List.of("before", "after"), entrySnapshot.getValue());
        throwsType(NullPointerException.class, () -> rates.put(null, 1));
        var shipping = TreeMapLesson.newShipping();
        equal(3000, shipping.ceilingKey(1500));
        equal(5000, shipping.ceilingEntry(1500).getValue());
        equal(3000, shipping.ceilingKey(3000));
        equal(5000, shipping.higherKey(3000));
        equal(null, shipping.ceilingEntry(6000));
        Comparator<String> order = Comparator.comparingInt(String::length)
                .thenComparing(Comparator.naturalOrder());
        for (String a : List.of("pen", "tea", "book", "pen")) {
            for (String z : List.of("pen", "tea", "book", "pen")) equal(a.equals(z), order.compare(a,z)==0);
        }
        output("floor", "null\n2026-03-01 / 12000");
        output("neighbors", "lower=2026-01-01=10000\nfloor=2026-03-01=12000\nceiling=2026-03-01=12000\nhigher=2026-06-01=15000\nbefore-first=null\nempty=null");
        output("shipping", "3000=5000\n3000=5000\n5000=7000\nnull");
        output("range", "{2026-03-01=12000, 2026-06-01=15000}\nexcluded-start={2026-06-01=15000}\nincluded-end={2026-03-01=12000, 2026-06-01=15000, 2026-09-01=18000}");
        output("view", "16000\nfalse\n{2026-04-01=14000, 2026-06-01=16000}");
        output("copy", "out-of-range=IllegalArgumentException\noriginal-september=18000\ncopy={2026-06-01=15000}\nview={2026-03-01=12000, 2026-06-01=15000}\noriginal-march=12000");
        output("entry", "12000\n13000\nsetValue=UnsupportedOperationException\noriginal-after-failure=13000");
        output("length", "1\n3000");
        output("tie", "[pen, tea, book]\n2000");
        System.out.println("PASS: " + checks + " checks");
    }
}
