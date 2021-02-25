package common.utils.file;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;

public class PathIterable implements Iterable<Path> {
    public PathIterable(Path rootFolder) {

    }

    @NotNull
    @Override
    public Iterator<Path> iterator() {
        return Collections.emptyListIterator();
    }
}
