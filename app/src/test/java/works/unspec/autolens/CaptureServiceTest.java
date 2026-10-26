package works.unspec.autolens;

import static org.junit.Assert.*;

import android.os.Looper;
import android.app.KeyguardManager;
import android.content.Intent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import java.time.Duration;
import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30, 35})
@LooperMode(LooperMode.Mode.PAUSED)
public class CaptureServiceTest {
    private ServiceController<FakeCaptureService> controller;
    private FakeCaptureService service;

    @Before public void connect() {
        controller = Robolectric.buildService(FakeCaptureService.class).create();
        service = controller.get();
        service.onServiceConnected();
    }

    @After public void disconnect() {
        controller.destroy();
    }

    @Test public void captureWaitsOneSecondAndRepeatedLaunchesDoNotDuplicateIt() {
        service.requestCapture();
        service.requestCapture();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(999));
        assertEquals(0, service.screenshots);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1));
        assertEquals(1, service.screenshots);
    }

    @Test public void interruptionCancelsPendingCapture() {
        service.requestCapture();
        service.onInterrupt();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2));
        assertEquals(0, service.screenshots);
    }

    @Test public void accessibilityEventsNeverTriggerScreenshots() {
        service.onAccessibilityEvent(null);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2));
        assertEquals(0, service.screenshots);
    }

    @Test public void lockedDeviceIsNeverCaptured() {
        Shadows.shadowOf(service.getSystemService(KeyguardManager.class)).setKeyguardLocked(true);
        service.requestCapture();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        assertEquals(0, service.screenshots);
        Intent error = Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity();
        assertEquals(R.string.capture_failed, error.getIntExtra(ErrorActivity.EXTRA_MESSAGE, 0));
    }

    @Test public void timeoutShowsErrorAndAllowsNewRequest() {
        service.requestCapture();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10));
        Intent error = Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity();
        assertEquals(R.string.capture_timeout, error.getIntExtra(ErrorActivity.EXTRA_MESSAGE, 0));
        service.requestCapture();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        assertEquals(2, service.screenshots);
    }

    public static class FakeCaptureService extends CaptureService {
        int screenshots;
        @Override public void takeScreenshot(int displayId, Executor executor, TakeScreenshotCallback callback) {
            screenshots++;
        }
    }
}
