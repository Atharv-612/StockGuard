package com.stockguard.service;

import com.stockguard.dto.AuthDto;
import com.stockguard.entity.Category;
import com.stockguard.entity.NotificationPreference;
import com.stockguard.entity.Shop;
import com.stockguard.entity.User;
import com.stockguard.exception.BusinessException;
import com.stockguard.exception.ResourceNotFoundException;
import com.stockguard.repository.CategoryRepository;
import com.stockguard.repository.NotificationPreferenceRepository;
import com.stockguard.repository.ShopRepository;
import com.stockguard.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final CategoryRepository categoryRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       ShopRepository shopRepository,
                       CategoryRepository categoryRepository,
                       NotificationPreferenceRepository notificationPreferenceRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
        this.categoryRepository = categoryRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.auditService = auditService;
    }

    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("An account with email " + request.getEmail() + " already exists");
        }

        // 1. Create Shop
        Shop shop = new Shop();
        shop.setName(request.getShopName());
        shop.setOwnerName(request.getOwnerName());
        shop.setPhone(request.getPhone());
        shop.setEmail(request.getEmail());
        shop.setCurrency("INR");
        shop.setMinStockDefault(10);
        shop.setExpiryWarningDays(7);
        Shop savedShop = shopRepository.save(shop);

        // 2. Create User as SHOP_OWNER
        User user = new User();
        user.setShop(savedShop);
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getOwnerName());
        user.setPhone(request.getPhone());
        user.setRole("SHOP_OWNER");
        user.setActive(true);
        User savedUser = userRepository.save(user);

        // 3. Create Default Categories for grocery shop
        seedDefaultCategories(savedShop);

        // 4. Create Default Notification Preferences
        NotificationPreference pref = new NotificationPreference();
        pref.setShop(savedShop);
        pref.setWhatsappPhone(request.getPhone());
        pref.setEnableLowStock(true);
        pref.setEnableExpiry(true);
        pref.setEnableDailySales(true);
        pref.setExpiryWarningDays(7);
        notificationPreferenceRepository.save(pref);

        // 5. Generate JWT Token
        String token = jwtService.generateToken(savedUser.getEmail(), savedShop.getId(), savedUser.getRole());

        auditService.logEvent(savedShop.getId(), savedUser.getId(), savedUser.getEmail(),
                "REGISTER_SUCCESS", "SHOP", savedShop.getId().toString(),
                "New shop registered: " + savedShop.getName() + " with owner: " + savedUser.getEmail(), null, "SUCCESS");

        return new AuthDto.AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getRole(),
                savedShop.getId(),
                savedShop.getName(),
                savedShop.getCurrency()
        );
    }

    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        Shop shop = user.getShop();
        String token = jwtService.generateToken(user.getEmail(), shop.getId(), user.getRole());

        auditService.logEvent(shop.getId(), user.getId(), user.getEmail(),
                "LOGIN_SUCCESS", "AUTH", user.getId().toString(),
                "User logged in successfully (Role: " + user.getRole() + ")", null, "SUCCESS");

        return new AuthDto.AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                shop.getId(),
                shop.getName(),
                shop.getCurrency()
        );
    }

    @Transactional
    public AuthDto.UserResponse createUserForShop(AuthDto.CreateUserRequest request) {
        User currentUser = getCurrentAuthenticatedUser();
        Shop shop = currentUser.getShop();

        // Enforce role creation constraints: only MANAGER or STAFF
        String targetRole = request.getRole() != null ? request.getRole().trim().toUpperCase() : "STAFF";
        if (!targetRole.equals("MANAGER") && !targetRole.equals("STAFF")) {
            throw new BusinessException("Assigned role must be either 'MANAGER' or 'STAFF'");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("User with email " + request.getEmail() + " already exists");
        }

        User newUser = new User();
        newUser.setShop(shop);
        newUser.setEmail(request.getEmail());
        newUser.setFullName(request.getFullName());
        newUser.setPhone(request.getPhone());
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(targetRole);
        newUser.setActive(true);

        User saved = userRepository.save(newUser);

        auditService.logEvent(shop.getId(), currentUser.getId(), currentUser.getEmail(),
                "USER_CREATED", "USER", saved.getId().toString(),
                "Created team member " + saved.getEmail() + " with role " + targetRole, null, "SUCCESS");

        return new AuthDto.UserResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getFullName(),
                saved.getPhone(),
                saved.getRole(),
                saved.getActive()
        );
    }

    public List<AuthDto.UserResponse> getUsersForCurrentShop() {
        Shop shop = getCurrentUserShop();
        return userRepository.findByShopIdOrderByCreatedAtAsc(shop.getId()).stream()
                .map(u -> new AuthDto.UserResponse(
                        u.getId(),
                        u.getEmail(),
                        u.getFullName(),
                        u.getPhone(),
                        u.getRole(),
                        u.getActive()
                ))
                .collect(java.util.stream.Collectors.toList());
    }

    public User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException("User is not authenticated");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + email));
    }

    public Shop getCurrentUserShop() {
        return getCurrentAuthenticatedUser().getShop();
    }

    private void seedDefaultCategories(Shop shop) {
        List<Category> defaultCategories = List.of(
                new Category(shop, "Dairy & Eggs", "Milk, Butter, Cheese, Paneer, Curd, Eggs", "fa-cow"),
                new Category(shop, "Bakery & Bread", "Fresh bread, Buns, Toast, Cookies", "fa-bread-slice"),
                new Category(shop, "Grains & Staples", "Rice, Wheat Flour, Dal, Pulses, Sugar, Salt", "fa-wheat-awn"),
                new Category(shop, "Beverages", "Tea, Coffee, Soft Drinks, Juices, Bottled Water", "fa-mug-hot"),
                new Category(shop, "Snacks & Confectionery", "Biscuits, Chips, Chocolates, Namkeen", "fa-cookie"),
                new Category(shop, "Cooking Oils & Spices", "Mustard Oil, Sunflower Oil, Ghee, Spices", "fa-bottle-droplet"),
                new Category(shop, "Household & Cleaning", "Detergent, Soaps, Dishwashers, Paper Towels", "fa-pump-soap")
        );
        categoryRepository.saveAll(defaultCategories);
    }
}
