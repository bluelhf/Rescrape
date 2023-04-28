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
}
