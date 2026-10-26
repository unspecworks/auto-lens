package works.unspec.autolens;

import static org.junit.Assert.*;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class LensShareTest {
    @Test public void directLensIsPreferredOverGoogleApp() {
        List<ComponentName> result = LensShare.targets(List.of(
                receiver(LensShare.GOOGLE_PACKAGE, "google.LensActivity", true, true),
                receiver(LensShare.LENS_PACKAGE, "lens.ShareActivity", true, true)));
        assertEquals(2, result.size());
        assertEquals(LensShare.LENS_PACKAGE, result.get(0).getPackageName());
    }

    @Test public void rejectsUnrelatedPrivateAndDisabledReceivers() {
        List<ComponentName> result = LensShare.targets(List.of(
                receiver("other.app", "other.LensActivity", true, true),
                receiver(LensShare.GOOGLE_PACKAGE, "google.SearchActivity", true, true),
                receiver(LensShare.LENS_PACKAGE, "lens.Private", false, true),
                receiver(LensShare.LENS_PACKAGE, "lens.Disabled", true, false)));
        assertTrue(result.isEmpty());
    }

    @Test public void sharingGrantsReadAccessThroughClipDataAndStream() {
        Uri uri = Uri.parse("content://works.unspec.autolens.screenshots/screenshots/test.png");
        Intent intent = LensShare.imageIntent(uri);
        assertEquals(Intent.ACTION_SEND, intent.getAction());
        assertEquals("image/png", intent.getType());
        assertEquals(uri, intent.getParcelableExtra(Intent.EXTRA_STREAM));
        assertEquals(uri, intent.getClipData().getItemAt(0).getUri());
        assertNotEquals(0, intent.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION);
        assertEquals(0, intent.getFlags() & Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    private ResolveInfo receiver(String pkg, String name, boolean exported, boolean enabled) {
        ResolveInfo result = new ResolveInfo();
        result.activityInfo = new ActivityInfo();
        result.activityInfo.packageName = pkg;
        result.activityInfo.name = name;
        result.activityInfo.exported = exported;
        result.activityInfo.enabled = enabled;
        return result;
    }
}
