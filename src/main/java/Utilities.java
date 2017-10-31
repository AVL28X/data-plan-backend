import java.util.Calendar;
import java.util.Date;

public class Utilities {

    /**
     * given user and data plan, return the type of user
     * @param u - User object
     * @param dp - Data Plan object
     * @return - enum: user type
     */
    public static User.UserType calculateUserType(User u, DataPlan dp){
        double lightUsage = 0;
        double heavyUsage = 0;
        for(int i = 0; i < u.dailyWeights.length; i++)
            lightUsage += Math.pow( u.dailyWeights[i] / u.phi, 1 / u.alpha);

        for(int i = 0; i < u.dailyWeights.length; i++)
            heavyUsage += Math.pow( u.dailyWeights[i] / (dp.overage + u.phi), 1 / u.alpha);

        if(dp.quota > lightUsage)
            return User.UserType.LIGHT;

        if(dp.quota < heavyUsage)
            return User.UserType.HEAVY;

        return User.UserType.MODERATE;
    }

    /**
     *
     * @param u - user object
     * @param dp - data plan object
     * @return monthly utility
     */
    public static double calculateDataPlanUtility(User u, DataPlan dp){
        User.UserType userType = calculateUserType(u, dp);
        double utility = 0;
        //calculate utility based on user type
        if(userType == User.UserType.LIGHT) {
            for(int i = 0; i < u.dailyWeights.length; i++)
                utility += (u.alpha / (1 - u.alpha)) * Math.pow(u.dailyWeights[i], 1 / u.alpha) * Math.pow(u.phi, 1 - 1 / u.alpha);
            utility -= dp.price;
        }else if(userType == User.UserType.HEAVY){
            for(int i = 0; i < u.dailyWeights.length; i++)
                utility += Math.pow(u.dailyWeights[i], 1 / u.alpha);
            utility = Math.pow(utility, u.alpha);
            utility *= Math.pow(dp.quota, 1 - u.alpha) / (1 - u.alpha);
            utility -= u.phi * dp.quota;
            utility -= dp.price;
        }else{//moderate usage
            for(int i = 0; i < u.dailyWeights.length; i++)
                utility += (u.alpha / (1 - u.alpha)) * Math.pow(u.dailyWeights[i], 1 / u.alpha) * Math.pow(u.phi + dp.overage, 1 - 1 / u.alpha);
            utility += dp.overage * dp.quota;
            utility -= dp.price;

        }
        return utility;
    }

    /**
     * calculate dynamic programming optimal usages (Corollary 1 in the paper)
     * @param u
     * @param dp
     * @return
     */
    public static double[] getOptimalUsages(User u, DataPlan dp){
        double[] usages = new double[u.dailyWeights.length];
        User.UserType userType = calculateUserType(u, dp);
        if(userType == User.UserType.LIGHT) {
            for(int i = 0; i < u.dailyWeights.length; i++)
                usages[i] = Math.pow(u.dailyWeights[i] / u.phi, 1 / u.alpha);
        }else if(userType == User.UserType.HEAVY){
            for(int i = 0; i < u.dailyWeights.length; i++)
                usages[i] = Math.pow(u.dailyWeights[i] / ( u.phi + dp.overage), 1 / u.alpha);
        }else{//moderate usage
            double sum = 0;
            for(int i = 0; i < u.dailyWeights.length; i++)
                sum += Math.pow(u.dailyWeights[i], 1 / u.alpha);
            for(int i = 0; i < u.dailyWeights.length; i++)
                usages[i] = dp.quota * Math.pow(u.dailyWeights[i], 1 / u.alpha) / sum;
        }
        return usages;
    }

    /**
     * calculate ISP profit
     * @param dp
     * @param sigma
     * @param users
     * @return
     */
    public static double ISPProfit(DataPlan dp, double sigma, User[] users){
        double profit = 0;
        for(int i = 0; i < users.length; i++){
            User user = users[i];
            User.UserType userType = calculateUserType(user, dp);
            if(userType == User.UserType.LIGHT){
                double usage = 0;
                for(int j = 0; j < user.dailyWeights.length; j++)
                    usage += Math.pow(user.dailyWeights[j] / user.phi, 1 / user.alpha);
                profit += dp.price - sigma * usage;
            }else if(userType == User.UserType.HEAVY){
                double usage = 0;
                for(int j = 0; j < user.dailyWeights.length; j++)
                    usage += Math.pow(user.dailyWeights[j] / (dp.overage + user.phi), 1 / user.alpha);
                profit += dp.price + dp.overage * (usage - dp.quota) - sigma * usage;
            }else{//moderate usage
                profit += (dp.price - sigma * dp.quota);
            }
        }
        return profit;
    }

    public static Date[] daysOfMonth(int year, int month){
        year -= 1900;
        month -= 1;
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(year, month, 1));
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int myMonth=cal.get(Calendar.MONTH);
        int len = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        Date[] days = new Date[len];
        int count = 0;

        while (myMonth==cal.get(Calendar.MONTH)) {
            days[count] = (Date) cal.getTime().clone();
            count++;
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        return days;
    }

    public static void main(String[] args){
        Date[] dates = daysOfMonth(2017, 2);
        for(Date d : dates)
            System.out.println(d);
    }

}


