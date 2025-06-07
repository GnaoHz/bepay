package com.example.demomvn -v

import android.app.Application
import com.stripe.android.PaymentConfiguration

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PaymentConfiguration.init(
            applicationContext,
            "pk_test_51RWwDvQjO3Y8yCfk6wwNhjVBXAinhs060mONdn8gXtWjhokvWUNLVOXVEHvOlThLvgxKgZ1OPXJN0QZ7C4wUqpQZ005wXZVEmD"
        )
    }
}