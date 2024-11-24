package com.stripe.sample;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.ApiResource;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.SubscriptionCreateParams;
import io.github.cdimascio.dotenv.Dotenv;

import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static spark.Spark.*;

public class Server {
  private static final Gson gson = new Gson();

  static Map<String,Object> buildSetupIntentParamsMap(String customerID) throws JsonProcessingException, StripeException {
    ObjectMapper mapper = new ObjectMapper();
    String setupIntentParamsTemplate = """
        {
          "payment_method_types": [ "acss_debit" ],
          "customer": "%s",
          "description": "######## My Setup Intent!!",
          "payment_method_options": {
            "acss_debit": {
              "currency": "cad",
              "mandate_options": {
                "transaction_type": "personal",
                "payment_schedule": "combined",
                "interval_description": "%s"
              },
              "verification_method": "instant"
            }
          }
        }
        """;
    Price deposit = Price.retrieve("price_1NpCOlFGXpRXhvrdCEbBgUIz");  // $6 one-shot
    Price installment = Price.retrieve("price_1NWK8SFGXpRXhvrdUay6ruJ6");
    NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.CANADA);

    String intervalDescription = String.format("monthly payments of %s %s on the 1th of each month. There is also a deposit of %s %s and possible penalties of %s %s in case of non-payment.",
        formatter.format(installment.getUnitAmount() / 100), installment.getCurrency(),
        formatter.format(deposit.getUnitAmount() / 100), deposit.getCurrency(),
        formatter.format(1), installment.getCurrency());

    setupIntentParamsTemplate = setupIntentParamsTemplate.replaceAll("\"%\\!([db])\"", "%$1");
    String setupIntentParams = String.format(setupIntentParamsTemplate, customerID, intervalDescription);
    @SuppressWarnings("unchecked") // See https://coderanch.com/t/383800/java/Generics-unchecked-assignment
    Map<String,Object> setupIntentMap = mapper.readValue(setupIntentParams, Map.class);

