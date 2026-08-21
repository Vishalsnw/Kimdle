package com.example.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {
    private const val TAG = "AdManager"

    // AdMob IDs provided by user
    const val ADMOB_APP_ID = "ca-app-pub-5538218540896625~5378944987"
    const val AD_UNIT_ID = "ca-app-pub-5538218540896625/8164477738"

    private var interstitialAd: InterstitialAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var isAdLoading = false
    private var lastAdShownTimestamp = 0L
    private const val AD_MIN_INTERVAL_MS = 60_000L // 1 minute interval to comply with AdMob frequency policy

    fun initialize(context: Context) {
        try {
            MobileAds.initialize(context) { initializationStatus ->
                Log.d(TAG, "Google Mobile Ads SDK Initialized: $initializationStatus")
                // Preload ad
                loadInterstitialAd(context.applicationContext)
                loadAppOpenAd(context.applicationContext)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MobileAds: ${e.message}", e)
        }
    }

    fun loadInterstitialAd(context: Context) {
        if (isAdLoading || interstitialAd != null) return
        isAdLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isAdLoading = false
                    Log.d(TAG, "Interstitial ad loaded successfully.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isAdLoading = false
                    Log.w(TAG, "Interstitial ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    fun loadAppOpenAd(context: Context) {
        if (appOpenAd != null) return
        val adRequest = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            AD_UNIT_ID,
            adRequest,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    Log.d(TAG, "App Open ad loaded successfully.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    appOpenAd = null
                    Log.w(TAG, "App Open ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Shows interstitial ad if available and interval policy passed.
     * Always calls onComplete() to ensure seamless user navigation.
     */
    fun showInterstitialAd(activity: Activity, onComplete: () -> Unit) {
        val now = System.currentTimeMillis()
        val currentAd = interstitialAd

        if (currentAd != null && (now - lastAdShownTimestamp >= AD_MIN_INTERVAL_MS)) {
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed.")
                    interstitialAd = null
                    lastAdShownTimestamp = System.currentTimeMillis()
                    loadInterstitialAd(activity.applicationContext)
                    onComplete()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "Interstitial ad failed to show: ${adError.message}")
                    interstitialAd = null
                    loadInterstitialAd(activity.applicationContext)
                    onComplete()
                }

                override fun onAdShowedFullScreenContent() {
                    lastAdShownTimestamp = System.currentTimeMillis()
                }
            }
            currentAd.show(activity)
        } else {
            // If ad is not ready or frequency cap active, preload and proceed immediately
            if (interstitialAd == null) {
                loadInterstitialAd(activity.applicationContext)
            }
            onComplete()
        }
    }

    /**
     * Shows App Open ad at launch if available
     */
    fun showAppOpenAdIfAvailable(activity: Activity, onComplete: () -> Unit = {}) {
        val currentAd = appOpenAd
        val now = System.currentTimeMillis()
        if (currentAd != null && (now - lastAdShownTimestamp >= AD_MIN_INTERVAL_MS)) {
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    lastAdShownTimestamp = System.currentTimeMillis()
                    loadAppOpenAd(activity.applicationContext)
                    onComplete()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    loadAppOpenAd(activity.applicationContext)
                    onComplete()
                }

                override fun onAdShowedFullScreenContent() {
                    lastAdShownTimestamp = System.currentTimeMillis()
                }
            }
            currentAd.show(activity)
        } else {
            onComplete()
        }
    }
}
