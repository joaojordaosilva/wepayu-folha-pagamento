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

		if (atributo.equals("nome"))
			return e.getNome();
		if (atributo.equals("endereco"))
			return e.getEndereco();
		if (atributo.equals("tipo"))
			return e.getTipo();

		if (atributo.equals("salario")) {
			int sal = e.getSalario();
			return String.format("%d,%02d", sal / 100, sal % 100);
		}

		if (atributo.equals("sindicalizado")) {
            return (e.getSindicato() != null) ? "true" : "false";
        }

		if (atributo.equals("comissao")) {
			if (e instanceof Comissionado)
				return ((Comissionado) e).getComissao();
			throw new Exception("Atributo nao existe.");
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

    // Para quando a flag valor for "false" (sair do sindicato)
    public void alteraEmpregado(String emp, String atributo, String valor) throws Exception {
        Empregado e = empregados.get(emp);
        if (e == null) throw new EmpregadoNaoExisteException();
        
        if (atributo.equals("sindicalizado") && valor.equals("false")) {
            e.setSindicato(null);
        }
    }

    // Para quando a flag valor for "true" (entrar no sindicato com id e taxa)
    public void alteraEmpregado(String emp, String atributo, String valor, String idSindicato, String taxaSindical) throws Exception {
        Empregado e = empregados.get(emp);
        if (e == null) throw new EmpregadoNaoExisteException();
        
        if (atributo.equals("sindicalizado") && valor.equals("true")) {
            if (buscarPorSindicato(idSindicato) != null) {
                throw new Exception("Ha outro empregado com esta identificacao de sindicato");
            }
            double taxa = Double.parseDouble(taxaSindical.replace(",", "."));
            e.setSindicato(new MembroSindicato(idSindicato, taxa));
        }
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
	
}