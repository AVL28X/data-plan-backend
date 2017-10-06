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

    @Override
    public String toString(){
        return "Quota: " + this.quota + "\n"
                + "Overage: " + this.overage + "\n"
                + "Price: " + this.price + "\n";
    }

}
