package org.cs5431;

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

    private BigInteger reconstructSecret(BigInteger[][] coordinateSubsets){
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

    public BigInteger recreateSecret(List<String> secrets, int minSubsets) throws SSS.NotEnoughSubsetsException {
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
            if (minSubsets >= uniqueCoord.size()){
                throw new NotEnoughSubsetsException("Need "+minSubsets+" unique coordinates. Only have "+uniqueCoord.size());
            }
        }
        catch (Exception ex){
            return null;
        }

        //Generate array of arrays of size minSubsets * 2

        BigInteger[][] subsetsUsed = new BigInteger[minSubsets][2];
        Iterator it = uniqueCoord.entrySet().iterator();
        int j = 0;
        while(it.hasNext() && j < minSubsets ){
            Map.Entry<BigInteger,BigInteger> pair = (Map.Entry<BigInteger,BigInteger>) it.next();
            subsetsUsed[j][0] = pair.getKey();
            subsetsUsed[j][1] = pair.getValue();
        }
        BigInteger secret = reconstructSecret(subsetsUsed);

        return secret;
    }

    public class NotEnoughSubsetsException extends Exception {
        NotEnoughSubsetsException(String message) {
            super(message);
        }
    }
}
