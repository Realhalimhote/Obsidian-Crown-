package com.example.ads

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicBoolean

enum class AdAvailability {
    OFFLINE,
    HIDDEN_BY_USER,
    CONSENT_REQUIRED,
    LOADING,
    READY,
    UNAVAILABLE
}

data class AdManagerUiState(
    val availability: AdAvailability = AdAvailability.OFFLINE,
    val isOnline: Boolean = false,
    val canRequestAds: Boolean = false,
    val isPrivacyOptionsRequired: Boolean = false,
    val statusMessage: String = "Offline — Crown & gameplay alternatives always available"
)

class RewardedAdManager(context: Context) {

    private val appContext = context.applicationContext
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(appContext)

    private var rewardedAd: RewardedAd? = null
    private val isMobileAdsInitialized = AtomicBoolean(false)
    private val isLoadingAd = AtomicBoolean(false)
    private val isShowingAd = AtomicBoolean(false)

    private val _uiState = MutableStateFlow(AdManagerUiState())
    val uiState: StateFlow<AdManagerUiState> = _uiState.asStateFlow()

    companion object {
        // Production Rewarded Ad Unit ID ("All adds")
        const val PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-7863837204146938/5339783430"
        // Official Google AdMob Test Rewarded Ad Unit ID for debug development
        const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    }

