package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;
import java.util.List;

public abstract class Empregado implements Serializable {
    private String nome;
    private String endereco;
    private String tipo;
    private int salario;
    private MembroSindicato sindicato;
    private String metodoPagamento = "emMaos"; 
    private String banco;
    private String agencia;
    private String contaCorrente;

    // Construtor corrigido: removido o throws EmpregadoNaoExisteException
    public Empregado(String nome, String endereco, String tipo, int salario) {
        this.nome = nome;
        this.endereco = endereco;
        this.tipo = tipo;
        this.salario = salario;
    }

    public String getNome() { return nome; }
    public String getEndereco() { return endereco; }
    public String getTipo() { return tipo; }
    public int getSalario() { return salario; }
    
    public void setNome(String nome) { this.nome = nome; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public void setSalario(int salario) { this.salario = salario; }
    
    public void setSindicato(MembroSindicato sindicato) { this.sindicato = sindicato; }
    public MembroSindicato getSindicato() { return sindicato; }
    
    public void setMetodoPagamento(String metodoPagamento) { this.metodoPagamento = metodoPagamento; }
    public String getMetodoPagamento() { return metodoPagamento; }
    
    public void setBanco(String banco) { this.banco = banco; }
    public String getBanco() { return banco; }
    
    public void setAgencia(String agencia) { this.agencia = agencia; }
    public String getAgencia() { return agencia; }
    
    public void setContaCorrente(String contaCorrente) { this.contaCorrente = contaCorrente; }
    public String getContaCorrente() { return contaCorrente; }

    
    // MÉTODOS POLIMÓRFICOS (Comportamento padrão: lançar erro)
  
   
    public String getComissao() throws Exception {
        throw new Exception("Empregado nao eh comissionado.");
    }
    
    public void setComissao(String comissao) throws Exception {
        throw new Exception("Empregado nao eh comissionado.");
    }
    
    public void adicionarCartao(CartaoDePonto cartao) throws Exception {
        throw new Exception("Empregado nao eh horista.");
    }
    
    public List<CartaoDePonto> getCartoes() throws Exception {
        throw new Exception("Empregado nao eh horista.");
    }
    
    public void adicionarVenda(ResultadoDeVenda venda) throws Exception {
        throw new Exception("Empregado nao eh comissionado.");
    }
    
    public List<ResultadoDeVenda> getVendas() throws Exception {
        throw new Exception("Empregado nao eh comissionado.");
    }
}