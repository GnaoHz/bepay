package com.example.demo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CheckoutActivity : AppCompatActivity() {
    companion object {
        private const val BACKEND_URL = "http://10.0.2.2:4242"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CheckoutScreen()
        }
    }

    @Composable
    private fun CheckoutScreen() {
        var paymentIntentClientSecret by remember { mutableStateOf<String?>(null) }

        var error by remember { mutableStateOf<String?>(null) }

        val paymentSheet = rememberPaymentSheet { paymentResult ->
            when (paymentResult) {
                is PaymentSheetResult.Completed -> showToast("Payment complete!")
                is PaymentSheetResult.Canceled -> showToast("Payment canceled!")
                is PaymentSheetResult.Failed -> {
                    error = paymentResult.error.localizedMessage ?: paymentResult.error.message
                }
            }
        }

        error?.let { errorMessage ->
            ErrorAlert(
                errorMessage = errorMessage,
                onDismiss = {
                    error = null
                }
            )
        }

        LaunchedEffect(Unit) {
            fetchPaymentIntent().onSuccess { clientSecret ->
                paymentIntentClientSecret = clientSecret
            }.onFailure { paymentIntentError ->
                error = paymentIntentError.localizedMessage ?: paymentIntentError.message
            }
        }

        PayButton(
            enabled = paymentIntentClientSecret != null,
            onClick = {
                paymentIntentClientSecret?.let {
                    onPayClicked(
                        paymentSheet = paymentSheet,
                        paymentIntentClientSecret = it,
                    )
                }
            }
        )
    }

    @Composable
    private fun PayButton(
        enabled: Boolean,
        onClick: () -> Unit
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            onClick = onClick
        ) {
            Text("Pay now")
        }
    }

    @Composable
    private fun ErrorAlert(
        errorMessage: String,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            title = {
                Text(text = "Error occurred during checkout")
            },
            text = {
                Text(text = errorMessage)
            },
            onDismissRequest = onDismiss,
            confirmButton = {
                Button(onDismiss) {
                    Text(text = "Ok")
                }
            }
        )
    }

    private suspend fun fetchPaymentIntent(): Result<String> = suspendCoroutine { continuation ->
        val url = "$BACKEND_URL/create-payment-intent"

        val shoppingCartContent = """
            {
                "items": [
                    {"id":"xl-tshirt"}
                ]
            }
        """

        val mediaType = "application/json; charset=utf-8".toMediaType()

        val body = shoppingCartContent.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        OkHttpClient()
            .newCall(request)
            .enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resume(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        continuation.resume(Result.failure(Exception(response.message)))
                    } else {
                        val clientSecret = extractClientSecretFromResponse(response)

                        clientSecret?.let { secret ->
                            continuation.resume(Result.success(secret))
                        } ?: run {
                            val error = Exception("Could not find payment intent client secret in response!")

                            continuation.resume(Result.failure(error))
                        }
                    }
                }
            })
    }

    private fun extractClientSecretFromResponse(response: Response): String? {
        return try {
            val responseData = response.body?.string()
            val responseJson = responseData?.let { JSONObject(it) } ?: JSONObject()

            responseJson.getString("clientSecret")
        } catch (exception: JSONException) {
            null
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this,  message, Toast.LENGTH_LONG).show()
        }
    }

    private fun onPayClicked(
        paymentSheet: PaymentSheet,
        paymentIntentClientSecret: String,
    ) {
        val configuration = PaymentSheet.Configuration.Builder(merchantDisplayName = "Example, Inc.")
            .build()

        // Present Payment Sheet
        paymentSheet.presentWithPaymentIntent(paymentIntentClientSecret, configuration)
    }
}