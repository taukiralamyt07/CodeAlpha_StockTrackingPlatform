import java.io.*;
import java.util.*;

public class Main {
    private static final Scanner sc = new Scanner(System.in);
    private static final String DATA_FILE = "portfolio.dat";

    public static void main(String[] args) {
        StockMarket market = new StockMarket();
        Portfolio portfolio = new Portfolio();
        loadPortfolio(portfolio);

        boolean running = true;
        while (running) {
            System.out.println("\n===== CODEALPHA STOCK TRADING PLATFORM =====");
            System.out.println("1. Display Market Data");
            System.out.println("2. Buy Stock");
            System.out.println("3. Sell Stock");
            System.out.println("4. View Portfolio");
            System.out.println("5. Add/Update Market Stock");
            System.out.println("6. Save & Exit");
            System.out.print("Choose: ");
            String choice = sc.nextLine();

            try {
                switch (choice) {
                    case "1" -> market.displayStocks();
                    case "2" -> buy(market, portfolio);
                    case "3" -> sell(market, portfolio);
                    case "4" -> portfolio.display(market);
                    case "5" -> addStock(market);
                    case "6" -> { savePortfolio(portfolio); running = false; }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (Exception e) {
                System.out.println("Operation failed: " + e.getMessage());
            }
        }
        sc.close();
    }

    private static void buy(StockMarket market, Portfolio p) {
        System.out.print("Symbol: "); String symbol = sc.nextLine().toUpperCase();
        Stock stock = market.get(symbol);
        if (stock == null) { System.out.println("Stock not found."); return; }
        System.out.print("Quantity: "); int qty = Integer.parseInt(sc.nextLine());
        if (qty <= 0) throw new IllegalArgumentException("Quantity must be positive.");
        double cost = qty * stock.getPrice();
        if (cost > p.getCash()) throw new IllegalArgumentException("Insufficient cash.");
        p.buy(symbol, qty, stock.getPrice());
        System.out.printf("Bought %d %s for ₹%.2f%n", qty, symbol, cost);
    }

    private static void sell(StockMarket market, Portfolio p) {
        System.out.print("Symbol: "); String symbol = sc.nextLine().toUpperCase();
        Stock stock = market.get(symbol);
        if (stock == null) { System.out.println("Stock not found."); return; }
        System.out.print("Quantity: "); int qty = Integer.parseInt(sc.nextLine());
        if (qty <= 0) throw new IllegalArgumentException("Quantity must be positive.");
        p.sell(symbol, qty, stock.getPrice());
        System.out.printf("Sold %d %s for ₹%.2f%n", qty, symbol, qty * stock.getPrice());
    }

    private static void addStock(StockMarket market) {
        System.out.print("Symbol: "); String symbol = sc.nextLine().toUpperCase();
        System.out.print("Company name: "); String name = sc.nextLine();
        System.out.print("Price: "); double price = Double.parseDouble(sc.nextLine());
        if (price <= 0) throw new IllegalArgumentException("Price must be positive.");
        market.add(new Stock(symbol, name, price));
        System.out.println("Stock added/updated.");
    }

    private static void savePortfolio(Portfolio p) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            out.writeObject(p);
            System.out.println("Portfolio saved.");
        } catch (IOException e) { System.out.println("Could not save portfolio."); }
    }

    private static void loadPortfolio(Portfolio p) {
        File f = new File(DATA_FILE);
        if (!f.exists()) return;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
            Portfolio saved = (Portfolio) in.readObject();
            p.copyFrom(saved);
            System.out.println("Saved portfolio loaded.");
        } catch (Exception e) { System.out.println("Starting with a new portfolio."); }
    }
}

