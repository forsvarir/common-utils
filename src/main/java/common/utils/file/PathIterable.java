package common.utils.file;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class PathIterable implements Iterable<Path> {
    private final Path rootFolder;
    private final Path fileToResumeAfter;

    public PathIterable(Path rootFolder) {
        this.rootFolder = rootFolder;
        fileToResumeAfter = rootFolder;
    }

    public PathIterable(Path rootFolder, Path fileToResumeAfter) {
        this.rootFolder = rootFolder;
        this.fileToResumeAfter = fileToResumeAfter;
    }

    @NotNull
    @Override
    public Iterator<Path> iterator() {
        return new PathIterator(rootFolder, fileToResumeAfter);
    }

    static class PathIterator implements Iterator<Path> {

        private List<Path> toProcess;

        private PathIterator(Path rootFolder, Path fileToResumeAfter) {
            setupIterationFromStartPosition(rootFolder, fileToResumeAfter);
        }

        private void setupIterationFromStartPosition(Path rootFolder, Path fileToResumeAfter) {
            try {
                toProcess = Files.list(rootFolder).collect(Collectors.toList());

                if (!rootFolder.equals(fileToResumeAfter)) {
                    skipFilesUntil(fileToResumeAfter);
                }
            } catch (IOException e) {
                toProcess = Collections.emptyList();
                e.printStackTrace();
            }
        }

        private void skipFilesUntil(Path fileToResumeAfter) {
            while (hasNext() && !next().equals(fileToResumeAfter)) {

            }
        }

        @Override
        public boolean hasNext() {
            return !toProcess.isEmpty();
        }

        @Override
        public Path next() {
            var firstItem = toProcess.get(0);
            toProcess.remove(0);
            return firstItem;
        }
    }
}
