package br.ufal.ic.p2.wepayu;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoExisteException;
import br.ufal.ic.p2.wepayu.models.Assalariado;
import br.ufal.ic.p2.wepayu.models.CartaoDePonto;
import br.ufal.ic.p2.wepayu.models.Comissionado;
import br.ufal.ic.p2.wepayu.models.Empregado;
import br.ufal.ic.p2.wepayu.models.Horista;
import br.ufal.ic.p2.wepayu.models.MembroSindicato;
import br.ufal.ic.p2.wepayu.models.ResultadoDeVenda;
import br.ufal.ic.p2.wepayu.models.TaxaServico;

public class Facade {
	
    private Map<String, Empregado> empregados;
    private List<String> agendasDisponiveis;
    private final String ARQUIVO_DADOS = "empregados.dat";
    
    private Stack<byte[]> undoStack = new Stack<>();
    private Stack<byte[]> redoStack = new Stack<>();
    private boolean sistemaEncerrado = false;

    // =========================================================
    // 1. INICIALIZAÇÃO E CICLO DE VIDA DO SISTEMA
    // =========================================================

    @SuppressWarnings("unchecked")
    public Facade() {
        File arquivo = new File(ARQUIVO_DADOS);
        if (arquivo.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(ARQUIVO_DADOS))) {
                empregados = (Map<String, Empregado>) ois.readObject();
                agendasDisponiveis = (List<String>) ois.readObject();
            } catch (Exception e) {
                throw new RuntimeException("Erro ao carregar dados corrompidos: " + e.getMessage());
            }
        } else {
            iniciarDadosZerados();
        }
    }
    
    private void iniciarDadosZerados() {
        empregados = new LinkedHashMap<>();
        agendasDisponiveis = new ArrayList<>(Arrays.asList("mensal $", "semanal 5", "semanal 2 5"));
    }

    private void verificarEncerramento() throws Exception {
        if (sistemaEncerrado) throw new Exception("Nao pode dar comandos depois de encerrarSistema.");
    }
    
    public void zerarSistema() throws Exception {
        verificarEncerramento();
    	byte[] backup = capturarEstado();
        iniciarDadosZerados();
        new File(ARQUIVO_DADOS).delete();
        confirmarEstado(backup);
    }

    public void encerrarSistema() {
    	this.sistemaEncerrado = true;
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ARQUIVO_DADOS))) {
            oos.writeObject(empregados);
            oos.writeObject(agendasDisponiveis);
        } catch (Exception e) {
            throw new RuntimeException("Falha critica ao salvar sistema: " + e.getMessage());
        }
    }

    // =========================================================
    // 2. GESTÃO DE AGENDAS DE PAGAMENTO (US10)
    // =========================================================
    
    private boolean validarDescricaoAgenda(String desc) {
        String[] p = desc.split(" ");
        if (p.length == 0) return false;
        
        if (p[0].equals("mensal")) {
            if (p.length != 2) return false;
            if (p[1].equals("$")) return true;
            try {
                int dia = Integer.parseInt(p[1]);
                return dia >= 1 && dia <= 28;
            } catch (Exception e) { return false; }
        } else if (p[0].equals("semanal")) {
            if (p.length == 2) {
                try {
                    int dia = Integer.parseInt(p[1]);
                    return dia >= 1 && dia <= 7;
                } catch (Exception e) { return false; }
            } else if (p.length == 3) {
                try {
                    int semana = Integer.parseInt(p[1]);
                    int dia = Integer.parseInt(p[2]);
                    return semana >= 1 && semana <= 52 && dia >= 1 && dia <= 7;
                } catch (Exception e) { return false; }
            }
        }
        return false;
    }

    public void criarAgendaDePagamentos(String descricao) throws Exception {
        verificarEncerramento();
        byte[] backup = capturarEstado();
        
        if (!validarDescricaoAgenda(descricao)) {
            throw new Exception("Descricao de agenda invalida");
        }
        if (agendasDisponiveis.contains(descricao)) {
            throw new Exception("Agenda de pagamentos ja existe");
        }
        
        agendasDisponiveis.add(descricao);
        confirmarEstado(backup);
    }

    // =========================================================
    // 3. CRIAÇÃO, REMOÇÃO E CONSULTA DE EMPREGADOS
    // =========================================================

    public int getNumeroDeEmpregados() throws Exception {
        verificarEncerramento();
        return empregados.size();
    }

	public String criarEmpregado(String nome, String endereco, String tipo, String salario) throws Exception {
        verificarEncerramento();
		byte[] backup = capturarEstado();
		
		if (nome == null || nome.isEmpty()) throw new Exception("Nome nao pode ser nulo.");
		if (endereco == null || endereco.isEmpty()) throw new Exception("Endereco nao pode ser nulo.");
		if (tipo == null || (!tipo.equals("horista") && !tipo.equals("assalariado") && !tipo.equals("comissionado"))) throw new Exception("Tipo invalido.");
		if (tipo.equals("comissionado")) throw new Exception("Tipo nao aplicavel.");
		if (salario == null || salario.isEmpty()) throw new Exception("Salario nao pode ser nulo.");

		double salDouble;
		try { salDouble = Double.parseDouble(salario.replace(",", ".")); } 
        catch (NumberFormatException e) { throw new Exception("Salario deve ser numerico."); }
		if (salDouble < 0) throw new Exception("Salario deve ser nao-negativo.");

		int salInt = (int) Math.round(salDouble * 100);
		String id = UUID.randomUUID().toString().substring(0, 8);

		Empregado emp = tipo.equals("horista") ? new Horista(nome, endereco, tipo, salInt) : new Assalariado(nome, endereco, tipo, salInt);
        emp.setAgendaPagamento(tipo.equals("horista") ? "semanal 5" : "mensal $");

		empregados.put(id, emp);
		confirmarEstado(backup);
		return id;
	}

	public String criarEmpregado(String nome, String endereco, String tipo, String salario, String comissao) throws Exception {
        verificarEncerramento();
		byte[] backup = capturarEstado();
		
		if (nome == null || nome.isEmpty()) throw new Exception("Nome nao pode ser nulo.");
		if (endereco == null || endereco.isEmpty()) throw new Exception("Endereco nao pode ser nulo.");
		if (tipo == null || (!tipo.equals("horista") && !tipo.equals("assalariado") && !tipo.equals("comissionado"))) throw new Exception("Tipo invalido.");
		if (tipo.equals("horista") || tipo.equals("assalariado")) throw new Exception("Tipo nao aplicavel.");
		if (salario == null || salario.isEmpty()) throw new Exception("Salario nao pode ser nulo.");
        
		double salDouble;
		try { salDouble = Double.parseDouble(salario.replace(",", ".")); } 
        catch (NumberFormatException e) { throw new Exception("Salario deve ser numerico."); }
		if (salDouble < 0) throw new Exception("Salario deve ser nao-negativo.");
		int salInt = (int) Math.round(salDouble * 100);

		if (comissao == null || comissao.isEmpty()) throw new Exception("Comissao nao pode ser nula.");
		double comDouble;
		try { comDouble = Double.parseDouble(comissao.replace(",", ".")); } 
        catch (NumberFormatException e) { throw new Exception("Comissao deve ser numerica."); }
		if (comDouble < 0) throw new Exception("Comissao deve ser nao-negativa.");

		String id = UUID.randomUUID().toString().substring(0, 8);
		Empregado emp = new Comissionado(nome, endereco, tipo, salInt, comissao);
        emp.setAgendaPagamento("semanal 2 5");

		empregados.put(id, emp);
		confirmarEstado(backup);
		return id;
	}
	
	public void removerEmpregado(String emp) throws Exception {
        verificarEncerramento();
		byte[] backup = capturarEstado();
        if (emp == null || emp.isEmpty()) throw new Exception("Identificacao do empregado nao pode ser nula.");
        if (!empregados.containsKey(emp)) throw new EmpregadoNaoExisteException();
        
        empregados.remove(emp);
        confirmarEstado(backup);
    }

	public String getAtributoEmpregado(String emp, String atributo) throws Exception {
        verificarEncerramento();
	    if (emp == null || emp.isEmpty()) throw new Exception("Identificacao do empregado nao pode ser nula.");
	    Empregado e = empregados.get(emp);
	    if (e == null) throw new EmpregadoNaoExisteException();

	    if (atributo.equals("nome")) return e.getNome();
	    if (atributo.equals("endereco")) return e.getEndereco();
	    if (atributo.equals("tipo")) return e.getTipo();
	    if (atributo.equals("salario")) {
	        int sal = e.getSalario();
	        return String.format("%d,%02d", sal / 100, sal % 100);
	    }
	    if (atributo.equals("sindicalizado")) return (e.getSindicato() != null) ? "true" : "false";
	    if (atributo.equals("comissao")) return e.getComissao(); 
	    if (atributo.equals("metodoPagamento")) return e.getMetodoPagamento();
        if (atributo.equals("agendaPagamento")) return e.getAgendaPagamento();
	    
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
	
	public String getEmpregadoPorNome(String nome, int indice) throws Exception {
        verificarEncerramento();
	    int count = 0;
	    for (Map.Entry<String, Empregado> entry : empregados.entrySet()) {
	        if (entry.getValue().getNome().contains(nome)) {
	            count++;
	            if (count == indice) return entry.getKey();
	        }
	    }
	    throw new Exception("Nao ha empregado com esse nome.");
	}
    
    // =========================================================
    // 4. LANÇAMENTOS (CARTÕES DE PONTO, VENDAS E TAXAS)
    // =========================================================

    public void lancaCartao(String emp, String data, String horas) throws Exception {
        verificarEncerramento();
    	byte[] backup = capturarEstado();
        if (emp == null || emp.isEmpty()) throw new Exception("Identificacao do empregado nao pode ser nula.");
        Empregado e = empregados.get(emp);
        if (e == null) throw new EmpregadoNaoExisteException();

        LocalDate d = parseData(data, "Data invalida.");
        double h = Double.parseDouble(horas.replace(",", "."));
        if (h <= 0) throw new Exception("Horas devem ser positivas.");

        e.adicionarCartao(new CartaoDePonto(d, h));
        confirmarEstado(backup);
    }
    
    public String getHorasNormaisTrabalhadas(String emp, String dataInicial, String dataFinal) throws Exception {
        verificarEncerramento();
        return formatarHoras(calcularHoras(emp, dataInicial, dataFinal, false));
    }

    public String getHorasExtrasTrabalhadas(String emp, String dataInicial, String dataFinal) throws Exception {
        verificarEncerramento();
        return formatarHoras(calcularHoras(emp, dataInicial, dataFinal, true));
    }
    
    public void lancaVenda(String emp, String data, String valor) throws Exception {
        verificarEncerramento();
    	byte[] backup = capturarEstado();
        if (emp == null || emp.isEmpty()) throw new Exception("Identificacao do empregado nao pode ser nula.");
        Empregado e = empregados.get(emp);
        if (e == null) throw new EmpregadoNaoExisteException();

        LocalDate d = parseData(data, "Data invalida.");
        double v = Double.parseDouble(valor.replace(",", "."));
        if (v <= 0) throw new Exception("Valor deve ser positivo.");

        e.adicionarVenda(new ResultadoDeVenda(d, v)); 
        confirmarEstado(backup);
    }

    public String getVendasRealizadas(String emp, String dataInicial, String dataFinal) throws Exception {
        verificarEncerramento();
        Empregado e = empregados.get(emp);
        if (e == null) throw new EmpregadoNaoExisteException();

        LocalDate inicio = parseData(dataInicial, "Data inicial invalida.");
        LocalDate fim = parseData(dataFinal, "Data final invalida.");
        if (inicio.isAfter(fim)) throw new Exception("Data inicial nao pode ser posterior aa data final.");

        double total = 0;
        for (ResultadoDeVenda v : e.getVendas()) {
            if (!v.getData().isBefore(inicio) && v.getData().isBefore(fim)) {
                total += v.getValor();
            }
        }
        return String.format("%.2f", total).replace(".", ",");
    }

    public void lancaTaxaServico(String membro, String data, String valor) throws Exception {
        verificarEncerramento();
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
        verificarEncerramento();
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

    // =========================================================
    // 5. ALTERAÇÃO DE DADOS DOS EMPREGADOS
    // =========================================================

    public void alteraEmpregado(String emp, String atributo, String valor) throws Exception {
        verificarEncerramento();
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
            String com = (e.getTipo().equals("comissionado")) ? e.getComissao() : "0";
            atualizarTipoEmpregado(emp, e, valor, e.getSalario(), com);
        } else if (atributo.equals("salario")) {
            if (valor == null || valor.isEmpty()) throw new Exception("Salario nao pode ser nulo.");
            double sal;
            try { sal = Double.parseDouble(valor.replace(",", ".")); } 
            catch (Exception ex) { throw new Exception("Salario deve ser numerico."); }
            if (sal < 0) throw new Exception("Salario deve ser nao-negativo.");
            e.setSalario((int) Math.round(sal * 100));
        } else if (atributo.equals("comissao")) {
            if (valor == null || valor.isEmpty()) throw new Exception("Comissao nao pode ser nula.");
            double c;
            try { c = Double.parseDouble(valor.replace(",", ".")); } 
            catch (Exception ex) { throw new Exception("Comissao deve ser numerica."); }
            if (c < 0) throw new Exception("Comissao deve ser nao-negativa.");
            e.setComissao(valor);
        } else if (atributo.equals("metodoPagamento")) {
            if (!valor.equals("emMaos") && !valor.equals("correios") && !valor.equals("banco")) throw new Exception("Metodo de pagamento invalido.");
            e.setMetodoPagamento(valor);
        } else if (atributo.equals("sindicalizado")) {
            if (!valor.equals("true") && !valor.equals("false")) throw new Exception("Valor deve ser true ou false.");
            if (valor.equals("false")) e.setSindicato(null);
        } else if (atributo.equals("agendaPagamento")) {
            if (!agendasDisponiveis.contains(valor)) {
                throw new Exception("Agenda de pagamento nao esta disponivel");
            }
            e.setAgendaPagamento(valor);
        } else {
            throw new Exception("Atributo nao existe.");
        }
        confirmarEstado(backup);
    }

    public void alteraEmpregado(String emp, String atributo, String valor, String extra) throws Exception {
        verificarEncerramento();
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

    public void alteraEmpregado(String emp, String atributo, String valor, String idSindicato, String taxaSindical) throws Exception {
        verificarEncerramento();
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
        } else {
            throw new Exception("Atributo nao existe ou combinacao invalida.");
        }
        confirmarEstado(backup);
    }

    public void alteraEmpregado(String emp, String atributo, String valor1, String banco, String agencia, String contaCorrente) throws Exception {
        verificarEncerramento();
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
        } else {
            throw new Exception("Atributo nao existe ou combinacao invalida.");
        }
        confirmarEstado(backup);
    }

    // =========================================================
    // 6. MÉTODOS E FORMATAÇÃO
    // =========================================================

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
        novo.setAgendaPagamento(e.getAgendaPagamento()); 
        
        empregados.put(emp, novo);
    }

    private Empregado buscarPorSindicato(String idSindicato) {
        for (Empregado e : empregados.values()) {
            if (e.getSindicato() != null && e.getSindicato().getIdSindicato().equals(idSindicato)) {
                return e;
            }
        }
        return null;
    }

    private LocalDate parseData(String dataStr, String msgErro) throws Exception {
        try {
            String[] p = dataStr.split("/");
            return LocalDate.of(Integer.parseInt(p[2]), Integer.parseInt(p[1]), Integer.parseInt(p[0]));
        } catch (Exception e) {
            throw new Exception(msgErro);
        }
    }

    private String formatarHoras(double horas) {
        if (horas % 1 == 0) return String.valueOf((long) horas);
        return String.valueOf(horas).replace(".", ",");
    }

    private String formatarString(String s, int width) { return String.format("%-" + width + "s", s); }
    private String formatarNumero(double num, int width) { 
        String val = String.format(Locale.US, "%.2f", num).replace(".", ",");
        if (width <= 0) return val;
        return String.format("%" + width + "s", val);
    }
    private String formatarInteiro(double num, int width) { return String.format("%" + width + "s", (long)num); }

    private double truncarDuasCasas(double val) { return Math.floor(val * 100.0 + 1e-6) / 100.0; }

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

    private LocalDate obterUltimoDiaUtil(LocalDate data) {
        LocalDate ultimo = data.with(TemporalAdjusters.lastDayOfMonth());
        if (ultimo.getDayOfWeek() == DayOfWeek.SATURDAY) return ultimo.minusDays(1);
        if (ultimo.getDayOfWeek() == DayOfWeek.SUNDAY) return ultimo.minusDays(2);
        return ultimo;
    }

    // =========================================================
    // 7. CÁLCULO DA FOLHA DE PAGAMENTO
    // =========================================================

    private boolean isPayDay(String agenda, LocalDate curr) {
        String[] p = agenda.split(" ");
        if (p[0].equals("mensal")) {
            if (p[1].equals("$")) return curr.equals(obterUltimoDiaUtil(curr));
            int dia = Integer.parseInt(p[1]);
            return curr.getDayOfMonth() == dia;
        } else if (p[0].equals("semanal")) {
            int y = p.length == 3 ? Integer.parseInt(p[1]) : 1;
            int x = p.length == 3 ? Integer.parseInt(p[2]) : Integer.parseInt(p[1]);
            
            if (curr.getDayOfWeek().getValue() != x) return false;
            
            LocalDate base = LocalDate.of(2005, 1, 1);
            while (base.getDayOfWeek().getValue() != x) {
                base = base.plusDays(1);
            }
            
            if (curr.isBefore(base)) return false;
            long weeks = ChronoUnit.WEEKS.between(base, curr);
            return (weeks + 1) % y == 0;
        }
        return false;
    }

    private int getDiasPeriodo(String agenda, LocalDate curr) {
        if (agenda.startsWith("mensal")) return curr.lengthOfMonth();
        String[] p = agenda.split(" ");
        int y = p.length == 3 ? Integer.parseInt(p[1]) : 1;
        return y * 7;
    }

    private LocalDate getInicioPeriodo(String agenda, LocalDate curr) {
        if (agenda.equals("mensal $")) return curr.withDayOfMonth(1);
        if (agenda.startsWith("mensal")) return curr.minusMonths(1).plusDays(1);
        String[] p = agenda.split(" ");
        int y = p.length == 3 ? Integer.parseInt(p[1]) : 1;
        return curr.minusDays(y * 7 - 1);
    }

    private double getFatorSalario(String agenda) {
        if (agenda.startsWith("mensal")) return 1.0;
        String[] p = agenda.split(" ");
        int y = p.length == 3 ? Integer.parseInt(p[1]) : 1;
        return (12.0 * y) / 52.0;
    }

    private double calcularHoras(String emp, String dataInicial, String dataFinal, boolean isExtra) throws Exception {
        Empregado e = empregados.get(emp);
        if (e == null) throw new EmpregadoNaoExisteException();

        LocalDate inicio = parseData(dataInicial, "Data inicial invalida.");
        LocalDate fim = parseData(dataFinal, "Data final invalida.");
        if (inicio.isAfter(fim)) throw new Exception("Data inicial nao pode ser posterior aa data final.");

        double total = 0;
        for (CartaoDePonto c : e.getCartoes()) {
            if (!c.getData().isBefore(inicio) && c.getData().isBefore(fim)) {
                total += isExtra ? c.getHorasExtras() : c.getHorasNormais();
            }
        }
        return total;
    }

    private double calcHoras(Empregado e, LocalDate inicio, LocalDate fimBusca, boolean extra) throws Exception {
        double t = 0;
        for (CartaoDePonto c : e.getCartoes()) {
            if (!c.getData().isBefore(inicio) && c.getData().isBefore(fimBusca)) t += extra ? c.getHorasExtras() : c.getHorasNormais();
        }
        return t;
    }

    private double calcVendas(Empregado e, LocalDate inicio, LocalDate fimBusca) throws Exception {
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

    private double calcularSalarioBrutoDinamicamente(Empregado e, LocalDate pInicio, LocalDate curr) throws Exception {
        if (e.getTipo().equals("horista")) {
            double hn = calcHoras(e, pInicio, curr.plusDays(1), false);
            double he = calcHoras(e, pInicio, curr.plusDays(1), true);
            double sal = e.getSalario() / 100.0;
            return truncarDuasCasas((hn * sal) + (he * sal * 1.5));
        } else if (e.getTipo().equals("assalariado")) {
            return truncarDuasCasas((e.getSalario() / 100.0) * getFatorSalario(e.getAgendaPagamento()));
        } else {
            double fixo = truncarDuasCasas((e.getSalario() / 100.0) * getFatorSalario(e.getAgendaPagamento()));
            double vendas = calcVendas(e, pInicio, curr.plusDays(1));
            double cPct = Double.parseDouble(e.getComissao().replace(",", "."));
            return truncarDuasCasas(fixo + truncarDuasCasas(vendas * cPct));
        }
    }

    private double simularDividaSindicato(Empregado e, LocalDate dataDaFolha) throws Exception {
        if (e.getSindicato() == null) return 0.0;
        
        LocalDate inicioContrato = LocalDate.of(2005, 1, 1);
        if (e.getTipo().equals("horista")) {
            inicioContrato = null;
            for (CartaoDePonto c : e.getCartoes()) {
                if (inicioContrato == null || c.getData().isBefore(inicioContrato)) inicioContrato = c.getData();
            }
            if (inicioContrato == null) return 0.0; 
        }

        double divida = 0.0;
        LocalDate curr = inicioContrato;
        
        while (curr.isBefore(dataDaFolha)) {
            if (isPayDay(e.getAgendaPagamento(), curr)) {
                LocalDate pInicio = getInicioPeriodo(e.getAgendaPagamento(), curr);
                int diasPeriodo = getDiasPeriodo(e.getAgendaPagamento(), curr);
                
                double bruto = calcularSalarioBrutoDinamicamente(e, pInicio, curr);
                double taxas = truncarDuasCasas(calcDescontos(e, diasPeriodo, pInicio, curr.plusDays(1)));
                divida = truncarDuasCasas(divida + taxas);
                double pago = Math.min(bruto, divida);
                divida = truncarDuasCasas(divida - pago);
            }
            curr = curr.plusDays(1);
        }
        return divida;
    }

    private String gerarTextoFolha(String data) throws Exception {
        LocalDate d = parseData(data, "Data invalida.");
        StringBuilder sb = new StringBuilder();

        sb.append("FOLHA DE PAGAMENTO DO DIA ").append(d.toString()).append("\n");
        sb.append("====================================\n\n");

        double totalFolha = 0.0; 

        // ----------------- HORISTAS -----------------
        sb.append("===============================================================================================================================\n");
        sb.append("===================== HORISTAS ================================================================================================\n");
        sb.append("===============================================================================================================================\n");
        sb.append("Nome                                 Horas Extra Salario Bruto Descontos Salario Liquido Metodo\n");
        sb.append("==================================== ===== ===== ============= ========= =============== ======================================\n");

        double totHHoras = 0, totHExtras = 0, totHBruto = 0, totHDesc = 0, totHLiq = 0;
        for (Empregado e : getEmpregadosPorTipo("horista")) {
            if (!isPayDay(e.getAgendaPagamento(), d)) continue;

            LocalDate inicio = getInicioPeriodo(e.getAgendaPagamento(), d);
            double hn = calcHoras(e, inicio, d.plusDays(1), false);
            double he = calcHoras(e, inicio, d.plusDays(1), true);
            double bruto = calcularSalarioBrutoDinamicamente(e, inicio, d);
            
            double descAtual = truncarDuasCasas(calcDescontos(e, getDiasPeriodo(e.getAgendaPagamento(), d), inicio, d.plusDays(1)));
            double dividaAnterior = simularDividaSindicato(e, d);
            double totalDevido = truncarDuasCasas(descAtual + dividaAnterior);
            
            double desc = bruto == 0 ? 0 : Math.min(totalDevido, bruto);
            double liq = truncarDuasCasas(bruto - desc);

            sb.append(formatarString(e.getNome(), 36)).append(" ")
                .append(formatarInteiro(hn, 5)).append(" ").append(formatarInteiro(he, 5)).append(" ")
                .append(formatarNumero(bruto, 13)).append(" ").append(formatarNumero(desc, 9)).append(" ")
                .append(formatarNumero(liq, 15)).append(" ").append(formatarMetodo(e)).append("\n");

            totHHoras += hn; totHExtras += he; totHBruto += bruto; totHDesc += desc; totHLiq += liq;
            totalFolha += bruto;
        }
        sb.append("\n").append(formatarString("TOTAL HORISTAS", 36)).append(" ")
          .append(formatarInteiro(totHHoras, 5)).append(" ").append(formatarInteiro(totHExtras, 5)).append(" ")
          .append(formatarNumero(totHBruto, 13)).append(" ").append(formatarNumero(totHDesc, 9)).append(" ")
          .append(formatarNumero(totHLiq, 15)).append("\n\n");

        // ----------------- ASSALARIADOS -----------------
        sb.append("===============================================================================================================================\n");
        sb.append("===================== ASSALARIADOS ============================================================================================\n");
        sb.append("===============================================================================================================================\n");
        sb.append("Nome                                             Salario Bruto Descontos Salario Liquido Metodo\n");
        sb.append("================================================ ============= ========= =============== ======================================\n");

        double totABruto = 0, totADesc = 0, totALiq = 0;
        for (Empregado e : getEmpregadosPorTipo("assalariado")) {
            if (!isPayDay(e.getAgendaPagamento(), d)) continue;

            LocalDate inicio = getInicioPeriodo(e.getAgendaPagamento(), d);
            double bruto = calcularSalarioBrutoDinamicamente(e, inicio, d);
            
            double descAtual = truncarDuasCasas(calcDescontos(e, getDiasPeriodo(e.getAgendaPagamento(), d), inicio, d.plusDays(1)));
            double dividaAnterior = simularDividaSindicato(e, d);
            double totalDevido = truncarDuasCasas(descAtual + dividaAnterior);
            
            double desc = bruto == 0 ? 0 : Math.min(totalDevido, bruto);
            double liq = truncarDuasCasas(bruto - desc);

            sb.append(formatarString(e.getNome(), 48)).append(" ")
                .append(formatarNumero(bruto, 13)).append(" ").append(formatarNumero(desc, 9)).append(" ")
                .append(formatarNumero(liq, 15)).append(" ").append(formatarMetodo(e)).append("\n");

            totABruto += bruto; totADesc += desc; totALiq += liq;
            totalFolha += bruto;
        }
        sb.append("\n").append(formatarString("TOTAL ASSALARIADOS", 48)).append(" ")
          .append(formatarNumero(totABruto, 13)).append(" ").append(formatarNumero(totADesc, 9)).append(" ")
          .append(formatarNumero(totALiq, 15)).append("\n\n");

        // ----------------- COMISSIONADOS -----------------
        sb.append("===============================================================================================================================\n");
        sb.append("===================== COMISSIONADOS ===========================================================================================\n");
        sb.append("===============================================================================================================================\n");
        sb.append("Nome                  Fixo     Vendas   Comissao Salario Bruto Descontos Salario Liquido Metodo\n");
        sb.append("===================== ======== ======== ======== ============= ========= =============== ======================================\n");

        double totCFixo = 0, totCVendas = 0, totCComissao = 0, totCBruto = 0, totCDesc = 0, totCLiq = 0;
        for (Empregado e : getEmpregadosPorTipo("comissionado")) {
            if (!isPayDay(e.getAgendaPagamento(), d)) continue;

            LocalDate inicio = getInicioPeriodo(e.getAgendaPagamento(), d);
            double fixo = truncarDuasCasas((e.getSalario() / 100.0) * getFatorSalario(e.getAgendaPagamento()));
            double vendas = calcVendas(e, inicio, d.plusDays(1));
            double cPct = Double.parseDouble(e.getComissao().replace(",", "."));
            double comissao = truncarDuasCasas(vendas * cPct);
            double bruto = calcularSalarioBrutoDinamicamente(e, inicio, d);
            
            double descAtual = truncarDuasCasas(calcDescontos(e, getDiasPeriodo(e.getAgendaPagamento(), d), inicio, d.plusDays(1)));
            double dividaAnterior = simularDividaSindicato(e, d);
            double totalDevido = truncarDuasCasas(descAtual + dividaAnterior);
            
            double desc = bruto == 0 ? 0 : Math.min(totalDevido, bruto);
            double liq = truncarDuasCasas(bruto - desc);

            sb.append(formatarString(e.getNome(), 21)).append(" ")
                .append(formatarNumero(fixo, 8)).append(" ").append(formatarNumero(vendas, 8)).append(" ")
                .append(formatarNumero(comissao, 8)).append(" ").append(formatarNumero(bruto, 13)).append(" ")
                .append(formatarNumero(desc, 9)).append(" ").append(formatarNumero(liq, 15)).append(" ")
                .append(formatarMetodo(e)).append("\n");

            totCFixo += fixo; totCVendas += vendas; totCComissao += comissao;
            totCBruto += bruto; totCDesc += desc; totCLiq += liq;
            totalFolha += bruto;
        }
        sb.append("\n").append(formatarString("TOTAL COMISSIONADOS", 21)).append(" ")
          .append(formatarNumero(totCFixo, 8)).append(" ").append(formatarNumero(totCVendas, 8)).append(" ")
          .append(formatarNumero(totCComissao, 8)).append(" ").append(formatarNumero(totCBruto, 13)).append(" ")
          .append(formatarNumero(totCDesc, 9)).append(" ").append(formatarNumero(totCLiq, 15)).append("\n\n");

        sb.append("TOTAL FOLHA: ").append(formatarNumero(totalFolha, 0)).append("\n");
        return sb.toString();
    }

    public String totalFolha(String data) throws Exception {
        verificarEncerramento();
        String folha = gerarTextoFolha(data);
        String[] linhas = folha.split("\n");
        return linhas[linhas.length - 1].replace("TOTAL FOLHA: ", "").trim();
    }

    public void rodaFolha(String data, String saida) throws Exception {
        verificarEncerramento();
    	byte[] backup = capturarEstado();
        String texto = gerarTextoFolha(data);
        try (PrintWriter out = new PrintWriter(saida)) {
            out.print(texto);
        }
        confirmarEstado(backup);
    }
    
    // =========================================================
    // 8-(UNDO / REDO E ESTADOS)
    // =========================================================

    private byte[] capturarEstado() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this.empregados);
            oos.writeObject(this.agendasDisponiveis);
            oos.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("ERRO AO TIRAR A FOTOGRAFIA: " + e.getMessage());
        }
    }

    private void confirmarEstado(byte[] estadoAnterior) {
        if (estadoAnterior != null) {
            undoStack.push(estadoAnterior);
            redoStack.clear(); 
        }
    }
    
    @SuppressWarnings("unchecked")
    private void restaurarEstado(byte[] estado) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(estado);
            ObjectInputStream ois = new ObjectInputStream(bais);
            this.empregados = (java.util.Map<String, Empregado>) ois.readObject();
            this.agendasDisponiveis = (List<String>) ois.readObject();
            ois.close();
        } catch (Exception e) {
            throw new RuntimeException("Erro critico ao restaurar estado: " + e.getMessage());
        }
    }
    
    public void undo() throws Exception {
        verificarEncerramento();
        if (undoStack.isEmpty()) throw new Exception("Nao ha comando a desfazer.");
        byte[] estadoAtual = capturarEstado();
        redoStack.push(estadoAtual);
        byte[] estadoAnterior = undoStack.pop();
        restaurarEstado(estadoAnterior);
    }

    public void redo() throws Exception {
        verificarEncerramento();
        if (redoStack.isEmpty()) throw new Exception("Nao ha comando a refazer.");
        byte[] estadoAtual = capturarEstado();
        undoStack.push(estadoAtual);
        byte[] proximoEstado = redoStack.pop();
        restaurarEstado(proximoEstado);
    }
}