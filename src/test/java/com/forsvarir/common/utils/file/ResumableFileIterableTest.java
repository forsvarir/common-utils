package com.forsvarir.common.utils.file;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

class ResumableFileIterableTest {
    FileSystem dummyFileSystem = Jimfs.newFileSystem(Configuration.unix());

    @Test
    void emptyFolder_nothingToIterate() throws IOException {
        final Path rootFolder = asPath("/emptyFolder");
        Files.createDirectory(rootFolder);

        var iterable = new ResumableFileIterable(rootFolder);
        var filesToIterate = StreamSupport.stream(iterable.spliterator(), false);

        assertThat(filesToIterate).isEmpty();
    }

    @Test
    void resumeFromLastFile_nothingToIterate() throws IOException {
        final Path rootFolder = asPath("/folder");
        Files.createDirectory(rootFolder);
        final Path lastFile = asPath("/folder/file.txt");
        Files.createFile(lastFile);

        var iterable = new ResumableFileIterable(rootFolder, lastFile);
        var filesToIterate = StreamSupport.stream(iterable.spliterator(), false);

        assertThat(filesToIterate).isEmpty();
    }

    @Test
    void folderWithSingleFile_returnsSingleFile() throws IOException {
        final Path rootFolder = asPath("/folder");
        Files.createDirectory(rootFolder);
        final Path lastFile = asPath("/folder/file.txt");
        Files.createFile(lastFile);

        var iterable = new ResumableFileIterable(rootFolder);
        var filesToIterate = StreamSupport.stream(iterable.spliterator(), false);

        assertThat(filesToIterate.map(Path::toString)).containsExactly("/folder/file.txt");
    }

    @Test
    void folderWithSingleFile_resumeFromNull_returnsSingleFile() throws IOException {
        final Path rootFolder = asPath("/folder");
        Files.createDirectory(rootFolder);
        final Path lastFile = asPath("/folder/file.txt");
        Files.createFile(lastFile);

        var iterable = new ResumableFileIterable(rootFolder, null);
        var filesToIterate = StreamSupport.stream(iterable.spliterator(), false);

        assertThat(filesToIterate.map(Path::toString)).containsExactly("/folder/file.txt");
    }

    @Test
    void folderWithSingleFile_resumeFromRoot_returnsSingleFile() throws IOException {
        final Path rootFolder = asPath("/folder");
        Files.createDirectory(rootFolder);
        final Path lastFile = asPath("/folder/file.txt");
        Files.createFile(lastFile);

        var iterable = new ResumableFileIterable(rootFolder, rootFolder);
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

        var iterable = new ResumableFileIterable(rootFolder, rootFolder);
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

        var iterable = new ResumableFileIterable(rootFolder, asPath("/folder/file2.txt"));
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

        var iterable = new ResumableFileIterable(rootFolder, asPath("/folder/file2.txt"));
        var filesToIterate = StreamSupport.stream(iterable.spliterator(), false);

        assertThat(filesToIterate.map(Path::toString)).containsExactly(
                "/folder/file3.txt",
                "/folder/file4.txt"
        );
    }

    @Test
    void nestedFolderAndFile_folderIgnoredReturnsFile() throws IOException {
        createDirectories(Arrays.asList(
                "/root",
                "/root/nestedFolder"
        ));
        createFiles(Collections.singletonList(
                "/root/file1.txt"));

        var iterable = new ResumableFileIterable(asPath("/root"));
        var filesToIterate = StreamSupport.stream(iterable.spliterator(), false);

        assertThat(filesToIterate.map(Path::toString)).containsExactly(
                "/root/file1.txt"
        );
    }

    @Test
    void nestedFolderContainingOneFile_returnsFile() throws IOException {
        createDirectories(Arrays.asList(
                "/root",
                "/root/nestedFolder"
        ));
        createFiles(Collections.singletonList(
                "/root/nestedFolder/file1.txt"));

        var iterable = new ResumableFileIterable(asPath("/root"));
        var filesToIterate = StreamSupport.stream(iterable.spliterator(), false);

        assertThat(filesToIterate.map(Path::toString)).containsExactly(
                "/root/nestedFolder/file1.txt"
        );
    }

