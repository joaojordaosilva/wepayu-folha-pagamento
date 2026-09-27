package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;
import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoExisteException;


public abstract class Empregado implements Serializable {
	private String nome;
	private String endereco;
	private String tipo;
	private int salario;
	private MembroSindicato sindicato;

	public Empregado(String nome, String endereco, String tipo, int salario) throws EmpregadoNaoExisteException {
		this.nome = nome;
		this.endereco = endereco;
		this.tipo = tipo;
		this.salario = salario;
	}

	public String getNome() {
		return nome;
	}

	public String getEndereco() {
		return endereco;
	}

	public String getTipo() {
		return tipo;
	}

	public int getSalario() {
		return salario;
	}
	
	public void setSindicato(MembroSindicato sindicato) {
        this.sindicato = sindicato;
    }

    public MembroSindicato getSindicato() {
        return sindicato;
    }
}