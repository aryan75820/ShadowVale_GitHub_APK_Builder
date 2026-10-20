package com.pawarjigaming.shadowvale;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;

public class MainActivity extends Activity {
    private WebView webView;
    private AdView bannerView;
    private RewardedInterstitialAd interstitialAd;
    private RewardedInterstitialAd rewardedAd;

    // Exact Ad Unit IDs from Pawar Ji Gaming AdMob Account:
    private static final String BANNER_UNIT_ID = "ca-app-pub-8251220250880290/8023542749";
    private static final String INTERSTITIAL_UNIT_ID = "ca-app-pub-8251220250880290/9714319357";
    private static final String REWARDED_UNIT_ID = "ca-app-pub-8251220250880290/8215114432";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        FrameLayout root = new FrameLayout(this);
        webView = new WebView(this);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new AdMobBridge(), "AndroidAdMob");
        webView.loadUrl("file:///android_asset/index.html");
        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Initialize Google Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> {
            loadInterstitial();
            loadRewarded();
        });

        // Bottom Banner Ad
        bannerView = new AdView(this);
        bannerView.setAdSize(AdSize.BANNER);
        bannerView.setAdUnitId(BANNER_UNIT_ID);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL
        );
        root.addView(bannerView, params);
        bannerView.loadAd(new AdRequest.Builder().build());

        setContentView(root);
    }

    private void loadInterstitial() {
        RewardedInterstitialAd.load(this, INTERSTITIAL_UNIT_ID, new AdRequest.Builder().build(),
            new RewardedInterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(RewardedInterstitialAd ad) {
                    interstitialAd = ad;
                }
                @Override
                public void onAdFailedToLoad(LoadAdError err) {
                    interstitialAd = null;
                }
            });
    }

    private void loadRewarded() {
        RewardedInterstitialAd.load(this, REWARDED_UNIT_ID, new AdRequest.Builder().build(),
            new RewardedInterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(RewardedInterstitialAd ad) {
                    rewardedAd = ad;
                }
                @Override
                public void onAdFailedToLoad(LoadAdError err) {
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
                if (bannerView != null) {
                    bannerView.setVisibility(visible ? View.VISIBLE : View.GONE);
                }
            });
        }

        @JavascriptInterface
        public void showInterstitial(String unitId) {
            runOnUiThread(() -> {
                if (interstitialAd != null) {
                    interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            interstitialAd = null;
                            loadInterstitial();
                            webView.evaluateJavascript("if(window.onAdMobInterstitialClosed) window.onAdMobInterstitialClosed();", null);
                        }
                    });
                    interstitialAd.show(MainActivity.this, rewardItem -> {});
                } else {
                    loadInterstitial();
                    webView.evaluateJavascript("if(window.onAdMobInterstitialClosed) window.onAdMobInterstitialClosed();", null);
                }
            });
        }

        @JavascriptInterface
        public void showRewarded(String unitId) {
            runOnUiThread(() -> {
                if (rewardedAd != null) {
                    rewardedAd.show(MainActivity.this, rewardItem -> {
                        webView.evaluateJavascript("if(window.onAdMobRewardGranted) window.onAdMobRewardGranted();", null);
                        rewardedAd = null;
                        loadRewarded();
                    });
                } else {
                    loadRewarded();
                    webView.evaluateJavascript("if(window.onAdMobRewardGranted) window.onAdMobRewardGranted();", null);
                }
            });
        }
    }
}
