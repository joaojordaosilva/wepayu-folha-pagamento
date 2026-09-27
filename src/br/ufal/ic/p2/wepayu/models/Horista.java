package br.ufal.ic.p2.wepayu.models;

import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoExisteException;
import java.util.ArrayList;
import java.util.List;


public class Horista extends Empregado {
	public Horista(String nome, String endereco, String tipo, int salario) throws EmpregadoNaoExisteException {
		super(nome, endereco, tipo, salario);
	}
	
	private List<CartaoDePonto> cartoes = new ArrayList<>();

    public void adicionarCartao(CartaoDePonto cartao) {
        this.cartoes.add(cartao);
    }

    public List<CartaoDePonto> getCartoes() {
        return cartoes;
    }
}