package hse.var.api;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import hse.var.ctx.CalcContext;
import hse.var.stock.Stock;
import org.apache.log4j.Logger;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

public class YahooFinanceAPI {

    private static final Logger logger = Logger.getLogger(YahooFinanceAPI.class);

    public static final YahooFinanceAPI INSTANCE = new YahooFinanceAPI();

    private static final String HISTORICAL_DATA_API_URL = "https://apidojo-yahoo-finance-v1.p.rapidapi.com/stock/v2/get-historical-data?";
    private static final String X_RAPIDAPI_HOST = "apidojo-yahoo-finance-v1.p.rapidapi.com";
    private static final String X_RAPIDAPI_KEY = "5be39e9ccbmshe635d6652c00855p1adba3jsn4caa95514947";

    private HttpResponse<String> getResponse(CalcContext ctx) throws UnirestException {
        return Unirest.get(getAPIUrl(ctx))
                      .header("x-rapidapi-host", X_RAPIDAPI_HOST)
                      .header("x-rapidapi-key", X_RAPIDAPI_KEY)
                      .asString();
    }

    private String getAPIUrl(CalcContext ctx) {
        StringBuilder sb = new StringBuilder(HISTORICAL_DATA_API_URL);
        sb.append("frequency=1d&filter=history&period1=").append(localDateToEpochSeconds(ctx.getStartDate()));
        sb.append("&period2=").append(localDateToEpochSeconds(ctx.getEndDate().plusDays(1)));
        sb.append("&symbol=").append(ctx.getStockCode());
        return sb.toString();
    }

    public List<Stock> getHistoricalData(CalcContext ctx) {
        HttpResponse<String> response;
        try {
            response = getResponse(ctx);
        } catch (UnirestException ue) {
            logger.error("Failed to fetch historical data!", ue);
            return null;
        }
        List<Stock> res = ResponseParser.PARSER.parse(response.getBody());
        res.sort(Comparator.comparing(Stock::getDate));
        return res;
    }

    private long localDateToEpochSeconds(LocalDate date) {
        ZoneId zoneId = ZoneId.systemDefault();
        return date.atStartOfDay(zoneId).toEpochSecond();
    }
}
