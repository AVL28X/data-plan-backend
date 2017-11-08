import com.opencsv.CSVReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class DataPlan {

    public int id;
    public String name;
    public String description;

    public double quota;
    public double overage;
    public double price;

    public DataPlan(double quota, double overage, double price){
        this.quota = quota; // A in paper
        this.overage = overage; // pi in paper
        this.price = price; // P in paper
    }

    public DataPlan(String name, String description, double quota, double overage, double price) {
        this.name = name;
        this.description = description;
        this.quota = quota;
        this.overage = overage;
        this.price = price;
    }

    @Override
    public String toString(){
        return "Name: " + this.name + "\n"
                + "Description: " + this.description + "\n"
                + "Quota: " + this.quota + "\n"
                + "Overage: " + this.overage + "\n"
                + "Price: " + this.price + "\n";
    }


    public static DataPlan[] getDataPlansFromCSV(String fname){
        CSVReader reader = null;
        try {
            reader = new CSVReader(new FileReader(fname), ',');
            List<String[]> rows = reader.readAll();
            DataPlan[] dps = new DataPlan[rows.size() - 1];
            for(int i = 1; i < rows.size(); i++){
                String name = rows.get(i)[0];
                String description = rows.get(i)[1];
                double quota = Double.MAX_VALUE;
                if(!rows.get(i)[2].equals("unlimited"))
                    quota = Double.parseDouble(rows.get(i)[2]);
                double overage = Double.parseDouble(rows.get(i)[3]);
                double price = Double.parseDouble(rows.get(i)[4]);
                DataPlan dp = new DataPlan(name, description, quota, overage, price);
                dps[i - 1] = dp;
            }
            return dps;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) throws IOException {

    }

}
