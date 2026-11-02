package com.example.project.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lightweight, asynchronous image loader for loading, downsampling, and caching images
 * from local Android Content URIs, server storage keys, and remote HTTP/HTTPS URLs.
 * Operates without third-party library dependencies.
 */
public class ImageLoader {

    private static final String TAG = "ImageLoader";
    private static volatile ImageLoader instance;

    private final Context appContext;
    private final LruCache<String, Bitmap> memoryCache;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    private ImageLoader(@NonNull Context context) {
        this.appContext = context.getApplicationContext();

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = Math.max(1024, maxMemory / 8);

        this.memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(@NonNull String key, @NonNull Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        this.executorService = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Gets singleton ImageLoader instance.
     *
     * @param context Application or Activity context
     * @return Singleton ImageLoader instance
     */
    public static synchronized ImageLoader getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new ImageLoader(context);
        }
        return instance;
    }

    /**
     * Loads an image asynchronously into an ImageView with placeholder fallback.
     *
     * @param imageView        Target ImageView
     * @param imageUriOrKey    Local Uri string, server storage key, or HTTP URL
     * @param placeholderResId Fallback drawable resource ID
     */
    public void loadImage(@NonNull ImageView imageView,
                          @Nullable String imageUriOrKey,
                          @DrawableRes int placeholderResId) {
        if (placeholderResId != 0) {
            imageView.setImageResource(placeholderResId);
        }

        if (imageUriOrKey == null || imageUriOrKey.trim().isEmpty()) {
            return;
        }

        final String normalized = normalizeUriOrUrl(imageUriOrKey.trim());
        imageView.setTag(normalized);

        // 1. Check in-memory bitmap cache
        Bitmap cached = memoryCache.get(normalized);
        if (cached != null) {
            imageView.setImageBitmap(cached);
            return;
        }

        // 2. Load asynchronously in background thread
        executorService.execute(() -> {
            Bitmap bitmap = null;
            try {
                if (normalized.startsWith("content://") || normalized.startsWith("file://")) {
                    bitmap = loadFromContentUri(Uri.parse(normalized));
                } else if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
                    bitmap = loadFromNetwork(normalized);
                }
            } catch (Throwable t) {
                Log.w(TAG, "Failed to load image from " + normalized + ": " + t.getMessage());
            }

            final Bitmap finalBitmap = bitmap;
            mainHandler.post(() -> {
                if (finalBitmap != null) {
                    memoryCache.put(normalized, finalBitmap);
                    Object currentTag = imageView.getTag();
                    if (normalized.equals(currentTag)) {
                        imageView.setImageBitmap(finalBitmap);
                    }
                } else if (placeholderResId != 0) {
                    Object currentTag = imageView.getTag();
                    if (normalized.equals(currentTag)) {
                        imageView.setImageResource(placeholderResId);
                    }
                }
            });
        });
    }

    /**
     * Normalizes a storage key or URL to a loadable URI/URL.
     * Rewrites localhost/127.0.0.1 to 10.0.2.2 for Android Emulator compatibility.
     *
     * @param input Raw input string
     * @return Normalized URL or Uri string
     */
    @NonNull
    public String normalizeUriOrUrl(@NonNull String input) {
        String trimmed = input.trim();
        if (trimmed.startsWith("content://") || trimmed.startsWith("file://")) {
            return trimmed;
        }

        // If it's already an HTTP URL
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            if (trimmed.contains("localhost:8080")) {
                return trimmed.replace("localhost:8080", "10.0.2.2:8080");
            }
            if (trimmed.contains("127.0.0.1:8080")) {
                return trimmed.replace("127.0.0.1:8080", "10.0.2.2:8080");
            }
            return trimmed;
        }

        // If it's a relative path like /media/...
        if (trimmed.startsWith("/media/")) {
            return "http://10.0.2.2:8080" + trimmed;
        }

        // If it's a storage key like spots/... or avatars/...
        return "http://10.0.2.2:8080/media/" + trimmed;
    }

    @Nullable
    private Bitmap loadFromContentUri(@NonNull Uri uri) {
        try {
            ContentResolver resolver = appContext.getContentResolver();
            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            try (InputStream in = resolver.openInputStream(uri)) {
                if (in == null) return null;
                BitmapFactory.decodeStream(in, null, boundsOptions);
            }

            int inSampleSize = calculateInSampleSize(boundsOptions, 800, 800);
            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = inSampleSize;

            try (InputStream in = resolver.openInputStream(uri)) {
                if (in == null) return null;
                return BitmapFactory.decodeStream(in, null, decodeOptions);
            }
        } catch (Exception e) {
            Log.e(TAG, "loadFromContentUri error: " + e.getMessage());
            return null;
        }
    }

    @Nullable
    private Bitmap loadFromNetwork(@NonNull String urlString) {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setInstanceFollowRedirects(true);
            conn.connect();

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                in = conn.getInputStream();
                byte[] data = FileUtils.readStreamBytes(in);
                if (data == null || data.length == 0) return null;

                BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
                boundsOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(data, 0, data.length, boundsOptions);

                int inSampleSize = calculateInSampleSize(boundsOptions, 800, 800);
                BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
                decodeOptions.inSampleSize = inSampleSize;

                return BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
            } else {
                Log.w(TAG, "Network image load failed with HTTP " + code + " for " + urlString);
            }
        } catch (Exception e) {
            Log.e(TAG, "loadFromNetwork error for " + urlString + ": " + e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
