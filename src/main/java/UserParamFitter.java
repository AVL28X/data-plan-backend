import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.RealVector;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class UserParamFitter {
    public final double[] usages;
    public final Date[] dates;
    public final User.UserType userType;
    public double overage;
    public boolean fitted = false;
    public double[] params; // first 6 are weights, weight 7 = 1 - sum of other weights, last two are alpha and phis

    public double[] weightsStd;
    public double phiStd;
    public double alphaStd;

    public class UserParamsStd{
        double w1;
        double w2;
        double w3;
        double w4;
        double w5;
        double w6;
        double w7;
        double alpha;
        double phi;
    }


    public UserParamFitter(double[] usages, Date[] dates, double pi, User.UserType userType) throws Exception{
        if(usages.length != dates.length)
            throw new Exception("Usages and dates should be of same length");
        this.usages = usages;
        this.dates = dates;
        this.overage = pi;
        this.userType = userType;
        fit();
    }

    public double[] getWeights(){
        return Arrays.copyOfRange(params, 0, 6);
    }

    public double getDailyWeight(int dayOfWeek){
        if(dayOfWeek == 6)
            return 0.25 - getSumWeight(this.params);
        else
            return params[dayOfWeek % 7];

    }

    public double getPhi(){
        return params[6];
    }

    public double getAlpha(){
        return params[7];
    }


    public double getSumWeight(double[] params){
        double sum = 0;
        for(int i = 0; i < 6; i++)
            sum += params[i];
        return sum;
    }

    public void fit(){
        MultivariateVectorFunction vectorFunction = new MultivariateVectorFunction() {
            @Override
            public double[] value(double[] params) throws IllegalArgumentException {
                //calculate errors
                double[] results = new double[usages.length];
                for(int i = 0; i < usages.length; i++){
                    double w;

                    if( dayOfWeek(dates[i]) == 6 ) {
                        w = 0.25 - getSumWeight(params);
                        System.out.println("w = " + w);
                    }else
                        w = params[dayOfWeek(dates[i])];


                    results[i] = predictedUsage(w, params[6], params[7]);
                }
                return results;
            }
        };

        //Jacobian
        MultivariateMatrixFunction jacobianFunction = new MultivariateMatrixFunction(){
            @Override
            public double[][] value(double[] params) throws IllegalArgumentException {
                double[][] jacobian = new double[usages.length][params.length];
                for(int i = 0; i < usages.length; i++)
                    for(int j = 0; j < params.length; j++)
                        jacobian[i][j] = 0;

                double phi = params[6];
                double alpha = params[7];
                for(int i = 0; i < usages.length; i++) {
                    int day = dayOfWeek(dates[i]);
                    double w = params[day];
                    if(day == 6)
                        w = 0.25 - getSumWeight(params);
                    double usagePredicted = predictedUsage(w, phi, alpha);

                    if (userType == User.UserType.LIGHT || userType == User.UserType.MODERATE) {
                        //update diff w.r.t w_j
                        if(day != 6)
                            jacobian[i][day] +=  Math.pow(w / phi, 1 / alpha - 1) / (alpha * phi);
                        else {
                            for(int j = 0; j < 6; j++)
                                jacobian[i][j] -= Math.pow(w / phi, 1 / alpha - 1) / (alpha * phi);
                        }
                        //update diff w.r.t phi
                        jacobian[i][6] -=  Math.pow(w / phi, 1 / alpha - 1) * w / (alpha * phi * phi);
                        //update diff w.r.t alpha
                        jacobian[i][7] -=  usagePredicted / (alpha * alpha) * Math.log( w / phi);
                    } else {
                        //update diff w.r.t w_j
                        if(day != 6)
                            jacobian[i][day] +=  Math.pow(w / (phi + overage), 1 / alpha - 1) / (alpha * (phi + overage));
                        else {
                            for(int j = 0; j < 6; j++)
                                jacobian[i][j] -= Math.pow(w / (phi + overage), 1 / alpha - 1) / (alpha * (phi + overage));
                        }
                        //update diff w.r.t phi
                        jacobian[i][6] -=  Math.pow(w / (phi + overage), 1 / alpha - 1) * w / (alpha * (phi + overage) * (phi + overage));
                        //update diff w.r.t alpha
                        jacobian[i][7] -= usagePredicted / (alpha * alpha) * Math.log(w / (phi + overage));
                    }
                }
                return jacobian;
            }
        };

        double[] initialParams = new double[8];
        for(int i = 0; i < 6; i++)
            initialParams[i] = 0.25 / 7;
        initialParams[6] = 0.01;
        initialParams[7] = 1;

        LeastSquaresProblem problem = new LeastSquaresBuilder().
                start(initialParams).
                model(vectorFunction, jacobianFunction).
                target(usages).
                lazyEvaluation(false).
                maxEvaluations(1000000).
                maxIterations(1000000).
                build();

        LeastSquaresOptimizer.Optimum optimum = new LevenbergMarquardtOptimizer().optimize(problem);
        RealVector sol = optimum.getPoint();

        //System.out.println(optimum.getCost());
        //System.out.println(optimum.getResiduals());
        //System.out.println(optimum.getRMS());
        this.params = sol.toArray();
        fitted =true;
    }

//    @Deprecated
//    public void fit2(){
//        MultivariateVectorFunction vectorFunction = new MultivariateVectorFunction() {
//            @Override
//            public double[] value(double[] params) throws IllegalArgumentException {
//                //calculate errors
//                double sumErrors = 0;
//                double[] results = new double[usages.length];
//                for(int i = 0; i < usages.length; i++){
//                    double w = params[dayOfWeek(dates[i])];
//                    results[i] = predictedUsage(w, params[7], params[8]);
//                }
//                return results;
//            }
//        };
//
//        //Jacobian
//        MultivariateMatrixFunction jacobianFunction = new MultivariateMatrixFunction(){
//            @Override
//            public double[][] value(double[] params) throws IllegalArgumentException {
//                double[][] jacobian = new double[usages.length][params.length];
//                for(int i = 0; i < usages.length; i++)
//                    for(int j = 0; j < 9; j++)
//                        jacobian[i][j] = 0;
//
//                double phi = params[7];
//                double alpha = params[8];
//                for(int i = 0; i < usages.length; i++) {
//                    int day = dayOfWeek(dates[i]);
//                    double w = params[day];
//                    double usagePredicted = predictedUsage(w, params[6], params[7]);
//                    double error = usagePredicted - usages[i];
//
//                    if (userType == User.UserType.LIGHT || userType == User.UserType.MODERATE) {
//                        //update diff w.r.t w_j
//                        jacobian[i][day] +=  Math.pow(w / phi, 1 / alpha - 1) / (alpha * phi);
//                        //update diff w.r.t phi
//                        jacobian[i][7] -=  Math.pow(w / phi, 1 / alpha - 1) * w / (alpha * phi * phi);
//
//                        jacobian[i][7] = 0;
//                        //update diff w.r.t alpha
//                        jacobian[i][8] -=  usagePredicted / (alpha * alpha) * Math.log(w / phi);
//                    } else {
//                        //update diff w.r.t w_j
//                        jacobian[i][day] += Math.pow(w / (phi + overage), 1 / alpha - 1) / (alpha * (phi + overage));
//                        //update diff w.r.t phi
//                        jacobian[i][7] -=  Math.pow(w / (phi + overage), 1 / alpha - 1) * w / (alpha * (phi + overage) * (phi + overage));
//                        jacobian[i][7] = 0;
//                        //update diff w.r.t alpha
//                        jacobian[i][8] -= usagePredicted / (alpha * alpha) * Math.log(w / (phi + overage));
//                    }
//                }
//                return jacobian;
//            }
//        };
//
//        double[] initialParams = new double[9];
//        for(int i = 0; i < 7; i++)
//            initialParams[i] = 1.0 / 30;
//        initialParams[7] = 0.02;
//        initialParams[8] = 1;
//
//        LeastSquaresProblem problem = new LeastSquaresBuilder().
//                start(initialParams).
//                model(vectorFunction, jacobianFunction).
//                target(usages).
//                lazyEvaluation(false).
//                maxEvaluations(1000).
//                maxIterations(100000000).
//                build();
//
//        LeastSquaresOptimizer.Optimum optimum = new LevenbergMarquardtOptimizer().optimize(problem);
//        RealVector sol = optimum.getPoint();
//
//        this.params = sol.toArray();
//    }
//


    @Override
    public String toString(){
        if(!fitted)
            return "Not fitted!!!\n";
        StringBuilder res = new StringBuilder();
        for(int i = 0; i < 6; i++)
            res.append("Weight").append(i + 1).append(": ").append(params[i]).append("\n");
        res.append("Weight7: ").append(0.25 - getSumWeight(params)).append("\n");
        res.append("Phi: ").append(params[6]).append("\n");
        res.append("Alpha: ").append(params[7]).append("\n");
        return res.toString();
    }


    public int dayOfWeek(Date date){
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek - 1;
    }

    public double predictedUsage(double weight, double phi, double alpha){
        if(userType == User.UserType.LIGHT || userType == User.UserType.MODERATE)
            return Math.pow(weight / phi, 1.0 / alpha);
        else
            return Math.pow(weight / (phi + overage), 1.0 / alpha);
    }

    /**
     * Testing
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        double[] usages = new double[30];
        Date[] dates = new Date[30];

        Date today = new Date();
        for(int i = 0; i < 30; i++) {
            System.out.println();
            usages[i] =  10 * (i % 7 + 1);
            Date date = new Date(today.getTime() + (1000 * 60 * 60 * 24 * i));
            dates[i] = date;
        }

        UserParamFitter userParamFitter = new UserParamFitter(usages, dates, 0.01, User.UserType.HEAVY);

        for(int i = 0; i < 30; i++) {
            System.out.println(userParamFitter.dayOfWeek(dates[i]));
        }

        userParamFitter.fit();
        System.out.println(userParamFitter);
    }


    /**
     * getSimulatedParamStds: assumes usages follows normal distribution, simulate w_i and solve alpha and phi to calculate
     * standard deviations of alpha and phi
     */
    public UserParamsStd getSimulatedParamStds(int numPaths) throws Exception {
        double usageStd = calcStd(this.usages);
        double[] alphas = new double[numPaths];
        double[] phis = new double[numPaths];
        double[][] weights = new double[8][numPaths];

        Random rand = new Random();
        for(int i = 0; i < numPaths; i++){
            double[] simulatedUsages = Arrays.copyOf(usages, usages.length);
            // add perturbations
            for(int j = 0; j < simulatedUsages.length; j++){
                simulatedUsages[j] += rand.nextGaussian() * usageStd;
                simulatedUsages[j] = Math.max(simulatedUsages[j], 0);
            }

            UserParamFitter userParamsFitter = new UserParamFitter(simulatedUsages, this.dates, this.overage, this.userType);
            for(int j = 1; j < 8; j++)
                weights[j][i] = userParamsFitter.getDailyWeight(j);
            alphas[i] = userParamsFitter.getAlpha();
            phis[i] = userParamsFitter.getPhi();
        }

        UserParamsStd userParamsStd = new UserParamsStd();
        userParamsStd.w1 = calcStd(weights[1]);
        userParamsStd.w2 = calcStd(weights[2]);
        userParamsStd.w3 = calcStd(weights[3]);
        userParamsStd.w4 = calcStd(weights[4]);
        userParamsStd.w5 = calcStd(weights[5]);
        userParamsStd.w6 = calcStd(weights[6]);
        userParamsStd.w7 = calcStd(weights[7]);

        userParamsStd.alpha = calcStd(alphas);
        userParamsStd.phi = calcStd(phis);

        return userParamsStd;
    }

    public static double calcStd(double[] nums){
        double sum = 0;
        double totalVar = 0;
        for(double num : nums){
            sum += num;
        }
        double mean = sum / nums.length;
        for(double num : nums){
            totalVar += (num - mean) * (num - mean);
        }
        return Math.sqrt(totalVar / (nums.length - 1));
    }




}
