package com.jskierbi.amazon_login

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse.RequestStatus.FAILED
import com.amazon.device.iap.model.PurchaseUpdatesResponse.RequestStatus.NOT_SUPPORTED
import com.amazon.device.iap.model.PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL
import com.amazon.device.iap.model.UserDataResponse
import com.amazon.identity.auth.device.AuthError
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager
import com.amazon.identity.auth.device.authorization.api.AuthorizationListener
import com.amazon.identity.auth.device.authorization.api.AuthzConstants
import com.amazon.identity.auth.device.shared.APIListener
import rx.Observable
import rx.android.schedulers.AndroidSchedulers.mainThread

/**
 * Created by q on 09/05/16.
 */
class LoginActivity : Activity() {

  companion object {
    val TAG = LoginActivity::class.java.simpleName
  }

  val mAuthManager by lazy { AmazonAuthorizationManager(this, Bundle.EMPTY) }
  val authScopes = arrayOf("profile", "postal_code")
  val skus = setOf(
      "sku.testing.subscription.monthly",
      "sku.testing.subscription.year")

  val labelLoginState: TextView by bindView(R.id.label_status)

  override fun onCreate(state: Bundle?) {
    super.onCreate(state)
    setContentView(R.layout.activity_login)

    PurchasingService.registerListener(this@LoginActivity, PurchasingListenerImpl())

    onClick(R.id.btn_login_with_amazon) {
      mAuthManager.authorize(authScopes, Bundle.EMPTY, AuthListener())
    }

    onClick(R.id.btn_get_items) {
      PurchasingService.getUserData()
      PurchasingService.getProductData(skus)
      PurchasingService.getPurchaseUpdates(true)
    }

    onClick(R.id.btn_buy) {
      toast("Implementation pending...")
    }
  }

  inner class AuthListener : AuthorizationListener {
    override fun onSuccess(bundle: Bundle?) = mAuthManager.getProfile(ProfiListener()).unit
    override fun onError(p0: AuthError?) = Log.d(TAG, "AuthListener.onError()").unit
    override fun onCancel(p0: Bundle?) = Log.d(TAG, "AuthListener.onCancel()").unit
  }

  inner class ProfiListener : APIListener {
    override fun onError(error: AuthError?) {
      Log.d(TAG, "ProfiListener.onError")
      Observable.just(error).observeOn(mainThread()).subscribe { labelLoginState.text = "Login failed!" }
    }

    override fun onSuccess(response: Bundle) {
      Log.d(TAG, "ProfiListener.onSuccess()")

      val profileBundle = response.getBundle(AuthzConstants.BUNDLE_KEY.PROFILE.`val`);
      val name = profileBundle.getString(AuthzConstants.PROFILE_KEY.NAME.`val`);
      val email = profileBundle.getString(AuthzConstants.PROFILE_KEY.EMAIL.`val`);
      val account = profileBundle.getString(AuthzConstants.PROFILE_KEY.USER_ID.`val`);
      val zipcode = profileBundle.getString(AuthzConstants.PROFILE_KEY.POSTAL_CODE.`val`);

      Log.d(TAG, """
          $name
          $email
          $account
          $zipcode
      """)

      Observable.just(true).observeOn(mainThread()).subscribe { labelLoginState.text = "Login success: $name, $email" }
    }
  }

  lateinit var currentUserId: String
  lateinit var currentMarketplace: String

  inner class PurchasingListenerImpl : PurchasingListener {

    override fun onPurchaseResponse(response: PurchaseResponse) {
      Log.d(TAG, ">> onPurchaseResponse: ${response.requestStatus}, $response")
      when (response.requestStatus) {
        PurchaseResponse.RequestStatus.SUCCESSFUL -> {
          Log.d(TAG, ">> SUCCESSFUL")
          currentUserId = response.userData.userId
          currentMarketplace = response.userData.marketplace

        }
      }
    }

    override fun onProductDataResponse(response: ProductDataResponse) {
      // Product info goes here...
      Log.d(TAG, ">> onProductDataResponse: ${response.requestStatus}, $response")
      response.unavailableSkus.forEach { Log.d(TAG, ">> Unavailable SKU: $it") }
      for ((sku, product) in response.productData) {
        Log.d(TAG, """
        Product: ${product.title}
        Type: ${product.productType}
        SKU: ${product.sku}
        Price: ${product.price}
        Description: ${product.description}
        """)
      }
    }

    override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
      // Receipt updates here
      Log.d(TAG, ">> onPurchaseUpdatesResponse: ${response.requestStatus}, $response")
      when (response.requestStatus) {
        SUCCESSFUL -> {
          response.receipts.forEach { Log.d(TAG, ">> receipt: ${it.receiptId}") }
          if (response.hasMore()) PurchasingService.getPurchaseUpdates(false)
        }
        FAILED -> {
        }
        NOT_SUPPORTED -> {
        }
      }
    }

    override fun onUserDataResponse(response: UserDataResponse) {
      // User info goes here
      Log.d(TAG, ">> onUserDataResponse: ${response.requestStatus}, $response")
      when (response.requestStatus) {
        SUCCESSFUL -> {
          currentUserId = response.userData.userId
          currentMarketplace = response.userData.marketplace
        }
      }
    }
  }

}

inline fun Activity.onClick(id: Int, crossinline listener: (View) -> Unit) = findViewById(id)!!.setOnClickListener { listener(it) }
fun Activity.toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

val Any?.unit: Unit get() = Unit