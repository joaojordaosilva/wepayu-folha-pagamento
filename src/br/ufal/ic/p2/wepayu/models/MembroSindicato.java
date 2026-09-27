package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MembroSindicato implements Serializable {
    private String idSindicato;
    private double taxaSindical;
    private List<TaxaServico> taxas = new ArrayList<>();

    public MembroSindicato(String idSindicato, double taxaSindical) {
        this.idSindicato = idSindicato;
        this.taxaSindical = taxaSindical;
    }

    public String getIdSindicato() { return idSindicato; }
    public double getTaxaSindical() { return taxaSindical; }
    public void adicionarTaxa(TaxaServico taxa) { taxas.add(taxa); }
    public List<TaxaServico> getTaxas() { return taxas; }
}