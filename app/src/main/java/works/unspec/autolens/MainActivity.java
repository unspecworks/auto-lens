package works.unspec.autolens;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

public final class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long connectDeadline;
    private boolean launched;
    private boolean showingSetup;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
    }

    @Override protected void onResume() {
        super.onResume();
        if (!launched) {
            connectDeadline = SystemClock.uptimeMillis() + 3000;
            tryCapture();
        }
    }

    private void tryCapture() {
        if (isFinishing() || launched) return;
        CaptureService service = CaptureService.connectedInstance();
        if (service != null) {
            launched = true;
            // No UI remains on screen when the delayed screenshot is taken.
            service.requestCapture();
            finishAndRemoveTask();
            return;
        }
        if (isServiceEnabled() && SystemClock.uptimeMillis() < connectDeadline) {
            handler.postDelayed(this::tryCapture, 100);
            return;
        }
        showSetup(isServiceEnabled());
    }

    private boolean isServiceEnabled() {
        AccessibilityManager manager = getSystemService(AccessibilityManager.class);
        ComponentName expected = new ComponentName(this, CaptureService.class);
        for (AccessibilityServiceInfo info : manager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            if (expected.equals(ComponentName.unflattenFromString(info.getId()))) return true;
        }
        return false;
    }

    private void showSetup(boolean connectionFailed) {
        if (showingSetup) return;
        showingSetup = true;
        ScreenUi ui = new ScreenUi(this);
        ui.eyebrow("AUTO LENS  /  01");
        ui.title(R.string.setup_title);
        ui.body(R.string.setup_subtitle);
        ui.heading(R.string.permission_title);
        ui.body(R.string.permission_body);
        if (connectionFailed) ui.body(R.string.connection_failed);
        ui.button(R.string.permission_button, view -> openAccessibilitySettings());
        ui.body(R.string.usage_hint);
        ui.caption(R.string.restricted_hint);
        ui.button(R.string.cancel, view -> finishAndRemoveTask());
        ui.show();
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            android.widget.Toast.makeText(this, R.string.settings_unavailable,
                    android.widget.Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onPause() {
        handler.removeCallbacksAndMessages(null);
        super.onPause();
    }
}
