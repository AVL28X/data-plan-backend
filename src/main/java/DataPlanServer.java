import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import javax.rmi.CORBA.Util;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Server that manages startup/shutdown of a server.
 */
public class DataPlanServer {
    private static final Logger logger = Logger.getLogger(DataPlanServer.class.getName());

    private Server server;

    private void start() throws IOException {
    /* The port on which the server should run */
        int port = 50051;
        server = ServerBuilder.forPort(port)
                .addService(new DataPlanServiceImpl())
                .build()
                .start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                DataPlanServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        final DataPlanServer server = new DataPlanServer();
        server.start();
        server.blockUntilShutdown();
    }

    public static User convertUserParamsToUser(UserParams userParams){
        double[] weights = new double[28];
        for(int i = 0; i < weights.length; i++){
            int dayOfWeek = i % 7;
            if(dayOfWeek == 0)
                dayOfWeek = 7;

            switch (dayOfWeek){
                case 1:
                    weights[i] = userParams.getW1();
                    break;
                case 2:
                    weights[i] = userParams.getW2();
                    break;
                case 3:
                    weights[i] = userParams.getW3();
                    break;
                case 4:
                    weights[i] = userParams.getW4();
                    break;
                case 5:
                    weights[i] = userParams.getW5();
                    break;
                case 6:
                    weights[i] = userParams.getW6();
                    break;
                case 7:
                    weights[i] = userParams.getW7();
                    break;
                default:
                    break;
            }
        }

        User user = new User(weights);
        user.setAlpha(userParams.getAlpha());
        user.setPhi(userParams.getPhi());
        return user;
    }

    /**
     * generate a random normal user using given user parameters and standard deviation
     * @param userParams
     * @param userParamsStd
     * @return
     */
    public static User generateRandomNormUser(UserParams userParams, UserParamsStd userParamsStd){
        double[] weights = new double[28];
        Random random = new Random();
        for(int i = 0; i < weights.length; i++){
            int dayOfWeek = i % 7;
            if(dayOfWeek == 0)
                dayOfWeek = 7;

            switch (dayOfWeek){
                case 1:
                    weights[i] = userParams.getW1() + random.nextGaussian() * userParamsStd.getW1();
                    break;
                case 2:
                    weights[i] = userParams.getW2() + random.nextGaussian() * userParamsStd.getW2();
                    break;
                case 3:
                    weights[i] = userParams.getW3() + random.nextGaussian() * userParamsStd.getW3();
                    break;
                case 4:
                    weights[i] = userParams.getW4() + random.nextGaussian() * userParamsStd.getW4();
                    break;
                case 5:
                    weights[i] = userParams.getW5() + random.nextGaussian() * userParamsStd.getW5();
                    break;
                case 6:
                    weights[i] = userParams.getW6() + random.nextGaussian() * userParamsStd.getW6();
                    break;
                case 7:
                    weights[i] = userParams.getW7() + random.nextGaussian() * userParamsStd.getW7();
                    break;
                default:
                    break;
            }
        }

        User user = new User(weights);
        user.setAlpha(userParams.getAlpha() + random.nextGaussian() * userParamsStd.getAlpha());
        user.setPhi(userParams.getPhi() + random.nextGaussian() * userParamsStd.getPhi());
        return user;
    }

