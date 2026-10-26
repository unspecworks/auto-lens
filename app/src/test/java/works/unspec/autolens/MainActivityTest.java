package works.unspec.autolens;

import static org.junit.Assert.*;

import android.content.Intent;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {30, 35})
public class MainActivityTest {
    @Test public void missingPermissionShowsDisclosureWithoutOpeningSettingsOrCapturing() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            assertFalse(activity.isFinishing());
            assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
            View root = activity.findViewById(android.R.id.content);
            assertNotNull(findText(root, activity.getString(R.string.permission_body)));
            View consent = findText(root, activity.getString(R.string.permission_button));
            assertNotNull(consent);
            consent.performClick();
            Intent settings = Shadows.shadowOf(activity).getNextStartedActivity();
            assertNotNull(settings);
            assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, settings.getAction());
        }
    }

    private View findText(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findText(group.getChildAt(i), text);
                if (found != null) return found;
            }
        }
        return null;
    }
}
