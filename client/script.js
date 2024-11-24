var stripeElements = function(publicKey, setupIntent) {

  var stripe = Stripe(publicKey);
  /*// [me]:i18n
  var stripe = Stripe(publicKey, {
    locale: 'fr' // Remplacez 'fr' par le code de langue souhaité
  });*/

  var elements = stripe.elements();

  // Element styles
  var style = {
    base: {
      fontSize: "15px",
      color: "#32325d",
      fontFamily:
        "-apple-system, BlinkMacSystemFont, Segoe UI, Roboto, sans-serif",
      fontSmoothing: "antialiased",
      "::placeholder": {
        color: "rgba(0,0,0,0.4)"
      }
    }
  };

  const form = document.getElementById('payment-form');
  const accountholderName = document.getElementById('accountholder-name');
  const email = document.getElementById('email');
  // const submitButton = document.getElementById('submit-button');
  const clientSecret = setupIntent.client_secret;

  form.addEventListener('submit', async (event) => {
    event.preventDefault();

    const {setupIntent, error} = await stripe.confirmAcssDebitSetup(
        clientSecret,
        {
          payment_method: {
            billing_details: {
              name: accountholderName.value,
              email: email.value,
            },
          },
        }
    ).then(function(result) {
      if (result.error) {

        // Inform the customer that there was an error.
        console.log(result.error.message);

        changeLoadingState(false);
        var displayError = document.getElementById("pad-errors");
        displayError.textContent = result.error.message;
      } else {
        // Handle next step based on SetupIntent's status.
        console.log("SetupIntent ID: " + result.setupIntent.id);
        console.log("SetupIntent status: " + result.setupIntent.status);

        // The PaymentMethod was successfully set up
        orderComplete(stripe, result.setupIntent.client_secret);
      }
    });
  });
};


var getSetupIntent = function(publicKey) {
  return fetch("/create-setup-intent", {
    method: "post",
    headers: {
      "Content-Type": "application/json"
    }
  })
    .then(function(response) {
      return response.json();
    })
    .then(function(setupIntent) {
      stripeElements(publicKey, setupIntent);
    });
};


var getPublicKey = function() {
  return fetch("/public-key", {
    method: "get",
    headers: {
      "Content-Type": "application/json"
    }
  })
    .then(function(response) {
      return response.json();
    })
    .then(function(response) {
      getSetupIntent(response.publicKey);
    });
};


// Show a spinner on payment submission
var changeLoadingState = function(isLoading) {
  if (isLoading) {
    document.querySelector("button").disabled = true;
    document.querySelector("#spinner").classList.remove("hidden");
    document.querySelector("#button-text").classList.add("hidden");
  } else {
    document.querySelector("button").disabled = false;
    document.querySelector("#spinner").classList.add("hidden");
    document.querySelector("#button-text").classList.remove("hidden");
  }
};


/* Shows a success / error message when the payment is complete */
var orderComplete = function(stripe, clientSecret) {
  stripe.retrieveSetupIntent(clientSecret).then(function(result) {
    var setupIntent = result.setupIntent;
    var setupIntentJson = JSON.stringify(setupIntent, null, 2);

    document.querySelector(".sr-payment-form").classList.add("hidden");
    document.querySelector(".sr-result").classList.remove("hidden");
    document.querySelector("pre").textContent = setupIntentJson;
    setTimeout(function() {
      document.querySelector(".sr-result").classList.add("expand");
    }, 200);

    changeLoadingState(false);
  });
};

getPublicKey();
