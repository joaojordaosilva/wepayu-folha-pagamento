package br.ufal.ic.p2.wepayu.models;

import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoExisteException;
import java.util.ArrayList;
import java.util.List;

public class Comissionado extends Empregado {
	private String comissao;

	public Comissionado(String nome, String endereco, String tipo, int salario, String comissao)
			throws EmpregadoNaoExisteException {
		super(nome, endereco, tipo, salario);
		this.comissao = comissao;
	}

	public String getComissao() {
		return comissao;
	}
	
	private List<ResultadoDeVenda> vendas = new ArrayList<>();

    public void adicionarVenda(ResultadoDeVenda venda) {
        this.vendas.add(venda);
    }

    public List<ResultadoDeVenda> getVendas() {
        return vendas;
    }
    
    public void setComissao(String comissao) {
        this.comissao = comissao;
    }
	
}