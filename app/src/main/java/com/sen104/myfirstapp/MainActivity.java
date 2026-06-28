package com.sen104.myfirstapp;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    TextView tvResult, tvExpression;
    LinearLayout layoutScientific;
    Button btnDegRad;

    List<Object> tokens = new ArrayList<>();
    String currentInput = "";
    boolean isDegree = true;

    String pendingScientificOp = "";
    double pendingScientificFirst = 0;
    boolean waitingForSecondSciOperand = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvResult     = findViewById(R.id.tvResult);
        tvExpression = findViewById(R.id.tvExpression);
        layoutScientific = findViewById(R.id.layoutScientific);
        btnDegRad    = findViewById(R.id.btnDegRad);

        findViewById(R.id.btnModeBasic).setOnClickListener(v ->
                layoutScientific.setVisibility(View.GONE));
        findViewById(R.id.btnModeScientific).setOnClickListener(v ->
                layoutScientific.setVisibility(View.VISIBLE));

        int[]    numIds = {R.id.btn0,R.id.btn1,R.id.btn2,R.id.btn3,R.id.btn4,
                R.id.btn5,R.id.btn6,R.id.btn7,R.id.btn8,R.id.btn9};
        String[] numVals = {"0","1","2","3","4","5","6","7","8","9"};
        for (int i = 0; i < numIds.length; i++) {
            final String val = numVals[i];
            findViewById(numIds[i]).setOnClickListener(v -> appendDigit(val));
        }

        findViewById(R.id.btnDot).setOnClickListener(v -> appendDot());

        findViewById(R.id.btnAdd).setOnClickListener(v -> appendOperator("+"));
        findViewById(R.id.btnSubtract).setOnClickListener(v -> appendOperator("-"));
        findViewById(R.id.btnMultiply).setOnClickListener(v -> appendOperator("×"));
        findViewById(R.id.btnDivide).setOnClickListener(v -> appendOperator("÷"));

        findViewById(R.id.btnEquals).setOnClickListener(v -> evaluateExpression());

        findViewById(R.id.btnClear).setOnClickListener(v -> clearAll());
        findViewById(R.id.btnBackspace).setOnClickListener(v -> backspace());
        findViewById(R.id.btnToggleSign).setOnClickListener(v -> toggleSign());
        findViewById(R.id.btnPercent).setOnClickListener(v -> applyPercent());

        findViewById(R.id.btnSin).setOnClickListener(v -> applyTrig("sin"));
        findViewById(R.id.btnCos).setOnClickListener(v -> applyTrig("cos"));
        findViewById(R.id.btnTan).setOnClickListener(v -> applyTrig("tan"));
        findViewById(R.id.btnSinh).setOnClickListener(v -> applyHyperbolic("sinh"));
        findViewById(R.id.btnCosh).setOnClickListener(v -> applyHyperbolic("cosh"));
        findViewById(R.id.btnTanh).setOnClickListener(v -> applyHyperbolic("tanh"));
        findViewById(R.id.btnLog).setOnClickListener(v -> applySingleOp("log"));
        findViewById(R.id.btnLn).setOnClickListener(v -> applySingleOp("ln"));
        findViewById(R.id.btnSquare).setOnClickListener(v -> applySingleOp("x²"));
        findViewById(R.id.btnSqrt).setOnClickListener(v -> applySingleOp("√"));
        findViewById(R.id.btnInverse).setOnClickListener(v -> applySingleOp("1/x"));
        findViewById(R.id.btnAbs).setOnClickListener(v -> applySingleOp("|x|"));
        findViewById(R.id.btnFactorial).setOnClickListener(v -> applySingleOp("n!"));

        findViewById(R.id.btnPi).setOnClickListener(v -> insertConstant(Math.PI, "π"));
        findViewById(R.id.btnE).setOnClickListener(v -> insertConstant(Math.E, "e"));

        findViewById(R.id.btnPow).setOnClickListener(v -> startTwoOpSci("xʸ"));
        findViewById(R.id.btnPerm).setOnClickListener(v -> startTwoOpSci("nPr"));
        findViewById(R.id.btnComb).setOnClickListener(v -> startTwoOpSci("nCr"));
        findViewById(R.id.btnMod).setOnClickListener(v -> startTwoOpSci("mod"));

        btnDegRad.setOnClickListener(v -> {
            isDegree = !isDegree;
            btnDegRad.setText(isDegree ? "DEG" : "RAD");
        });
    }

    private void appendDigit(String digit) {
        currentInput += digit;
        tvResult.setText(currentInput);
        updateExpressionDisplay();
    }

    private void appendDot() {
        if (!currentInput.contains(".")) {
            if (currentInput.isEmpty()) currentInput = "0";
            currentInput += ".";
            tvResult.setText(currentInput);
        }
    }


    private void appendOperator(String op) {
        if (currentInput.isEmpty() && tokens.isEmpty()) return;

        if (!currentInput.isEmpty()) {
            tokens.add(Double.parseDouble(currentInput));
            currentInput = "";
        } else if (!tokens.isEmpty()) {
            // Replace last operator if user pressed two operators in a row
            Object last = tokens.get(tokens.size() - 1);
            if (last instanceof String) {
                tokens.set(tokens.size() - 1, op);
                tvExpression.setText(buildExpressionString());
                return;
            }
        }

        tokens.add(op);
        currentInput = "";
        tvResult.setText("0");
        updateExpressionDisplay();
    }

    private void evaluateExpression() {
        // Handle pending two-operand scientific op first
        if (waitingForSecondSciOperand) {
            finishTwoOpSci();
            return;
        }

        if (!currentInput.isEmpty()) {
            tokens.add(Double.parseDouble(currentInput));
            currentInput = "";
        }

        if (tokens.isEmpty()) return;

        tvExpression.setText(buildExpressionString() + " =");

        try {
            double result = computeTokens(new ArrayList<>(tokens));
            String resultStr = formatNum(result);
            tvResult.setText(resultStr);

            tokens.clear();
            currentInput = resultStr;
        } catch (Exception e) {
            tvResult.setText("Error");
            tokens.clear();
            currentInput = "";
        }
    }


    private double computeTokens(List<Object> list) {
        int i = 1;
        while (i < list.size()) {
            Object token = list.get(i);
            if (token instanceof String) {
                String op = (String) token;
                if (op.equals("×") || op.equals("÷")) {
                    double left  = (Double) list.get(i - 1);
                    double right = (Double) list.get(i + 1);
                    double res;
                    if (op.equals("×")) {
                        res = left * right;
                    } else {
                        if (right == 0) throw new ArithmeticException("Division by zero");
                        res = left / right;
                    }
                    list.set(i - 1, res);
                    list.remove(i);
                    list.remove(i);
                } else {
                    i += 2;
                }
            } else {
                i++;
            }
        }

        double result = (Double) list.get(0);
        i = 1;
        while (i < list.size()) {
            String op    = (String) list.get(i);
            double right = (Double) list.get(i + 1);
            if (op.equals("+")) result += right;
            else if (op.equals("-")) result -= right;
            i += 2;
        }

        return result;
    }

    private String buildExpressionString() {
        StringBuilder sb = new StringBuilder();
        for (Object token : tokens) {
            if (token instanceof Double) {
                sb.append(formatNum((Double) token));
            } else {
                sb.append(" ").append(token).append(" ");
            }
        }
        if (!currentInput.isEmpty()) {
            if (!tokens.isEmpty()) sb.append(currentInput);
            else sb.append(currentInput);
        }
        return sb.toString().trim();
    }

    private void updateExpressionDisplay() {
        tvExpression.setText(buildExpressionString());
    }

    private void clearAll() {
        tokens.clear();
        currentInput = "";
        waitingForSecondSciOperand = false;
        pendingScientificOp = "";
        tvResult.setText("0");
        tvExpression.setText("");
    }

    private void backspace() {
        if (!currentInput.isEmpty()) {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
            tvResult.setText(currentInput.isEmpty() ? "0" : currentInput);
            updateExpressionDisplay();
        } else if (!tokens.isEmpty()) {
            tokens.remove(tokens.size() - 1);
            if (!tokens.isEmpty() && tokens.get(tokens.size()-1) instanceof Double) {
                currentInput = formatNum((Double) tokens.remove(tokens.size()-1));
                tvResult.setText(currentInput);
            }
            updateExpressionDisplay();
        }
    }

    private void toggleSign() {
        if (!currentInput.isEmpty() && !currentInput.equals("0")) {
            currentInput = currentInput.startsWith("-")
                    ? currentInput.substring(1)
                    : "-" + currentInput;
            tvResult.setText(currentInput);
        }
    }

    private void applyPercent() {
        if (!currentInput.isEmpty()) {
            double val = Double.parseDouble(currentInput) / 100.0;
            currentInput = formatNum(val);
            tvResult.setText(currentInput);
        }
    }

    private void insertConstant(double value, String label) {
        currentInput = formatNum(value);
        tvResult.setText(currentInput);
        tvExpression.setText(label);
    }

    private void applyTrig(String func) {
        if (currentInput.isEmpty()) return;
        double val = Double.parseDouble(currentInput);
        double angle = isDegree ? Math.toRadians(val) : val;
        double result;
        switch (func) {
            case "sin": result = Math.sin(angle); break;
            case "cos": result = Math.cos(angle); break;
            case "tan":
                if (Math.abs(Math.cos(angle)) < 1e-10) {
                    tvResult.setText("Undefined"); return;
                }
                result = Math.tan(angle); break;
            default: return;
        }
        tvExpression.setText(func + "(" + formatNum(val) + ")");
        currentInput = formatNum(result);
        tvResult.setText(currentInput);
    }

    private void applyHyperbolic(String func) {
        if (currentInput.isEmpty()) return;
        double val = Double.parseDouble(currentInput);
        double result;
        switch (func) {
            case "sinh": result = Math.sinh(val); break;
            case "cosh": result = Math.cosh(val); break;
            case "tanh": result = Math.tanh(val); break;
            default: return;
        }
        tvExpression.setText(func + "(" + formatNum(val) + ")");
        currentInput = formatNum(result);
        tvResult.setText(currentInput);
    }

    private void applySingleOp(String op) {
        if (currentInput.isEmpty()) return;
        double val = Double.parseDouble(currentInput);
        double result;
        String expr;
        switch (op) {
            case "log":
                if (val <= 0) { tvResult.setText("Error"); return; }
                result = Math.log10(val); expr = "log(" + formatNum(val) + ")"; break;
            case "ln":
                if (val <= 0) { tvResult.setText("Error"); return; }
                result = Math.log(val); expr = "ln(" + formatNum(val) + ")"; break;
            case "x²":
                result = val * val; expr = formatNum(val) + "²"; break;
            case "√":
                if (val < 0) { tvResult.setText("Error"); return; }
                result = Math.sqrt(val); expr = "√(" + formatNum(val) + ")"; break;
            case "1/x":
                if (val == 0) { tvResult.setText("Error"); return; }
                result = 1.0 / val; expr = "1/(" + formatNum(val) + ")"; break;
            case "|x|":
                result = Math.abs(val); expr = "|" + formatNum(val) + "|"; break;
            case "n!":
                if (val < 0 || val != (long) val || val > 20) {
                    tvResult.setText("Error"); return;
                }
                result = factorial((long) val); expr = (long) val + "!"; break;
            default: return;
        }
        tvExpression.setText(expr);
        currentInput = formatNum(result);
        tvResult.setText(currentInput);
    }

    private void startTwoOpSci(String op) {
        if (currentInput.isEmpty()) return;
        pendingScientificFirst = Double.parseDouble(currentInput);
        pendingScientificOp = op;
        waitingForSecondSciOperand = true;
        tvExpression.setText(formatNum(pendingScientificFirst) + " " + op + " ?");
        currentInput = "";
        tvResult.setText("0");
    }

    private void finishTwoOpSci() {
        if (currentInput.isEmpty()) return;
        double n = pendingScientificFirst;
        double r = Double.parseDouble(currentInput);
        double result;
        String expr;
        switch (pendingScientificOp) {
            case "xʸ":
                result = Math.pow(n, r);
                expr = formatNum(n) + "^" + formatNum(r) + " ="; break;
            case "nPr":
                if (r < 0 || r > n || n != (long)n || r != (long)r) {
                    tvResult.setText("Error"); resetSciState(); return;
                }
                result = factorial((long)n) / factorial((long)(n - r));
                expr = (long)n + "P" + (long)r + " ="; break;
            case "nCr":
                if (r < 0 || r > n || n != (long)n || r != (long)r) {
                    tvResult.setText("Error"); resetSciState(); return;
                }
                result = factorial((long)n) / (factorial((long)r) * factorial((long)(n-r)));
                expr = (long)n + "C" + (long)r + " ="; break;
            case "mod":
                if (r == 0) { tvResult.setText("Error"); resetSciState(); return; }
                result = n % r;
                expr = formatNum(n) + " mod " + formatNum(r) + " ="; break;
            default: return;
        }
        tvExpression.setText(expr);
        currentInput = formatNum(result);
        tvResult.setText(currentInput);
        tokens.clear();
        resetSciState();
    }

    private void resetSciState() {
        waitingForSecondSciOperand = false;
        pendingScientificOp = "";
    }

    private long factorial(long n) {
        if (n <= 1) return 1;
        return n * factorial(n - 1);
    }

    private String formatNum(double val) {
        if (Double.isNaN(val)) return "Error";
        if (Double.isInfinite(val)) return val > 0 ? "∞" : "-∞";
        if (val == (long) val) return String.valueOf((long) val);
        return String.valueOf(val);
    }
}
