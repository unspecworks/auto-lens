package works.unspec.autolens;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public final class ErrorActivity extends Activity {
    static final String EXTRA_MESSAGE = "message";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        int message = getIntent().getIntExtra(EXTRA_MESSAGE, R.string.capture_failed);
        ScreenUi ui = new ScreenUi(this);
        ui.eyebrow("AUTO LENS");
        ui.title(R.string.error_title);
        ui.body(message);
        if (message == R.string.lens_missing || message == R.string.share_failed) {
            ui.button(R.string.lens_store, view -> openStore());
        }
        ui.button(R.string.close, view -> finishAndRemoveTask());
        ui.show();
    }

    private void openStore() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://play.google.com/store/apps/details?id=" + LensShare.LENS_PACKAGE)));
            finishAndRemoveTask();
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.store_unavailable, Toast.LENGTH_LONG).show();
        }
    }
}
