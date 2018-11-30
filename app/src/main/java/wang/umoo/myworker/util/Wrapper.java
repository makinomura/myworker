package wang.umoo.myworker.util;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Wrapper<T> {
    private T value;

    private Wrapper(T value) {
        this.value = value;
    }

    private Wrapper() {
    }

    public static <T> Wrapper<T> of(T value) {
        return new Wrapper<>(value);
    }

    public static <T> Wrapper<T> empty() {
        return new Wrapper<>();
    }

    public T unwrap() {
        return value;
    }

    public void unwrap(Consumer<T> consumer) {
        consumer.accept(value);
    }

    public void wrap(T value) {
        this.value = value;
    }

    public void wrap(Supplier<T> supplier) {
        this.value = supplier.get();
    }
}
