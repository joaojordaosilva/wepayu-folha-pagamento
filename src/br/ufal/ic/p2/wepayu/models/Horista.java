package br.ufal.ic.p2.wepayu.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Horista extends Empregado {
    private List<CartaoDePonto> cartoes = new ArrayList<>();

    public Horista(String nome, String endereco, String tipo, int salario) {
        super(nome, endereco, tipo, salario);
    }
	
    @Override
    public void adicionarCartao(CartaoDePonto cartao) {
        this.cartoes.add(cartao);
    }

    @Override
    public List<CartaoDePonto> getCartoes() {
        // Retorna uma visão imutável da lista
        return Collections.unmodifiableList(cartoes);
    }
}