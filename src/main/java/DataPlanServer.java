import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import javax.rmi.CORBA.Util;
import java.io.IOException;
import java.util.Date;
import java.util.List;
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

    /**
     * Implementation of GetUserParams API
     */
    static class DataPlanServiceImpl extends DataPlanServiceGrpc.DataPlanServiceImplBase {
        @Override
        public void getUserParam(UserParamRequest request, StreamObserver<UserParams> responseObserver){
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
                userParamFitter.fit();
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
                responseObserver.onNext(userParams);
                responseObserver.onCompleted();
            }catch (Exception e){

            }
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

            double[] weights = new double[28];
            for(int i = 0; i < weights.length; i++){
                int dayOfWeek = i % 7;
                if(dayOfWeek == 0)
                    dayOfWeek = 7;

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