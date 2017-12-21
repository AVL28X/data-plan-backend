/*
 * Copyright 2015, gRPC Authors All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * A simple client that requests service from data plan server
 */
public class DataPlanClient {
    private static final Logger logger = Logger.getLogger(DataPlanClient.class.getName());
    private final ManagedChannel channel;
    private final DataPlanServiceGrpc.DataPlanServiceBlockingStub blockingStub;

    /**
     * Construct client connecting to Data Plan server at host:port
     */
    public DataPlanClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext(true)
                .build());
    }

    /**
     * Construct client for accessing RouteGuide server using the existing channel.
     */
    DataPlanClient(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = DataPlanServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * generate dates of current month for testing
     * @return
     */
    public static Date[] generateTestDates(){
        Date[] dates = new Date[30];
        Date today = new Date();
        for (int i = 0; i < 30; i++) {
            Date date = new Date(today.getTime() + (1000 * 60 * 60 * 24 * i));
            dates[i] = date;
        }
        return dates;
    }

    /**
     * generate random usages, usage[i] = (i % 7) * 10 + Math.random() * 1;
     * @param dates
     * @return
     */
    public static double[] generateRandomUsages(Date[] dates){
        double[] usages = new double[dates.length];
        for (int i = 0; i < dates.length; i++) {
            usages[i] = (i % 7) * 10 + Math.random() * 1;
        }
        return usages;
    }

    /**
     * get calibrated user parameters from server
     * @param dates
     * @param usages
     * @param overage - overage charge
     * @return - calibrated user parameter
     */
    public UserParamResponse getUserParams(Date[] dates, double[] usages, double overage) {

        UserParamRequest.Builder builder = UserParamRequest.newBuilder();
        for (int i = 0; i < dates.length; i++) {
            Date date = dates[i];
            Usage usage = Usage.newBuilder().setUsage(usages[i]).setDay(date.getDay()).setMonth(date.getMonth()).setYear(date.getYear()).build();
            builder.addUsages(usage);
        }
        builder.setOverage(overage);
        return this.blockingStub.getUserParam(builder.build());
    }

    /**
     * get recommended usages of current month from server
     * @param year
     * @param month
     * @param userParams
     * @param dataPlanMsg
     * @return
     */
    public UsagesResponse getRecommendUsages(int year, int month, UserParams userParams, DataPlanMsg dataPlanMsg){
        RecommendUsagesRequest request = RecommendUsagesRequest.newBuilder().setDataPlan(dataPlanMsg).setUserParams(userParams).setYear(year).setMonth(month).build();
        return this.blockingStub.getRecommendUsages(request);
    }

    /**
     * get unility of user and data plan
     * @param userParams
     * @param dataPlanMsg
     * @return utility
     */
    public double getUtility(UserParams userParams, DataPlanMsg dataPlanMsg){
        UtilityRequest request = UtilityRequest.newBuilder().setDataPlan(dataPlanMsg).setUserParams(userParams).build();
        UtilityResponse response = this.blockingStub.getUtility(request);
        return response.getUtility();
    }

    /**
     * get recommended data plan
     * @param userParams
     * @return a list of data plans
     */
    public List<DataPlanMsg> getRecommendDataPlans(UserParams userParams){
        DataPlanRequest request = DataPlanRequest.newBuilder().setUserParams(userParams).build();
        DataPlanResponse response = this.blockingStub.getRecommendedDataPlans(request);
        return response.getDataPlansList();
    }

    /**
     * get detailed utilities of data plans
     * @param userParams
     * @param userParamsStd
     * @return sorted data plans with max, min utilities
     */
    public List<DataPlanMsg2> getRecommendDataPlans2(UserParams userParams, UserParamsStd userParamsStd){
        DataPlanRequest2 request = DataPlanRequest2.newBuilder().setUserParams(userParams).setUserParamsStd(userParamsStd).build();
        DataPlanResponse2 response = this.blockingStub.getRecommendedDataPlans2(request);
        List<DataPlanMsg2> modifiableList = new ArrayList<DataPlanMsg2>(response.getDataPlansList());
        Collections.sort(modifiableList,
                new Comparator<DataPlanMsg2>() {
                    @Override
                    public int compare(DataPlanMsg2 o1, DataPlanMsg2 o2) {
                        if (o1.getUtility() > o2.getUtility())
                            return -1;
                        else
                            return 1;
                    }
                }

        );
        return modifiableList;
    }

    /**
     * Hello word for testing
     */
    public void helloWorld(){
        HWRequest request = HWRequest.newBuilder().setWord("Hello from client").build();
        System.out.println(request);
        HWResponse response = this.blockingStub.helloWorld(request);
        System.out.println(response);
    }

    /**
     * Test the data plan client and server from server side
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        DataPlanClient client = new DataPlanClient(host, 50051);

        //Test Hello World
        client.helloWorld();

        //A pseudo data plan and get recommended usages
        DataPlanMsg dataPlanMsg = DataPlanMsg.newBuilder().setQuota(1000).setOverage(0.01).setPrice(35).build();
        System.out.println("Pseudo data plan created");
        System.out.println(dataPlanMsg);

        //Test parameter estimation
        Date[] dates = generateTestDates();
        double[] usages = generateRandomUsages(dates);
        UserParamResponse userParamsResponse = client.getUserParams(dates, usages, dataPlanMsg.getOverage());
        UserParams userParams = userParamsResponse.getUserParams();
        UserParamsStd userParamsStd = userParamsResponse.getUserParamsStd();
        System.out.println("Calibrated Params:");
        System.out.println(userParams);
        System.out.println("Standard Deviation:");
        System.out.println(userParamsStd);

        UsagesResponse response = client.getRecommendUsages(2017, 12, userParams, dataPlanMsg);
        System.out.println("Recommended Usages: ");
        System.out.println(response);

        // Calculate Utility
        double utility = client.getUtility(userParams, dataPlanMsg);
        System.out.println("Utility of data plan: ");
        System.out.println(utility);
        // Get DataPlans
        List<DataPlanMsg> dataPlanMsgs = client.getRecommendDataPlans(userParams);
        System.out.println("Recommended Data Plans");
        for(DataPlanMsg dp : dataPlanMsgs){
            System.out.println(dp);
        }

        // Get DataPlans and max, min utilities
        List<DataPlanMsg2> dataPlanMsgs2 = client.getRecommendDataPlans2(userParams, userParamsStd);
        System.out.println("Data Plans and max/min utilities");
        for(DataPlanMsg2 dp : dataPlanMsgs2){
            System.out.println(dp);
        }
        client.shutdown();
    }
}
