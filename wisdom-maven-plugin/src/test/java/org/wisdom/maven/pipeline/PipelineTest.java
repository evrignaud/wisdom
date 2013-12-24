package org.wisdom.maven.pipeline;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.logging.Log;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wisdom.maven.Watcher;
import org.wisdom.maven.WatchingException;
import org.wisdom.maven.utils.WatcherUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Check pipeline behavior.
 */
public class PipelineTest {

    public static final File FAKE = new File("target/fake-source");
    public static final File SOURCES = new File(FAKE, "src/main");

    Pipeline pipeline;
    private SpyWatcher textWatcher;
    private SpyWatcher mdWatcher;

    @Before
    public void setUp() throws IOException {
        FileUtils.forceMkdir(SOURCES);
        textWatcher = new SpyWatcher(SOURCES, "txt");
        mdWatcher = new SpyWatcher(SOURCES, "md");
        Mojo mojo = mock(Mojo.class);
        Log log = mock(Log.class);
        when(mojo.getLog()).thenReturn(log);
        pipeline = new Pipeline(mojo, FAKE, Arrays.asList(textWatcher, mdWatcher));
        pipeline.watch();
    }

    @After
    public void tearDown() {
        pipeline.shutdown();
        FileUtils.deleteQuietly(FAKE);
    }

    @Test
    public void testAdditionUpdateAndDeleteOfFile() throws IOException {
        File txt = new File(SOURCES, "touch.txt");
        txt.createNewFile();
        assertThat(txt.isFile()).isTrue();
        waitPullPeriod();
        assertThat(textWatcher.added).containsExactly("touch.txt");
        assertThat(mdWatcher.added).isEmpty();

        FileUtils.touch(txt);
        waitPullPeriod();
        assertThat(textWatcher.added).containsExactly("touch.txt");
        assertThat(textWatcher.updated).containsExactly("touch.txt");

        FileUtils.deleteQuietly(txt);
        waitPullPeriod();
        assertThat(textWatcher.added).containsExactly("touch.txt");
        assertThat(textWatcher.updated).containsExactly("touch.txt");
        assertThat(textWatcher.deleted).containsExactly("touch.txt");
    }

    @Test
    public void testAdditionUpdateAndDeleteOfSubFiles() throws IOException {
        File dir = new File(SOURCES, "foo");
        dir.mkdirs();
        File md = new File(SOURCES, "foo/touch.md");
        md.createNewFile();
        assertThat(md.isFile()).isTrue();
        waitPullPeriod();
        assertThat(mdWatcher.added).containsExactly("touch.md");

        FileUtils.touch(md);
        waitPullPeriod();
        assertThat(mdWatcher.added).containsExactly("touch.md");
        assertThat(mdWatcher.updated).containsExactly("touch.md");

        FileUtils.deleteQuietly(md);
        waitPullPeriod();
        assertThat(mdWatcher.added).containsExactly("touch.md");
        assertThat(mdWatcher.updated).containsExactly("touch.md");
        assertThat(mdWatcher.deleted).containsExactly("touch.md");
    }

    private void waitPullPeriod() {
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class SpyWatcher implements Watcher {

        private final String extension;
        private final File root;
        List<String> added = new ArrayList<>();
        List<String> updated = new ArrayList<>();
        List<String> deleted = new ArrayList<>();

        public SpyWatcher(File root, String extension) {
            this.extension = extension;
            this.root = root;
        }

        @Override
        public boolean accept(File file) {
            return WatcherUtils.isInDirectory(file, root)  && file.getName().endsWith("." + extension);
        }

        @Override
        public boolean fileCreated(File file) throws WatchingException {
            added.add(file.getName());
            return true;
        }

        @Override
        public boolean fileUpdated(File file) throws WatchingException {
            updated.add(file.getName());
            return true;
        }

        @Override
        public boolean fileDeleted(File file) throws WatchingException {
            deleted.add(file.getName());
            return true;
        }
    }
}
