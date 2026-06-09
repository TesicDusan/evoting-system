package org.unibl.etf.krz.evoting;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.unibl.etf.krz.evoting.ca.CAInitializer;

import java.security.Security;

public class App 
{
    public static void main(String[] args) throws Exception
    {
        //Registracija Bouncy Castle
        Security.addProvider(new BouncyCastleProvider());
        System.out.println("E-Voting sistem se pokrece...");

        //Inicijalizacija CA
        CAInitializer.initialize();
        System.out.println("CA tijela inicijalizovana");
        //Ulazna tacka za GUI
    }
}
