package com.forsvarir.common.utils.file;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class FileWalkerTest {
    @Test
    void walk_root_returnsStream() {
        ResumableFileIterable mockIterable = Mockito.mock(ResumableFileIterable.class);
        List<Path> knownList = Arrays.asList(Path.of("item1"), Path.of("item2"), Path.of("item3"));
        when(mockIterable.spliterator()).thenReturn(knownList.spliterator());

        Stream<Path> items = FileWalker.walk(Path.of("/tmp/nowhere"), (r,f) -> mockIterable);

        assertThat(items).containsExactly(knownList.get(0), knownList.get(1), knownList.get(2));
    }

    @Test
    void walk_root_passesPathToIterable() {
        ResumableFileIterable mockIterable = Mockito.mock(ResumableFileIterable.class);
        List<Path> parameterList = new ArrayList<>();
        when(mockIterable.spliterator()).thenReturn(parameterList.spliterator());

        Stream<Path> items = FileWalker.walk(Path.of("/tmp/nowhere"), (root, from) ->
        {
            parameterList.add(root);
            parameterList.add(from);
            return mockIterable;
        });

        assertThat(items).containsExactly(Path.of("/tmp/nowhere"), Path.of("/tmp/nowhere"));
    }

    @Test
    void walk_root_from_returnsStream() {
        ResumableFileIterable mockIterable = Mockito.mock(ResumableFileIterable.class);
        List<Path> knownList = Arrays.asList(Path.of("item1"), Path.of("item2"), Path.of("item3"));
        when(mockIterable.spliterator()).thenReturn(knownList.spliterator());

        Stream<Path> items = FileWalker.walk(Path.of("/tmp/nowhere"), Path.of("/tmp/from"), (r, f) -> mockIterable);

        assertThat(items).containsExactly(knownList.get(0), knownList.get(1), knownList.get(2));
    }

    @Test
    void walk_root_from_passesPathsToIterable() {
        ResumableFileIterable mockIterable = Mockito.mock(ResumableFileIterable.class);
        List<Path> parameterList = new ArrayList<>();
        when(mockIterable.spliterator()).thenReturn(parameterList.spliterator());

        Stream<Path> items = FileWalker.walk(Path.of("/tmp/nowhere"),
                Path.of("/tmp/from"),
                (root, from) ->
                {
                    parameterList.add(root);
                    parameterList.add(from);
                    return mockIterable;
                });

        assertThat(items).containsExactly(Path.of("/tmp/nowhere"), Path.of("/tmp/from"));
    }
}