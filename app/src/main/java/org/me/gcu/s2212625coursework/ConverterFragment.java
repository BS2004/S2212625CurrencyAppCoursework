package org.me.gcu.s2212625coursework;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ConverterFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_RATIO = "ratio";

    private String title;
    private double ratio;

    TextView currencyTitle, resultText;
    EditText inputAmount;
    Button convertBtn, flipBtn;

    boolean flipped = false;

    public static ConverterFragment newInstance(String title, double ratio) {
        ConverterFragment fragment = new ConverterFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putDouble(ARG_RATIO, ratio);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE);
            ratio = getArguments().getDouble(ARG_RATIO, 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_converter, container, false);

        currencyTitle = view.findViewById(R.id.currencyTitle);
        inputAmount = view.findViewById(R.id.inputAmount);
        resultText = view.findViewById(R.id.resultText);
        convertBtn = view.findViewById(R.id.convertBtn);
        flipBtn = view.findViewById(R.id.flipBtn);

        Button homeButton = view.findViewById(R.id.homeButton);

        homeButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new HomeFragment(), false);
            }
        });

        if (!TextUtils.isEmpty(title)) {
            currencyTitle.setText(title + "\n(GBP --> " +  extractCurrencyCode(title));
        } else {
            currencyTitle.setText("Converter");
        }

        convertBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                convert();
            }
        });

        flipBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flip();
            }
        });

        return view;
    }

    private String extractCurrencyCode(String title) {

        int start = title.lastIndexOf("(");
        int end = title.lastIndexOf(")");

        if (start != -1 && end != -1 && end > start) {
            return title.substring(start + 1, end);
        }
        return "";
    }

    private void convert() {
        String inputStr = inputAmount.getText().toString();
        if (TextUtils.isEmpty(inputStr)) {
            resultText.setText("Enter an amount");
            return;
        }

        double amount = 0;
        try {
            amount = Double.parseDouble(inputStr);
        } catch (NumberFormatException e) {
            resultText.setText("Invalid number");
            return;
        }

        double result;

        if (!flipped) {

            result = amount * ratio;
            resultText.setText(amount + " GBP = " + result + extractCurrencyCode(title));
        } else {
            // Foreign → GBP
            if (ratio == 0) {
                resultText.setText("Conversion rate unavailable");
                return;
            }
            result = amount / ratio;
            resultText.setText(amount + extractCurrencyCode(title) + " = " + result + " GBP");
        }
    }

    private void flip() {
        flipped = !flipped;

        if (!flipped) {
            currencyTitle.setText(title + "\n(GBP --> )" + extractCurrencyCode(title));
        } else {
            currencyTitle.setText(title + "\n(" + extractCurrencyCode(title) + " --> GBP)");
        }

        resultText.setText("");
    }
}

