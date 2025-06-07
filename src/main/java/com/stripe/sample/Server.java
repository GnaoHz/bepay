package com.stripe.sample;

import java.nio.file.Paths;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.staticFiles;
import static spark.Spark.port;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

public class Server {
  private static Gson gson = new Gson();

  static class CreatePaymentItem {
    @SerializedName("id")
    String id;

    public String getId() {
      return id;
    }
    @SerializedName("amount")
    Long amount;

    public Long getAmount() {
      return amount;
    }
  }

  static class CreatePayment {
    @SerializedName("items")
    CreatePaymentItem[] items;

    public CreatePaymentItem[] getItems() {
      return items;
    }
  }

  static class CreatePaymentResponse {
    private String clientSecret;
    public CreatePaymentResponse(String clientSecret) {
      this.clientSecret = clientSecret;
    }
  }

  static int calculateOrderAmount(CreatePaymentItem[] items) {
    // Calculate the order total on the server to prevent
    // people from directly manipulating the amount on the client
    int total = 0;
    for (CreatePaymentItem item : items) {
      total += item.getAmount();
    }
    return total;
  }


  public static void main(String[] args) {
    port(4242);
    staticFiles.externalLocation(Paths.get("public").toAbsolutePath().toString());

    // This is your test secret API key.
    Stripe.apiKey = "sk_test_51RWwDvQjO3Y8yCfkeoIGrTisEk1FC3V3sTw0VwtJ4VeVQLLCFlaI41HAv0Iry4UKPK6BvCtirVk22ZTVgRsdYSOB003LQaiWwt";

    post("/create-payment-intent", (request, response) -> {
      response.type("application/json");
      CreatePayment postBody = gson.fromJson(request.body(), CreatePayment.class);

      PaymentIntentCreateParams params =
              PaymentIntentCreateParams.builder()
                      .setAmount(new Long(calculateOrderAmount(postBody.getItems())))
                      .setCurrency("vnd")
                      // In the latest version of the API, specifying the `automatic_payment_methods` parameter is optional because Stripe enables its functionality by default.
                      .setAutomaticPaymentMethods(
                              PaymentIntentCreateParams.AutomaticPaymentMethods
                                      .builder()
                                      .setEnabled(true)
                                      .build()
                      )
                      .build();

      // Create a PaymentIntent with the order amount and currency
      PaymentIntent paymentIntent = PaymentIntent.create(params);

      CreatePaymentResponse paymentResponse = new CreatePaymentResponse(paymentIntent.getClientSecret(), paymentIntent.getId());
      return gson.toJson(paymentResponse);
    });
  }
}