package forsvarir.common.utils.file;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
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

        final private Deque<Queue<Path>> toProcess = new ArrayDeque<>();

        private ResumableFileIterator(Path rootFolder, Path fileToResumeAfter) {
            setupIterationFromStartPosition(rootFolder, fileToResumeAfter);
        }

        @Override
        public boolean hasNext() {
            Queue<Path> unopenedFolders = new LinkedList<>();
            for (var folderToProcess : toProcess) {
                for (var itemInFolder : folderToProcess) {
                    if (!Files.isDirectory(itemInFolder)) {
                        return true;
                    }
                    if (shouldIterateFolder(itemInFolder)) {
                        unopenedFolders.add(itemInFolder);
                    }
                }
            }
            while (!unopenedFolders.isEmpty()) {
                for (var file : listFilesInFolder((unopenedFolders.poll()))) {
                    if (!Files.isDirectory(file)) {
                        return true;
                    }
                    if (shouldIterateFolder(file)) {
                        unopenedFolders.add(file);
                    }
                }
            }
            return false;
        }

        @Override
        @NotNull
        public Path next() {
            while (!toProcess.isEmpty()) {
                var currentFolder = toProcess.peek();
                if (!currentFolder.isEmpty()) {
                    var currentItem = currentFolder.poll();
                    if (!Files.isDirectory(currentItem)) {
                        return currentItem;
                    }
                    if (shouldIterateFolder(currentItem)) {
                        toProcess.push(listFilesInFolder(currentItem));
                    }
                } else {
                    toProcess.poll();
                }
            }
            throw new RuntimeException("Next called after last item");
        }

        private void setupIterationFromStartPosition(Path rootFolder, Path fileToResumeAfter) {
            toProcess.add(listFilesInFolder(rootFolder));

            if (rootFolder.equals(fileToResumeAfter)) {
                return;
            }

            skipFilesUntil(fileToResumeAfter);
        }

        private void skipFilesUntil(Path fileToResumeAfter) {
            String startFromLocation = fileToResumeAfter.toString();

            while (!toProcess.isEmpty()) {
                var currentFolder = toProcess.peek();
                if (!currentFolder.isEmpty()) {
                    var currentItem = currentFolder.poll();
                    if (startFromLocation.equals(currentItem.toString())) {
                        return;
                    }
                    if (shouldIterateFolder(currentItem) && startFromLocation.startsWith(currentItem.toString())) {
                        toProcess.push(listFilesInFolder(currentItem));
                    }
                } else {
                    toProcess.poll();
                }
            }
        }

        private boolean shouldIterateFolder(Path path) {
            return Files.isDirectory(path) &&
                    !Files.isSymbolicLink(path) &&
                    Files.isReadable(path);
        }

        @NotNull
        private LinkedList<Path> listFilesInFolder(Path folder) {
            try {
                return Files.list(folder).collect(Collectors.toCollection(LinkedList::new));
            } catch (IOException e) {
                throw new RuntimeException("Failed to iterate path: " + folder.toString());
            }
        }
    }
}
