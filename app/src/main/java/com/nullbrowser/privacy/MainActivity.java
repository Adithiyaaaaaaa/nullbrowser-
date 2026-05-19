package com.nullbrowser.privacy;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Debug;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS = "privacy_state";
    private static final String KEY_LAST_ACTIVE = "last_active_at";
    private static final long TEN_DAYS_MS = 10L * 24L * 60L * 60L * 1000L;
    private static final String HOME = "https://duckduckgo.com/";

    private WebView webView;
    private EditText addressBar;
    private TextView statusLine;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        wipeIfInactive();
        buildUi();
        configureWebView();
        checkRuntimeRiskSignals();

        String initialUrl = getIntentUrl();
        webView.loadUrl(initialUrl == null ? HOME : initialUrl);
    }

    @Override
    protected void onResume() {
        super.onResume();
        wipeIfInactive();
        if (Debug.isDebuggerConnected()) {
            panicWipe("Debugger detected. Private data wiped.");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        prefs.edit().putLong(KEY_LAST_ACTIVE, System.currentTimeMillis()).apply();
    }

    @Override
    protected void onDestroy() {
        clearSessionData(false);
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        super.onBackPressed();
    }

    private void buildUi() {
        int ink = Color.rgb(17, 20, 24);
        int panel = Color.rgb(244, 246, 248);
        int accent = Color.rgb(14, 143, 110);
        int danger = Color.rgb(179, 38, 30);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(dp(6), dp(6), dp(6), dp(4));
        bar.setBackgroundColor(panel);

        Button back = navButton("Back", ink);
        Button forward = navButton("Next", ink);
        Button reload = navButton("Reload", ink);
        Button vpn = navButton("VPN", accent);
        Button wipe = navButton("WIPE", danger);

        addressBar = new EditText(this);
        addressBar.setSingleLine(true);
        addressBar.setTextColor(ink);
        addressBar.setHintTextColor(Color.rgb(105, 112, 119));
        addressBar.setHint("Search or enter address");
        addressBar.setTextSize(14);
        addressBar.setSelectAllOnFocus(true);
        addressBar.setImeOptions(EditorInfo.IME_ACTION_GO);
        addressBar.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);
        addressBar.setBackgroundColor(Color.WHITE);
        addressBar.setPadding(dp(10), 0, dp(10), 0);

        LinearLayout.LayoutParams small = new LinearLayout.LayoutParams(dp(58), dp(44));
        LinearLayout.LayoutParams urlParams = new LinearLayout.LayoutParams(0, dp(44), 1);
        urlParams.setMargins(dp(4), 0, dp(4), 0);

        bar.addView(back, small);
        bar.addView(forward, small);
        bar.addView(addressBar, urlParams);
        bar.addView(reload, small);
        bar.addView(vpn, small);
        bar.addView(wipe, small);

        statusLine = new TextView(this);
        statusLine.setTextColor(Color.rgb(74, 81, 88));
        statusLine.setTextSize(12);
        statusLine.setPadding(dp(10), dp(3), dp(10), dp(5));
        statusLine.setBackgroundColor(panel);
        statusLine.setText("Private mode: no app history, screenshots blocked, session cleared on exit");

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);

        root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));
        root.addView(statusLine, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(26)));
        root.addView(webView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);

        back.setOnClickListener(v -> {
            if (webView.canGoBack()) {
                webView.goBack();
            }
        });
        forward.setOnClickListener(v -> {
            if (webView.canGoForward()) {
                webView.goForward();
            }
        });
        reload.setOnClickListener(v -> webView.reload());
        vpn.setOnClickListener(v -> prepareVpn());
        wipe.setOnClickListener(v -> confirmPanicWipe());
        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
            if (actionId == EditorInfo.IME_ACTION_GO || enter) {
                webView.loadUrl(normalizeAddress(addressBar.getText().toString()));
                return true;
            }
            return false;
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(false);
        settings.setSaveFormData(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setGeolocationEnabled(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setUserAgentString(settings.getUserAgentString() + " NullBrowser/0.1");

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, false);

        webView.clearHistory();
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) {
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                addressBar.setText(url);
                view.clearHistory();
                CookieManager.getInstance().flush();
            }
        });
    }

    private void wipeIfInactive() {
        long lastActiveAt = prefs.getLong(KEY_LAST_ACTIVE, 0L);
        if (lastActiveAt > 0L && System.currentTimeMillis() - lastActiveAt >= TEN_DAYS_MS) {
            clearSessionData(true);
            Toast.makeText(this, "Inactive for 10 days. Private data wiped.", Toast.LENGTH_LONG).show();
        }
    }

    private void confirmPanicWipe() {
        new AlertDialog.Builder(this)
                .setTitle("Panic wipe?")
                .setMessage("This clears browser cookies, cache, storage, and local privacy state now.")
                .setPositiveButton("Wipe", (dialog, which) -> panicWipe("Private data wiped."))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void panicWipe(String message) {
        clearSessionData(true);
        if (webView != null) {
            webView.loadUrl("about:blank");
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void clearSessionData(boolean clearPrefs) {
        if (webView != null) {
            webView.clearCache(true);
            webView.clearHistory();
            webView.clearFormData();
        }
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookies(null);
        cookieManager.flush();
        WebStorage.getInstance().deleteAllData();
        if (clearPrefs) {
            prefs.edit().clear().putLong(KEY_LAST_ACTIVE, System.currentTimeMillis()).apply();
        }
    }

    private void checkRuntimeRiskSignals() {
        if (RootSignals.looksRooted()) {
            statusLine.setText("Warning: rooted device signals detected. Use panic wipe if this is unexpected.");
        }
        if (Debug.isDebuggerConnected()) {
            panicWipe("Debugger detected. Private data wiped.");
        }
    }

    private void prepareVpn() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, 100);
            return;
        }
        Toast.makeText(this, "VPN permission ready. Server routing is the next milestone.", Toast.LENGTH_LONG).show();
    }

    private String getIntentUrl() {
        Intent intent = getIntent();
        if (intent == null || intent.getData() == null) {
            return null;
        }
        return normalizeAddress(intent.getData().toString());
    }

    private String normalizeAddress(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return HOME;
        }
        String lower = value.toLowerCase(Locale.US);
        if (lower.startsWith("https://") || lower.startsWith("http://")) {
            return value;
        }
        if (value.contains(".") && !value.contains(" ")) {
            return "https://" + value;
        }
        return "https://duckduckgo.com/?q=" + Uri.encode(value);
    }

    private Button navButton(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(10);
        button.setTextColor(color);
        button.setAllCaps(false);
        button.setPadding(0, 0, 0, 0);
        return button;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
