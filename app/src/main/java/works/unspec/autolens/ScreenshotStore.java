package works.unspec.autolens;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;

final class ScreenshotStore {
    private static final long MAX_AGE_MS = 24 * 60 * 60 * 1000L;

    static Uri save(Context context, Bitmap bitmap) throws IOException {
        File directory = new File(context.getCacheDir(), "screenshots");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Cannot create cache");
        prune(directory, System.currentTimeMillis());
        File file = new File(directory, UUID.randomUUID() + ".png");
        try (FileOutputStream output = new FileOutputStream(file)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) throw new IOException("PNG encoding failed");
        } catch (IOException | RuntimeException exception) {
            file.delete();
            throw exception;
        }
        return FileProvider.getUriForFile(context, context.getPackageName() + ".screenshots", file);
    }

    static void prune(File directory, long now) {
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".png"));
        if (files == null) return;
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        // Keep at most four old images plus the new image. Do not delete the image
        // immediately after sharing: Lens may read the URI asynchronously.
        for (int i = 0; i < files.length; i++) {
            if (i >= 4 || now - files[i].lastModified() > MAX_AGE_MS) files[i].delete();
        }
    }
}
