package automail;

public class Statistics {
    private static int packagesNormal = 0;
    private static int packagesCaution = 0;
    private static int weightNormal = 0;
    private static int weightCaution = 0;
    private static int timeWrap = 0;
    private static int timeUnwrap = 0;

    public static int packagesNormal() {
    	return packagesNormal;
    }
    
    public static void packagesNormalIncrement() {
    	packagesNormal++;
    }
    
    public static int packagesCaution() {
    	return packagesCaution;
    }
    
    public static void packagesCautionIncrement() {
    	packagesCaution++;
    }
    
    public static int weightNormal() {
    	return weightNormal;
    }
    
    public static void weightNormalIncrement(int weight) {
    	weightNormal += weight;
    }
    
    public static int weightCaution() {
    	return weightCaution;
    }
    
    public static void weightCautionIncrement(int weight) {
    	weightCaution += weight;
    }
    
    public static int timeWrap() {
    	return timeWrap;
    }
    
    public static void timeWrapIncrement() {
    	timeWrap += 2;
    }
    
    public static int timeUnwrap() {
    	return timeUnwrap;
    }
    
    public static void timeUnwrapIncrement() {
    	timeUnwrap++;
    }
}