    return setupIntentMap;
  }

  public static void main(String[] args) {
    port(4242);
    Dotenv dotenv = Dotenv.load();

    Stripe.apiKey = dotenv.get("STRIPE_SECRET_KEY");

    staticFiles.externalLocation(
        Paths.get(Paths.get("").toAbsolutePath().toString(), dotenv.get("STATIC_DIR")).normalize().toString());

    get("/public-key", (request, response) -> {
      response.type("application/json");

      JsonObject publicKey = new JsonObject();
      publicKey.addProperty("publicKey", dotenv.get("STRIPE_PUBLISHABLE_KEY"));
      return publicKey.toString();
    });

    post("/create-customer", (request, response) -> {
      response.type("application/json");

      Map<String, Object> params = new HashMap<>();
      params.put(
          "description",
          "My First Test Customer (created for API docs at https://www.stripe.com/docs/api)"
      );

      Customer customer = Customer.create(params);

      return gson.toJson(customer);
    });

    post("/create-setup-intent", (request, response) -> {
      response.type("application/json");

      Map<String,Object> setupIntentParams = Server.buildSetupIntentParamsMap("cus_OIw13ZtzDnj9Y9");

      /* SetupIntentCreateParams params = SetupIntentCreateParams.builder()
          .addPaymentMethodType("acss_debit")
          .setCustomer("cus_OIw13ZtzDnj9Y9") // myAcss: hjoly@xn.com
          .setPaymentMethodOptions(
              SetupIntentCreateParams.PaymentMethodOptions.builder()
                  .setAcssDebit(
                      SetupIntentCreateParams.PaymentMethodOptions.AcssDebit.builder()
                          .setCurrency(
                              SetupIntentCreateParams.PaymentMethodOptions.AcssDebit.Currency.CAD
                          )
                          .setMandateOptions(
                              SetupIntentCreateParams.PaymentMethodOptions.AcssDebit.MandateOptions.builder()
                                  .addDefaultFor(
                                      SetupIntentCreateParams.PaymentMethodOptions.AcssDebit.MandateOptions.DefaultFor.INVOICE
                                  )
                                  .addDefaultFor(
                                      SetupIntentCreateParams.PaymentMethodOptions.AcssDebit.MandateOptions.DefaultFor.SUBSCRIPTION
                                  )
                                  .setPaymentSchedule(                                // [!] Not accepted when using "default_for"
                                    SetupIntentCreateParams.PaymentMethodOptions.AcssDebit.MandateOptions.PaymentSchedule.INTERVAL
                                  )
                                  .setIntervalDescription("First day of every month") // [!] Not accepted when using "default_for"
                                  .setTransactionType(
                                    SetupIntentCreateParams.PaymentMethodOptions.AcssDebit.MandateOptions.TransactionType.PERSONAL
                                  )
                                  .build()
                          )
                          // Instant only verification (see https://stripe.com/docs/payments/acss-debit/set-up-payment?platform=web#web-instant-only)
                          .setVerificationMethod(
                              SetupIntentCreateParams.PaymentMethodOptions.AcssDebit.VerificationMethod.INSTANT
                          )
                          .build()
                  )
                  .build()
          )
          .build();

      SetupIntent setupIntent = SetupIntent.create(params); */
      SetupIntent setupIntent = SetupIntent.create(setupIntentParams);

      // [N] The returned SetupIntent includes a client secret that the client side uses to securely complete the payment process.
      String outcome = gson.toJson(setupIntent);
      return outcome;
    });

    post("/webhook", (request, response) -> {
      String payload = request.body();
      String sigHeader = request.headers("Stripe-Signature");
      String endpointSecret = dotenv.get("STRIPE_WEBHOOK_SECRET");

      Event event = null;

      try {
        event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
      } catch (SignatureVerificationException e) {
        // Invalid signature
        response.status(400);
        return "";
      }

      EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

      switch (event.getType()) {
        case "setup_intent.created": {
          SetupIntent setupIntent = ApiResource.GSON.fromJson(deserializer.getRawJson(), SetupIntent.class);
          System.out.printf("A new SetupIntent id=\"%s\" was created.%n", setupIntent.getId());
        }
        break;

        // [N] Since the setup intent was successful, let's use its account info to create a deposit (via a PaymentIntent).
        case "setup_intent.succeeded": {
          SetupIntent setupIntent = ApiResource.GSON.fromJson(deserializer.getRawJson(), SetupIntent.class);
          System.out.printf("A SetupIntent id=\"%s\" has successfully set up a PaymentMethod.%n", setupIntent.getId());

          Price deposit = Price.retrieve("price_1NpCOlFGXpRXhvrdCEbBgUIz");  // $6 one-shot
          PaymentMethod paymentMethod = PaymentMethod.retrieve(setupIntent.getPaymentMethod());
          Mandate mandate = Mandate.retrieve(setupIntent.getMandate());
          Customer customer = Customer.retrieve(setupIntent.getCustomer());

          // [N] Accept future payments (https://stripe.com/docs/payments/acss-debit/set-up-payment?platform=web#charge-later)
          PaymentIntentCreateParams params =
              PaymentIntentCreateParams.builder()
                  .addPaymentMethodType(paymentMethod.getType())
                  .setPaymentMethod(paymentMethod.getId())
                  .setCustomer(customer.getId())
                  .setMandate(mandate.getId())
                  .setConfirm(true)
                  .setAmount(deposit.getUnitAmount())
                  .setCurrency(deposit.getCurrency())
                  .build();

          PaymentIntent paymentIntent = PaymentIntent.create(params);
        }
        break;

        case "setup_intent.setup_failed":
          System.out.println("A SetupIntent has failed the attempt to set up a PaymentMethod.");
        break;

        // [N] Occurs whenever a new payment method is attached to a customer (https://stripe.com/docs/api/events/types#event_types-payment_method.attached).
        case "payment_method.attached": {
          PaymentMethod paymentMethod = ApiResource.GSON.fromJson(deserializer.getRawJson(), PaymentMethod.class);

          // At this point, associate the ID of the Customer object with your
          // own internal representation of a customer, if you have one.
          Customer customer = Customer.retrieve(paymentMethod.getCustomer());

          System.out.printf("A PaymentMethod id=\"%s\" has successfully been saved to a Customer.%n", paymentMethod.getId());

          // Optional: update the Customer billing information with billing details from the PaymentMethod
          CustomerUpdateParams params = new CustomerUpdateParams.Builder()
              .setEmail(paymentMethod.getBillingDetails().getEmail())
              .build();

          customer.update(params);
          System.out.println("Customer successfully updated.");
        }
        break;

        case "payment_intent.succeeded": {
          PaymentIntent paymentIntent = ApiResource.GSON.fromJson(deserializer.getRawJson(), PaymentIntent.class);

          System.out.printf("The PaymentIntent id=\"%s\" of %s has succeeded.%n", paymentIntent.getId(), paymentIntent.getAmount());

          // TODO Create a schedule.
          PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentIntent.getPaymentMethod());
          Customer customer = Customer.retrieve(paymentIntent.getCustomer());

          // Note: The weeklyPmt ($3 / week) already contains the recurrence information (Weekly).
          Price weeklyPmt = Price.retrieve("price_1NWK8SFGXpRXhvrdUay6ruJ6");

          // From https://stackoverflow.com/a/22992578
          LocalDate twoDaysLater = LocalDate.now().plusDays(2);
          Long epochFrstDaily = twoDaysLater.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();

          SubscriptionCreateParams subscriptionCreateParams = SubscriptionCreateParams
              .builder()
              .setCustomer(customer.getId())
              .addItem(
                  SubscriptionCreateParams.Item.builder().setPrice(weeklyPmt.getId()).build()
              )
              .setPaymentBehavior(
                  SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE
              )
              .setDefaultPaymentMethod(paymentMethod.getId())/*
              .setPaymentSettings(
                SubscriptionCreateParams.PaymentSettings
                    .builder()
                    .addPaymentMethodType(
                      SubscriptionCreateParams.PaymentSettings.PaymentMethodType.ACSS_DEBIT
                    )
                    .build()
              )*/
              .setBillingCycleAnchor(epochFrstDaily)
              .setProrationBehavior(SubscriptionCreateParams.ProrationBehavior.NONE) // [!] To avoid a proration before the service begins.
              .setCollectionMethod(SubscriptionCreateParams.CollectionMethod.CHARGE_AUTOMATICALLY) // [!] To avoid sending invoice on each month (see https://stripe.com/docs/api/subscriptions/create#create_subscription-collection_method)

              // [N] I think the following is useless as the Price already contains the recurrence information.
              /*.setPendingInvoiceItemInterval(
                SubscriptionCreateParams.PendingInvoiceItemInterval.builder().setInterval(SubscriptionCreateParams.PendingInvoiceItemInterval.Interval.DAY).build()
              )*/
              .addExpand("latest_invoice.payment_intent")
              .build();

          Subscription subscription = Subscription.create(subscriptionCreateParams);

          System.out.printf("The Subscription id=\"%s\" has been created.%n", subscription.getId());
        }
        break;

        default:
          // Unexpected event type
          response.status(400);
          return "";
      }

      response.status(200);
      return "";
    });
  }
}
