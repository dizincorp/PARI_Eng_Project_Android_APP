package com.example.project.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * Utility helper for preparing multipart file uploads and reading Android Content URIs safely.
 */
public final class FileUtils {

    private static final String TAG = "FileUtils";
    private static final int BUFFER_SIZE = 8192;
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5 MB (matching backend limit)

    private FileUtils() {
    }

    /**
     * Converts a content or file Uri into an OkHttp MultipartBody.Part for file upload.
     *
     * @param context  Application or Activity context
     * @param partName Parameter name expected by backend (e.g. "file")
     * @param fileUri  Android Uri pointing to image in storage
     * @return Prepared {@link MultipartBody.Part}, or null if conversion fails
     */
    @Nullable
    public static MultipartBody.Part prepareImagePart(@NonNull Context context,
                                                      @NonNull String partName,
                                                      @NonNull Uri fileUri) {
        try {
            ContentResolver resolver = context.getContentResolver();
            String mimeType = resolver.getType(fileUri);
            if (mimeType == null || (!mimeType.equalsIgnoreCase("image/jpeg")
                    && !mimeType.equalsIgnoreCase("image/png")
                    && !mimeType.equalsIgnoreCase("image/webp"))) {
                mimeType = "image/jpeg";
            }

            String filename = queryFileName(resolver, fileUri);
            if (filename == null || filename.trim().isEmpty()) {
                String ext = mimeType.contains("png") ? "png" : (mimeType.contains("webp") ? "webp" : "jpg");
                filename = "upload_" + System.currentTimeMillis() + "." + ext;
            }

            byte[] bytes = readUriBytes(resolver, fileUri);
            if (bytes == null || bytes.length == 0) {
                Log.e(TAG, "prepareImagePart: Uri bytes were empty for " + fileUri);
                return null;
            }

            if (bytes.length > MAX_FILE_SIZE) {
                Log.w(TAG, "prepareImagePart: Image exceeds 5MB limit (" + bytes.length + " bytes)");
            }

            RequestBody requestBody = RequestBody.create(MediaType.parse(mimeType), bytes);
            return MultipartBody.Part.createFormData(partName, filename, requestBody);
        } catch (Exception e) {
            Log.e(TAG, "prepareImagePart error reading Uri: " + fileUri, e);
            return null;
        }
    }

    /**
     * Reads all bytes from an input stream into a byte array.
     *
     * @param in InputStream to read from
     * @return Byte array containing stream data, or null on error
     */
    @Nullable
    public static byte[] readStreamBytes(@Nullable InputStream in) {
        if (in == null) return null;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "readStreamBytes error: " + e.getMessage(), e);
            return null;
        }
    }

    @Nullable
    private static byte[] readUriBytes(@NonNull ContentResolver resolver, @NonNull Uri uri) {
        try (InputStream in = resolver.openInputStream(uri)) {
            return readStreamBytes(in);
        } catch (Exception e) {
            Log.e(TAG, "readUriBytes error: " + e.getMessage(), e);
            return null;
        }
    }

    @Nullable
    private static String queryFileName(@NonNull ContentResolver resolver, @NonNull Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        return cursor.getString(nameIndex);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return uri.getLastPathSegment();
    }
}
