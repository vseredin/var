package hse.var.stock;

import java.time.LocalDate;

public class Stock {

    private LocalDate date;
    private double price;

    public Stock() {}

    public Stock(LocalDate date, double price) {
        this.date = date;
        this.price = price;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getPrice() {
        return price;
    }
}
