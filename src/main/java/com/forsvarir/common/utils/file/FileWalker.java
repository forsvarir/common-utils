package com.forsvarir.common.utils.file;

import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class FileWalker {
    public static Stream<Path> walk(Path root) {
        return walk(root, ResumableFileIterable::new);
    }

    public static Stream<Path> walk(Path root, Path from) {
        return walk(root, from, ResumableFileIterable::new);
    }

    static Stream<Path> walk(Path root, BiFunction<Path, Path, ResumableFileIterable> iterableSupplier) {
        return StreamSupport.stream(iterableSupplier.apply(root, root).spliterator(), false);
    }

    static Stream<Path> walk(Path root, Path from, BiFunction<Path, Path, ResumableFileIterable> iterableSupplier) {
        return StreamSupport.stream(iterableSupplier.apply(root, from).spliterator(), false);
    }
}
