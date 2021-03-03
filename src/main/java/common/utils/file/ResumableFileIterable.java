package common.utils.file;

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

        private void setupIterationFromStartPosition(Path rootFolder, Path fileToResumeAfter) {
            try {
                toProcess.add(Files.list(rootFolder).collect(Collectors.toCollection(LinkedList::new)));

                if (!rootFolder.equals(fileToResumeAfter)) {
                    skipFilesUntil(fileToResumeAfter);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void skipFilesUntil(Path fileToResumeAfter) {
            while (hasNext()) {
                if(next().equals(fileToResumeAfter)) {
                    return;
                }
            }
        }

        @Override
        public boolean hasNext() {
            Queue<Path> unopenedFolders = new LinkedList<>();
            for (var folderToProcess : toProcess) {
                for (var itemInFolder : folderToProcess) {
                    if (!Files.isDirectory(itemInFolder)) {
                        return true;
                    } else {
                        if (Files.isDirectory(itemInFolder)) {
                            unopenedFolders.add(itemInFolder);
                        }
                    }
                }
            }
            while (!unopenedFolders.isEmpty()) {
                try {
                    for (var file : Files.list(unopenedFolders.poll()).collect(Collectors.toList())) {
                        if (Files.isDirectory(file)) {
                            unopenedFolders.add(file);
                        } else {
                            return true;
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return false;
        }

        @Override
        public Path next() {
            while (!toProcess.isEmpty()) {
                var currentFolder = toProcess.peek();
                if (!currentFolder.isEmpty()) {
                    var currentItem = currentFolder.poll();
                    if (Files.isDirectory(currentItem)) {
                        try {
                            toProcess.push(Files.list(currentItem).collect(Collectors.toCollection(LinkedList::new)));
                        } catch (IOException e) {
                            System.out.println("failed to iterate path " + currentItem.toString());
                        }
                    } else {
                        return currentItem;
                    }
                } else {
                    toProcess.poll();
                }
            }
            throw new RuntimeException("Next called after last item");
        }
    }
}
