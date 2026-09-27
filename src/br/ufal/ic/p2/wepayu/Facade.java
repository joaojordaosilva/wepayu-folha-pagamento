package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoExisteException;
import br.ufal.ic.p2.wepayu.models.*;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDate;

public class Facade {
    private Map<String, Empregado> empregados;
    private final String ARQUIVO_DADOS = "empregados.dat";

    public Facade() {
        // Tenta carregar o arquivo ao iniciar a Facade
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(ARQUIVO_DADOS))) {
            empregados = (Map<String, Empregado>) ois.readObject();
        } catch (Exception e) {
            // Se o arquivo não existir ou der erro, inicia um mapa vazio
            empregados = new LinkedHashMap<>();
        }
    }
    
    public void zerarSistema() {
        empregados.clear();
        // Opcional: apagar o arquivo fisicamente aqui também
        new File(ARQUIVO_DADOS).delete();
    }

    public void encerrarSistema() {
        // Salva o mapa no arquivo ao encerrar
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ARQUIVO_DADOS))) {
            oos.writeObject(empregados);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	
	//US1
	public String criarEmpregado(String nome, String endereco, String tipo, String salario) throws Exception {

		if (nome == null || nome.isEmpty())
			throw new Exception("Nome nao pode ser nulo.");
		if (endereco == null || endereco.isEmpty())
			throw new Exception("Endereco nao pode ser nulo.");
		if (tipo == null || (!tipo.equals("horista") && !tipo.equals("assalariado") && !tipo.equals("comissionado"))) {
			throw new Exception("Tipo invalido.");
		}

		if (tipo.equals("comissionado"))
			throw new Exception("Tipo nao aplicavel.");
		if (salario == null || salario.isEmpty())
			throw new Exception("Salario nao pode ser nulo.");

		double salDouble;
		try {
			salDouble = Double.parseDouble(salario.replace(",", "."));
		} catch (NumberFormatException e) {
			throw new Exception("Salario deve ser numerico.");
		}
		if (salDouble < 0)
			throw new Exception("Salario deve ser nao-negativo.");

		int salInt = (int) Math.round(salDouble * 100);
		String id = UUID.randomUUID().toString().substring(0, 8);

		Empregado emp = tipo.equals("horista") ? new Horista(nome, endereco, tipo, salInt)
				: new Assalariado(nome, endereco, tipo, salInt);

		empregados.put(id, emp);
		return id;
	}

	public String criarEmpregado(String nome, String endereco, String tipo, String salario, String comissao)
			throws Exception {
		if (nome == null || nome.isEmpty())
			throw new Exception("Nome nao pode ser nulo.");
		if (endereco == null || endereco.isEmpty())
			throw new Exception("Endereco nao pode ser nulo.");
		if (tipo == null || (!tipo.equals("horista") && !tipo.equals("assalariado") && !tipo.equals("comissionado"))) {
			throw new Exception("Tipo invalido.");
		}
		if (tipo.equals("horista") || tipo.equals("assalariado"))
			throw new Exception("Tipo nao aplicavel.");

		if (salario == null || salario.isEmpty())
			throw new Exception("Salario nao pode ser nulo.");
		double salDouble;
		try {
			salDouble = Double.parseDouble(salario.replace(",", "."));
		} catch (NumberFormatException e) {
			throw new Exception("Salario deve ser numerico.");
		}
		if (salDouble < 0)
			throw new Exception("Salario deve ser nao-negativo.");
		int salInt = (int) Math.round(salDouble * 100);

		if (comissao == null || comissao.isEmpty())
			throw new Exception("Comissao nao pode ser nula.");
		double comDouble;
		try {
			comDouble = Double.parseDouble(comissao.replace(",", "."));
		} catch (NumberFormatException e) {
			throw new Exception("Comissao deve ser numerica.");
		}
		if (comDouble < 0)
			throw new Exception("Comissao deve ser nao-negativa.");

		String id = UUID.randomUUID().toString().substring(0, 8);
		Empregado emp = new Comissionado(nome, endereco, tipo, salInt, comissao);
		empregados.put(id, emp);
		return id;
	}

	public String getAtributoEmpregado(String emp, String atributo) throws Exception {
	    if (emp == null || emp.isEmpty())
	        throw new Exception("Identificacao do empregado nao pode ser nula.");

	    Empregado e = empregados.get(emp);
	    if (e == null)
	        throw new EmpregadoNaoExisteException();

	    if (atributo.equals("nome")) return e.getNome();
	    if (atributo.equals("endereco")) return e.getEndereco();
	    if (atributo.equals("tipo")) return e.getTipo();

	    if (atributo.equals("salario")) {
	        int sal = e.getSalario();
	        return String.format("%d,%02d", sal / 100, sal % 100);
	    }

	    if (atributo.equals("sindicalizado")) {
	        return (e.getSindicato() != null) ? "true" : "false";
	    }

	    if (atributo.equals("comissao")) {
	        if (!(e instanceof Comissionado)) throw new Exception("Empregado nao eh comissionado.");
	        return ((Comissionado) e).getComissao();
	    }

	    if (atributo.equals("metodoPagamento")) return e.getMetodoPagamento();
	    
	    if (atributo.equals("banco") || atributo.equals("agencia") || atributo.equals("contaCorrente")) {
	        if (!e.getMetodoPagamento().equals("banco")) throw new Exception("Empregado nao recebe em banco.");
	        if (atributo.equals("banco")) return e.getBanco();
	        if (atributo.equals("agencia")) return e.getAgencia();
	        return e.getContaCorrente();
	    }
	    
	    if (atributo.equals("idSindicato")) {
	        if (e.getSindicato() == null) throw new Exception("Empregado nao eh sindicalizado.");
	        return e.getSindicato().getIdSindicato();
	    }
	    
	    if (atributo.equals("taxaSindical")) {
	        if (e.getSindicato() == null) throw new Exception("Empregado nao eh sindicalizado.");
	        return String.format("%.2f", e.getSindicato().getTaxaSindical()).replace(".", ",");
	    }
	    
	    throw new Exception("Atributo nao existe.");	
	}
	//US1_1
	
	public String getEmpregadoPorNome(String nome, int indice) throws Exception {
	    int count = 0;
	    
	    for (Map.Entry<String, Empregado> entry : empregados.entrySet()) {
	        if (entry.getValue().getNome().contains(nome)) {
	            count++;
	            if (count == indice) {
	                return entry.getKey();
	            }
	        }
	    }
	    
	    throw new Exception("Nao ha empregado com esse nome.");
	}
	
	
	//US2
	public void removerEmpregado(String emp) throws Exception {
        if (emp == null || emp.isEmpty()) {
            throw new Exception("Identificacao do empregado nao pode ser nula.");
        }
        
        if (!empregados.containsKey(emp)) {
            throw new EmpregadoNaoExisteException();
        }
        
        empregados.remove(emp);
    }
	
	//US3
	
	private LocalDate parseData(String dataStr, String msgErro) throws Exception {
        try {
            String[] p = dataStr.split("/");
            return LocalDate.of(Integer.parseInt(p[2]), Integer.parseInt(p[1]), Integer.parseInt(p[0]));
        } catch (Exception e) {
            throw new Exception(msgErro);
        }
    }

    // Formata o double para não mostrar "8.0" se for um número inteiro
    private String formatarHoras(double horas) {
        if (horas % 1 == 0) {
            return String.valueOf((long) horas);
        }
        return String.valueOf(horas).replace(".", ",");
    }
    
    public void lancaCartao(String emp, String data, String horas) throws Exception {
        if (emp == null || emp.isEmpty()) throw new Exception("Identificacao do empregado nao pode ser nula.");
        
        Empregado e = empregados.get(emp);
        if (e == null) throw new EmpregadoNaoExisteException();
        if (!(e instanceof Horista)) throw new Exception("Empregado nao eh horista.");

        LocalDate d = parseData(data, "Data invalida.");
        
        double h = Double.parseDouble(horas.replace(",", "."));
        if (h <= 0) throw new Exception("Horas devem ser positivas.");

        ((Horista) e).adicionarCartao(new CartaoDePonto(d, h));
    }
    
    private double calcularHoras(String emp, String dataInicial, String dataFinal, boolean isExtra) throws Exception {
        Empregado e = empregados.get(emp);
        if (e == null) throw new EmpregadoNaoExisteException();
        if (!(e instanceof Horista)) throw new Exception("Empregado nao eh horista.");

        LocalDate inicio = parseData(dataInicial, "Data inicial invalida.");
        LocalDate fim = parseData(dataFinal, "Data final invalida.");

        if (inicio.isAfter(fim)) throw new Exception("Data inicial nao pode ser posterior aa data final.");

        double total = 0;
        for (CartaoDePonto c : ((Horista) e).getCartoes()) {
            // A data do cartão tem de ser >= inicio e < fim
            if (!c.getData().isBefore(inicio) && c.getData().isBefore(fim)) {
                total += isExtra ? c.getHorasExtras() : c.getHorasNormais();
            }
        }
        return total;
    }

    public String getHorasNormaisTrabalhadas(String emp, String dataInicial, String dataFinal) throws Exception {
        return formatarHoras(calcularHoras(emp, dataInicial, dataFinal, false));
    }

    public String getHorasExtrasTrabalhadas(String emp, String dataInicial, String dataFinal) throws Exception {
        return formatarHoras(calcularHoras(emp, dataInicial, dataFinal, true));
    }
    
    
	//US4
    
    public void lancaVenda(String emp, String data, String valor) throws Exception {
        if (emp == null || emp.isEmpty()) throw new Exception("Identificacao do empregado nao pode ser nula.");
        
        Empregado e = empregados.get(emp);
        if (e == null) throw new EmpregadoNaoExisteException();
        if (!(e instanceof Comissionado)) throw new Exception("Empregado nao eh comissionado.");

        LocalDate d = parseData(data, "Data invalida.");
        
        double v = Double.parseDouble(valor.replace(",", "."));
        if (v <= 0) throw new Exception("Valor deve ser positivo.");

        ((Comissionado) e).adicionarVenda(new ResultadoDeVenda(d, v));
    }

    public String getVendasRealizadas(String emp, String dataInicial, String dataFinal) throws Exception {
        Empregado e = empregados.get(emp);
        if (e == null) throw new EmpregadoNaoExisteException();
        if (!(e instanceof Comissionado)) throw new Exception("Empregado nao eh comissionado.");

        LocalDate inicio = parseData(dataInicial, "Data inicial invalida.");
        LocalDate fim = parseData(dataFinal, "Data final invalida.");

        if (inicio.isAfter(fim)) throw new Exception("Data inicial nao pode ser posterior aa data final.");

        double total = 0;
        for (ResultadoDeVenda v : ((Comissionado) e).getVendas()) {
            // Regra: inclui o dia inicial e exclui o dia final
            if (!v.getData().isBefore(inicio) && v.getData().isBefore(fim)) {
                total += v.getValor();
            }
        }
        
        // Formata para 2 casas decimais e troca ponto por vírgula
        return String.format("%.2f", total).replace(".", ",");
    }
    
    
 //US5

    private Empregado buscarPorSindicato(String idSindicato) {
        for (Empregado e : empregados.values()) {
            if (e.getSindicato() != null && e.getSindicato().getIdSindicato().equals(idSindicato)) {
                return e;
            }
        }
        return null;
    }



    public void lancaTaxaServico(String membro, String data, String valor) throws Exception {
        if (membro == null || membro.isEmpty()) throw new Exception("Identificacao do membro nao pode ser nula.");
        
        Empregado emp = buscarPorSindicato(membro);
        if (emp == null) throw new Exception("Membro nao existe.");

        LocalDate d = parseData(data, "Data invalida.");
        
        double v = Double.parseDouble(valor.replace(",", "."));
        if (v <= 0) throw new Exception("Valor deve ser positivo.");

        emp.getSindicato().adicionarTaxa(new TaxaServico(d, v));
    }

    public String getTaxasServico(String emp, String dataInicial, String dataFinal) throws Exception {
        Empregado e = empregados.get(emp);
        if (e == null) throw new EmpregadoNaoExisteException();
        if (e.getSindicato() == null) throw new Exception("Empregado nao eh sindicalizado.");

        LocalDate inicio = parseData(dataInicial, "Data inicial invalida.");
        LocalDate fim = parseData(dataFinal, "Data final invalida.");

        if (inicio.isAfter(fim)) throw new Exception("Data inicial nao pode ser posterior aa data final.");

        double total = 0;
        for (TaxaServico t : e.getSindicato().getTaxas()) {
            if (!t.getData().isBefore(inicio) && t.getData().isBefore(fim)) {
                total += t.getValor();
            }
        }
        
        return String.format("%.2f", total).replace(".", ",");
    }
	
    //US6 
    
 // Método auxiliar para trocar a classe do empregado sem perder os dados vitais
    private void atualizarTipoEmpregado(String emp, Empregado e, String novoTipo, int novoSalario, String novaComissao) throws Exception {
        Empregado novo = null;
        if (novoTipo.equals("horista")) novo = new Horista(e.getNome(), e.getEndereco(), novoTipo, novoSalario);
        else if (novoTipo.equals("assalariado")) novo = new Assalariado(e.getNome(), e.getEndereco(), novoTipo, novoSalario);
        else if (novoTipo.equals("comissionado")) novo = new Comissionado(e.getNome(), e.getEndereco(), novoTipo, novoSalario, novaComissao);
        
        novo.setSindicato(e.getSindicato());
        novo.setMetodoPagamento(e.getMetodoPagamento());
        novo.setBanco(e.getBanco());
        novo.setAgencia(e.getAgencia());
        novo.setContaCorrente(e.getContaCorrente());
        
        empregados.put(emp, novo); // Substitui no mapa
    }

    // SOBRECARGA 1: 3 Argumentos (Trata nome, endereco, salario, metodoPagamento simples e desligamento do sindicato)
    public void alteraEmpregado(String emp, String atributo, String valor) throws Exception {
        if (emp == null || emp.isEmpty()) throw new Exception("Identificacao do empregado nao pode ser nula.");
        Empregado e = empregados.get(emp);
        if (e == null) throw new EmpregadoNaoExisteException();

        if (atributo.equals("nome")) {
            if (valor == null || valor.isEmpty()) throw new Exception("Nome nao pode ser nulo.");
            e.setNome(valor);
        } else if (atributo.equals("endereco")) {
            if (valor == null || valor.isEmpty()) throw new Exception("Endereco nao pode ser nulo.");
            e.setEndereco(valor);
        } else if (atributo.equals("tipo")) {
            if (!valor.equals("horista") && !valor.equals("assalariado") && !valor.equals("comissionado")) throw new Exception("Tipo invalido.");
            atualizarTipoEmpregado(emp, e, valor, e.getSalario(), (e instanceof Comissionado) ? ((Comissionado)e).getComissao() : "0");
        } else if (atributo.equals("salario")) {
            if (valor == null || valor.isEmpty()) throw new Exception("Salario nao pode ser nulo.");
            double sal;
            try { sal = Double.parseDouble(valor.replace(",", ".")); } 
            catch (Exception ex) { throw new Exception("Salario deve ser numerico."); }
            if (sal < 0) throw new Exception("Salario deve ser nao-negativo.");
            e.setSalario((int) Math.round(sal * 100));
        } else if (atributo.equals("comissao")) {
            if (!(e instanceof Comissionado)) throw new Exception("Empregado nao eh comissionado.");
            if (valor == null || valor.isEmpty()) throw new Exception("Comissao nao pode ser nula.");
            double c;
            try { c = Double.parseDouble(valor.replace(",", ".")); } 
            catch (Exception ex) { throw new Exception("Comissao deve ser numerica."); }
            if (c < 0) throw new Exception("Comissao deve ser nao-negativa.");
            ((Comissionado) e).setComissao(valor);
        } else if (atributo.equals("metodoPagamento")) {
            if (!valor.equals("emMaos") && !valor.equals("correios") && !valor.equals("banco")) throw new Exception("Metodo de pagamento invalido.");
            e.setMetodoPagamento(valor);
        } else if (atributo.equals("sindicalizado")) {
            if (!valor.equals("true") && !valor.equals("false")) throw new Exception("Valor deve ser true ou false.");
            if (valor.equals("false")) e.setSindicato(null);
        } else {
            throw new Exception("Atributo nao existe.");
        }
    }

    // SOBRECARGA 2: 4 Argumentos (Trata mudança de tipo que exige comissão ou salário junto)
    public void alteraEmpregado(String emp, String atributo, String valor, String extra) throws Exception {
        if (emp == null || emp.isEmpty()) throw new Exception("Identificacao do empregado nao pode ser nula.");
        Empregado e = empregados.get(emp);
        if (e == null) throw new EmpregadoNaoExisteException();

        if (atributo.equals("tipo") && valor.equals("comissionado")) {
            if (extra == null || extra.isEmpty()) throw new Exception("Comissao nao pode ser nula.");
            double c;
            try { c = Double.parseDouble(extra.replace(",", ".")); } 
            catch (Exception ex) { throw new Exception("Comissao deve ser numerica."); }
            if (c < 0) throw new Exception("Comissao deve ser nao-negativa.");
            atualizarTipoEmpregado(emp, e, valor, e.getSalario(), extra);
        } else if (atributo.equals("tipo") && valor.equals("horista")) {
            if (extra == null || extra.isEmpty()) throw new Exception("Salario nao pode ser nulo.");
            double s;
            try { s = Double.parseDouble(extra.replace(",", ".")); } 
            catch (Exception ex) { throw new Exception("Salario deve ser numerico."); }
            if (s < 0) throw new Exception("Salario deve ser nao-negativo.");
            atualizarTipoEmpregado(emp, e, valor, (int) Math.round(s * 100), "0");
        } else {
            throw new Exception("Atributo nao existe.");
        }
    }

    // SOBRECARGA 3: 5 Argumentos (Trata adesão ao sindicato)
    public void alteraEmpregado(String emp, String atributo, String valor, String idSindicato, String taxaSindical) throws Exception {
        if (emp == null || emp.isEmpty()) throw new Exception("Identificacao do empregado nao pode ser nula.");
        Empregado e = empregados.get(emp);
        if (e == null) throw new EmpregadoNaoExisteException();

        if (atributo.equals("sindicalizado") && valor.equals("true")) {
            if (idSindicato == null || idSindicato.isEmpty()) throw new Exception("Identificacao do sindicato nao pode ser nula.");
            if (taxaSindical == null || taxaSindical.isEmpty()) throw new Exception("Taxa sindical nao pode ser nula.");
            double t;
            try { t = Double.parseDouble(taxaSindical.replace(",", ".")); } 
            catch (Exception ex) { throw new Exception("Taxa sindical deve ser numerica."); }
            if (t < 0) throw new Exception("Taxa sindical deve ser nao-negativa.");
            if (buscarPorSindicato(idSindicato) != null) throw new Exception("Ha outro empregado com esta identificacao de sindicato");
            
            e.setSindicato(new MembroSindicato(idSindicato, t));
        }
    }

    // SOBRECARGA 4: 6 Argumentos (Trata mudança para pagamento bancário)
    public void alteraEmpregado(String emp, String atributo, String valor1, String banco, String agencia, String contaCorrente) throws Exception {
        if (emp == null || emp.isEmpty()) throw new Exception("Identificacao do empregado nao pode ser nula.");
        Empregado e = empregados.get(emp);
        if (e == null) throw new EmpregadoNaoExisteException();

        if (atributo.equals("metodoPagamento") && valor1.equals("banco")) {
            if (banco == null || banco.isEmpty()) throw new Exception("Banco nao pode ser nulo.");
            if (agencia == null || agencia.isEmpty()) throw new Exception("Agencia nao pode ser nulo.");
            if (contaCorrente == null || contaCorrente.isEmpty()) throw new Exception("Conta corrente nao pode ser nulo.");
            
            e.setMetodoPagamento("banco");
            e.setBanco(banco);
            e.setAgencia(agencia);
            e.setContaCorrente(contaCorrente);
        }
    }
    
}