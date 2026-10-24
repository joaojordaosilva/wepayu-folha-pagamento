package br.ufal.ic.p2.wepayu.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Comissionado extends Empregado {
    private String comissao;
    private List<ResultadoDeVenda> vendas = new ArrayList<>();

    public Comissionado(String nome, String endereco, String tipo, int salario, String comissao) {
        super(nome, endereco, tipo, salario);
        this.comissao = comissao;
    }

    @Override
    public String getComissao() {
        return comissao;
    }
    
    @Override
    public void setComissao(String comissao) {
        this.comissao = comissao;
    }
	
    @Override
    public void adicionarVenda(ResultadoDeVenda venda) {
        this.vendas.add(venda);
    }

    @Override
    public List<ResultadoDeVenda> getVendas() {
        // Retorna uma visão imutável da lista
        return Collections.unmodifiableList(vendas);
    }
}