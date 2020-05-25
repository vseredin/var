package hse.var;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import hse.var.calc.VarCalculator;
import hse.var.calc.VarResult;
import hse.var.ctx.CalcContext;
import hse.var.stock.Stock;

import java.time.LocalDate;
import java.util.Map;

@SpringUI
public class MainView extends UI {

    private VerticalLayout root;
    private TextField stockCodeTextField;
    private TextField confidenceLevelTextField;
    private TextField timePeriodTextField;
    private DateField startDateField;
    private DateField endDateField;
    private VerticalLayout settingsLayout;
    private VerticalLayout resultsLayout;
    private TabSheet tabSheet;
    private Grid<Stock> stockData;
    private Label result;
    private Button calculate;

    private Binder<CalcContext> binder;

    @Override
    protected void init(VaadinRequest request) {
        root = new VerticalLayout();
        root.setDefaultComponentAlignment(Alignment.MIDDLE_CENTER);
        setContent(root);

        Label header = new Label("VaR calculator");
        header.addStyleName(ValoTheme.LABEL_H2);
        header.addStyleName(ValoTheme.LABEL_BOLD);
        root.addComponent(header);

        initSettingsLayout();
        initResultsLayout();

        tabSheet = new TabSheet();
        tabSheet.setWidth("25%");
        tabSheet.addTab(settingsLayout, "Settings");
        tabSheet.addTab(resultsLayout, "Results");
        root.addComponent(tabSheet);
    }

    private void initSettingsLayout() {
        settingsLayout = new VerticalLayout();
        settingsLayout.setDefaultComponentAlignment(Alignment.MIDDLE_CENTER);

        stockCodeTextField = new TextField("Stock code");
        stockCodeTextField.setPlaceholder("amzn");

        confidenceLevelTextField = new TextField("Confidence Level (in %)");
        confidenceLevelTextField.setPlaceholder("95");

        timePeriodTextField = new TextField("Time period (in days)");
        timePeriodTextField.setPlaceholder("1");

        startDateField = new DateField("Start date");
        startDateField.setRangeEnd(LocalDate.now().minusDays(2));
        startDateField.addValueChangeListener(valueChangeEvent -> {
            if (valueChangeEvent.getValue() != null) {
                endDateField.setRangeStart(valueChangeEvent.getValue().plusDays(2));
            } else {
                endDateField.setRangeStart(LocalDate.MIN);
            }
        });
        endDateField = new DateField("End date");
        endDateField.addValueChangeListener(valueChangeEvent -> {
            if (valueChangeEvent.getValue() != null) {
                startDateField.setRangeEnd(valueChangeEvent.getValue().minusDays(2));
            } else {
                startDateField.setRangeEnd(LocalDate.now().minusDays(2));
            }
        });
        endDateField.setRangeEnd(LocalDate.now());

        binder = new Binder<>(CalcContext.class);

        binder.forField(stockCodeTextField)
                .asRequired("Stoke code is required")
                .bind("stockCode");
        binder.forField(confidenceLevelTextField)
                .asRequired("Confidence level is required")
                .withConverter(new StringToIntegerConverter("Confidence level needs to be a number"))
                .withValidator(new IntegerRangeValidator("Confidence level needs to be between 1 and 99", 1, 99))
                .bind("confidenceLevel");
        binder.forField(timePeriodTextField)
                .asRequired("Time period is required")
                .withConverter(new StringToIntegerConverter("Time period needs to be a number"))
                .withValidator(new IntegerRangeValidator("Confidence level needs to be at least!", 1, null))
                .bind("timePeriod");
        binder.forField(startDateField)
                .asRequired("Start date is required")
                .bind("startDate");
        binder.forField(endDateField)
                .asRequired("End date is required")
                .bind("endDate");

        binder.addStatusChangeListener(status -> {
            calculate.setEnabled(!status.hasValidationErrors());
        });

        calculate = new Button("Calculate");
        calculate.addStyleName(ValoTheme.BUTTON_PRIMARY);
        calculate.setIcon(VaadinIcons.CALC);
        calculate.addClickListener(clickEvent -> {
            CalcContext ctx = new CalcContext();
            try {
                binder.writeBean(ctx);
            } catch (ValidationException e) {
                Notification.show(e.getValidationErrors().get(0).getErrorMessage(), Notification.Type.ERROR_MESSAGE);
                return;
            }
            VarResult varResult;
            try {
                varResult = VarCalculator.CALCULATOR.calculate(ctx);
            } catch (IllegalArgumentException e) {
                Notification.show(e.getMessage(), Notification.Type.ERROR_MESSAGE);
                return;
            }
            if (varResult == null) {
                Notification.show("Some error while fetching/parsing data occurred! Probably specified stock code not found", Notification.Type.ERROR_MESSAGE);
                return;
            }
            fillResultsLayout(varResult);
        });
        settingsLayout.addComponents(stockCodeTextField, confidenceLevelTextField, timePeriodTextField, startDateField, endDateField, calculate);
    }

    private void initResultsLayout() {
        resultsLayout = new VerticalLayout();
        resultsLayout.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);

        result = new Label("No data");
        result.setContentMode(ContentMode.PREFORMATTED);
        stockData = new Grid<>();
        stockData.setWidth("80%");
        stockData.addColumn(Stock::getDate).setCaption("Date");
        stockData.addColumn(s -> Math.round(s.getPrice() * 100.0) / 100.0).setCaption("Price");

        resultsLayout.addComponents(result, stockData);
    }

    private void fillResultsLayout(VarResult varResult) {
        stockData.setItems(varResult.getHistoricalData());
        tabSheet.setSelectedTab(1);
        result.setValue(generateResultContent(varResult.getResults()));
    }

    private String generateResultContent(Map<String, Double> metrics) {
        StringBuilder sb = new StringBuilder();
        for (String key : metrics.keySet()) {
            sb.append(key).append(": ").append(metrics.get(key)).append("\n");
        }
        return sb.toString();
    }
}