    @Test
    void nestedFolderContainingTwoFilesResumedFromFirstFile_returnsRemainingFile() throws IOException {
        createDirectories(Arrays.asList(
                "/root",
                "/root/nestedFolder"
        ));
        createFiles(Arrays.asList(
                "/root/nestedFolder/file1.txt",
                "/root/nestedFolder/file2.txt"
        ));

        var iterable = new ResumableFileIterable(asPath("/root"), asPath("/root/nestedFolder/file1.txt"));
        var filesToIterate = StreamSupport.stream(iterable.spliterator(), false);

        assertThat(filesToIterate.map(Path::toString)).containsExactly(
                "/root/nestedFolder/file2.txt"
        );
    }

    @Test
    void linkedFolder_returnsNonLinkedFiles() throws IOException {
        createDirectories(Arrays.asList(
                "/root",
                "/linkedToFolder"
        ));
        createFiles(Arrays.asList(
                "/root/file1.txt",
                "/linkedToFolder/file2.txt"
        ));

        Files.createSymbolicLink(asPath("/root/linkFromFolder"),
                asPath("/linkedToFolder"));

        var iterable = new ResumableFileIterable(asPath("/root"));
        var filesToIterate = StreamSupport.stream(iterable.spliterator(), false);

        assertThat(filesToIterate.map(Path::toString)).containsExactly("/root/file1.txt");
    }

    @Test
    void nestedLinkedFolder_returnsNonLinkedFiles() throws IOException {
        createDirectories(Arrays.asList(
                "/root",
                "/root/nestedFolder",
                "/linkedToFolder"
        ));
        createFiles(Arrays.asList(
                "/root/file1.txt",
                "/linkedToFolder/file3.txt"
        ));

        Files.createSymbolicLink(asPath("/root/nestedFolder/linkFromFolder"),
                asPath("/linkedToFolder"));

        var iterable = new ResumableFileIterable(asPath("/root"));
        var filesToIterate = StreamSupport.stream(iterable.spliterator(), false);

        assertThat(filesToIterate.map(Path::toString)).containsExactly("/root/file1.txt");
    }

    @Test
    void linkedFolderWithFilesAfter_returnsNonLinkedFiles() throws IOException {
        createDirectories(Arrays.asList(
                "/root",
                "/linkedToFolder",
                "/root/zzLastFolder"
        ));
        createFiles(Arrays.asList(
                "/root/file1.txt",
                "/linkedToFolder/file2.txt",
                "/root/zzLastFolder/file3.txt"
        ));

        Files.createSymbolicLink(asPath("/root/linkFromFolder"),
                asPath("/linkedToFolder"));

        var iterable = new ResumableFileIterable(asPath("/root"));
        var filesToIterate = StreamSupport.stream(iterable.spliterator(), false);

        assertThat(filesToIterate.map(Path::toString)).containsExactly(
                "/root/file1.txt",
                "/root/zzLastFolder/file3.txt");
    }

    @Test
    @Disabled("JimFS doesn't support permissions")
    void unReadableFolder_returnsReadableFiles() throws IOException {
        createDirectories(Arrays.asList(
                "/root",
                "/root/unReadableFolder"
        ));
        createFiles(Arrays.asList(
                "/root/file1.txt",
                "/root/unReadableFolder/file2.txt"
        ));

        Files.setPosixFilePermissions(asPath("/root/unReadableFolder"), Collections.emptySet());

        var iterable = new ResumableFileIterable(asPath("/root"));
        var filesToIterate = StreamSupport.stream(iterable.spliterator(), false);

        assertThat(filesToIterate.map(Path::toString)).containsExactly("/root/file1.txt");
    }

    @NotNull
    private Path asPath(String path) {
        return dummyFileSystem.getPath(path);
    }

    private void createDirectories(List<String> foldersToCreate) throws IOException {
        for (var path : foldersToCreate) {
            Files.createDirectory(asPath(path));
        }
    }

    private void createFiles(List<String> pathsToCreate) throws IOException {
        for (var path : pathsToCreate) {
            Files.createFile(asPath(path));
        }
    }
}