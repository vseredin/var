package hse.var.calc;

import hse.var.ctx.CalcContext;
import hse.var.stock.Stock;

import java.util.List;
import java.util.Map;

public class VarResult {

    private final List<Stock> historicalData;
    private final CalcContext ctx;
    private Map<String, Double> results;

    public static final String MEAN_KEY = "Mean";
    public static final String STD_KEY = "Standard deviation";
    public static final String ONE_DAY_DELTA_NORMAL_VAR_KEY = "Delta normal VaR (1 day)";
    public static final String ONE_DAY_HISTORICAL_VAR_KEY = "Historical VaR (1 day)";

    public VarResult(List<Stock> historicalData, CalcContext ctx) {
        this.historicalData = historicalData;
        this.ctx = ctx;
    }

    public void setResults(Map<String, Double> results) {
        this.results = results;
    }

    public List<Stock> getHistoricalData() {
        return historicalData;
    }

    public CalcContext getCtx() {
        return ctx;
    }

    public Map<String, Double> getResults() {
        return results;
    }
}
