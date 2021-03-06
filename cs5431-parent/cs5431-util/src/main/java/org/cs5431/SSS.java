package org.cs5431;

import java.io.PrintStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by user on 13/5/2017.
 */
public class SSS {
    //Shamir Secret Sharing

    public int numParts; // Degree of Polynomial
    public int numSubsets; // Number of Subsets required to reconstruct
    public BigInteger secret; //Big secret!
    public SecureRandom generator = new SecureRandom();
    public int[] subsets;
    public BigInteger[][] coordinates; // Dimension is numParts by 2, [x,f(x)], x starts from 1;
    public BigInteger[] polynomial; //Degree of polynomial = # subsets, where x^0 coeff is secret

    public SSS(BigInteger secret){
        //Default is 3 parts with any 2 points
        this.numParts = 3;
        this.numSubsets = 2;
        this.secret = secret;
        //generatePolynomial();
    }

    public SSS(int nParts, int nSubsets, BigInteger secret){
        assert (nParts >= 2 && nSubsets >= 2): "At least 3 parts and 2 subsets";
        assert (  nParts >= nSubsets  ):"Number of Subsets must be lower than number of parts";
        this.numParts = nParts;
        this.numSubsets = nSubsets;
        this.secret = secret;
        //generatePolynomial();
    }


    public BigInteger[] generatePolynomial(){
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
        //deep copy
        BigInteger retPoly[] = new BigInteger[this.polynomial.length];
        System.arraycopy(this.polynomial, 0, retPoly, 0, this.polynomial.length);
        return retPoly;
    }

    public BigInteger applyPoly(BigInteger x, BigInteger[] polynomial){
        //Input x into the polynomial
        //Return the output
        BigInteger y = BigInteger.ZERO;
        for (int i = 0;i < polynomial.length;i++){
            BigInteger coefficient = polynomial[i];
            //int exponent = i;
            BigInteger update = coefficient.multiply(x.pow(i));
            y = y.add(update);
        }
        return y;
    }

    public BigInteger applyPoly(int x, BigInteger[] polynomial){
        //If input int
        BigInteger X = BigInteger.valueOf(x);
        return applyPoly(X,polynomial);
    }

    public BigInteger[][] generateCoordinates(){
        //Generate # coordinates = this.numParts
        int numCoordinates = this.numParts;
        int x = 1; // Start from x=1 end at x = numParts
        this.coordinates = new BigInteger[this.numParts][2];
        for (int i = 0; i<this.coordinates.length;i++){
            this.coordinates[i][0] = BigInteger.valueOf(x);
            this.coordinates[i][1] = applyPoly(x,this.polynomial);
            x++;
        }
        BigInteger retCoord[][] = new BigInteger[this.coordinates.length][];
        for (int i = 0; i < this.coordinates.length; i++) {
            retCoord[i] = new BigInteger[this.coordinates[i].length];
            System.arraycopy(this.coordinates[i], 0, retCoord[i], 0, this.coordinates[i].length);
        }
        return retCoord;
    }

    public HashMap<BigInteger, BigInteger> checkSubsets(BigInteger[][] coordinateSubsets){
        //Ensure that there are enough UNIQUE subsets
        //Need == numSubsets
        //Use dictionary
        //Check length first
        //assert(coordinateSubsets.length >= this.numSubsets - 1):"Not enough subsets";
        //Check uniqueness
        //TODO checkSubsets
        HashMap<BigInteger,BigInteger> uniqueCoord = new HashMap<BigInteger,BigInteger>();
        for (int i = 0; i < coordinateSubsets.length ; i++){
            uniqueCoord.put(coordinateSubsets[i][0],coordinateSubsets[i][1]);
        }
        return uniqueCoord;
    }

    public BigInteger reconstructSecret(BigInteger[][] coordinateSubsets){
        //Guaranteed to have minimum required
        //Need precheck to ensure that they are unique

        //What we want
        BigInteger L0 = BigInteger.ZERO;
        //Summation Term
        for (int i = 0; i < coordinateSubsets.length;i++){
            BigInteger outside = (coordinateSubsets[i][1]);
            //Compute inside
            BigInteger inside = BigInteger.ONE;
            //The product term
            for (int m = 0; m<coordinateSubsets.length;m++){
                if(m != i){
                    inside = inside.multiply(coordinateSubsets[m][0]);
                    inside = inside.divide(coordinateSubsets[m][0].subtract(coordinateSubsets[i][0]));
                }
            }
            L0 = L0.add(outside.multiply(inside));
        }
        return L0;

    }


    public List<String> generateSecrets() {
        //TODO
        this.polynomial = generatePolynomial();
        BigInteger[][] coordinates = generateCoordinates();
        //Format is x:f(x)
        //e.g. 5:123456
        List<String> secrets = new ArrayList<String>();
        for (int i = 0 ; i < coordinates.length;i++){
            String coordinateStr = coordinates[i][0].toString() + ":" + coordinates[i][1].toString();
            secrets.add(coordinateStr);
        }
        return secrets;
    }

    public BigInteger recreateSecret(List<String> secrets, int minSubsets) throws NotEnoughSubsetsException{
        //min Subsets is the minimum number of unique secrets required to reconstruct
        //Parse as x:f(x)
        int k = secrets.size();
        BigInteger[][] coordinates = new BigInteger[k][2];
        for (int i = 0;i < k ; i++){
            String[] parts = secrets.get(i).split(":");
            coordinates[i][0] = new BigInteger(parts[0]);
            coordinates[i][1] = new BigInteger(parts[1]);
        }
        HashMap<BigInteger,BigInteger> uniqueCoord = checkSubsets(coordinates);
        if (minSubsets >= uniqueCoord.size()){
            throw new NotEnoughSubsetsException("Need "+minSubsets+" unique coordinates. Only have "+uniqueCoord.size());
        }
        //Generate array of arrays of size minSubsets * 2

        BigInteger[][] subsetsUsed = new BigInteger[minSubsets][2];
        Iterator it = uniqueCoord.entrySet().iterator();
        int j = 0;
        while(it.hasNext() && j < minSubsets ){
            Map.Entry<BigInteger,BigInteger> pair = (Map.Entry<BigInteger,BigInteger>) it.next();
            subsetsUsed[j][0] = pair.getKey();
            subsetsUsed[j][1] = pair.getValue();
            j++;
        }
        BigInteger secret = reconstructSecret(subsetsUsed);

        return secret;
    }

    public static class NotEnoughSubsetsException extends Exception {
        NotEnoughSubsetsException(String message) {
            super(message);
        }
    }
}
