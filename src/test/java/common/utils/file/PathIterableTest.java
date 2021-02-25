package common.utils.file;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

class PathIterableTest {
    FileSystem dummyFileSystem = Jimfs.newFileSystem(Configuration.unix());

    @Test
    void emptyFolder_nothingToIterate() throws IOException {
        final Path rootFolder = dummyFileSystem.getPath("/emptyFolder");
        Files.createDirectory(rootFolder);

        var iterable = new PathIterable(rootFolder);
        var files = StreamSupport.stream(iterable.spliterator(), false);

        assertThat(files).isEmpty();
    }
}