package org.cs5431;

import java.io.PrintStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Created by user on 13/5/2017.
 */
public class SSS {
    //Shamir Secret Sharing

    public int numParts; // Degree of Polynomial
    public int numSubsets; // Number of Subsets required to reconstruct
    private BigInteger secret; //Big secret!
    private SecureRandom generator = new SecureRandom();
    private int[] subsets;
    private BigInteger[][] coordinates; // Dimension is numParts by 2, [x,f(x)], x starts from 1;
    private BigInteger[] polynomial; //Degree of polynomial = # subsets, where x^0 coeff is secret

    public SSS(BigInteger secret){
        //Default is 3 parts with any 2 points
        this.numParts = 3;
        this.numSubsets = 2;
        this.secret = secret;
    }

    public SSS(int nParts, int nSubsets, BigInteger secret){
        assert (nParts >= 3 && nSubsets >= 2): "At least 3 parts and 2 subsets";
        assert (  nParts >= nSubsets  ):"Number of Subsets must be lower than number of parts";
        this.numParts = nParts;
        this.numSubsets = nSubsets;
        this.secret = secret;
    }


    private void generatePolynomial(){
        //Generate k-1 numbers, where they are for x, x^2,...,x^(k-1)
        //x^0 coeff is the secret itself
        this.subsets = new int[this.numSubsets-1];
        //Randomly generate coefficients securely
        for (int i = 0; i < this.subsets.length; i++){
            this.subsets[i] = generator.nextInt();
        }
        //Save the polynomial
        this.polynomial = new BigInteger[this.numSubsets];
        this.polynomial[0] = this.secret;
        for (int i = 1; i<this.polynomial.length;i++ ){
            this.polynomial[i]= BigInteger.valueOf(this.subsets[i-1]);
        }

    }

    private BigInteger applyPoly(BigInteger x){
        //Input x into the polynomial
        //Return the output
        BigInteger y = BigInteger.ZERO;
        for (int i = 0;i < this.polynomial.length;i++){
            BigInteger coefficient = this.polynomial[i];
            //int exponent = i;
            BigInteger update = coefficient.multiply(x.pow(i));
            y = y.add(update);
        }
        return y;
    }

    private BigInteger applyPoly(int x){
        //If input int
        BigInteger X = BigInteger.valueOf(x);
        return applyPoly(X);
    }

    private void generateCoordinates(){
        //Generate # coordinates = this.numParts
        int numCoordinates = this.numParts;
        int x = 1; // Start from x=1 end at x = numParts
        for (int i = 0; i<this.coordinates.length;i++){
            this.coordinates[i][0] = BigInteger.valueOf(x);
            this.coordinates[i][1] = applyPoly(x);
            x++;
        }
    }

    private BigInteger reconstructSecret(BigInteger[][] coordinateSubsets){
        assert(coordinateSubsets.length >= this.numSubsets - 1):"Not enough subsets";

        //TODO: Check how to reconstruct
        return null;
    }


    public BigInteger[][] disseminateCoordinates(){
        return this.coordinates;
    }

}
