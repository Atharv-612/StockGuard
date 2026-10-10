package com.stockguard.service;

import com.stockguard.entity.*;
import com.stockguard.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DataSeederService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeederService.class);

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductBatchRepository batchRepository;
    private final SaleRepository saleRepository;
    private final StockMovementRepository stockMovementRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeederService(ShopRepository shopRepository,
                             UserRepository userRepository,
                             CategoryRepository categoryRepository,
                             ProductRepository productRepository,
                             ProductBatchRepository batchRepository,
                             SaleRepository saleRepository,
                             StockMovementRepository stockMovementRepository,
                             NotificationPreferenceRepository preferenceRepository,
                             PasswordEncoder passwordEncoder) {
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.saleRepository = saleRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.preferenceRepository = preferenceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("StockGuard database already contains user data. Skipping default seed.");
            return;
        }

        log.info("Seeding realistic sample grocery data for StockGuard demo store...");

        // 1. Create Demo Shop
        Shop shop = new Shop();
        shop.setName("StockGuard Demo Grocery & Dairy");
        shop.setOwnerName("Atharv Raut");
        shop.setPhone("+91 9876543210");
        shop.setEmail("demo@stockguard.com");
        shop.setAddress("Shop #4, Green Avenue Market, Pune, India");
        shop.setCurrency("INR");
        shop.setMinStockDefault(15);
        shop.setExpiryWarningDays(7);
        Shop savedShop = shopRepository.save(shop);

        // 2. Create Demo User
        User user = new User();
        user.setShop(savedShop);
        user.setEmail("demo@stockguard.com");
        user.setPasswordHash(passwordEncoder.encode("demo123"));
        user.setFullName("Atharv Raut");
        user.setPhone("+91 9876543210");
        user.setRole("OWNER");
        user.setActive(true);
        User savedUser = userRepository.save(user);

        // 3. Create Notification Preferences
        NotificationPreference pref = new NotificationPreference();
        pref.setShop(savedShop);
        pref.setWhatsappPhone("+919876543210");
        pref.setEnableLowStock(true);
        pref.setEnableExpiry(true);
        pref.setEnableDailySales(true);
        pref.setExpiryWarningDays(7);
        preferenceRepository.save(pref);

        // 4. Categories
        Category catDairy = categoryRepository.save(new Category(savedShop, "Dairy & Eggs", "Milk, Curd, Butter, Cheese, Eggs", "fa-cow"));
        Category catBakery = categoryRepository.save(new Category(savedShop, "Bakery & Bread", "Fresh bread, Buns, Rusk", "fa-bread-slice"));
        Category catStaples = categoryRepository.save(new Category(savedShop, "Grains & Staples", "Rice, Wheat Flour, Sugar, Dal", "fa-wheat-awn"));
        Category catBeverages = categoryRepository.save(new Category(savedShop, "Beverages", "Tea, Coffee, Soft Drinks, Juices", "fa-mug-hot"));
        Category catSnacks = categoryRepository.save(new Category(savedShop, "Packaged Snacks", "Biscuits, Chips, Namkeen", "fa-cookie"));
        Category catOils = categoryRepository.save(new Category(savedShop, "Cooking Oils", "Sunflower Oil, Mustard Oil, Ghee", "fa-bottle-droplet"));
        Category catClean = categoryRepository.save(new Category(savedShop, "Household & Detergents", "Detergent powder, Dishwash bars", "fa-pump-soap"));

        LocalDate today = LocalDate.now();

        // 5. Seed 16+ Realistic Grocery Products with Batches
        seedItem(savedShop, catDairy, "Amul Taaza Homogenised Toned Milk 1L", "MILK-001", "890126201001", "Nutritious toned milk",
                new BigDecimal("54.00"), new BigDecimal("60.00"), 10, "pack",
                List.of(
                        new BatchSeed("BATCH-MLK-01", 30, 24, today.minusDays(5), today.plusDays(4)), // Expiring soon!
                        new BatchSeed("BATCH-MLK-02", 40, 40, today.minusDays(1), today.plusDays(10)) // Safe
                ));

        seedItem(savedShop, catDairy, "Nestle A+ Nourish Dahi / Curd 400g", "CURD-001", "890105885231", "Fresh natural curd",
                new BigDecimal("35.00"), new BigDecimal("42.00"), 8, "cup",
                List.of(
                        new BatchSeed("BATCH-CRD-01", 15, 6, today.minusDays(12), today.minusDays(2)), // Expired!
                        new BatchSeed("BATCH-CRD-02", 20, 18, today.minusDays(2), today.plusDays(5)) // Expiring soon
                ));

        seedItem(savedShop, catDairy, "Amul Pasteurized Butter 500g", "BTR-001", "890126202002", "Pure creamy butter",
                new BigDecimal("245.00"), new BigDecimal("275.00"), 6, "pack",
                List.of(
                        new BatchSeed("BATCH-BTR-01", 20, 16, today.minusDays(20), today.plusDays(90)) // Safe
                ));

        seedItem(savedShop, catDairy, "Britannia Processed Cheese Slices 200g", "CHS-001", "890106312456", "Rich melting cheese slices",
                new BigDecimal("130.00"), new BigDecimal("150.00"), 8, "pack",
                List.of(
                        new BatchSeed("BATCH-CHS-01", 15, 12, today.minusDays(30), today.plusDays(60)) // Safe
                ));

        seedItem(savedShop, catBakery, "Harvest Gold White Bread 400g", "BRD-001", "890204300101", "Daily fresh soft bread",
                new BigDecimal("32.00"), new BigDecimal("40.00"), 10, "loaf",
                List.of(
                        new BatchSeed("BATCH-BRD-01", 20, 8, today.minusDays(4), today.plusDays(1)), // Expiring tomorrow!
                        new BatchSeed("BATCH-BRD-02", 25, 25, today.minusDays(1), today.plusDays(4)) // Expiring soon
                ));

        seedItem(savedShop, catDairy, "Farm Fresh White Eggs (Pack of 6)", "EGG-006", "890800100606", "Protein rich farm fresh eggs",
                new BigDecimal("42.00"), new BigDecimal("55.00"), 12, "tray",
                List.of(
                        new BatchSeed("BATCH-EGG-01", 30, 22, today.minusDays(6), today.plusDays(8)) // Safe
                ));

        seedItem(savedShop, catSnacks, "Parle-G Gold Biscuits 1kg", "BIS-001", "890171910203", "India's favorite glucose biscuits",
                new BigDecimal("95.00"), new BigDecimal("110.00"), 15, "pack",
                List.of(
                        new BatchSeed("BATCH-PAR-01", 40, 35, today.minusDays(45), today.plusDays(180)) // Safe
                ));

        seedItem(savedShop, catStaples, "India Gate Basmati Rice Feast Rozzana 5kg", "RIC-001", "890153700501", "Aromatic long grain basmati rice",
                new BigDecimal("380.00"), new BigDecimal("450.00"), 5, "bag",
                List.of(
                        new BatchSeed("BATCH-RIC-01", 15, 11, today.minusDays(60), today.plusDays(300)) // Safe
                ));

        seedItem(savedShop, catStaples, "Aashirvaad Shudh Chakki Atta 10kg", "ATT-001", "890103001202", "100% whole wheat flour",
                new BigDecimal("410.00"), new BigDecimal("465.00"), 5, "bag",
                List.of(
                        new BatchSeed("BATCH-ATT-01", 12, 4, today.minusDays(40), today.plusDays(120)) // Low stock!
                ));

        seedItem(savedShop, catOils, "Fortune Sunlite Refined Sunflower Oil 1L", "OIL-001", "890600728101", "Light and healthy cooking oil",
                new BigDecimal("120.00"), new BigDecimal("145.00"), 10, "pouch",
                List.of(
                        new BatchSeed("BATCH-OIL-01", 25, 20, today.minusDays(30), today.plusDays(240)) // Safe
                ));

        seedItem(savedShop, catStaples, "Madhur Pure & Hygienic Sugar 1kg", "SUG-001", "890601441010", "Sulphur free refined crystalline sugar",
                new BigDecimal("45.00"), new BigDecimal("52.00"), 15, "pack",
                List.of(
                        new BatchSeed("BATCH-SUG-01", 50, 42, today.minusDays(20), today.plusDays(365)) // Safe
                ));

        seedItem(savedShop, catBeverages, "Tata Tea Gold 500g", "TEA-001", "890105201101", "Rich taste gently rolled tea leaves",
                new BigDecimal("260.00"), new BigDecimal("310.00"), 8, "pack",
                List.of(
                        new BatchSeed("BATCH-TEA-01", 20, 15, today.minusDays(50), today.plusDays(300)) // Safe
                ));

        seedItem(savedShop, catBeverages, "Nescafe Classic Coffee Jar 100g", "COF-001", "890105800101", "100% pure instant coffee",
                new BigDecimal("280.00"), new BigDecimal("330.00"), 6, "jar",
                List.of(
                        new BatchSeed("BATCH-COF-01", 14, 3, today.minusDays(60), today.plusDays(250)) // Low stock!
                ));

        seedItem(savedShop, catSnacks, "Lay's Classic Salted Potato Chips 50g", "SNK-001", "890149110101", "Crispy golden fried potato chips",
                new BigDecimal("16.00"), new BigDecimal("20.00"), 20, "pack",
                List.of(
                        new BatchSeed("BATCH-LAY-01", 50, 38, today.minusDays(25), today.plusDays(70)) // Safe
                ));

        seedItem(savedShop, catBeverages, "Coca-Cola Original Taste 750ml", "BEV-001", "890176401201", "Refreshing carbonated beverage",
                new BigDecimal("32.00"), new BigDecimal("40.00"), 15, "bottle",
                List.of(
                        new BatchSeed("BATCH-COK-01", 30, 24, today.minusDays(30), today.plusDays(90)) // Safe
                ));

        seedItem(savedShop, catClean, "Surf Excel Easy Wash Detergent Powder 1kg", "CLN-001", "890103004040", "Superior stain removal detergent",
                new BigDecimal("125.00"), new BigDecimal("145.00"), 8, "pack",
                List.of(
                        new BatchSeed("BATCH-SRF-01", 25, 21, today.minusDays(40), today.plusDays(500)) // Safe
                ));

        // 6. Seed Sample Recent Sales for realistic charts and revenue
        seedSampleSales(savedShop, savedUser);

        log.info("Realistic grocery sample data successfully seeded!");
    }

    private void seedItem(Shop shop, Category cat, String name, String sku, String barcode, String desc,
                          BigDecimal cost, BigDecimal sell, int minStock, String unit,
                          List<BatchSeed> batches) {
        Product prod = new Product();
        prod.setShop(shop);
        prod.setCategory(cat);
        prod.setName(name);
        prod.setSku(sku);
        prod.setBarcode(barcode);
        prod.setDescription(desc);
        prod.setCostPrice(cost);
        prod.setSellingPrice(sell);
        prod.setMinStockThreshold(minStock);
        prod.setUnit(unit);

        int totalStock = batches.stream().mapToInt(b -> b.currentQty).sum();
        prod.setCurrentStock(totalStock);
        Product savedProduct = productRepository.save(prod);

        for (BatchSeed b : batches) {
            ProductBatch pb = new ProductBatch();
            pb.setProduct(savedProduct);
            pb.setBatchNumber(b.batchNo);
            pb.setInitialQuantity(b.initialQty);
            pb.setCurrentQuantity(b.currentQty);
            pb.setCostPrice(cost);
            pb.setSellingPrice(sell);
            pb.setMfgDate(b.mfg);
            pb.setExpiryDate(b.exp);
            pb.setStatus(b.exp.isBefore(LocalDate.now()) ? "EXPIRED" : "ACTIVE");
            ProductBatch savedBatch = batchRepository.save(pb);

            // Record initial movement
            StockMovement movement = new StockMovement();
            movement.setShop(shop);
            movement.setProduct(savedProduct);
            movement.setBatch(savedBatch);
            movement.setMovementType("PURCHASE");
            movement.setQuantityChanged(b.initialQty);
            movement.setQuantityAfter(b.initialQty);
            movement.setReference("PO-INIT-" + b.batchNo);
            movement.setNotes("Initial stock intake");
            stockMovementRepository.save(movement);
        }
    }

    private void seedSampleSales(Shop shop, User user) {
        List<Product> products = productRepository.findByShopIdOrderByCreatedAtDesc(shop.getId());
        if (products.isEmpty()) return;

        LocalDate today = LocalDate.now();

        // Create historical sales for the last 5 days
        for (int dayOffset = 5; dayOffset >= 0; dayOffset--) {
            LocalDateTime saleTime = today.minusDays(dayOffset).atTime(11 + dayOffset, 30);

            Sale sale = new Sale();
            sale.setShop(shop);
            sale.setUser(user);
            sale.setInvoiceNumber("INV-2026040" + (10 - dayOffset));
            sale.setCustomerName(dayOffset == 0 ? "Pooja Sharma" : "Rajesh Kumar");
            sale.setCustomerPhone("+91 982341234" + dayOffset);
            sale.setPaymentMethod(dayOffset % 2 == 0 ? "UPI" : "CASH");
            sale.setSaleDate(saleTime);

            Product p1 = products.get(dayOffset % products.size());
            Product p2 = products.get((dayOffset + 2) % products.size());

            BigDecimal l1 = p1.getSellingPrice().multiply(BigDecimal.valueOf(2));
            BigDecimal l2 = p2.getSellingPrice().multiply(BigDecimal.valueOf(1));
            BigDecimal sub = l1.add(l2);

            sale.setSubtotal(sub);
            sale.setDiscount(BigDecimal.ZERO);
            sale.setTax(BigDecimal.ZERO);
            sale.setTotalAmount(sub);

            SaleItem item1 = new SaleItem();
            item1.setSale(sale);
            item1.setProduct(p1);
            item1.setProductName(p1.getName());
            item1.setQuantity(2);
            item1.setUnitPrice(p1.getSellingPrice());
            item1.setTotalPrice(l1);

            SaleItem item2 = new SaleItem();
            item2.setSale(sale);
            item2.setProduct(p2);
            item2.setProductName(p2.getName());
            item2.setQuantity(1);
            item2.setUnitPrice(p2.getSellingPrice());
            item2.setTotalPrice(l2);

            sale.setItems(List.of(item1, item2));
            saleRepository.save(sale);
        }
    }

    private static class BatchSeed {
        String batchNo;
        int initialQty;
        int currentQty;
        LocalDate mfg;
        LocalDate exp;

        BatchSeed(String batchNo, int initialQty, int currentQty, LocalDate mfg, LocalDate exp) {
            this.batchNo = batchNo;
            this.initialQty = initialQty;
            this.currentQty = currentQty;
            this.mfg = mfg;
            this.exp = exp;
        }
    }
}
