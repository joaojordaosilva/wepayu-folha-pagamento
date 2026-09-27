package br.ufal.ic.p2.wepayu.models;

import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoExisteException;

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
}