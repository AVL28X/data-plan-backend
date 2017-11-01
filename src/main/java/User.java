import java.util.Arrays;
import java.util.List;

public class User {
    public enum UserType{
        LIGHT, MODERATE, HEAVY
    }

    public String userId;

    public double[] dailyUsages;
    public double[] dailyWeights;
    public double alpha = 0.4;
    public double phi = 0.008;
    public DataPlan currentDataPlan;

    public User(){
        dailyUsages = new double[30];
        dailyWeights = new double[30];
        Arrays.fill(dailyWeights, 1.0 / 30);
        currentDataPlan = new DataPlan(1000, 0.005, 5.0);
    }

    public User(double[] dailyWeights, DataPlan dp){
        this.dailyWeights = dailyWeights;
        this.currentDataPlan = dp;
    }

    public double calculateUtility(){
        return Utilities.calculateDataPlanUtility(this, this.currentDataPlan);
    }


    public DataPlan getRecommendedPlan(DataPlan[] dataPlans){
        DataPlan bestDP = this.currentDataPlan;
        double bestUtility = calculateUtility();
        for(DataPlan dp : dataPlans){
            double utility = Utilities.calculateDataPlanUtility(this, dp) ;
            if( utility > bestUtility) {
                bestUtility = utility;
                bestDP = dp;
            }
        }
        System.out.println("Best data plan:");
        System.out.println(bestDP);
        return bestDP;
    }

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    public double getPhi() {
        return phi;
    }

    public void setPhi(double phi) {
        this.phi = phi;
    }

    public UserType getUserType(){
        return Utilities.calculateUserType(this, this.currentDataPlan);
    }

    public DataPlan getRecommendedPlan(List<DataPlan> dataPlans){
        return getRecommendedPlan((DataPlan[]) dataPlans.toArray());
    }


}
