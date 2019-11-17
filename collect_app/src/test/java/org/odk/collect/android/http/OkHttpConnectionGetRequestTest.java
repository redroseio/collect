package com.redrosecps.collect.android.http;

import android.webkit.MimeTypeMap;

import com.redrosecps.collect.android.http.openrosa.okhttp.OkHttpConnection;
import com.redrosecps.collect.android.http.openrosa.okhttp.OkHttpOpenRosaServerClientProvider;
import com.redrosecps.collect.android.http.openrosa.OpenRosaHttpInterface;

import okhttp3.OkHttpClient;

public class OkHttpConnectionGetRequestTest extends OpenRosaGetRequestTest {

    @Override
    protected OpenRosaHttpInterface buildSubject() {
        return new OkHttpConnection(
                new OkHttpOpenRosaServerClientProvider(new OkHttpClient()),
                new CollectThenSystemContentTypeMapper(MimeTypeMap.getSingleton()),
                USER_AGENT
        );
    }
}