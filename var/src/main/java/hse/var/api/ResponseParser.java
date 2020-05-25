package hse.var.api;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import hse.var.stock.Stock;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class ResponseParser {

    private static final Logger logger = Logger.getLogger(ResponseParser.class.getName());

    public static final ResponseParser PARSER = new ResponseParser();

    private static final String DATE_KEY = "date";
    private static final String CLOSE_KEY = "close";

    public List<Stock> parse(String jsonString) {
        List<Stock> res = new ArrayList<>();
        try {
            JSONObject obj = new JSONObject(jsonString);
            JSONArray arr = obj.getJSONArray("prices");
            for (int i = 0; i < arr.length(); i++) {
                long date = arr.getJSONObject(i).getLong(DATE_KEY);
                double price = arr.getJSONObject(i).optDouble(CLOSE_KEY, -1);
                if (price != -1) {
                    res.add(new Stock(getValueAsLocalDate(date), price));
                }
            }
            return res;
        } catch (JSONException e) {
            logger.error("Failed to parse response", e);
            return null;
        }
    }

    private LocalDate getValueAsLocalDate(long l) {
        return Instant.ofEpochMilli(l * 1000).atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
