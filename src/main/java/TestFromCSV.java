import com.opencsv.CSVReader;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class TestFromCSV {
    public static class UserTest{
        public String userId;
        public double[] usages;
        public Date[] dates;
        public String host = "localhost";

        public UserTest(String userId, double[] usages, Date[] dates) {
            this.userId = userId;
            this.usages = usages;
            this.dates = dates;
        }

        public void test() throws InterruptedException {
            DataPlanClient client = new DataPlanClient(host, 50051);

//            System.out.println("Test User : " + userId);
//            //Test Hello World
//            client.helloWorld();

            //A pseudo data plan and get recommended usages
            DataPlanMsg dataPlanMsg = DataPlanMsg.newBuilder().setQuota(1e10).setOverage(0.01).setPrice(0).build();
            System.out.println("Pseudo data plan created");
            System.out.println(dataPlanMsg);

            UserParamResponse userParamsResponse = client.getUserParams(dates, usages, dataPlanMsg.getOverage());
            UserParams userParams = userParamsResponse.getUserParams();
            UserParamsStd userParamsStd = userParamsResponse.getUserParamsStd();
            System.out.println("Calibrated Params:");
            System.out.println(userParams);
            System.out.println("Standard Deviation:");
            System.out.println(userParamsStd);

//            UsagesResponse response = client.getRecommendUsages(2017, 12, userParams, dataPlanMsg);
//            System.out.println("Recommended Usages: ");
//            System.out.println(response);

            // Calculate Utility
            double utility = client.getUtility(userParams, dataPlanMsg);
            System.out.println("Utility of data plan: ");
            System.out.println(utility);
//            // Get DataPlans
//            List<DataPlanMsg> dataPlanMsgs = client.getRecommendDataPlans(userParams);
//            System.out.println("Recommended Data Plans");
//            for(DataPlanMsg dp : dataPlanMsgs){
//                System.out.println(dp);
//            }


            // Get DataPlans and max, min utilities
            List<DataPlanMsg2> dataPlanMsgs2 = client.getRecommendDataPlans2(userParams, userParamsStd);
            System.out.println("Data Plans and max/min utilities");
            for(DataPlanMsg2 dp : dataPlanMsgs2){
                System.out.println(dp);
            }
            client.shutdown();
        }

        @Override
        public String toString() {
            return "UserTest{" +
                    "userId='" + userId + '\'' +
                    ", usages=" + Arrays.toString(usages) +
                    ", dates=" + Arrays.toString(dates) +
                    '}';
        }
    }


    public static Date[] generateDates(Date start, int len){
        Date[] dates = new Date[len];
        for (int i = 0; i < len; i++) {
            Date date = new Date(start.getTime() + (1000 * 60 * 60 * 24 * i));
            dates[i] = date;
        }
        return dates;
    }

    public static UserTest[] generateUserTestsFromCSV(String fname){
        CSVReader reader;
        try {
            reader = new CSVReader(new FileReader(fname), ',');
            List<String[]> rows = reader.readAll();

            UserTest[] userTests = new UserTest[rows.size()];
            int count = 0;
            for(String[] row : rows){
                String userId = row[0];
                Date[] dates = generateDates(new Date(), row.length - 1);
                double[] usages = new double[row.length - 1];
                for(int i = 1; i < row.length; i++)
                    usages[i - 1] = Double.valueOf(row[i]) / 1000 / 1000;
                userTests[count++] = new UserTest(userId, usages, dates);
            }
            return userTests;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static void main(String[] args) throws Exception {
        UserTest[] userTests = generateUserTestsFromCSV("daily.csv");
        for(int i = 0; i < 3 && i < userTests.length; i++){
            System.out.println(userTests[i]);
            userTests[i].test();
        }
    }
}