class Stock implements Serializable {
    private final String symbol, companyName;
    private double price;
    public Stock(String symbol, String companyName, double price) { this.symbol = symbol; this.companyName = companyName; this.price = price; }
    public String getSymbol() { return symbol; }
    public String getCompanyName() { return companyName; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}

class StockMarket {
    private final Map<String, Stock> stocks = new LinkedHashMap<>();
    public StockMarket() {
        add(new Stock("TCS", "Tata Consultancy Services", 3800));
        add(new Stock("INFY", "Infosys", 1600));
        add(new Stock("RELIANCE", "Reliance Industries", 2900));
        add(new Stock("HDFCBANK", "HDFC Bank", 1750));
        add(new Stock("ITC", "ITC Limited", 500));
    }
    public void add(Stock s) { stocks.put(s.getSymbol(), s); }
    public Stock get(String symbol) { return stocks.get(symbol); }
    public void displayStocks() {
        System.out.println("\n--- MARKET DATA ---");
        System.out.printf("%-12s %-32s %12s%n", "SYMBOL", "COMPANY", "PRICE");
        for (Stock s : stocks.values()) System.out.printf("%-12s %-32s ₹%10.2f%n", s.getSymbol(), s.getCompanyName(), s.getPrice());
    }
}

class Holding implements Serializable {
    int quantity; double averageBuyPrice;
    Holding(int quantity, double averageBuyPrice) { this.quantity = quantity; this.averageBuyPrice = averageBuyPrice; }
}

class Portfolio implements Serializable {
    private double cash = 100000;
    private final Map<String, Holding> holdings = new HashMap<>();
    private final List<String> transactions = new ArrayList<>();

    public double getCash() { return cash; }
    public void buy(String symbol, int qty, double price) {
        double cost = qty * price;
        Holding h = holdings.get(symbol);
        if (h == null) holdings.put(symbol, new Holding(qty, price));
        else {
            h.averageBuyPrice = ((h.quantity * h.averageBuyPrice) + cost) / (h.quantity + qty);
            h.quantity += qty;
        }
        cash -= cost;
        transactions.add("BUY " + qty + " " + symbol + " @ ₹" + price);
    }
    public void sell(String symbol, int qty, double price) {
        Holding h = holdings.get(symbol);
        if (h == null || h.quantity < qty) throw new IllegalArgumentException("Not enough shares owned.");
        h.quantity -= qty; cash += qty * price;
        transactions.add("SELL " + qty + " " + symbol + " @ ₹" + price);
        if (h.quantity == 0) holdings.remove(symbol);
    }
    public void display(StockMarket market) {
        double marketValue = 0, invested = 0;
        System.out.println("\n--- PORTFOLIO ---");
        System.out.printf("Cash: ₹%.2f%n", cash);
        if (holdings.isEmpty()) System.out.println("No holdings.");
        else {
            System.out.printf("%-12s %8s %15s %15s %15s%n", "SYMBOL", "QTY", "AVG BUY", "CURRENT", "P/L");
            for (Map.Entry<String,Holding> e : holdings.entrySet()) {
                Stock s = market.get(e.getKey()); Holding h = e.getValue();
                double current = s == null ? h.averageBuyPrice : s.getPrice();
                double pnl = (current - h.averageBuyPrice) * h.quantity;
                invested += h.averageBuyPrice * h.quantity; marketValue += current * h.quantity;
                System.out.printf("%-12s %8d ₹%13.2f ₹%13.2f ₹%13.2f%n", e.getKey(), h.quantity, h.averageBuyPrice, current, pnl);
            }
        }
        System.out.printf("Invested Value: ₹%.2f%n", invested);
        System.out.printf("Current Stock Value: ₹%.2f%n", marketValue);
        System.out.printf("Total Portfolio Value: ₹%.2f%n", cash + marketValue);
        System.out.println("Transactions: " + transactions.size());
    }
    public void copyFrom(Portfolio other) { this.cash = other.cash; this.holdings.clear(); this.holdings.putAll(other.holdings); this.transactions.clear(); this.transactions.addAll(other.transactions); }
}
