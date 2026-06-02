package org.unibl.etf.krz.evoting;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;

public class App 
{
    public static void main( String[] args )
    {
        //Registracija Bouncy Castle
        Security.addProvider(new BouncyCastleProvider());

        System.out.println("E-Voting sistem se pokrece...");
        //Ulazna tacka za GUI
    }
}
