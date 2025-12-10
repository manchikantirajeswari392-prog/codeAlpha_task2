import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class StockTradingSimulator {

    // --- Core Classes ---

    static class Stock implements Serializable {
        private String symbol;
        private String name;
        private double price;

        public Stock(String symbol, String name, double price) {
            this.symbol = symbol;
            this.name = name;
            this.price = price;
        }

        public String getSymbol() { return symbol; }
        public String getName() { return name; }
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }

        @Override
        public String toString() {
            return String.format("%-5s %-15s $%.2f", symbol, name, price);
        }
    }

    static class User implements Serializable {
        private String username;
        private double cash;
        private Map<String, Integer> portfolio;
        private Map<String, Double> averagePurchasePrices;

        public User(String username, double initialCash) {
            this.username = username;
            this.cash = initialCash;
            this.portfolio = new HashMap<>();
            this.averagePurchasePrices = new HashMap<>();
        }

        public String getUsername() { return username; }
        public double getCash() { return cash; }
        public Map<String, Integer> getPortfolio() { return portfolio; }
        public void setCash(double cash) { this.cash = cash; }

        public double getPortfolioValue(Map<String, Stock> market) {
            double value = 0;
            for (Map.Entry<String, Integer> entry : portfolio.entrySet()) {
                String symbol = entry.getKey();
                int quantity = entry.getValue();
                if (market.containsKey(symbol)) {
                    value += market.get(symbol).getPrice() * quantity;
                }
            }
            return value;
        }

        public void addStock(String symbol, int quantity, double currentPrice) {
            int currentQuantity = portfolio.getOrDefault(symbol, 0);
            double currentTotalCost = averagePurchasePrices.getOrDefault(symbol, 0.0) * currentQuantity;
            double newTotalCost = currentTotalCost + (currentPrice * quantity);
            int newTotalQuantity = currentQuantity + quantity;

            portfolio.put(symbol, newTotalQuantity);
            averagePurchasePrices.put(symbol, newTotalCost / newTotalQuantity);
        }

        public void removeStock(String symbol, int quantity) {
            int currentQuantity = portfolio.getOrDefault(symbol, 0);
            if (currentQuantity >= quantity) {
                portfolio.put(symbol, currentQuantity - quantity);
                if (portfolio.get(symbol) == 0) {
                    portfolio.remove(symbol);
                    averagePurchasePrices.remove(symbol);
                }
            }
        }
    }

    static class Transaction implements Serializable {
        private String username;
        private String stockSymbol;
        private String type;
        private int quantity;
        private double price;
        private LocalDateTime timestamp;

        public Transaction(String username, String stockSymbol, String type, int quantity, double price) {
            this.username = username;
            this.stockSymbol = stockSymbol;
            this.type = type;
            this.quantity = quantity;
            this.price = price;
            this.timestamp = LocalDateTime.now();
        }
    }

    // --- Main Trading Environment ---

    private static final String PORTFOLIO_FILE = "portfolio.ser";
    private Map<String, Stock> market;
    private User currentUser;
    private Scanner scanner;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Random random = new Random();

    public StockTradingSimulator() {
        this.market = new HashMap<>();
        this.scanner = new Scanner(System.in);
        initializeMarket();
        loadPortfolio();
    }

    private void initializeMarket() {
        market.put("AAPL", new Stock("AAPL", "Apple Inc.", 175.00));
        market.put("GOOG", new Stock("GOOG", "Alphabet Inc.", 145.00));
        market.put("MSFT", new Stock("MSFT", "Microsoft Corp.", 295.00));
        market.put("AMZN", new Stock("AMZN", "Amazon.com Inc.", 135.00));
        market.put("TSLA", new Stock("TSLA", "Tesla, Inc.", 220.00));
    }

    private void loadPortfolio() {
        try (FileInputStream fis = new FileInputStream(PORTFOLIO_FILE);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            currentUser = (User) ois.readObject();
            System.out.println("Portfolio loaded successfully for user: " + currentUser.getUsername());
        } catch (FileNotFoundException e) {
            System.out.println("No existing portfolio found. Creating a new one.");
            currentUser = new User("Trader1", 10000.00);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void savePortfolio() {
        try (FileOutputStream fos = new FileOutputStream(PORTFOLIO_FILE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(currentUser);
            System.out.println("Portfolio saved successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        scheduler.scheduleAtFixedRate(this::updateMarketPrices, 0, 2, TimeUnit.MINUTES);

        boolean exit = false;
        while (!exit) {
            displayMenu();
            int choice = -1;
            try {
                choice = scanner.nextInt();
                scanner.nextLine();
            } catch (java.util.InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine();
                continue;
            }

            switch (choice) {
                case 1:
                    displayMarket();
                    break;
                case 2:
                    displayPortfolio();
                    break;
                case 3:
                    buyStock();
                    break;
                case 4:
                    sellStock();
                    break;
                case 5:
                    exit = true;
                    savePortfolio();
                    System.out.println("Exiting. Thank you for trading!");
                    scheduler.shutdown();
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
            System.out.println("\n----------------------------------\n");
        }
    }

    private void updateMarketPrices() {
        for (Stock stock : market.values()) {
            double change = (random.nextDouble() - 0.5) * stock.getPrice() * 0.05;
            double newPrice = Math.max(0.1, stock.getPrice() + change);
            stock.setPrice(newPrice);
        }
    }

    private void displayMenu() {
        System.out.println("Stock Trading Simulator");
        System.out.println("1. View Market");
        System.out.println("2. View Portfolio");
        System.out.println("3. Buy Stock");
        System.out.println("4. Sell Stock");
        System.out.println("5. Exit");
        System.out.print("Enter your choice: ");
    }

    private void displayMarket() {
        System.out.println("Current Market Prices");
        System.out.println("---------------------");
        System.out.println("Symbol Name            Price");
        for (Stock stock : market.values()) {
            System.out.println(stock);
        }
    }

    private void displayPortfolio() {
        System.out.println("User Portfolio: " + currentUser.getUsername());
        System.out.printf("Cash: $%.2f\n", currentUser.getCash());
        System.out.printf("Total Portfolio Value: $%.2f\n", currentUser.getPortfolioValue(market));
        System.out.println("---------------------");
        System.out.println("Holdings:");
        if (currentUser.getPortfolio().isEmpty()) {
            System.out.println("  No stocks held.");
        } else {
            for (Map.Entry<String, Integer> entry : currentUser.getPortfolio().entrySet()) {
                String symbol = entry.getKey();
                int quantity = entry.getValue();
                double currentPrice = market.get(symbol).getPrice();
                System.out.printf("  %s: %d shares (Current value: $%.2f)\n", symbol, quantity, quantity * currentPrice);
            }
        }
    }

    private void buyStock() {
        System.out.println("Available stocks: " + market.keySet());
        System.out.print("Enter stock symbol to buy: ");
        String symbol = scanner.nextLine().toUpperCase();
        if (!market.containsKey(symbol)) {
            System.out.println("Invalid stock symbol. Please choose from the available list.");
            return;
        }

        System.out.print("Enter quantity to buy: ");
        int quantity = -1;
        try {
            quantity = scanner.nextInt();
            scanner.nextLine();
        } catch (java.util.InputMismatchException e) {
            System.out.println("Invalid input. Please enter a number.");
            scanner.nextLine();
            return;
        }

        if (quantity <= 0) {
            System.out.println("Quantity must be a positive number.");
            return;
        }

        Stock stock = market.get(symbol);
        double cost = stock.getPrice() * quantity;
        if (currentUser.getCash() >= cost) {
            currentUser.setCash(currentUser.getCash() - cost);
            currentUser.addStock(symbol, quantity, stock.getPrice());
            new Transaction(currentUser.getUsername(), symbol, "BUY", quantity, stock.getPrice());
            System.out.printf("Bought %d shares of %s for $%.2f.\n", quantity, symbol, cost);
        } else {
            System.out.println("Insufficient funds.");
        }
    }

    private void sellStock() {
        String userStocks = currentUser.getPortfolio().keySet().stream()
                .filter(market::containsKey)
                .collect(Collectors.joining(", "));

        if (userStocks.isEmpty()) {
            System.out.println("You don't own any tradeable stocks.");
            return;
        }

        System.out.println("Your current holdings: " + userStocks);
        System.out.print("Enter stock symbol to sell: ");
        String symbol = scanner.nextLine().toUpperCase();
        if (!currentUser.getPortfolio().containsKey(symbol)) {
            System.out.println("You do not own this stock.");
            return;
        }

        System.out.print("Enter quantity to sell: ");
        int quantity = -1;
        try {
            quantity = scanner.nextInt();
            scanner.nextLine();
        } catch (java.util.InputMismatchException e) {
            System.out.println("Invalid input. Please enter a number.");
            scanner.nextLine();
            return;
        }

        if (quantity <= 0) {
            System.out.println("Quantity must be a positive number.");
            return;
        }

        int ownedQuantity = currentUser.getPortfolio().get(symbol);
        if (quantity > ownedQuantity) {
            System.out.println("You can't sell more shares than you own.");
            return;
        }

        double averageBuyPrice = currentUser.averagePurchasePrices.get(symbol);
        double currentSellPrice = market.get(symbol).getPrice();
        double totalProfit = (currentSellPrice - averageBuyPrice) * quantity;
        double proceeds = currentSellPrice * quantity;

        currentUser.setCash(currentUser.getCash() + proceeds);
        currentUser.removeStock(symbol, quantity);
        new Transaction(currentUser.getUsername(), symbol, "SELL", quantity, currentSellPrice);

        System.out.printf("Sold %d shares of %s for $%.2f.\n", quantity, symbol, proceeds);

        if (totalProfit > 0) {
            System.out.printf("🎉 Profit: $%.2f\n", totalProfit);
        } else if (totalProfit < 0) {
            System.out.printf("😔 Loss: $%.2f\n", totalProfit);
        } else {
            System.out.println("No profit or loss on this sale.");
        }
    }

    public static void main(String[] args) {
        StockTradingSimulator simulator = new StockTradingSimulator();
        simulator.run();
    }
}