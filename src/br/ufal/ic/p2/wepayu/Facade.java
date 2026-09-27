package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoExisteException;
import br.ufal.ic.p2.wepayu.models.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Facade {
	private Map<String, Empregado> empregados = new HashMap<>();

	public void zerarSistema() {
		empregados.clear();
	}

	public void encerrarSistema() {
	}

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

		if (atributo.equals("sindicalizado"))
			return "false";

		if (atributo.equals("comissao")) {
			if (e instanceof Comissionado)
				return ((Comissionado) e).getComissao();
			throw new Exception("Atributo nao existe.");
		}

		throw new Exception("Atributo nao existe.");
	}
	
	public void removerEmpregado(String emp) throws Exception {
        if (emp == null || emp.isEmpty()) {
            throw new Exception("Identificacao do empregado nao pode ser nula.");
        }
        
        if (!empregados.containsKey(emp)) {
            throw new EmpregadoNaoExisteException();
        }
        
        empregados.remove(emp);
    }
	
	
}