    /**
     * Implementation of GetUserParams API
     */
    static class DataPlanServiceImpl extends DataPlanServiceGrpc.DataPlanServiceImplBase {
        @Override
        public void getUserParam(UserParamRequest request, StreamObserver<UserParamResponse> responseObserver){
            try {
                List<Usage> usages = request.getUsagesList();

                // Process request to fit user parameters
                double[] dailyUsages = new double[usages.size()];
                Date[] dates = new Date[usages.size()];
                for (int i = 0; i < usages.size(); i++) {
                    dailyUsages[i] = usages.get(i).getUsage();
                    dates[i] = new Date(usages.get(i).getYear() - 1900, usages.get(i).getMonth() - 1, usages.get(i).getDay());
                }

                User.UserType userType = User.UserType.LIGHT;
                if (request.getOverage() > 0)
                    userType = User.UserType.HEAVY;
                UserParamFitter userParamFitter = new UserParamFitter(dailyUsages, dates, request.getOverage(), userType);

                UserParamFitter.UserParamsStd userParamsStd = userParamFitter.getSimulatedParamStds(1000);

                UserParamsStd userParamsStdProto = UserParamsStd.newBuilder()
                        .setW1(userParamsStd.w1)
                        .setW2(userParamsStd.w2)
                        .setW3(userParamsStd.w3)
                        .setW4(userParamsStd.w4)
                        .setW5(userParamsStd.w5)
                        .setW6(userParamsStd.w6)
                        .setW7(userParamsStd.w7)
                        .setAlpha(userParamsStd.alpha)
                        .setPhi(userParamsStd.phi)
                        .build();


                UserParams userParams = UserParams.newBuilder()
                        .setW1(userParamFitter.getDailyWeight(1))
                        .setW2(userParamFitter.getDailyWeight(2))
                        .setW3(userParamFitter.getDailyWeight(3))
                        .setW4(userParamFitter.getDailyWeight(4))
                        .setW5(userParamFitter.getDailyWeight(5))
                        .setW6(userParamFitter.getDailyWeight(6))
                        .setW7(userParamFitter.getDailyWeight(7))
                        .setAlpha(userParamFitter.getAlpha())
                        .setPhi(userParamFitter.getPhi())
                        .build();

                System.out.println("Response:");
                System.out.println(userParams);
                System.out.println(userParamsStd);
                responseObserver.onNext(UserParamResponse.newBuilder().setUserParams(userParams).setUserParamsStd(userParamsStdProto).build());
                responseObserver.onCompleted();
            }catch (Exception e){

            }
        }

