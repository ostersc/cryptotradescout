
package com.crypto.trading.tax;

public class TaxResult {
    private final double proceeds;
    private final double costBasis;
    private final double shortTermGain;
    private final double longTermGain;
    
    public TaxResult(double proceeds, double costBasis, double shortTermGain, double longTermGain) {
        this.proceeds = proceeds;
        this.costBasis = costBasis;
        this.shortTermGain = shortTermGain;
        this.longTermGain = longTermGain;
    }
    
    // Getters
    public double getProceeds() { return proceeds; }
    public double getCostBasis() { return costBasis; }
    public double getShortTermGain() { return shortTermGain; }
    public double getLongTermGain() { return longTermGain; }
    public double getTotalGain() { return shortTermGain + longTermGain; }
}
