package br.ufal.ic.p2.wepayu.models;

import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoExisteException;

public class Assalariado extends Empregado {
	public Assalariado(String nome, String endereco, String tipo, int salario) throws EmpregadoNaoExisteException {
		super(nome, endereco, tipo, salario);
	}
}