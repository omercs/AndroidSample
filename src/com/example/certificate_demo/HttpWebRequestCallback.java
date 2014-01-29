/*
 * Copyright Microsoft Corporation (c) All Rights Reserved.
 */

package com.example.certificate_demo;

public interface HttpWebRequestCallback {
    void onComplete(HttpWebResponse response, Exception exception);
}
