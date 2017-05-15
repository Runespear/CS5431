package org.cs5431;

import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;

import java.math.BigInteger;
import java.util.*;

public class SSS_Reconstructor{

    public SSS_Reconstructor(){
    }

    private HashMap<BigInteger, BigInteger> checkSubsets(BigInteger[][] coordinateSubsets){
        //Ensure that there are enough UNIQUE subsets
        //Need == numSubsets
        //Use dictionary
        //Check length first
        //Check uniqueness
        //TODO checkSubsets
        HashMap<BigInteger,BigInteger> uniqueCoord = new HashMap<BigInteger,BigInteger>();
        for (int i = 0; i < coordinateSubsets.length ; i++){
            uniqueCoord.put(coordinateSubsets[i][0],coordinateSubsets[i][1]);
        }
        return uniqueCoord;
    }

    private BigRational reconstructSecret(BigInteger[][] coordinateSubsets){
        //Guaranteed to have minimum required
        //Need precheck to ensure that they are unique

        //What we want
        BigRational L0 = BigRational.ZERO;
        //Summation Term
        for (int i = 0; i < coordinateSubsets.length;i++){
            BigRational outside = new BigRational(coordinateSubsets[i][1],BigInteger.ONE);
            //Compute inside
            BigRational inside = BigRational.ONE;
            //The product term
            for (int m = 0; m<coordinateSubsets.length;m++){
                if(m != i){
                    BigInteger numerator = coordinateSubsets[m][0];
                    BigInteger denominator = coordinateSubsets[m][0].subtract(coordinateSubsets[i][0]);
                    BigRational fraction = new BigRational(numerator,denominator);
                    inside = inside.times(fraction);
                }
            }
            L0 = L0.plus(outside.times(inside));
        }
        return L0;

    }

    public BigRational recreateSecretR(List<String> secrets, int minSubsets) throws SSS.NotEnoughSubsetsException {
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
        try{
            if (minSubsets > uniqueCoord.size()){
                throw new NotEnoughSubsetsException("Need "+minSubsets+" unique coordinates. Only have "+uniqueCoord.size());
            }
        }
        catch (Exception ex){
            return null;
        }
        if (Constants.DEBUG_MODE) System.out.println("Success!");
        //Generate array of arrays of size minSubsets * 2

        BigInteger[][] subsetsUsed = new BigInteger[minSubsets][2];
        Iterator it = uniqueCoord.entrySet().iterator();
        int j = 0;
        while(j < minSubsets ){ //&&it.hasNext()
            Map.Entry<BigInteger,BigInteger> pair = (Map.Entry<BigInteger,BigInteger>) it.next();
            subsetsUsed[j][0] = pair.getKey();
            subsetsUsed[j][1] = pair.getValue();
            j++;
        }
        
        BigRational secret = reconstructSecret(subsetsUsed);

        return secret;
    }

    public BigInteger recreateSecret(List<String> secrets, int minSubsets) throws SSS.NotEnoughSubsetsException {
        BigRational asd = recreateSecretR(secrets,minSubsets);
        return asd.num;
    }

    public static class NotEnoughSubsetsException extends Exception {
        NotEnoughSubsetsException(String message) {
            super(message);
        }
    }
}
