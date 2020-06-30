package com.talybin.aircat;

import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

class Streams {

    // Open InputStream from Uri supporting following types:
    // gzip, zip, plain.
    static InputStream openInputStream(Uri uri) throws IOException {
        InputStream is = openUriStream(uri);

        if (!is.markSupported())
            is = new BufferedInputStream(is);

        is.mark(1024);

        // Check if Gz
        try {
            return new GZIPInputStream(is);
        }
        catch (IOException ignored) {
            is.reset();
        };
        // Check if zip
        try {
            ZipInputStream zis = new ZipInputStream(is);
            if (zis.getNextEntry() != null)
                return zis;
        }
        catch (IOException ignored) {
            is.reset();
        };
        // Assume it is a plain text
        return is;
    }

    // Open InputStream from Uri
    static private InputStream openUriStream(Uri uri) throws FileNotFoundException {
        // TODO check if type not content (ex. http) use another resolver
        return App.getInstance().getContentResolver().openInputStream(uri);
    }
}
