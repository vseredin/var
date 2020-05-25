package hse.var.calc;

import hse.var.ctx.CalcContext;
import hse.var.stock.Stock;
import hse.var.api.YahooFinanceAPI;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VarCalculator {

    private static final Map<Integer, Double> DEFAULT_Z_VALUES;
    static {
        DEFAULT_Z_VALUES = new LinkedHashMap<>();
        DEFAULT_Z_VALUES.put(99, 2.33);
        DEFAULT_Z_VALUES.put(95, 1.65);
        DEFAULT_Z_VALUES.put(90, 1.28);
    }

    public static final VarCalculator CALCULATOR = new VarCalculator();

    public VarResult calculate(CalcContext ctx) {
        List<Stock> historicalData = YahooFinanceAPI.INSTANCE.getHistoricalData(ctx);
        if (historicalData == null) {
            return null;
        }

        VarResult res = new VarResult(historicalData, ctx);
        Map<String, Double> resultsMap = new LinkedHashMap<>();

        int size = historicalData.size();

        if (size < 3) {
            throw new IllegalArgumentException("Between start and end date must be at least 3 trading days!");
        }

        double[] prices = historicalData.stream().mapToDouble(Stock::getPrice).toArray();
        double lastPrice = prices[size - 1];

        calculateExtraMetrics(prices, resultsMap);

        double[] yields = new double[size -1];
        for (int i = 1; i < size; i++) {
            yields[i - 1] = Math.log(prices[i] / prices[i - 1]);
        }
        if (DEFAULT_Z_VALUES.containsKey(ctx.getConfidenceLevel())) {
            calculateDeltaNormalVar(yields, lastPrice, ctx, resultsMap);
        }
        calculateHistoricalVar(yields, lastPrice, ctx, resultsMap);

        res.setResults(resultsMap);
        return res;
    }

    private void calculateDeltaNormalVar(double[] yields, double lastPrice, CalcContext ctx, Map<String, Double> resultsMap) {
        double std = getStd(yields, getMean(yields));
        double var = DEFAULT_Z_VALUES.get(ctx.getConfidenceLevel()) * std * lastPrice;

        resultsMap.put(VarResult.ONE_DAY_DELTA_NORMAL_VAR_KEY, var);

        if (ctx.getTimePeriod() != 1) {
            double varN = DEFAULT_Z_VALUES.get(ctx.getConfidenceLevel()) * std * lastPrice * Math.sqrt(ctx.getTimePeriod());
            String varNKey = "Delta normal VaR (" + ctx.getTimePeriod() + " days)";
            resultsMap.put(varNKey, varN);
        }
    }

    private void calculateHistoricalVar(double[] yields, double lastPrice, CalcContext ctx, Map<String, Double> resultsMap) {
        Arrays.sort(yields);
        Percentile p = new Percentile();
        double percentile = p.evaluate(yields, 100 - ctx.getConfidenceLevel());
        double minForecastPrice = lastPrice * (1 + percentile);
        double var = lastPrice - minForecastPrice;

        resultsMap.put(VarResult.ONE_DAY_HISTORICAL_VAR_KEY, var);

        if (ctx.getTimePeriod() != 1) {
            double minForecastPriceN = lastPrice * (1 + percentile * Math.sqrt(ctx.getTimePeriod()));
            double varN = lastPrice - minForecastPriceN;

            String varNKey = "Historical VaR (" + ctx.getTimePeriod() + " days)";
            resultsMap.put(varNKey, varN);
        }
    }

    private void calculateExtraMetrics(double[] prices, Map<String, Double> results) {
        double mean = getMean(prices);
        double std = getStd(prices, mean);
        results.put(VarResult.MEAN_KEY, mean);
        results.put(VarResult.STD_KEY, std);
    }

    private static double getMean(double[] array) {
        double sum = 0;
        for (double v : array) {
            sum += v;
        }
        return sum / array.length;
    }

    private static double getStd(double[] array, double mean) {
        return Math.sqrt(getVariance(array, mean));
    }

    private static double getVariance(double[] array, double mean)
    {
        double squaredDiff = 0;
        for (double v : array) {
            squaredDiff += Math.pow(v - mean, 2);
        }

        return squaredDiff / array.length;
    }
}
