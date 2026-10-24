package br.ufal.ic.p2.wepayu;

import easyaccept.EasyAccept;
import java.io.File;

public class Main {
    public static void main(String[] args) {
    	
        String[] args2 = { "br.ufal.ic.p2.wepayu.Facade", "testes/us10_1.txt" };
        EasyAccept.main(args2);
    }
}