    fun isNetworkConnected(): Boolean {
        return runCatching {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.getOrDefault(false)
    }

    fun refreshState(activity: Activity?, hideOffers: Boolean) {
        if (hideOffers) {
            rewardedAd = null
            _uiState.update {
                it.copy(
                    availability = AdAvailability.HIDDEN_BY_USER,
                    statusMessage = "Rewarded ad offers hidden"
                )
            }
            return
        }

        val online = isNetworkConnected()
        if (!online) {
            _uiState.update {
                it.copy(
                    availability = AdAvailability.OFFLINE,
                    isOnline = false,
                    statusMessage = "Offline — use Royal Crowns or gameplay rewards"
                )
            }
            return
        }

        if (activity == null) {
            _uiState.update {
                it.copy(
                    isOnline = true,
                    availability = if (rewardedAd != null) AdAvailability.READY else AdAvailability.UNAVAILABLE
                )
            }
            return
        }

        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        runCatching {
            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    val canRequest = consentInformation.canRequestAds()
                    val privacyReq = consentInformation.privacyOptionsRequirementStatus ==
                        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                    _uiState.update {
                        it.copy(
                            isOnline = true,
                            canRequestAds = canRequest,
                            isPrivacyOptionsRequired = privacyReq
                        )
                    }
                    if (canRequest) {
                        initializeAndPreload(hideOffers)
                    } else {
                        _uiState.update {
                            it.copy(
                                availability = AdAvailability.CONSENT_REQUIRED,
                                statusMessage = "Privacy consent required before loading optional ads"
                            )
                        }
                    }
                },
                { _ ->
                    // Fallback gracefully on UMP error without blocking gameplay
                    val canRequest = consentInformation.canRequestAds()
                    _uiState.update {
                        it.copy(
                            isOnline = true,
                            canRequestAds = canRequest,
                            availability = if (canRequest && rewardedAd != null) AdAvailability.READY else AdAvailability.UNAVAILABLE,
                            statusMessage = "Ad service unavailable right now"
                        )
                    }
                }
            )
        }.onFailure {
            _uiState.update {
                it.copy(
                    availability = AdAvailability.UNAVAILABLE,
                    statusMessage = "Ad consent check skipped"
                )
            }
        }
    }

    private fun initializeAndPreload(hideOffers: Boolean) {
        if (hideOffers || !isNetworkConnected() || !consentInformation.canRequestAds()) return
        if (rewardedAd != null) {
            _uiState.update {
                it.copy(
                    availability = AdAvailability.READY,
                    statusMessage = "Optional Rewarded Ad Ready"
                )
            }
            return
        }

        if (isMobileAdsInitialized.compareAndSet(false, true)) {
            runCatching {
                MobileAds.initialize(appContext) {
                    loadRewardedAd(hideOffers)
                }
            }.onFailure {
                isMobileAdsInitialized.set(false)
                _uiState.update {
                    it.copy(
                        availability = AdAvailability.UNAVAILABLE,
                        statusMessage = "Ad SDK unavailable"
                    )
                }
            }
        } else {
            loadRewardedAd(hideOffers)
        }
    }

    fun loadRewardedAd(hideOffers: Boolean) {
        if (hideOffers || !isNetworkConnected() || !consentInformation.canRequestAds()) return
        if (rewardedAd != null || !isLoadingAd.compareAndSet(false, true)) return

        _uiState.update {
            it.copy(
                availability = AdAvailability.LOADING,
                statusMessage = "Preparing optional rewarded offer..."
            )
        }

        val adUnitId = if (BuildConfig.DEBUG) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID
        val request = AdRequest.Builder().build()

        runCatching {
            RewardedAd.load(
                appContext,
                adUnitId,
                request,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        isLoadingAd.set(false)
                        rewardedAd = ad
                        _uiState.update {
                            it.copy(
                                availability = AdAvailability.READY,
                                statusMessage = "Optional Rewarded Ad Ready"
                            )
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isLoadingAd.set(false)
                        rewardedAd = null
                        _uiState.update {
                            it.copy(
                                availability = AdAvailability.UNAVAILABLE,
                                statusMessage = "No ad available right now — try later or use Crowns"
                            )
                        }
                    }
                }
            )
        }.onFailure {
            isLoadingAd.set(false)
            rewardedAd = null
            _uiState.update {
                it.copy(
                    availability = AdAvailability.UNAVAILABLE,
                    statusMessage = "Ad load skipped"
                )
            }
        }
    }

    fun showRewardedAd(
        activity: Activity?,
        hideOffers: Boolean,
        onRewardEarned: () -> Unit,
        onStatusMessage: (String) -> Unit
    ) {
        if (hideOffers) {
            onStatusMessage("Rewarded ad offers are hidden.")
            return
        }
        if (activity == null || !isNetworkConnected()) {
            onStatusMessage("Offline — optional rewarded ads require internet. Crown alternatives work offline.")
            refreshState(activity, hideOffers)
            return
        }

        if (!consentInformation.canRequestAds()) {
            runCatching {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        onStatusMessage("Consent unavailable right now.")
                    } else {
                        refreshState(activity, hideOffers)
                        if (consentInformation.canRequestAds()) {
                            onStatusMessage("Consent updated — loading optional offer...")
                        }
                    }
                }
            }.onFailure {
                onStatusMessage("Unable to open consent dialog.")
            }
            return
        }

        val ad = rewardedAd
        if (ad == null) {
            loadRewardedAd(hideOffers)
            onStatusMessage("Loading optional ad... try again in a moment, or use Crowns.")
            return
        }

        if (!isShowingAd.compareAndSet(false, true)) {
            return
        }

        // Single-use guard prevents duplicate reward grants from any callback sequence
        val rewardGrantedOnce = AtomicBoolean(false)

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                isShowingAd.set(false)
                if (!rewardGrantedOnce.get()) {
                    onStatusMessage("Ad closed before completion — no blessing granted.")
                }
                loadRewardedAd(hideOffers)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                isShowingAd.set(false)
                onStatusMessage("Could not display ad right now.")
                loadRewardedAd(hideOffers)
            }
        }

        runCatching {
            ad.show(activity) {
                if (rewardGrantedOnce.compareAndSet(false, true)) {
                    onRewardEarned()
                }
            }
        }.onFailure {
            rewardedAd = null
            isShowingAd.set(false)
            onStatusMessage("Ad playback failed safely.")
        }
    }

    fun simulateDebugRewardedAd(
        onRewardEarned: () -> Unit,
        onStatusMessage: (String) -> Unit
    ) {
        if (!BuildConfig.DEBUG) {
            onStatusMessage("Simulated ads are restricted to debug builds.")
            return
        }
        val singleGrant = AtomicBoolean(false)
        if (singleGrant.compareAndSet(false, true)) {
            onRewardEarned()
        }
    }

    fun showPrivacyOptions(activity: Activity?, onStatusMessage: (String) -> Unit) {
        if (activity == null) return
        runCatching {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
                if (error != null) {
                    onStatusMessage("Privacy options unavailable right now.")
                } else {
                    refreshState(activity, hideOffers = false)
                }
            }
        }.onFailure {
            onStatusMessage("Privacy options unavailable.")
        }
    }
}
