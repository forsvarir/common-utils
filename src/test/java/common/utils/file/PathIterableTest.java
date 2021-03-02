package common.utils.file;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

class PathIterableTest {
    FileSystem dummyFileSystem = Jimfs.newFileSystem(Configuration.unix());

    @Test
    void emptyFolder_nothingToIterate() throws IOException {
        final Path rootFolder = asPath("/emptyFolder");
        Files.createDirectory(rootFolder);

        var iterable = new PathIterable(rootFolder);
        var filesToIterate = StreamSupport.stream(iterable.spliterator(), false);

        assertThat(filesToIterate).isEmpty();
    }

    @Test
    void resumeFromLastFile_nothingToIterate() throws IOException {
        final Path rootFolder = asPath("/folder");
        Files.createDirectory(rootFolder);
        final Path lastFile = asPath("/folder/file.txt");
        Files.createFile(lastFile);

        var iterable = new PathIterable(rootFolder, lastFile);
        var filesToIterate = StreamSupport.stream(iterable.spliterator(), false);

        assertThat(filesToIterate).isEmpty();
    }

    @Test
    void folderWithSingleFile_returnsSingleFile() throws IOException {
        final Path rootFolder = asPath("/folder");
        Files.createDirectory(rootFolder);
        final Path lastFile = asPath("/folder/file.txt");
        Files.createFile(lastFile);

        var iterable = new PathIterable(rootFolder);
        var filesToIterate = StreamSupport.stream(iterable.spliterator(), false);

        assertThat(filesToIterate.map(Path::toString)).containsExactly("/folder/file.txt");
    }

    @Test
    void folderWithSingleFile_resumeFromNull_returnsSingleFile() throws IOException {
        final Path rootFolder = asPath("/folder");
        Files.createDirectory(rootFolder);
        final Path lastFile = asPath("/folder/file.txt");
        Files.createFile(lastFile);

        var iterable = new PathIterable(rootFolder, null);
        var filesToIterate = StreamSupport.stream(iterable.spliterator(), false);

        assertThat(filesToIterate.map(Path::toString)).containsExactly("/folder/file.txt");
    }

    @Test
    void folderWithSingleFile_resumeFromRoot_returnsSingleFile() throws IOException {
        final Path rootFolder = asPath("/folder");
        Files.createDirectory(rootFolder);
        final Path lastFile = asPath("/folder/file.txt");
        Files.createFile(lastFile);

        var iterable = new PathIterable(rootFolder, rootFolder);
        var filesToIterate = StreamSupport.stream(iterable.spliterator(), false);

        assertThat(filesToIterate.map(Path::toString)).containsExactly("/folder/file.txt");
    }

    @Test
    void folderWithMultipleFiles_returnsFilesInExpectedOrder() throws IOException {
        final Path rootFolder = asPath("/folder");
        Files.createDirectory(rootFolder);
        createFiles(Arrays.asList(
                "/folder/file1.txt",
                "/folder/file2.txt",
                "/folder/file3.txt"));

        var iterable = new PathIterable(rootFolder, rootFolder);
        var filesToIterate = StreamSupport.stream(iterable.spliterator(), false);

        assertThat(filesToIterate.map(Path::toString)).containsExactly(
                "/folder/file1.txt",
                "/folder/file2.txt",
                "/folder/file3.txt"
        );
    }

    @Test
    void folderWithMultipleFiles_resumeFromSecondFile_returnsFinalFile() throws IOException {
        final Path rootFolder = asPath("/folder");
        Files.createDirectory(rootFolder);
        createFiles(Arrays.asList(
                "/folder/file1.txt",
                "/folder/file2.txt",
                "/folder/file3.txt"));

        var iterable = new PathIterable(rootFolder, asPath("/folder/file2.txt"));
        var filesToIterate = StreamSupport.stream(iterable.spliterator(), false);

        assertThat(filesToIterate.map(Path::toString)).containsExactly(
                "/folder/file3.txt"
        );
    }

    @Test
    void folderWithMultipleFiles_resumeFromSecondFile_returnsRemainingFiles() throws IOException {
        final Path rootFolder = asPath("/folder");
        Files.createDirectory(rootFolder);
        createFiles(Arrays.asList(
                "/folder/file1.txt",
                "/folder/file3.txt",
                "/folder/file2.txt",
                "/folder/file4.txt"));

        var iterable = new PathIterable(rootFolder, asPath("/folder/file2.txt"));
        var filesToIterate = StreamSupport.stream(iterable.spliterator(), false);

        assertThat(filesToIterate.map(Path::toString)).containsExactly(
                "/folder/file3.txt",
                "/folder/file4.txt"
        );
    }

    @NotNull
    private Path asPath(String path) {
        return dummyFileSystem.getPath(path);
    }

    private void createFiles(List<String> pathsToCreate) throws IOException {
        for (var path : pathsToCreate) {
            Files.createFile(asPath(path));
        }
    }
}