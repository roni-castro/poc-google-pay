package com.example.taqtile.googlepayexample;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.identity.intents.model.UserAddress;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.CardInfo;
import com.google.android.gms.wallet.CardRequirements;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.stripe.android.model.Token;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 53;

    private View mPayWithGoogleButton;
    private PaymentsClient mPaymentsClient;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPaymentsClient =
                Wallet.getPaymentsClient(this,
                        new Wallet.WalletOptions.Builder()
                                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                                .build());

        mProgressBar = findViewById(R.id.progress_bar);
        mPayWithGoogleButton = findViewById(R.id.google_pay_btn);
        mPayWithGoogleButton.setEnabled(false);
        mPayWithGoogleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                payWithGoogle();
            }
        });

        isReadyToPay();
    }

    private void payWithGoogle() {
        PaymentDataRequest request = createPaymentDataRequest();
        if (request != null) {
            AutoResolveHelper.resolveTask(
                    mPaymentsClient.loadPaymentData(request),
                    MainActivity.this,
                    LOAD_PAYMENT_DATA_REQUEST_CODE);
        }
    }


    /**
     * Check if can show the google pay button or not
     */
    private void isReadyToPay() {
        mProgressBar.setVisibility(View.VISIBLE);
        IsReadyToPayRequest request = IsReadyToPayRequest.newBuilder()
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                .build();
        Task<Boolean> task = mPaymentsClient.isReadyToPay(request);
        task.addOnCompleteListener(
                new OnCompleteListener<Boolean>() {
                    public void onComplete(@NonNull Task<Boolean> task) {
                        try {
                            boolean result =
                                    task.getResult(ApiException.class);
                            mProgressBar.setVisibility(View.INVISIBLE);
                            if(result) {
                                mPayWithGoogleButton.setEnabled(true);
                            } else {
                                Toast.makeText(MainActivity.this, "No PWG", Toast.LENGTH_SHORT).show();
                                //hide Google as payment option
                            }
                        } catch (ApiException exception) {
                            Toast.makeText(MainActivity.this,
                                    "Exception: " + exception.getLocalizedMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case LOAD_PAYMENT_DATA_REQUEST_CODE:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        PaymentData paymentData = PaymentData.getFromIntent(data);
                        // You can get some data on the user's card, such as the brand and last 4 digits
                        CardInfo info = paymentData.getCardInfo();
                        // You can also pull the user address from the PaymentData object.
                        UserAddress address = paymentData.getShippingAddress();
                        // This is the raw string version of your Stripe token.
                        String rawToken = paymentData.getPaymentMethodToken().getToken();

                        // Now that you have a Stripe token object, charge that by using the id
                        Token stripeToken = Token.fromString(rawToken);
                        if (stripeToken != null) {
                            // This chargeToken function is a call to your own server, which should then connect
                            // to Stripe's API to finish the charge.
                            // chargeToken(stripeToken.getId());
                            Toast.makeText(MainActivity.this,
                                    "Token recebido: " + stripeToken.toString()
                                            + "\n Detalhe do cartão: " + info.getCardDescription(), Toast.LENGTH_LONG).show();
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(MainActivity.this,
                                "Canceled", Toast.LENGTH_LONG).show();

                        break;
                    case AutoResolveHelper.RESULT_ERROR:
                        Status status = AutoResolveHelper.getStatusFromIntent(data);
                        Toast.makeText(MainActivity.this,
                                "Got error " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();

                        // Log the status for debugging
                        // Generally there is no need to show an error to
                        // the user as the Google Payment API will do that
                        break;
                    default:
                        // Do nothing.
                }
                break; // Breaks the case LOAD_PAYMENT_DATA_REQUEST_CODE
            default:
                // Do nothing.
        }
    }

    private PaymentMethodTokenizationParameters createTokenizationParameters() {
        return PaymentMethodTokenizationParameters.newBuilder()
                .setPaymentMethodTokenizationType(WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY)
                .addParameter("gateway", "stripe")
                .addParameter("stripe:publishableKey", "pk_test_TYooMQauvdEDq54NiTphI7jx")
                .addParameter("stripe:version", "5.1.1")
                .build();
    }

    private PaymentDataRequest createPaymentDataRequest() {
        PaymentDataRequest.Builder request =
                PaymentDataRequest.newBuilder()
                        .setTransactionInfo(
                                TransactionInfo.newBuilder()
                                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                                        .setTotalPrice("0.01")
                                        .setCurrencyCode("BRL")
                                        .build())
                        .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                        .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                        .setCardRequirements(
                                CardRequirements.newBuilder()
                                        .addAllowedCardNetworks(Arrays.asList(
                                                WalletConstants.CARD_NETWORK_AMEX,
                                                WalletConstants.CARD_NETWORK_DISCOVER,
                                                WalletConstants.CARD_NETWORK_VISA,
                                                WalletConstants.CARD_NETWORK_MASTERCARD))
                                        .build());

        request.setPaymentMethodTokenizationParameters(createTokenizationParameters());
        return request.build();
    }
}
