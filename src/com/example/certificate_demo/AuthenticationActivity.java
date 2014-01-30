
package com.example.certificate_demo;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.SSLContext;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

/**
 * activity to launch webview for authentication
 * 
 * @author omercan
 */
public class AuthenticationActivity extends Activity {

    protected static final int BROWSER_CODE_CANCEL = 1110;

    private final String TAG = "AuthenticationActivity";

    private Button btnCancel;

    private boolean mRestartWebview = false;

    private WebView wv;

    private String mStartUrl;

    private ProgressDialog spinner;

    private String redirectUrl;

    /**
     * pass sslcontext that will provide cert in request
     */
    public static SSLContext sharedSSLContext;

    /**
     * JavascriptInterface to report page content in errors
     */
    private JavaScriptInterface mScriptInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        // Get the message from the intent
        Intent intent = getIntent();
        String targetUrl = intent.getStringExtra("TARGET_URL");
        String redirectUrl = intent.getStringExtra("REDIRECT_URI");

        Log.d(TAG, "OnCreate redirect" + redirectUrl);

        // cancel action will send the request back to onActivityResult method
        btnCancel = (Button)findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                confirmCancelRequest();
            }
        });

        // Spinner dialog to show some message while it is loading
        spinner = new ProgressDialog(this);
        spinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        spinner.setMessage("Loading");
        spinner.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                confirmCancelRequest();
            }
        });

        // Create the Web View to show the page
        wv = (WebView)findViewById(R.id.webView1);
        wv.getSettings().setJavaScriptEnabled(true);
        mScriptInterface = new JavaScriptInterface();
        wv.addJavascriptInterface(mScriptInterface, "ScriptInterface");
        wv.requestFocus(View.FOCUS_DOWN);

        // Set focus to the view for touch event
        wv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) {
                    if (!view.hasFocus()) {
                        view.requestFocus();
                    }
                }
                return false;
            }
        });

        wv.getSettings().setLoadWithOverviewMode(true);
        wv.getSettings().setDomStorageEnabled(true);
        wv.getSettings().setUseWideViewPort(true);
        wv.getSettings().setBuiltInZoomControls(true);
        wv.setWebViewClient(new CustomWebViewClient());
        wv.setVisibility(View.INVISIBLE);
        // wv.setCertificate(certificate);

        Log.v(TAG, "User agent:" + wv.getSettings().getUserAgentString());
        mStartUrl = targetUrl;

        final String postUrl = mStartUrl;

        wv.post(new Runnable() {
            @Override
            public void run() {
                // make https request
                // based on response load webview
                HttpWebRequest request = null;
                try {
                    request = new HttpWebRequest(new URL(postUrl));
                    request.setSSLContext(sharedSSLContext);
                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                // TODO update headers
                request.sendAsyncGet(new HttpWebRequestCallback() {

                    @Override
                    public void onComplete(HttpWebResponse response, Exception exception) {
                        Log.d(TAG, "Webrequest is complete");
                        if (exception != null) {
                            Log.e(TAG, "webrequest has exception:", exception);
                        }

                        if (response != null && response.getBody() != null) {
                            // load document from the https response
                            String html = "hello";
                            try {
                                html = new String(response.getBody(), "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            wv.loadDataWithBaseURL(postUrl, html, "text/html", "UTF-8", null);
                        }
                    }
                });
            }
        });

    }

    /**
     * activity sets result to go back to the caller
     * 
     * @param resultCode
     * @param data
     */
    private void ReturnToCaller(int resultCode, Intent data) {
        Log.d(TAG, "Return To Caller:" + resultCode);
        displaySpinner(false);

        if (data == null) {
            data = new Intent();
        }

        // if (mAuthRequest != null) {
        // // set request id related to this response to send the delegateId
        // Log.d(TAG, "Return To Caller REQUEST_ID:" +
        // mAuthRequest.getRequestId());
        // data.putExtra(AuthenticationConstants.Browser.REQUEST_ID,
        // mAuthRequest.getRequestId());
        // } else {
        // Log.w(TAG, "Request object is null");
        // }

        setResult(resultCode, data);
        this.finish();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "AuthenticationActivity onPause unregister receiver");
        super.onPause();

        mRestartWebview = true;
        // restart webview when it comes back from onresume
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "AuthenticationActivity onStart");
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // It can come here from onCreate,onRestart or onPause. It
        // will post the url again since webview could not start at the middle
        // of redirect url.
        // If it reaches the final url, it will set result back to caller.
        if (mRestartWebview) {
            Log.v(TAG, "Webview onResume will post start url again:" + mStartUrl);
            final String postUrl = mStartUrl;

            wv.post(new Runnable() {
                @Override
                public void run() {
                    wv.loadUrl("about:blank");// load blank first
                    wv.loadUrl(postUrl);
                }
            });
        }
        mRestartWebview = false;
    }

    @Override
    protected void onRestart() {
        Log.d(TAG, "AuthenticationActivity onRestart");
        super.onRestart();
        mRestartWebview = true;
    }

    @Override
    protected void onStop() {
        // Called when you are no longer visible to the user.
        Log.d(TAG, "AuthenticationActivity onStop");
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back button is pressed");

        // Ask user if they rally want to cancel the flow, if navigation is not
        // possible
        // User may navigated to another page and does not need confirmation to
        // go back to previous page.
        if (!wv.canGoBack()) {
            confirmCancelRequest();
        } else {
            // Don't use default back pressed action, since user can go back in
            // webview
            wv.goBack();
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig);
    }

    private void confirmCancelRequest() {
        new AlertDialog.Builder(AuthenticationActivity.this)
                .setTitle("title_confirmation_activity_authentication")
                .setMessage("confirmation_activity_authentication")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface arg0, int arg1) {
                        Intent resultIntent = new Intent();
                        ReturnToCaller(BROWSER_CODE_CANCEL, resultIntent);
                    }
                }).create().show();
    }

    /**
     * javascript injection to the loaded page to retrieve content
     */
    private class JavaScriptInterface {
        String mHtml;

        @JavascriptInterface
        public void setContent(String html) {
            mHtml = html;
        }

        public String getContent() {
            return mHtml;
        }
    }

    private class CustomWebViewClient extends WebViewClient {

        public static final int BROWSER_CODE_COMPLETE = 1770;

        private void loadContent(WebView view) {
            // Get page content to report
            // Load page content
            wv.loadUrl("javascript:window.ScriptInterface.setContent('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
        }

        private void reportContent(WebView view) {
            loadContent(view);
            if (mScriptInterface != null
                    && !StringExtensions.IsNullOrBlank(mScriptInterface.getContent())) {
                Log.v(TAG, "Webview content:" + mScriptInterface.getContent());
            }
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            Log.d(TAG, "shouldOverrideUrlLoading:url=" + url);
            displaySpinner(true);

            if (url.startsWith(redirectUrl)) {
                Log.v(TAG, "Webview reached redirecturl");

                // It is pointing to redirect. Final url can be processed to get
                // the code or error.
                Intent resultIntent = new Intent();
                resultIntent.putExtra("RESPONSE_FINAL_URL", url);

                ReturnToCaller(BROWSER_CODE_COMPLETE, resultIntent);
                view.stopLoading();
                return true;
            }

            return false;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description,
                String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            displaySpinner(false);
            Log.v(TAG, "Webview received an error. Errorcode:" + errorCode + " " + description);
            reportContent(view);
            // Intent resultIntent = new Intent();
            // resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE,
            // "Error Code:" + errorCode);
            // resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE,
            // description);
            // resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO,
            // mAuthRequest);
            // ReturnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR,
            // resultIntent);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            // Developer does not have option to control this for now
            super.onReceivedSslError(view, handler, error);
            displaySpinner(false);
            handler.cancel();
            Log.e(TAG, "Received ssl error");

        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            Log.v(TAG, "Page started:" + url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Log.v(TAG, "Page finished:" + url);
            displaySpinner(false);

            /*
             * Once web view is fully loaded,set to visible
             */
            wv.setVisibility(View.VISIBLE);

            // Load page content to use in reporting errors
            loadContent(wv);
        }
    }

    /**
     * handle spinner display
     * 
     * @param show
     */
    private void displaySpinner(boolean show) {
        if (!AuthenticationActivity.this.isFinishing() && spinner != null) {
            if (show && !spinner.isShowing()) {
                spinner.show();
            }

            if (!show && spinner.isShowing()) {
                spinner.dismiss();
            }
        }
    }
}
