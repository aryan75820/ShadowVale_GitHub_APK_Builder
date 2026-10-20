package com.pawarjigaming.shadowvale;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class MainActivity extends Activity {
    private WebView webView;
    private AdView bannerView;
    private LinearLayout root;
    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;
    private boolean bannerRequestedVisible = true;
    private boolean bannerLoaded = false;

    // Exact Ad Unit IDs from Pawar Ji Gaming AdMob Account:
    private static final String BANNER_UNIT_ID = "ca-app-pub-8251220250880290/8023542749";
    private static final String INTERSTITIAL_UNIT_ID = "ca-app-pub-8251220250880290/9714319357";
    private static final String REWARDED_UNIT_ID = "ca-app-pub-8251220250880290/8215114432";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF07080F);

        webView = new WebView(this);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(false);
        ws.setMediaPlaybackRequiresUserGesture(false);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new AdMobBridge(), "AndroidAdMob");
        root.addView(webView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        webView.loadUrl("file:///android_asset/index.html");

        // Keep the banner in its own layout row so it cannot cover game controls.
        bannerView = new AdView(this);
        bannerView.setAdSize(AdSize.BANNER);
        bannerView.setAdUnitId(BANNER_UNIT_ID);
        bannerView.setVisibility(View.GONE);
        bannerView.setAdListener(new AdListener() {
            @Override public void onAdLoaded() {
                bannerLoaded = true;
                if (bannerView != null && bannerRequestedVisible) bannerView.setVisibility(View.VISIBLE);
            }
            @Override public void onAdFailedToLoad(LoadAdError error) {
                bannerLoaded = false;
                if (bannerView != null) bannerView.setVisibility(View.GONE);
            }
        });
        root.addView(bannerView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        setContentView(root);

        MobileAds.initialize(this, initializationStatus -> runOnUiThread(() -> {
            loadInterstitial();
            loadRewarded();
            if (bannerView != null) bannerView.loadAd(new AdRequest.Builder().build());
        }));
    }

    private void loadInterstitial() {
        InterstitialAd.load(this, INTERSTITIAL_UNIT_ID, new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override public void onAdLoaded(InterstitialAd ad) {
                        interstitialAd = ad;
                    }
                    @Override public void onAdFailedToLoad(LoadAdError error) {
                        interstitialAd = null;
                    }
                });
    }

    private void loadRewarded() {
        RewardedAd.load(this, REWARDED_UNIT_ID, new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override public void onAdLoaded(RewardedAd ad) {
                        rewardedAd = ad;
                    }
                    @Override public void onAdFailedToLoad(LoadAdError error) {
                        rewardedAd = null;
                    }
                });
    }

    @Override
    public void onBackPressed() {
        if (webView != null) {
            webView.evaluateJavascript("if(window.NavigationManager) window.NavigationManager.handleBackNavigation();", null);
        } else {
            super.onBackPressed();
        }
    }

    public class AdMobBridge {
        @JavascriptInterface
        public void initialize(String bId, String iId, String rId) {}

        @JavascriptInterface
        public void setBannerVisible(final boolean visible, String unitId) {
            runOnUiThread(() -> {
                bannerRequestedVisible = visible;
                if (bannerView != null) {
                    bannerView.setVisibility(visible && bannerLoaded ? View.VISIBLE : View.GONE);
                }
            });
        }

        @JavascriptInterface
        public void showInterstitial(String unitId) {
            runOnUiThread(() -> {
                if (interstitialAd == null) {
                    loadInterstitial();
                    notifyJs("onAdMobInterstitialClosed");
                    return;
                }
                InterstitialAd adToShow = interstitialAd;
                interstitialAd = null;
                adToShow.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override public void onAdDismissedFullScreenContent() {
                        loadInterstitial();
                        notifyJs("onAdMobInterstitialClosed");
                    }
                    @Override public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError error) {
                        loadInterstitial();
                        notifyJs("onAdMobInterstitialClosed");
                    }
                });
                adToShow.show(MainActivity.this);
            });
        }

        @JavascriptInterface
        public void showRewarded(String unitId) {
            runOnUiThread(() -> {
                if (rewardedAd == null) {
                    loadRewarded();
                    notifyJs("onAdMobRewardedFailed");
                    return;
                }
                RewardedAd adToShow = rewardedAd;
                rewardedAd = null;
                adToShow.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override public void onAdDismissedFullScreenContent() {
                        loadRewarded();
                        notifyJs("onAdMobRewardedClosed");
                    }
                    @Override public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError error) {
                        loadRewarded();
                        notifyJs("onAdMobRewardedFailed");
                    }
                });
                adToShow.show(MainActivity.this, rewardItem -> notifyJs("onAdMobRewardGranted"));
            });
        }

        private void notifyJs(String callback) {
            if (webView != null) {
                webView.post(() -> webView.evaluateJavascript(
                        "if(window." + callback + ") window." + callback + "();", null));
            }
        }

    }

    @Override protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
        if (bannerView != null) bannerView.resume();
    }

    @Override protected void onPause() {
        if (webView != null) webView.onPause();
        if (bannerView != null) bannerView.pause();
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (bannerView != null) {
            bannerView.destroy();
            bannerView = null;
        }
        if (webView != null) {
            webView.removeJavascriptInterface("AndroidAdMob");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

}