        @Override
        public void getRecommendedDataPlans(DataPlanRequest request, StreamObserver<DataPlanResponse> responseObserver) {
            //Get available dataplans from server side
            DataPlan[] dps = DataPlan.getDataPlansFromCSV("Data Plans.csv");
            User user = convertUserParamsToUser(request.getUserParams());
            DataPlan[] topDataPlans = Utilities.getTopDataPlans(user, dps, 5);

            DataPlanResponse.Builder responseBuilder = DataPlanResponse.newBuilder();
            for(int i = 0; i < topDataPlans.length; i++){
                responseBuilder.addDataPlans(
                        DataPlanMsg.newBuilder()
                                .setDescription(topDataPlans[i].description)
                                .setName(topDataPlans[i].name)
                                .setQuota(topDataPlans[i].quota)
                                .setOverage(topDataPlans[i].overage)
                                .setPrice(topDataPlans[i].price).build()
                );
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        }


        @Override
        public void getRecommendedDataPlans2(DataPlanRequest2 request, StreamObserver<DataPlanResponse2> responseObserver) {

            DataPlan[] dps = DataPlan.getDataPlansFromCSV("Data Plans.csv");
            double[] utilities = new double[dps.length];
            double[] maxUtilities = new double[dps.length];
            double[] minUtilities = new double[dps.length];

            int numPaths = 1000;

            double[][] simulatedUtilities = new double[dps.length][numPaths];

            User user = convertUserParamsToUser(request.getUserParams());
            for(int i = 0; i < dps.length; i++){

                utilities[i] = Utilities.calculateDataPlanUtility(user, dps[i]);
                simulatedUtilities[i][0] = utilities[i];
            }

            for(int j = 1; j < numPaths; j++){
                User randomUser = generateRandomNormUser(request.getUserParams(), request.getUserParamsStd());
                for(int i = 0; i < dps.length; i++) {
                    double utility = Utilities.calculateDataPlanUtility(randomUser, dps[i]);
                    simulatedUtilities[i][j] = utility;
                }
            }


            for(int i = 0; i < dps.length; i++){
                //calculate 5 and 95 percentiles of dps
                Arrays.sort(simulatedUtilities[i]);
                minUtilities[i] = simulatedUtilities[i][(int)(numPaths * 0.05)];
                maxUtilities[i] = simulatedUtilities[i][(int)(numPaths * 0.95)];
            }

            DataPlanResponse2.Builder responseBuilder = DataPlanResponse2.newBuilder();
            for(int i = 0; i < dps.length; i++){
                responseBuilder.addDataPlans(
                        DataPlanMsg2.newBuilder()
                                .setDescription(dps[i].description)
                                .setName(dps[i].name)
                                .setQuota(dps[i].quota)
                                .setOverage(dps[i].overage)
                                .setPrice(dps[i].price)
                                .setUtility(utilities[i])
                                .setMaxUtility(maxUtilities[i])
                                .setMinUtility(minUtilities[i])
                                .build()
                );
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getRecommendUsages(RecommendUsagesRequest request, StreamObserver<UsagesResponse> responseObserver) {
            //create data plan and user from request
            DataPlan dp = new DataPlan(
                    request.getDataPlan().getQuota(),
                    request.getDataPlan().getOverage(),
                    request.getDataPlan().getQuota());

            System.out.println("Request: get recommend usages");
            System.out.println(request);

            //set up daily weights
            Date[] dates = Utilities.daysOfMonth(request.getYear(), request.getMonth());
            double[] weights = new double[dates.length];
            for(int i = 0; i < weights.length; i++){
                int dayOfWeek = Utilities.dayOfWeek(dates[i]);
                switch (dayOfWeek){
                    case 1:
                        weights[i] = request.getUserParams().getW1();
                        break;
                    case 2:
                        weights[i] = request.getUserParams().getW2();
                        break;
                    case 3:
                        weights[i] = request.getUserParams().getW3();
                        break;
                    case 4:
                        weights[i] = request.getUserParams().getW4();
                        break;
                    case 5:
                        weights[i] = request.getUserParams().getW5();
                        break;
                    case 6:
                        weights[i] = request.getUserParams().getW6();
                        break;
                    case 7:
                        weights[i] = request.getUserParams().getW7();
                        break;
                    default:
                        break;
                }
            }

            User user = new User(weights, dp);
            user.setAlpha(request.getUserParams().getAlpha());
            user.setPhi(request.getUserParams().getPhi());
            double[] usages = Utilities.getOptimalUsages(user, dp);

            UsagesResponse.Builder responseBuilder = UsagesResponse.newBuilder();
            for(int i = 0; i < dates.length; i++){
                Date date = dates[i];
                Usage usage = Usage.newBuilder().setUsage(usages[i]).setDay(date.getDate()).setMonth(date.getMonth() + 1).setYear(date.getYear() + 1900).build();
                responseBuilder.addUsages(usage);
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getUtility(UtilityRequest request, StreamObserver<UtilityResponse> responseObserver) {
            //create data plan and user from request
            DataPlan dp = new DataPlan(
                    request.getDataPlan().getQuota(),
                    request.getDataPlan().getOverage(),
                    request.getDataPlan().getPrice());
            System.out.println("Request: get utility");
            System.out.println(request);

            User user = convertUserParamsToUser(request.getUserParams());
            user.currentDataPlan = dp;
            double utility = Utilities.calculateDataPlanUtility(user, dp);
            UtilityResponse response = UtilityResponse.newBuilder().setUtility(utility).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }



        @Override
        public void helloWorld(HWRequest request, StreamObserver<HWResponse> responseObserver) {
            System.out.println("Request: hello World");
            System.out.println(request);
            responseObserver.onNext(HWResponse.newBuilder().setWord("Hello from server!").build());
            responseObserver.onCompleted();
        }
    }

}