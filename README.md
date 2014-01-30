AndroidSample
=============

Sample project for using client certificate in Android webview


This is based on KeyChainDemo inside the Android samples.

Steps:
HttpsUrlConnection is used to send the request with SSLcontext to provide client cert. It has Service that responds at https://localhost:8080. You can use the srevice to test different behaviors.
