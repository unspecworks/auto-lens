package works.unspec.autolens;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Small native setup/error screens; no persistent screen is shown on normal launches. */
final class ScreenUi {
    private final Activity activity;
    private final ScrollView scroll;
    private final LinearLayout content;

    ScreenUi(Activity activity) {
        this.activity = activity;
        scroll = new ScrollView(activity);
        scroll.setBackgroundColor(Color.rgb(245, 247, 252));
        scroll.setFillViewport(true);
        content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(28), dp(36), dp(28), dp(24));
        scroll.addView(content);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(
                    android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    void eyebrow(String text) {
        TextView view = text(text, 12, Color.rgb(33, 90, 232), 24);
        view.setLetterSpacing(0.15f);
        view.setTypeface(null, Typeface.BOLD);
    }

    void title(int resource) {
        text(activity.getString(resource), 30, Color.rgb(20, 30, 50), 16)
                .setTypeface(null, Typeface.BOLD);
    }

    void heading(int resource) {
        TextView view = text(activity.getString(resource), 19, Color.rgb(20, 30, 50), 12);
        view.setPadding(0, dp(20), 0, 0);
        view.setTypeface(null, Typeface.BOLD);
    }

    void body(int resource) { body(activity.getString(resource)); }
    void body(String value) { text(value, 16, Color.rgb(65, 78, 99), 20); }
    void caption(int resource) { text(activity.getString(resource), 13, Color.rgb(85, 98, 119), 16); }

    void button(int resource, View.OnClickListener action) {
        Button button = new Button(activity);
        button.setText(resource);
        button.setAllCaps(false);
        button.setMinHeight(dp(52));
        button.setOnClickListener(action);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(16);
        content.addView(button, params);
    }

    void show() { activity.setContentView(scroll); }

    private TextView text(String value, int size, int color, int margin) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setLineSpacing(dp(3), 1.08f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(margin);
        content.addView(view, params);
        return view;
    }

    private int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
}
