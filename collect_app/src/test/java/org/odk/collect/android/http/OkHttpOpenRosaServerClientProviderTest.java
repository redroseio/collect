package com.redrosecps.collect.android.http;

import com.redrosecps.collect.android.http.openrosa.okhttp.OkHttpOpenRosaServerClientProvider;
import com.redrosecps.collect.android.http.openrosa.OpenRosaServerClientProvider;

import okhttp3.OkHttpClient;
import okhttp3.tls.internal.TlsUtil;

public class OkHttpOpenRosaServerClientProviderTest extends OpenRosaServerClientProviderTest {

    @Override
    protected OpenRosaServerClientProvider buildSubject() {
        OkHttpClient baseClient = new OkHttpClient.Builder()
                .sslSocketFactory(
                        TlsUtil.localhost().sslSocketFactory(),
                        TlsUtil.localhost().trustManager())
                .build();
        
        return new OkHttpOpenRosaServerClientProvider(baseClient);
    }
}
