# Saving PAD Payment Intents Followed by Future Payments

This sample shows how to build a form to save a bank account using the ["Setup Intents API"](https://stripe.com/docs/api/setup_intents). It then uses the saved account to 
1. create a deposit and 
2. create a subscription for weekly payments.   
  
This sample evolved from the guide ["Save details for future payments with pre-authorized debit in Canada"](https://stripe.com/docs/payments/acss-debit/set-up-payment?platform=web#charge-later).

** Original Demo**

See a hosted version of the [sample with a card](https://q0sh7.sse.codesandbox.io/) in test mode or [fork on codesandbox.io](https://codesandbox.io/s/stripe-saving-card-without-payment-q0sh7) or [download the GitHub repository](https://github.com/stripe-samples/saving-card-without-payment/tree/main).



## How to create Price Objects with Stripe CLI Fixtures

Edit the `seed.json` fixture file with the desired information. Then export the fixture data:

```
cd saving-pad-without-payment
stripe fixtures seed.json
```
Once the prices got exported in the Stripe Repository, update the `Server.java` file so that it uses the prices' "lookup_key":
```
    PriceListParams params = PriceListParams
      .builder()
      .addLookupKeys("my_basic_coverage_monthly_pmt_key")
      .addLookupKeys("my_premium_coverage_monthly_pmt_key")
      .build();
```


## How to run locally

This sample assumes the presence of 
* a customer ([cus_OIw13ZtzDnj9Y9](https://dashboard.stripe.com/test/customers/cus_OIw13ZtzDnj9Y9)) and,
* a product ([prod_OIw0gvdmNS5usx](https://dashboard.stripe.com/test/products/prod_OIw0gvdmNS5usx)) which is composed of
  * a price for the deposit ([price_1NpCOlFGXpRXhvrdCEbBgUIz](https://dashboard.stripe.com/test/prices/price_1NpCOlFGXpRXhvrdCEbBgUIz)), 
  * and a price used for weekly payments ([price_1NWK8SFGXpRXhvrdUay6ruJ6](https://dashboard.stripe.com/test/prices/price_1NWK8SFGXpRXhvrdUay6ruJ6)).
  
These objects were initially created via a [seed.json](./seed.json) file and are available in the Stripe Dashboard (see section [How to create Price Objects with Stripe CLI Fixtures](#how-to-create-price-objects-with-stripe-cli-fixtures)).

The sample includes a server implementations in Java.

**1. Follow the server instructions on how to run:**

Follow the instructions in the server folder README on how to run.

**2. [Optional] Run a webhook locally:**

If you want to test with a local webhook on your machine, you can use the Stripe CLI to easily spin one up.

First [install the CLI](https://stripe.com/docs/stripe-cli) and [link your Stripe account](https://stripe.com/docs/stripe-cli#link-account).

Then you need to use the CLI to login
```
stripe login
```

Then you need to forward the webhook events to your server.
```
stripe listen --forward-to localhost:4242/webhook
```
You should see events logged in the console where the CLI is running. From then on, your server shall receive webhook events.

When you are ready to create a live webhook endpoint, follow our guide in the docs on [configuring a webhook endpoint in the dashboard](https://stripe.com/docs/webhooks/setup#configure-webhook-settings).


## Get support
If you found a bug or want to suggest a new [feature/use case/sample], please [file an issue](../../issues).

If you have questions, comments, or need help with code, we're here to help:
- on [Discord](https://stripe.com/go/developer-chat)
- on Twitter at [@StripeDev](https://twitter.com/StripeDev)
- on Stack Overflow at the [stripe-payments](https://stackoverflow.com/tags/stripe-payments/info) tag
- by [email](mailto:support+github@stripe.com)

Sign up to [stay updated with developer news](https://go.stripe.global/dev-digest).

## Author(s)

[@ctrudeau-stripe](https://twitter.com/trudeaucj)

## Events Created on Stripe's Dashboard on First Execution

````
The PaymentIntent id="pi_3NpEDqFGXpRXhvrd0tukpZa7" of 600 has succeeded.
A new SetupIntent was created.
The Subscription id="sub_1NpEEdFGXpRXhvrdHUoRMqQg" has been created.
````
|Event|At|
|----|----|
|[hjoly@xn.com subscribed to Recurrent daily](https://dashboard.stripe.com/test/events/evt_1NpEEeFGXpRXhvrdtAw1ADRn)|2023-09-11, 1:49:19 p.m.|
|[hjoly@xn.com's payment for an invoice for $0.00 succeeded](https://dashboard.stripe.com/test/events/evt_1NpEEeFGXpRXhvrdfnMlhXaR)|2023-09-11, 1:49:19 p.m.|
|[hjoly@xn.com's invoice for $0.00 was paid](https://dashboard.stripe.com/test/events/evt_1NpEEeFGXpRXhvrdAXvzeXou)|2023-09-11, 1:49:19 p.m.|
|[A draft invoice for $0.00 to hjoly@xn.com was finalised](https://dashboard.stripe.com/test/events/evt_1NpEEeFGXpRXhvrdnIC7FEjh)|2023-09-11, 1:49:19 p.m.|
|[A draft invoice was created](https://dashboard.stripe.com/test/events/evt_1NpEEeFGXpRXhvrdEtFVWMG3)|2023-09-11, 1:49:19 p.m.|
|[A proration adjustment for $0.61 was created for hjoly@xn.com](https://dashboard.stripe.com/test/events/evt_1NpEEeFGXpRXhvrd0kuX6kBf)|2023-09-11, 1:49:19 p.m.|
|[hjoly@xn.com's details were updated](https://dashboard.stripe.com/test/events/evt_1NpEEdFGXpRXhvrdvU3IBATG)|2023-09-11, 1:49:19 p.m.|
|[A new SetupIntent seti_1NpEEdFGXpRXhvrdzjMvGzqg was created](https://dashboard.stripe.com/test/events/evt_1NpEEdFGXpRXhvrd3XjxMNUo)|2023-09-11, 1:49:19 p.m.|
|[The payment pi_3NpEDqFGXpRXhvrd0tukpZa7 for $6.00 has succeeded](https://dashboard.stripe.com/test/events/evt_3NpEDqFGXpRXhvrd043DpTit)|2023-09-11, 1:48:37 p.m.|
|[hjoly@xn.com was charged $6.00](https://dashboard.stripe.com/test/events/evt_3NpEDqFGXpRXhvrd0q396RY0)|2023-09-11, 1:48:36 p.m.|

## Question
On September the 11th 2023, I have used a payment intent to create a one-shot payment of $6.00 and a subscription of $3.00 a week starting on the 13th of September. Both were attached to the same product, for a single customer. When I watch the resulting events on the Stripe Dashboard, I can see weird things concerning the subscription on its creation on September the 11th 2023: 
.1 There's a first invoice of $0.00. For this event, the Stripe Dashboard says "Invoice was finalised and automatically marked as paid because the amount due was $0.00".
.2 There's a second invoice which is a proration adjustment of $0.61. It is referring to the subscription with a description being "Time on Basic after 11 Sep 2023", a billing reason being "subscription_create" and finally,
.3 The weekly payments of $3.00 are collected starting from the 13th of September.
Can you explain these invoices for $0 and for $0.61 ?
It's a bit clumsy that an invoice of $0 gets sent to the customer. Can't we do anything against that?

https://docs.stripe.com/api/setup_intents/create#create_setup_intent-payment_method_options-acss_debit-mandate_options-interval_description
payment_method_options.acss_debit.mandate_options.interval_description



