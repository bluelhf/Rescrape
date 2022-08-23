package blue.lhf.rescrape;

import java.io.PrintStream;
import java.util.*;

public class LogView {
    private final List<Slot> slots = new ArrayList<>();

    private void ensureIndex(final int index) {
        while (slots.size() <= index)
            slots.add(new Slot());
    }

    public Slot slot(final int index) {
        synchronized (slots) {
            ensureIndex(index);
            return slots.get(index);
        }
    }

    public Slot slot() {
        synchronized (slots) {
            return slot(slots.size());
        }
    }

    public void print(final PrintStream stream) {
        stream.print("\u001B[2J\u001B[H");
        stream.flush();
        synchronized (slots) {
            for (final Slot s : slots) {
                if (s.line == null) {
                    continue;
                }
                stream.println(s.line);
            }
        }
        stream.flush();
    }

    public void free(Slot slot) {
        slots.remove(slot);
    }

    public static class Slot {
        private String line;

        public String get() {
            return line;
        }

        public void set(final String newLine) {
            this.line = newLine;
        }
    }
}
