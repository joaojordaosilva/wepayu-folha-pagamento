package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;
import java.time.LocalDate;

public class CartaoDePonto implements Serializable {
    private LocalDate data;
    private double horas;

    public CartaoDePonto(LocalDate data, double horas) {
        this.data = data;
        this.horas = horas;
    }

    public LocalDate getData() {
        return data;
    }

    public double getHorasNormais() {
        return Math.min(horas, 8.0);
    }

    public double getHorasExtras() {
        return Math.max(0, horas - 8.0);
    }
}