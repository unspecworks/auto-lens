package works.unspec.autolens;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class LensShare {
    static final String LENS_PACKAGE = "com.google.ar.lens";
    static final String GOOGLE_PACKAGE = "com.google.android.googlequicksearchbox";

    static Intent imageIntent(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_SEND)
                .setType("image/png")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClipData(ClipData.newRawUri("Screenshot", uri));
        return intent;
    }

    static int open(Context context, Uri uri) {
        Intent intent = imageIntent(uri);
        List<ResolveInfo> handlers = new ArrayList<>();
        for (String pkg : new String[]{LENS_PACKAGE, GOOGLE_PACKAGE}) {
            handlers.addAll(context.getPackageManager().queryIntentActivities(
                    new Intent(intent).setPackage(pkg), PackageManager.MATCH_DEFAULT_ONLY));
        }
        List<ComponentName> targets = targets(handlers);
        for (ComponentName component : targets) {
            try {
                context.startActivity(new Intent(intent).setComponent(component));
                return 0;
            } catch (ActivityNotFoundException | SecurityException exception) {
                // Try the next actual exported Lens receiver, if available.
            }
        }
        return targets.isEmpty() ? R.string.lens_missing : R.string.share_failed;
    }

    static List<ComponentName> targets(List<ResolveInfo> handlers) {
        List<ComponentName> results = new ArrayList<>();
        for (ResolveInfo handler : handlers) {
            ActivityInfo info = handler.activityInfo;
            if (info == null || !info.exported || !info.enabled || info.name == null) continue;
            if (LENS_PACKAGE.equals(info.packageName)
                    || (GOOGLE_PACKAGE.equals(info.packageName)
                    && info.name.toLowerCase(Locale.ROOT).contains("lens"))) {
                ComponentName component = new ComponentName(info.packageName, info.name);
                if (!results.contains(component)) results.add(component);
            }
        }
        results.sort(Comparator.comparingInt(name -> LENS_PACKAGE.equals(name.getPackageName()) ? 0 : 1));
        return results;
    }
}
