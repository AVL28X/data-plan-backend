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
}


