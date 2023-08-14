package blue.lhf.rescrape;

import java.net.URL;
import java.util.*;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.*;

public class SpliteratorUtil {
    public static UnaryOperator<Spliterator<URL>> filter(final Predicate<URL> predicate) {
        return spliterator -> new AbstractSpliterator<>(spliterator.estimateSize(), spliterator.characteristics()) {
            @Override
            public boolean tryAdvance(final Consumer<? super URL> action) {
                return spliterator.tryAdvance((url) -> {
                    if (predicate.test(url)) {
                        action.accept(url);
                    }
                });
            }
        };
    }

    record Pop<T>(T item, boolean isLast) {}

    public static Spliterator<Pop<URL>> pop(final Spliterator<URL> spliterator) {
        return new AbstractSpliterator<>(spliterator.estimateSize(), spliterator.characteristics()) {
            boolean hasNext = false;
            URL next = null;

            {
                advance();
            }

            private boolean advance() {
                if (!spliterator.tryAdvance((url) -> {
                    hasNext = true;
                    next = url;
                })) {
                    hasNext = false;
                    next = null;
                    return false;
                }

                return true;
            }

            @Override
            public boolean tryAdvance(final Consumer<? super Pop<URL>> action) {
                if (hasNext) {
                    final URL item = next;
                    final boolean isLast = !advance();
                    action.accept(new Pop<>(item, isLast));
                    return !isLast;
                }

                return false;
            }
        };
    }

    public static UnaryOperator<Spliterator<URL>> peek(final Consumer<URL> peek) {
        return spliterator -> new AbstractSpliterator<>(spliterator.estimateSize(), spliterator.characteristics()) {
            @Override
            public boolean tryAdvance(final Consumer<? super URL> action) {
                return spliterator.tryAdvance((url) -> {
                    peek.accept(url);
                    action.accept(url);
                });
            }
        };
    }

    public static <A, C> Function<Spliterator<A>, Spliterator<C>> partition(final Function<A, Function<A, C>> key) {
        return spliterator -> new AbstractSpliterator<>(spliterator.estimateSize(), spliterator.characteristics()) {
            @Override
            public boolean tryAdvance(final Consumer<? super C> action) {
                return spliterator.tryAdvance((a) -> {
                    final Function<A, C> consumer = key.apply(a);
                    if (consumer == null) return;

                    action.accept(consumer.apply(a));
                });
            }
        };
    }

    public static <T> Consumer<T> noop() {
        return (t) -> { };
    }

    public static <A, B> Consumer<A> bindR(final BiConsumer<A, B> f, final B b) {
        return (a) -> f.accept(a, b);
    }

    public static <A, B> UnaryOperator<A> bindR(final BiFunction<A, B, A> f, final B b) {
        return (a) -> f.apply(a, b);
    }
}
