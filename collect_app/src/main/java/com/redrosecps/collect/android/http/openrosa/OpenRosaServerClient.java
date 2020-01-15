package com.redrosecps.collect.android.http.openrosa;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.Date;

import okhttp3.Request;
import okhttp3.Response;

public interface OpenRosaServerClient {

    Response makeRequest(Request request, Date currentTime, @Nullable HttpCredentialsInterface credentials) throws IOException;
}
