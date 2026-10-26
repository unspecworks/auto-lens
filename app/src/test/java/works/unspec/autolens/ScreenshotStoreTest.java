package works.unspec.autolens;

import static org.junit.Assert.*;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.core.content.FileProvider;
import org.junit.Rule;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import java.io.File;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class ScreenshotStoreTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Before public void resetProviderRoots() {
        // Robolectric changes cacheDir per test; AndroidX keeps its path map static.
        // A real app process has one stable cacheDir, so clear only in the test harness.
        Map<?, ?> cache = ReflectionHelpers.getStaticField(FileProvider.class, "sCache");
        cache.clear();
    }

    @Test public void cleanupRemovesExpiredImagesAndKeepsFourRecentImages() throws Exception {
        File directory = temporary.newFolder();
        long now = System.currentTimeMillis();
        for (int i = 0; i < 6; i++) {
            File file = new File(directory, i + ".png");
            assertTrue(file.createNewFile());
            assertTrue(file.setLastModified(now - i * 1000));
        }
        File expired = new File(directory, "expired.png");
        assertTrue(expired.createNewFile());
        assertTrue(expired.setLastModified(now - 25 * 60 * 60 * 1000L));
        ScreenshotStore.prune(directory, now);
        assertEquals(4, directory.listFiles().length);
        assertTrue(new File(directory, "0.png").exists());
        assertFalse(expired.exists());
    }

    @Test public void savedScreenshotIsReadableThroughPrivateProvider() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        Bitmap bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888);
        Uri uri = ScreenshotStore.save(context, bitmap);
        bitmap.recycle();
        assertEquals("content", uri.getScheme());
        assertEquals("works.unspec.autolens.screenshots", uri.getAuthority());
        try (java.io.InputStream stream = context.getContentResolver().openInputStream(uri)) {
            assertNotNull(stream);
            assertTrue(stream.read() >= 0);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void providerDoesNotExposeOtherCacheFiles() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        File outside = new File(context.getCacheDir(), "private.txt");
        FileProvider.getUriForFile(context, "works.unspec.autolens.screenshots", outside);
    }
}
