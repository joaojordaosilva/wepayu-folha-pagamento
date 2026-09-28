package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoExisteException;
import br.ufal.ic.p2.wepayu.models.*;
import java.io.PrintWriter;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Stack;
import java.io.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDate;

public class Facade {
	
    private Map<String, Empregado> empregados;
    private final String ARQUIVO_DADOS = "empregados.dat";

    public int getNumeroDeEmpregados() {
        return empregados.size();
    }
    
    public Facade() {
        // Tenta carregar o arquivo ao iniciar a Facade
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(ARQUIVO_DADOS))) {
            empregados = (Map<String, Empregado>) ois.readObject();
        } catch (Exception e) {
            // Se o arquivo não existir ou der erro, inicia um mapa vazio
            empregados = new LinkedHashMap<>();
        }  
    }
    
    private Stack<byte[]> undoStack = new Stack<>();
    
    private Stack<byte[]> redoStack = new Stack<>();
    
    private boolean sistemaEncerrado = false;
    
    public void zerarSistema() {
    	byte[] backup = capturarEstado();
        empregados.clear();
        // Opcional: apagar o arquivo fisicamente aqui também
        new File(ARQUIVO_DADOS).delete();
        this.sistemaEncerrado = false;
        
        confirmarEstado(backup);
    }

    public void encerrarSistema() {
    	this.sistemaEncerrado = true;
    	
        // Salva o mapa no arquivo ao encerrar
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ARQUIVO_DADOS))) {
            oos.writeObject(empregados);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	
	//US1
	public String criarEmpregado(String nome, String endereco, String tipo, String salario) throws Exception {

		byte[] backup = capturarEstado();
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
		confirmarEstado(backup);
		return id;
	}

	public String criarEmpregado(String nome, String endereco, String tipo, String salario, String comissao)
			throws Exception {
		byte[] backup = capturarEstado();
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
		confirmarEstado(backup);
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
		
		byte[] backup = capturarEstado();
        if (emp == null || emp.isEmpty()) {
            throw new Exception("Identificacao do empregado nao pode ser nula.");
        }
        
        if (!empregados.containsKey(emp)) {
            throw new EmpregadoNaoExisteException();
        }
        
        empregados.remove(emp);
        confirmarEstado(backup);
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
    	byte[] backup = capturarEstado();
    	
        if (emp == null || emp.isEmpty()) throw new Exception("Identificacao do empregado nao pode ser nula.");
        
        Empregado e = empregados.get(emp);
        if (e == null) throw new EmpregadoNaoExisteException();
        if (!(e instanceof Horista)) throw new Exception("Empregado nao eh horista.");

        LocalDate d = parseData(data, "Data invalida.");
        
        double h = Double.parseDouble(horas.replace(",", "."));
        if (h <= 0) throw new Exception("Horas devem ser positivas.");

        ((Horista) e).adicionarCartao(new CartaoDePonto(d, h));
        confirmarEstado(backup);
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
    	
    	byte[] backup = capturarEstado();
        if (emp == null || emp.isEmpty()) throw new Exception("Identificacao do empregado nao pode ser nula.");
        
        Empregado e = empregados.get(emp);
        if (e == null) throw new EmpregadoNaoExisteException();
        if (!(e instanceof Comissionado)) throw new Exception("Empregado nao eh comissionado.");

        LocalDate d = parseData(data, "Data invalida.");
        
        double v = Double.parseDouble(valor.replace(",", "."));
        if (v <= 0) throw new Exception("Valor deve ser positivo.");

        ((Comissionado) e).adicionarVenda(new ResultadoDeVenda(d, v));
        
        confirmarEstado(backup);
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
    	byte[] backup = capturarEstado();
        if (membro == null || membro.isEmpty()) throw new Exception("Identificacao do membro nao pode ser nula.");
        
        Empregado emp = buscarPorSindicato(membro);
        if (emp == null) throw new Exception("Membro nao existe.");

        LocalDate d = parseData(data, "Data invalida.");
        
        double v = Double.parseDouble(valor.replace(",", "."));
        if (v <= 0) throw new Exception("Valor deve ser positivo.");

        emp.getSindicato().adicionarTaxa(new TaxaServico(d, v));
        
        confirmarEstado(backup);
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
    
 //trocar a classe do empregado sem perder os dados vitais
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

    // SOBRECARGA 1
    public void alteraEmpregado(String emp, String atributo, String valor) throws Exception {
    	
    	byte[] backup = capturarEstado();
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
        confirmarEstado(backup);
    }

    // SOBRECARGA 2
    public void alteraEmpregado(String emp, String atributo, String valor, String extra) throws Exception {
    	
    	byte[] backup = capturarEstado();
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
        confirmarEstado(backup);
    }

    // SOBRECARGA 3
    public void alteraEmpregado(String emp, String atributo, String valor, String idSindicato, String taxaSindical) throws Exception {
       
    	byte[] backup = capturarEstado();
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
        confirmarEstado(backup);
    }

    // SOBRECARGA 4
    public void alteraEmpregado(String emp, String atributo, String valor1, String banco, String agencia, String contaCorrente) throws Exception {
        
    	byte[] backup = capturarEstado();
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
        
        confirmarEstado(backup);
    }
    
 

 //FORMATAÇÃO PARA A FOLHA
    private String fStr(String s, int width) { return String.format("%-" + width + "s", s); }
    private String fNum(double num, int width) { 
        String val = String.format(Locale.US, "%.2f", num).replace(".", ",");
        if (width <= 0) return val;
        return String.format("%" + width + "s", val);
    }
    private String fInt(double num, int width) { return String.format("%" + width + "s", (long)num); }

    private double trunc2(double val) {
        // Corta as decimais extras sem arredondar para cima (ex: 42.048 vira 42.04)
        return Math.floor(val * 100.0 + 1e-6) / 100.0;
    }

    private String formatarMetodo(Empregado e) {
        if (e.getMetodoPagamento().equals("emMaos")) return "Em maos";
        if (e.getMetodoPagamento().equals("correios")) return "Correios, " + e.getEndereco();
        if (e.getMetodoPagamento().equals("banco")) return e.getBanco() + ", Ag. " + e.getAgencia() + " CC " + e.getContaCorrente();
        return "";
    }

    private List<Empregado> getEmpregadosPorTipo(String tipo) {
        List<Empregado> lista = new ArrayList<>();
        for (Empregado e : empregados.values()) { if (e.getTipo().equals(tipo)) lista.add(e); }
        lista.sort(Comparator.comparing(Empregado::getNome));
        return lista;
    }

    // CÁLCULO FINANCEIRO 
    private double calcHoras(Horista e, LocalDate inicio, LocalDate fimBusca, boolean extra) {
        double t = 0;
        for (CartaoDePonto c : e.getCartoes()) {
            if (!c.getData().isBefore(inicio) && c.getData().isBefore(fimBusca)) t += extra ? c.getHorasExtras() : c.getHorasNormais();
        }
        return t;
    }

    private double calcVendas(Comissionado e, LocalDate inicio, LocalDate fimBusca) {
        double t = 0;
        for (ResultadoDeVenda v : e.getVendas()) {
            if (!v.getData().isBefore(inicio) && v.getData().isBefore(fimBusca)) t += v.getValor();
        }
        return t;
    }

    private double calcDescontos(Empregado e, int dias, LocalDate inicio, LocalDate fimBusca) {
        if (e.getSindicato() == null) return 0;
        double fixo = e.getSindicato().getTaxaSindical() * dias;
        double avulso = 0;
        for (TaxaServico t : e.getSindicato().getTaxas()) {
            if (!t.getData().isBefore(inicio) && t.getData().isBefore(fimBusca)) avulso += t.getValor();
        }
        return fixo + avulso;
    }

    // Simula contracheques passados para descobrir se o empregado tem taxas atrasadas
    private double simularDividaSindicato(Empregado e, LocalDate dataDaFolha) {
        if (e.getSindicato() == null) return 0.0;
        
        LocalDate inicioContrato = LocalDate.of(2005, 1, 1);
        if (e.getTipo().equals("horista")) {
            inicioContrato = null;
            for (CartaoDePonto c : ((Horista)e).getCartoes()) {
                if (inicioContrato == null || c.getData().isBefore(inicioContrato)) inicioContrato = c.getData();
            }
            if (inicioContrato == null) return 0.0; 
        }

        double divida = 0.0;
        LocalDate curr = inicioContrato;
        
        while (curr.isBefore(dataDaFolha)) {
            boolean isPayDay = false;
            int diasPeriodo = 0;
            LocalDate pInicio = null;
            double bruto = 0.0;

            if (e.getTipo().equals("horista") && curr.getDayOfWeek() == DayOfWeek.FRIDAY) {
                isPayDay = true;
                diasPeriodo = 7;
                pInicio = curr.minusDays(6);
                double hn = calcHoras((Horista)e, pInicio, curr.plusDays(1), false);
                double he = calcHoras((Horista)e, pInicio, curr.plusDays(1), true);
                double sal = e.getSalario() / 100.0;
                bruto = trunc2((hn * sal) + (he * sal * 1.5));
            }
            else if (e.getTipo().equals("assalariado") && curr.equals(curr.with(TemporalAdjusters.lastDayOfMonth()))) {
                isPayDay = true;
                diasPeriodo = curr.lengthOfMonth();
                pInicio = curr.withDayOfMonth(1);
                bruto = trunc2(e.getSalario() / 100.0);
            }
            else if (e.getTipo().equals("comissionado") && curr.getDayOfWeek() == DayOfWeek.FRIDAY) {
                long diasDesdeJan1 = ChronoUnit.DAYS.between(LocalDate.of(2005, 1, 1), curr);
                if ((diasDesdeJan1 - 13) % 14 == 0) {
                    isPayDay = true;
                    diasPeriodo = 14;
                    pInicio = curr.minusDays(13);
                    double fixo = trunc2((e.getSalario() / 100.0) * 24.0 / 52.0);
                    double vendas = calcVendas((Comissionado)e, pInicio, curr.plusDays(1));
                    double cPct = Double.parseDouble(((Comissionado)e).getComissao().replace(",", "."));
                    bruto = trunc2(fixo + trunc2(vendas * cPct));
                }
            }
            
            if (isPayDay) {
                double taxas = trunc2(calcDescontos(e, diasPeriodo, pInicio, curr.plusDays(1)));
                divida = trunc2(divida + taxas);
                double pago = Math.min(bruto, divida);
                divida = trunc2(divida - pago);
            }
            curr = curr.plusDays(1);
        }
        return divida;
    }

    //GERAÇÃO DA FOLHA 
    private String gerarTextoFolha(String data) throws Exception {
        LocalDate d = parseData(data, "Data invalida.");
        StringBuilder sb = new StringBuilder();

        sb.append("FOLHA DE PAGAMENTO DO DIA ").append(d.toString()).append("\n");
        sb.append("====================================\n\n");

        boolean pagaHorista = d.getDayOfWeek() == DayOfWeek.FRIDAY;
        boolean pagaAssalariado = d.equals(d.with(TemporalAdjusters.lastDayOfMonth()));
        long diasDesdeJan1 = ChronoUnit.DAYS.between(LocalDate.of(2005, 1, 1), d);
        boolean pagaComissionado = (d.getDayOfWeek() == DayOfWeek.FRIDAY) && ((diasDesdeJan1 - 13) % 14 == 0);

        double totalFolha = 0.0; 

        //HORISTAS
        sb.append("===============================================================================================================================\n");
        sb.append("===================== HORISTAS ================================================================================================\n");
        sb.append("===============================================================================================================================\n");
        sb.append("Nome                                 Horas Extra Salario Bruto Descontos Salario Liquido Metodo\n");
        sb.append("==================================== ===== ===== ============= ========= =============== ======================================\n");

        double totHHoras = 0, totHExtras = 0, totHBruto = 0, totHDesc = 0, totHLiq = 0;
        if (pagaHorista) {
            for (Empregado e : getEmpregadosPorTipo("horista")) {
                LocalDate inicio = d.minusDays(6);
                LocalDate fim = d.plusDays(1);
                double hn = calcHoras((Horista)e, inicio, fim, false);
                double he = calcHoras((Horista)e, inicio, fim, true);
                double sal = e.getSalario() / 100.0;
                double bruto = trunc2((hn * sal) + (he * sal * 1.5));
                
                double descAtual = trunc2(calcDescontos(e, 7, inicio, fim));
                double dividaAnterior = simularDividaSindicato(e, d);
                double totalDevido = trunc2(descAtual + dividaAnterior);
                
                double desc = bruto == 0 ? 0 : Math.min(totalDevido, bruto);
                double liq = trunc2(bruto - desc);

                sb.append(fStr(e.getNome(), 36)).append(" ")
                  .append(fInt(hn, 5)).append(" ").append(fInt(he, 5)).append(" ")
                  .append(fNum(bruto, 13)).append(" ").append(fNum(desc, 9)).append(" ")
                  .append(fNum(liq, 15)).append(" ").append(formatarMetodo(e)).append("\n");

                totHHoras += hn; totHExtras += he; totHBruto += bruto; totHDesc += desc; totHLiq += liq;
                totalFolha += bruto;
            }
        }
        sb.append("\n").append(fStr("TOTAL HORISTAS", 36)).append(" ")
          .append(fInt(totHHoras, 5)).append(" ").append(fInt(totHExtras, 5)).append(" ")
          .append(fNum(totHBruto, 13)).append(" ").append(fNum(totHDesc, 9)).append(" ")
          .append(fNum(totHLiq, 15)).append("\n\n");

        //ASSALARIADOS 
        sb.append("===============================================================================================================================\n");
        sb.append("===================== ASSALARIADOS ============================================================================================\n");
        sb.append("===============================================================================================================================\n");
        sb.append("Nome                                             Salario Bruto Descontos Salario Liquido Metodo\n");
        sb.append("================================================ ============= ========= =============== ======================================\n");

        double totABruto = 0, totADesc = 0, totALiq = 0;
        if (pagaAssalariado) {
            for (Empregado e : getEmpregadosPorTipo("assalariado")) {
                LocalDate inicio = d.withDayOfMonth(1);
                LocalDate fim = d.plusDays(1);
                double bruto = trunc2(e.getSalario() / 100.0);
                
                double descAtual = trunc2(calcDescontos(e, d.lengthOfMonth(), inicio, fim));
                double dividaAnterior = simularDividaSindicato(e, d);
                double totalDevido = trunc2(descAtual + dividaAnterior);
                
                double desc = bruto == 0 ? 0 : Math.min(totalDevido, bruto);
                double liq = trunc2(bruto - desc);

                sb.append(fStr(e.getNome(), 48)).append(" ")
                  .append(fNum(bruto, 13)).append(" ").append(fNum(desc, 9)).append(" ")
                  .append(fNum(liq, 15)).append(" ").append(formatarMetodo(e)).append("\n");

                totABruto += bruto; totADesc += desc; totALiq += liq;
                totalFolha += bruto;
            }
        }
        sb.append("\n").append(fStr("TOTAL ASSALARIADOS", 48)).append(" ")
          .append(fNum(totABruto, 13)).append(" ").append(fNum(totADesc, 9)).append(" ")
          .append(fNum(totALiq, 15)).append("\n\n");

        // COMISSIONADOS
        sb.append("===============================================================================================================================\n");
        sb.append("===================== COMISSIONADOS ===========================================================================================\n");
        sb.append("===============================================================================================================================\n");
        sb.append("Nome                  Fixo     Vendas   Comissao Salario Bruto Descontos Salario Liquido Metodo\n");
        sb.append("===================== ======== ======== ======== ============= ========= =============== ======================================\n");

        double totCFixo = 0, totCVendas = 0, totCComissao = 0, totCBruto = 0, totCDesc = 0, totCLiq = 0;
        if (pagaComissionado) {
            for (Empregado e : getEmpregadosPorTipo("comissionado")) {
                LocalDate inicio = d.minusDays(13);
                LocalDate fim = d.plusDays(1);
                double fixo = trunc2((e.getSalario() / 100.0) * 24.0 / 52.0);
                double vendas = calcVendas((Comissionado)e, inicio, fim);
                double cPct = Double.parseDouble(((Comissionado)e).getComissao().replace(",", "."));
                double comissao = trunc2(vendas * cPct);
                double bruto = trunc2(fixo + comissao);
                
                double descAtual = trunc2(calcDescontos(e, 14, inicio, fim));
                double dividaAnterior = simularDividaSindicato(e, d);
                double totalDevido = trunc2(descAtual + dividaAnterior);
                
                double desc = bruto == 0 ? 0 : Math.min(totalDevido, bruto);
                double liq = trunc2(bruto - desc);

                sb.append(fStr(e.getNome(), 21)).append(" ")
                  .append(fNum(fixo, 8)).append(" ").append(fNum(vendas, 8)).append(" ")
                  .append(fNum(comissao, 8)).append(" ").append(fNum(bruto, 13)).append(" ")
                  .append(fNum(desc, 9)).append(" ").append(fNum(liq, 15)).append(" ")
                  .append(formatarMetodo(e)).append("\n");

                totCFixo += fixo; totCVendas += vendas; totCComissao += comissao;
                totCBruto += bruto; totCDesc += desc; totCLiq += liq;
                totalFolha += bruto;
            }
        }
        sb.append("\n").append(fStr("TOTAL COMISSIONADOS", 21)).append(" ")
          .append(fNum(totCFixo, 8)).append(" ").append(fNum(totCVendas, 8)).append(" ")
          .append(fNum(totCComissao, 8)).append(" ").append(fNum(totCBruto, 13)).append(" ")
          .append(fNum(totCDesc, 9)).append(" ").append(fNum(totCLiq, 15)).append("\n\n");

        sb.append("TOTAL FOLHA: ").append(fNum(totalFolha, 0)).append("\n");

        return sb.toString();
    }

    //US7 
    public String totalFolha(String data) throws Exception {
    	
        String folha = gerarTextoFolha(data);
        String[] linhas = folha.split("\n");
        return linhas[linhas.length - 1].replace("TOTAL FOLHA: ", "").trim();
    }

    public void rodaFolha(String data, String saida) throws Exception {
    	byte[] backup = capturarEstado();
    	
        String texto = gerarTextoFolha(data);
        try (PrintWriter out = new PrintWriter(saida)) {
            out.print(texto);
        }
        confirmarEstado(backup);
    }
    
    //US8
    
 // Cria uma cópia do estado atual do sistema
    private byte[] capturarEstado() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this.empregados);
            oos.close();
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("ERRO AO TIRAR A FOTOGRAFIA: " + e.getMessage());
        }
    }

    // Só guarda o snapshot na pilha se a operação terminar com sucesso
    
    private void confirmarEstado(byte[] estadoAnterior) {
    	
        if (estadoAnterior != null) {
        	
            undoStack.push(estadoAnterior);
            redoStack.clear(); // O redo é limpo sempre que uma nova ação é feita
        }
    }

    // Substitui a memória atual pelo snapshot guardado
    
    @SuppressWarnings("unchecked")
    private void restaurarEstado(byte[] estado) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(estado);
            ObjectInputStream ois = new ObjectInputStream(bais);
            this.empregados = (java.util.Map<String, Empregado>) ois.readObject();
          
            
            ois.close();
        } catch (Exception e) {}
    }
    
    public void undo() throws Exception {
        if (sistemaEncerrado) throw new Exception("Nao pode dar comandos depois de encerrarSistema.");
        if (undoStack.isEmpty()) throw new Exception("Nao ha comando a desfazer.");
        
        byte[] estadoAtual = capturarEstado();
        redoStack.push(estadoAtual);
        
        byte[] estadoAnterior = undoStack.pop();
        restaurarEstado(estadoAnterior);
    }

    public void redo() throws Exception {
        if (sistemaEncerrado) throw new Exception("Nao pode dar comandos depois de encerrarSistema.");
        if (redoStack.isEmpty()) throw new Exception("Nao ha comando a refazer.");
        
        byte[] estadoAtual = capturarEstado();
        undoStack.push(estadoAtual);
        
        byte[] proximoEstado = redoStack.pop();
        restaurarEstado(proximoEstado);
    }
    
}