package works.unspec.autolens;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.HardwareBuffer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CaptureService extends AccessibilityService {
    private static CaptureService instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private boolean busy;
    private int generation;

    static CaptureService connectedInstance() { return instance; }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    // Called only by our own foreground Activity, never by accessibility events.
    void requestCapture() {
        if (busy) return;
        busy = true;
        int request = ++generation;
        handler.postDelayed(() -> capture(request), 1000);
        handler.postDelayed(() -> fail(request, R.string.capture_timeout), 10000);
    }

    private boolean active(int request) { return busy && request == generation && instance == this; }

    private void capture(int request) {
        if (!active(request)) return;
        if (getSystemService(KeyguardManager.class).isKeyguardLocked()) {
            fail(request, R.string.capture_failed);
            return;
        }
        try {
            takeScreenshot(Display.DEFAULT_DISPLAY, getMainExecutor(), new TakeScreenshotCallback() {
                @Override public void onSuccess(ScreenshotResult result) {
                    if (!active(request)) {
                        result.getHardwareBuffer().close();
                        return;
                    }
                    worker.execute(() -> save(request, result));
                }

                @Override public void onFailure(int error) {
                    fail(request, error == ERROR_TAKE_SCREENSHOT_SECURE_WINDOW
                            ? R.string.secure_screen : R.string.capture_failed);
                }
            });
        } catch (RuntimeException exception) {
            fail(request, R.string.capture_failed);
        }
    }

    private void save(int request, ScreenshotResult result) {
        Bitmap hardwareBitmap = null;
        Bitmap softwareBitmap = null;
        try (HardwareBuffer buffer = result.getHardwareBuffer()) {
            hardwareBitmap = Bitmap.wrapHardwareBuffer(buffer, result.getColorSpace());
            if (hardwareBitmap == null) throw new IllegalStateException("No bitmap");
            softwareBitmap = hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false);
            if (softwareBitmap == null) throw new IllegalStateException("No bitmap copy");
            Uri uri = ScreenshotStore.save(this, softwareBitmap);
            handler.post(() -> share(request, uri));
        } catch (Exception exception) {
            handler.post(() -> fail(request, R.string.save_failed));
        } finally {
            if (softwareBitmap != null) softwareBitmap.recycle();
            if (hardwareBitmap != null) hardwareBitmap.recycle();
        }
    }

    private void share(int request, Uri uri) {
        if (!active(request)) return;
        int error = LensShare.open(this, uri);
        if (error != 0) {
            fail(request, error);
        } else {
            complete();
        }
    }

    private void fail(int request, int message) {
        if (!active(request)) return;
        complete();
        Intent intent = new Intent(this, ErrorActivity.class)
                .putExtra(ErrorActivity.EXTRA_MESSAGE, message)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void complete() {
        busy = false;
        handler.removeCallbacksAndMessages(null);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        // Intentionally unused: no window contents or gestures are collected.
    }

    @Override public void onInterrupt() {
        ++generation;
        complete();
    }

    @Override public void onDestroy() {
        if (instance == this) instance = null;
        ++generation;
        complete();
        worker.shutdown();
        super.onDestroy();
    }
}
