package com.jskierbi.amazon_login

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.amazon.identity.auth.device.AuthError
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager
import com.amazon.identity.auth.device.authorization.api.AuthorizationListener
import com.amazon.identity.auth.device.authorization.api.AuthzConstants
import com.amazon.identity.auth.device.shared.APIListener

/**
 * Created by q on 09/05/16.
 */
class LoginActivity : Activity() {

  companion object {
    val TAG = LoginActivity::class.java.simpleName
  }

  val mAuthManager by lazy { AmazonAuthorizationManager(this, Bundle.EMPTY) }

  override fun onCreate(state: Bundle?) {
    super.onCreate(state)
    setContentView(R.layout.activity_login)

    onClick(R.id.btn_login_with_amazon) {
      mAuthManager.authorize(arrayOf("profile", "postal_code"), Bundle.EMPTY, AuthListener())
    }
  }

  inner class AuthListener : AuthorizationListener {
    override fun onSuccess(bundle: Bundle?) {
      Log.d(TAG, "AuthListener.onSuccess()")
      mAuthManager.getProfile(ProfiListener())
    }

    override fun onError(p0: AuthError?) {
      Log.d(TAG, "AuthListener.onError()")
    }

    override fun onCancel(p0: Bundle?) {
      Log.d(TAG, "AuthListener.onCancel()")
    }

  }

  inner class ProfiListener : APIListener {
    override fun onError(error: AuthError?) {
      Log.d(TAG, "ProfiListener.onError")
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
    }

  }
}

inline fun Activity.onClick(id: Int, crossinline listener: (View) -> Unit) = findViewById(id)!!.setOnClickListener { listener(it) }
fun Activity.toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

