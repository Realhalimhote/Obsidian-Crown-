package com.example.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class IapProduct(
    val productId: String,
    val title: String,
    val description: String,
    val fallbackPrice: String,
    val isConsumable: Boolean
) {
    SOVEREIGN_SUPPORTER_PACK(
        productId = "sovereign_supporter_pack",
        title = "Sovereign Supporter Pack",
        description = "Increases offline progress cap from 12h to 24h and grants +15% Skill & Conquest XP permanently.",
        fallbackPrice = "$4.99",
        isConsumable = false
    ),
    VAULT_EXPANSION_PACK(
        productId = "vault_expansion_pack",
        title = "Vault Expansion Pack",
        description = "Permanently adds +100 Inventory Vault slots and unlocks the Auto-Salvage Common Junk toggle.",
        fallbackPrice = "$2.99",
        isConsumable = false
    ),
    CHRONICLE_FESTIVAL_BUNDLE(
        productId = "chronicle_festival_bundle",
        title = "Chronicle Festival Bundle",
        description = "Grants 5 Chronicle Festival Tokens to summon off-season Holiday Bosses + Royal Obsidian Warlord title.",
        fallbackPrice = "$3.99",
        isConsumable = true
    ),
    ROYAL_CROWN_POUCH(
        productId = "royal_crown_pouch",
        title = "Royal Crown Pouch",
        description = "Grants 250 Royal Crowns for optional Royal Speed-Ups and bonus Apex Boss attempts.",
        fallbackPrice = "$1.99",
        isConsumable = true
    )
}

class BillingManager(
    context: Context,
    private val onEntitlementGranted: (IapProduct) -> Unit,
    private val onStatusMessage: (String) -> Unit
) {
    private val _isPlayConnected = MutableStateFlow(false)
    val isPlayConnected: StateFlow<Boolean> = _isPlayConnected.asStateFlow()

    private val _productDetailsMap = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetailsMap: StateFlow<Map<String, ProductDetails>> = _productDetailsMap.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            onStatusMessage("Purchase canceled.")
        } else {
            onStatusMessage("Play Billing: ${billingResult.debugMessage.ifBlank { "Unavailable right now" }}")
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    fun startConnection() {
        if (billingClient.isReady) {
            _isPlayConnected.value = true
            queryProductsAndExistingPurchases()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isPlayConnected.value = true
                    queryProductsAndExistingPurchases()
                } else {
                    _isPlayConnected.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                _isPlayConnected.value = false
            }
        })
    }

    private fun queryProductsAndExistingPurchases() {
        val productList = IapProduct.entries.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it.productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, result ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val map = result.productDetailsList.associateBy { it.productId }
                _productDetailsMap.value = map
            }
        }

        val purchasesParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(purchasesParams) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
        }
    }

    fun purchase(activity: Activity?, product: IapProduct) {
        if (activity == null || !billingClient.isReady) {
            onStatusMessage("Google Play Billing is unavailable offline.")
            return
        }

        val details = _productDetailsMap.value[product.productId]
        if (details == null) {
            onStatusMessage("Product not yet available on Google Play.")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        for (productId in purchase.products) {
            val product = IapProduct.entries.firstOrNull { it.productId == productId } ?: continue
            if (product.isConsumable) {
                val consumeParams = ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.consumeAsync(consumeParams) { result, _ ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        onEntitlementGranted(product)
                    }
                }
            } else {
                if (!purchase.isAcknowledged) {
                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(ackParams) { result ->
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            onEntitlementGranted(product)
                        }
                    }
                } else {
                    onEntitlementGranted(product)
                }
            }
        }
    }

    fun destroy() {
        billingClient.endConnection()
    }
}
