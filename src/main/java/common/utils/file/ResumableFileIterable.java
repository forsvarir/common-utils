package common.utils.file;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class ResumableFileIterable implements Iterable<Path> {
    private final Path rootFolder;
    private final Path fileToResumeAfter;

    public ResumableFileIterable(Path rootFolder) {
        this.rootFolder = rootFolder;
        fileToResumeAfter = rootFolder;
    }

    public ResumableFileIterable(Path rootFolder, Path fileToResumeAfter) {
        this.rootFolder = rootFolder;
        this.fileToResumeAfter = null == fileToResumeAfter ? rootFolder : fileToResumeAfter;
    }

    @NotNull
    @Override
    public Iterator<Path> iterator() {
        return new ResumableFileIterator(rootFolder, fileToResumeAfter);
    }

    static class ResumableFileIterator implements Iterator<Path> {

        private List<Path> toProcess;

        private ResumableFileIterator(Path rootFolder, Path fileToResumeAfter) {
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
            for(var item : toProcess) {
                if(!Files.isDirectory(item)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Path next() {
            var item = toProcess.get(0);
            while(Files.isDirectory(item)) {
                toProcess.remove(0);
                item = toProcess.get(0);
            }
            toProcess.remove(0);
            return item;
        }
    }
}
