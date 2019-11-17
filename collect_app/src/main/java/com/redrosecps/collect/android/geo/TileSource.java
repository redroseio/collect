package com.redrosecps.collect.android.geo;

interface TileSource {
    byte[] getTileBlob(int zoom, int x, int y);

    String getContentType();  // a MIME type such as "image/jpeg"

    String getContentEncoding();  // either "identity" or "gzip"
}
