package com.redrosecps.collect.android.http.openrosa.okhttp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import com.burgstaller.okhttp.AuthenticationCacheInterceptor;
import com.burgstaller.okhttp.CachingAuthenticatorDecorator;
import com.burgstaller.okhttp.DispatchingAuthenticator;
import com.burgstaller.okhttp.basic.BasicAuthenticator;
import com.burgstaller.okhttp.digest.CachingAuthenticator;
import com.burgstaller.okhttp.digest.Credentials;
import com.burgstaller.okhttp.digest.DigestAuthenticator;
import com.redrosecps.collect.android.http.openrosa.HttpCredentials;
import com.redrosecps.collect.android.http.openrosa.HttpCredentialsInterface;
import com.redrosecps.collect.android.http.openrosa.OpenRosaServerClient;
import com.redrosecps.collect.android.http.openrosa.OpenRosaServerClientProvider;
import com.redrosecps.collect.android.preferences.GeneralKeys;
import com.redrosecps.collect.android.preferences.GeneralSharedPreferences;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpOpenRosaServerClientProvider implements OpenRosaServerClientProvider {

    private static final int CONNECTION_TIMEOUT = 30000;
    private static final int WRITE_CONNECTION_TIMEOUT = 60000; // it can take up to 27 seconds to spin up an Aggregate
    private static final int READ_CONNECTION_TIMEOUT = 60000; // it can take up to 27 seconds to spin up an Aggregate
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String OPEN_ROSA_VERSION_HEADER = "X-OpenRosa-Version";
    private static final String OPEN_ROSA_VERSION = "1.0";
    private static final String DATE_HEADER = "Date";

    private final OkHttpClient baseClient;

    private HttpCredentialsInterface lastCredentials;
    private OkHttpOpenRosaServerClient client;

    public OkHttpOpenRosaServerClientProvider(@NonNull OkHttpClient baseClient) {
        this.baseClient = baseClient;
    }

    @Override
    public OpenRosaServerClient get(String scheme, String userAgent, @Nullable HttpCredentialsInterface credentials) {
        if (client == null || credentialsHaveChanged(credentials)) {
            lastCredentials = credentials;
            client = createNewClient(scheme, userAgent, credentials);
        }

        return client;
    }

    private boolean credentialsHaveChanged(@Nullable HttpCredentialsInterface credentials) {
        return lastCredentials != null && !lastCredentials.equals(credentials);
    }

    @NonNull
    private OkHttpOpenRosaServerClient createNewClient(String scheme, String userAgent, @Nullable HttpCredentialsInterface credentials) {
        OkHttpClient.Builder builder = baseClient.newBuilder()
                .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(WRITE_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(READ_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .followRedirects(true);

        if (credentials != null) {
            Credentials cred = new Credentials(credentials.getUsername(), credentials.getPassword());

            DispatchingAuthenticator.Builder daBuilder = new DispatchingAuthenticator.Builder();
            daBuilder.with("digest", new DigestAuthenticator(cred));
            if (scheme.equalsIgnoreCase("https")) {
                daBuilder.with("basic", new BasicAuthenticator(cred));
            }

            DispatchingAuthenticator authenticator = daBuilder.build();
            ConcurrentHashMap<String, CachingAuthenticator> authCache = new ConcurrentHashMap<>();
            builder.authenticator(new CachingAuthenticatorDecorator(authenticator, authCache))
                    .addInterceptor(new AuthenticationCacheInterceptor(authCache)).build();
        }
        return new OkHttpOpenRosaServerClient(builder.build(), userAgent);
    }

    private static class OkHttpOpenRosaServerClient implements OpenRosaServerClient {

        private final OkHttpClient client;
        private final String userAgent;

        OkHttpOpenRosaServerClient(OkHttpClient client, String userAgent) {
            this.client = client;
            this.userAgent = userAgent;
        }

        @Override
        public Response makeRequest(Request request, Date currentTime, @Nullable HttpCredentialsInterface credentials) throws IOException {
            if(credentials == null) {
                String u = (String)GeneralSharedPreferences.getInstance().get(GeneralKeys.KEY_USERNAME);
                String p = (String)GeneralSharedPreferences.getInstance().get(GeneralKeys.KEY_PASSWORD);
                credentials = new HttpCredentials(u, p);
            }
            return client.newCall(request.newBuilder()
                    .addHeader(USER_AGENT_HEADER, userAgent)
                    .addHeader(OPEN_ROSA_VERSION_HEADER, OPEN_ROSA_VERSION)
                    .addHeader(DATE_HEADER, getHeaderDate(currentTime))
                    .addHeader("Authorization", okhttp3.Credentials.basic(credentials.getUsername(), credentials.getPassword()))
                    .build()).execute();
        }

        private static String getHeaderDate(Date currentTime) {
            SimpleDateFormat dateFormatGmt = new SimpleDateFormat("E, dd MMM yyyy hh:mm:ss zz", Locale.US);
            dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
            return dateFormatGmt.format(currentTime);
        }
    }
}
