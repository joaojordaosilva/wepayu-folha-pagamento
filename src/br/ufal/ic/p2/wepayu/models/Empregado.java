package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;
import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoExisteException;


public abstract class Empregado implements Serializable {
	private String nome;
	private String endereco;
	private String tipo;
	private int salario;
	private MembroSindicato sindicato;
	private String metodoPagamento = "emMaos"; // Padrão estabelecido pela regra de negocio
    private String banco;
    private String agencia;
    private String contaCorrente;

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
    
    public void setNome(String nome) { 
    	this.nome = nome; 
    	}
    public void setEndereco(String endereco) { 
    	this.endereco = endereco; 
    }
    public void setSalario(int salario) { 
    	this.salario = salario; 
    	}
    
 // Getters e Setters dos novos atributos bancários
    
    public void setMetodoPagamento(String metodoPagamento) {
    	this.metodoPagamento = metodoPagamento; 
    	}
    public String getMetodoPagamento() { 
    	return metodoPagamento; 
    	}
    
    public void setBanco(String banco) {
    	this.banco = banco; 
    	}
    public String getBanco() { 
    	return banco;
    	}
    
    public void setAgencia(String agencia) { 
    	this.agencia = agencia;
    	}
    public String getAgencia() { 
    	return agencia; 
    	}
    
    public void setContaCorrente(String contaCorrente) { 
    	this.contaCorrente = contaCorrente; 
    	}
    public String getContaCorrente() {
    	return contaCorrente; 
    	}
}