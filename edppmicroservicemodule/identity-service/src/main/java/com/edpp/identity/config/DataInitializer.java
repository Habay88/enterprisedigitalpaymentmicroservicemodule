package com.edpp.identity.config;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.edpp.identity.enums.CustomerStatus;
import com.edpp.identity.enums.CustomerType;
import com.edpp.identity.enums.RiskRating;
import com.edpp.identity.enums.TenantStatus;
import com.edpp.identity.enums.TenantType;
import com.edpp.identity.model.Address;
import com.edpp.identity.model.BvnVerification;
import com.edpp.identity.model.Customer;
import com.edpp.identity.model.KycDetails;
import com.edpp.identity.model.NinVerification;
import com.edpp.identity.model.Tenant;
import com.edpp.identity.repository.CustomerRepository;
import com.edpp.identity.repository.TenantRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
public class DataInitializer {

    private final TenantRepository tenantRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            log.info("==========================================");
            log.info("Initializing test data for 5 tenants...");
            log.info("==========================================");

            // Clear existing data
            customerRepository.deleteAll();
            tenantRepository.deleteAll();

            // Create 5 different tenants
            List<Tenant> tenants = createTenants();
            tenantRepository.saveAll(tenants);
            log.info("✅ Created {} tenants", tenants.size());

            // Create customers for each tenant
            for (Tenant tenant : tenants) {
                List<Customer> customers = createCustomersForTenant(tenant);
                customerRepository.saveAll(customers);
                log.info("✅ Created {} customers for tenant: {}", customers.size(), tenant.getTenantId());
            }

            log.info("==========================================");
            log.info("Data initialization complete!");
            log.info("Total Tenants: {}", tenantRepository.count());
            log.info("Total Customers: {}", customerRepository.count());
            log.info("==========================================");
            
            // Print summary for easy testing
            printTestSummary(tenants);
        };
    }

    private List<Tenant> createTenants() {
        return Arrays.asList(
            // Tenant 1: First Bank (Nigerian Bank)
            Tenant.builder()
                    .tenantId("FIRST_BANK")
                    .name("First Bank of Nigeria")
                    .schemaName("first_bank")
                    .domain("firstbank.localhost")
                    .status(TenantStatus.ACTIVE)
                    .tenantType(TenantType.BANK)
                    .contactEmail("admin@firstbank.com")
                    .contactPhone("+2348012345000")
                    .description("Leading Nigerian bank with full service banking")
                    .configuration(TenantConfiguration.builder()
                            .enableBvnValidation(true)
                            .enableNinValidation(true)
                            .enforceKyc(true)
                            .maxDailyTransactionLimit(10000000) // 10 million NGN
                            .maxMonthlyTransactionLimit(50000000) // 50 million NGN
                            .supportedCurrencies("NGN,USD,GBP,EUR")
                            .defaultLanguage("en")
                            .timezone("Africa/Lagos")
                            .maxFailedLoginAttempts(3)
                            .sessionTimeoutMinutes(30)
                            .enableMfa(true)
                            .webhookUrl("https://api.firstbank.com/webhook")
                            .callbackUrl("https://firstbank.com/callback")
                            .build())
                    .createdAt(LocalDateTime.now().minusMonths(24))
                    .createdBy("SYSTEM")
                    .build(),

            // Tenant 2: PayFast (Fintech / Payment Processor)
            Tenant.builder()
                    .tenantId("PAYFAST")
                    .name("PayFast Payment Solutions")
                    .schemaName("payfast")
                    .domain("payfast.localhost")
                    .status(TenantStatus.ACTIVE)
                    .tenantType(TenantType.FINTECH)
                    .contactEmail("support@payfast.com")
                    .contactPhone("+2348098765432")
                    .description("Modern fintech platform for digital payments")
                    .configuration(TenantConfiguration.builder()
                            .enableBvnValidation(true)
                            .enableNinValidation(false)
                            .enforceKyc(true)
                            .maxDailyTransactionLimit(5000000) // 5 million NGN
                            .maxMonthlyTransactionLimit(20000000) // 20 million NGN
                            .supportedCurrencies("NGN,USD,GHS,KES")
                            .defaultLanguage("en")
                            .timezone("Africa/Lagos")
                            .maxFailedLoginAttempts(5)
                            .sessionTimeoutMinutes(15)
                            .enableMfa(true)
                            .webhookUrl("https://api.payfast.com/webhook")
                            .callbackUrl("https://payfast.com/callback")
                            .build())
                    .createdAt(LocalDateTime.now().minusMonths(12))
                    .createdBy("SYSTEM")
                    .build(),

            // Tenant 3: MicroFinance Plus (Microfinance Bank)
            Tenant.builder()
                    .tenantId("MFB_PLUS")
                    .name("MicroFinance Plus Limited")
                    .schemaName("mfb_plus")
                    .domain("mfbplus.localhost")
                    .status(TenantStatus.ACTIVE)
                    .tenantType(TenantType.MICROFINANCE)
                    .contactEmail("info@mfbplus.com")
                    .contactPhone("+2348055566778")
                    .description("Microfinance bank serving rural communities")
                    .configuration(TenantConfiguration.builder()
                            .enableBvnValidation(true)
                            .enableNinValidation(true)
                            .enforceKyc(false)
                            .maxDailyTransactionLimit(1000000) // 1 million NGN
                            .maxMonthlyTransactionLimit(3000000) // 3 million NGN
                            .supportedCurrencies("NGN")
                            .defaultLanguage("en")
                            .timezone("Africa/Lagos")
                            .maxFailedLoginAttempts(3)
                            .sessionTimeoutMinutes(20)
                            .enableMfa(false)
                            .webhookUrl("https://api.mfbplus.com/webhook")
                            .callbackUrl("https://mfbplus.com/callback")
                            .build())
                    .createdAt(LocalDateTime.now().minusMonths(6))
                    .createdBy("SYSTEM")
                    .build(),

            // Tenant 4: GlobalPay (Payment Service Provider)
            Tenant.builder()
                    .tenantId("GLOBALPAY")
                    .name("GlobalPay International")
                    .schemaName("globalpay")
                    .domain("globalpay.localhost")
                    .status(TenantStatus.ACTIVE)
                    .tenantType(TenantType.PAYMENT_SERVICE_PROVIDER)
                    .contactEmail("support@globalpay.com")
                    .contactPhone("+442071234567")
                    .description("International payment gateway")
                    .configuration(TenantConfiguration.builder()
                            .enableBvnValidation(false)
                            .enableNinValidation(false)
                            .enforceKyc(true)
                            .maxDailyTransactionLimit(50000000) // 50 million NGN equivalent
                            .maxMonthlyTransactionLimit(200000000) // 200 million NGN equivalent
                            .supportedCurrencies("USD,EUR,GBP,NGN,GHS,KES,ZAR")
                            .defaultLanguage("en")
                            .timezone("UTC")
                            .maxFailedLoginAttempts(5)
                            .sessionTimeoutMinutes(30)
                            .enableMfa(true)
                            .webhookUrl("https://api.globalpay.com/webhook")
                            .callbackUrl("https://globalpay.com/callback")
                            .build())
                    .createdAt(LocalDateTime.now().minusMonths(36))
                    .createdBy("SYSTEM")
                    .build(),

            // Tenant 5: Merchant Direct (E-commerce Merchant)
            Tenant.builder()
                    .tenantId("MERCHANT_DIRECT")
                    .name("Merchant Direct Store")
                    .schemaName("merchant_direct")
                    .domain("merchant.localhost")
                    .status(TenantStatus.ACTIVE)
                    .tenantType(TenantType.MERCHANT)
                    .contactEmail("admin@merchantdirect.com")
                    .contactPhone("+2348033344455")
                    .description("Direct e-commerce merchant")
                    .configuration(TenantConfiguration.builder()
                            .enableBvnValidation(false)
                            .enableNinValidation(false)
                            .enforceKyc(false)
                            .maxDailyTransactionLimit(2000000) // 2 million NGN
                            .maxMonthlyTransactionLimit(10000000) // 10 million NGN
                            .supportedCurrencies("NGN,USD")
                            .defaultLanguage("en")
                            .timezone("Africa/Lagos")
                            .maxFailedLoginAttempts(5)
                            .sessionTimeoutMinutes(45)
                            .enableMfa(false)
                            .webhookUrl("https://api.merchantdirect.com/webhook")
                            .callbackUrl("https://merchantdirect.com/callback")
                            .build())
                    .createdAt(LocalDateTime.now().minusMonths(3))
                    .createdBy("SYSTEM")
                    .build()
        );
    }

    private List<Customer> createCustomersForTenant(Tenant tenant) {
        String tenantId = tenant.getTenantId();
        
        return Arrays.asList(
            // Customer 1: Individual with both BVN and NIN
            createCustomer(
                    "John", "Doe", 
                    "john.doe@" + tenantId.toLowerCase() + ".com",
                    "+2348012345001",
                    "12345678901",
                    "98765432101",
                    CustomerType.INDIVIDUAL,
                    tenantId,
                    true,
                    true,
                    "Lagos",
                    "Nigeria"
            ),
            
            // Customer 2: Individual with only BVN
            createCustomer(
                    "Jane", "Smith",
                    "jane.smith@" + tenantId.toLowerCase() + ".com",
                    "+2348012345002",
                    "22345678901",
                    null,
                    CustomerType.INDIVIDUAL,
                    tenantId,
                    true,
                    false,
                    "Abuja",
                    "Nigeria"
            ),
            
            // Customer 3: Individual with only NIN
            createCustomer(
                    "Michael", "Johnson",
                    "michael.j@" + tenantId.toLowerCase() + ".com",
                    "+2348012345003",
                    null,
                    "88765432109",
                    CustomerType.INDIVIDUAL,
                    tenantId,
                    false,
                    true,
                    "Port Harcourt",
                    "Nigeria"
            ),
            
            // Customer 4: Corporate customer
            createCorporateCustomer(
                    tenantId + " Enterprises Ltd",
                    "contact@" + tenantId.toLowerCase() + "-enterprises.com",
                    "+2348012345004",
                    "99876543210",
                    "77654321098",
                    CustomerType.CORPORATE,
                    tenantId,
                    "RC1234567",
                    "Lagos",
                    "Nigeria"
            ),
            
            // Customer 5: Foreign customer (no BVN/NIN)
            createCustomer(
                    "Sarah", "Williams",
                    "sarah.w@" + tenantId.toLowerCase() + ".com",
                    "+447911123456",
                    null,
                    null,
                    CustomerType.INDIVIDUAL,
                    tenantId,
                    false,
                    false,
                    "London",
                    "UK"
            ),
            
            // Customer 6: High-value customer
            createHighValueCustomer(
                    "David", "Obi",
                    "david.obi@" + tenantId.toLowerCase() + ".com",
                    "+2348012345006",
                    "32345678901",
                    "66543210987",
                    CustomerType.INDIVIDUAL,
                    tenantId,
                    RiskRating.LOW,
                    "Enugu",
                    "Nigeria"
            ),
            
            // Customer 7: Customer with complete KYC
            createKycCompletedCustomer(
                    "Blessing", "Okonkwo",
                    "blessing.o@" + tenantId.toLowerCase() + ".com",
                    "+2348012345007",
                    "42345678901",
                    "55432109876",
                    CustomerType.INDIVIDUAL,
                    tenantId,
                    "PASSPORT",
                    "A12345678",
                    LocalDateTime.now().plusYears(5)
            ),
            
            // Customer 8: Blocked customer (for testing block/unblock)
            createBlockedCustomer(
                    "Peter", "Obi",
                    "peter.obi@" + tenantId.toLowerCase() + ".com",
                    "+2348012345008",
                    "52345678901",
                    "44321098765",
                    CustomerType.INDIVIDUAL,
                    tenantId,
                    "Suspicious activity detected"
            ),
            
            // Customer 9: Pending activation
            createPendingCustomer(
                    "Grace", "Adeyemi",
                    "grace.a@" + tenantId.toLowerCase() + ".com",
                    "+2348012345009",
                    "62345678901",
                    "33210987654",
                    CustomerType.INDIVIDUAL,
                    tenantId
            ),
            
            // Customer 10: Customer with specific address
            createCustomerWithAddress(
                    "Emeka", "Okafor",
                    "emeka.o@" + tenantId.toLowerCase() + ".com",
                    "+2348012345010",
                    "72345678901",
                    "22109876543",
                    CustomerType.INDIVIDUAL,
                    tenantId,
                    "15 Awolowo Road",
                    "Ikeja",
                    "Lagos",
                    "Nigeria",
                    "100001"
            )
        );
    }

    private Customer createCustomer(String firstName, String lastName, String email, 
                                   String phone, String bvn, String nin, 
                                   CustomerType type, String tenantId,
                                   boolean bvnVerified, boolean ninVerified,
                                   String city, String country) {
        
        Customer customer = Customer.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phoneNumber(phone)
                .bvn(bvn)
                .nin(nin)
                .customerType(type)
                .status(CustomerStatus.ACTIVE)
                .riskRating(RiskRating.LOW)
                .dateOfBirth(LocalDateTime.of(1990, Month.JANUARY, 1, 0, 0))
                .taxId("TAX" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .residentialAddress(Address.builder()
                        .addressLine1("123 Main Street")
                        .addressLine2("Apt 4B")
                        .city(city)
                        .state(city)
                        .country(country)
                        .postalCode("100001")
                        .build())
                .tenantId(tenantId)
                .createdAt(LocalDateTime.now().minusDays(randomDays()))
                .createdBy("SYSTEM")
                .build();

        // Set BVN verification if applicable
        if (bvn != null && bvnVerified) {
            customer.setBvnVerification(BvnVerification.builder()
                    .verified(true)
                    .verifiedAt(LocalDateTime.now().minusDays(randomDays()))
                    .verifiedBy("SYSTEM")
                    .verificationReference("BVN_VER_" + UUID.randomUUID().toString().substring(0, 8))
                    .responseCode("00")
                    .responseMessage("BVN verified successfully")
                    .build());
        }

        // Set NIN verification if applicable
        if (nin != null && ninVerified) {
            customer.setNinVerification(NinVerification.builder()
                    .verified(true)
                    .verifiedAt(LocalDateTime.now().minusDays(randomDays()))
                    .verifiedBy("SYSTEM")
                    .verificationReference("NIN_VER_" + UUID.randomUUID().toString().substring(0, 8))
                    .responseCode("00")
                    .responseMessage("NIN verified successfully")
                    .build());
        }

        // Generate CIF number
        customer.setCifNumber(generateCifNumber(tenantId));

        return customer;
    }

    private Customer createCorporateCustomer(String businessName, String email, String phone,
                                            String bvn, String nin, CustomerType type,
                                            String tenantId, String registrationNumber,
                                            String city, String country) {
        
        Customer customer = createCustomer(
                businessName.split(" ")[0], 
                "Enterprises", 
                email, 
                phone, 
                bvn, 
                nin, 
                type, 
                tenantId,
                true, 
                true, 
                city, 
                country
        );
        
        customer.setTaxId(registrationNumber);
        return customer;
    }

    private Customer createHighValueCustomer(String firstName, String lastName, String email,
                                            String phone, String bvn, String nin,
                                            CustomerType type, String tenantId,
                                            RiskRating riskRating, String city, String country) {
        
        Customer customer = createCustomer(
                firstName, lastName, email, phone, bvn, nin, 
                type, tenantId, true, true, city, country
        );
        
        customer.setRiskRating(riskRating);
        return customer;
    }

    private Customer createKycCompletedCustomer(String firstName, String lastName, String email,
                                               String phone, String bvn, String nin,
                                               CustomerType type, String tenantId,
                                               String idType, String idNumber,
                                               LocalDateTime expiryDate) {
        
        Customer customer = createCustomer(
                firstName, lastName, email, phone, bvn, nin,
                type, tenantId, true, true, "Lagos", "Nigeria"
        );

        customer.setKycDetails(KycDetails.builder()
                .idType(idType)
                .idNumber(idNumber)
                .idExpiryDate(expiryDate)
                .kycCompleted(true)
                .kycVerifiedAt(LocalDateTime.now().minusDays(5))
                .kycVerifiedBy("ADMIN")
                .build());

        return customer;
    }

    private Customer createBlockedCustomer(String firstName, String lastName, String email,
                                          String phone, String bvn, String nin,
                                          CustomerType type, String tenantId,
                                          String blockReason) {
        
        Customer customer = createCustomer(
                firstName, lastName, email, phone, bvn, nin,
                type, tenantId, true, true, "Lagos", "Nigeria"
        );
        
        customer.setStatus(CustomerStatus.BLOCKED);
        return customer;
    }

    private Customer createPendingCustomer(String firstName, String lastName, String email,
                                          String phone, String bvn, String nin,
                                          CustomerType type, String tenantId) {
        
        Customer customer = createCustomer(
                firstName, lastName, email, phone, bvn, nin,
                type, tenantId, false, false, "Lagos", "Nigeria"
        );
        
        customer.setStatus(CustomerStatus.PENDING_ACTIVATION);
        return customer;
    }

    private Customer createCustomerWithAddress(String firstName, String lastName, String email,
                                              String phone, String bvn, String nin,
                                              CustomerType type, String tenantId,
                                              String addressLine1, String city, 
                                              String state, String country, String postalCode) {
        
        Customer customer = createCustomer(
                firstName, lastName, email, phone, bvn, nin,
                type, tenantId, true, true, city, country
        );

        customer.setResidentialAddress(Address.builder()
                .addressLine1(addressLine1)
                .city(city)
                .state(state)
                .country(country)
                .postalCode(postalCode)
                .build());

        return customer;
    }

    private String generateCifNumber(String tenantId) {
        String prefix = tenantId.length() > 3 ? tenantId.substring(0, 3).toUpperCase() : tenantId;
        return "CIF" + prefix + 
               LocalDateTime.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + 
               String.format("%06d", new java.util.Random().nextInt(999999));
    }

    private int randomDays() {
        return new java.util.Random().nextInt(365) + 1;
    }

    private void printTestSummary(List<Tenant> tenants) {
        log.info("\n");
        log.info("📋 TEST DATA SUMMARY");
        log.info("=====================");
        
        for (Tenant tenant : tenants) {
            long customerCount = customerRepository.findByTenantId(tenant.getTenantId()).size();
            log.info("\n🏦 Tenant: {} ({})", tenant.getName(), tenant.getTenantId());
            log.info("   ├─ Domain: {}", tenant.getDomain());
            log.info("   ├─ Type: {}", tenant.getTenantType());
            log.info("   ├─ Status: {}", tenant.getStatus());
            log.info("   ├─ Customers: {}", customerCount);
            log.info("   ├─ Supported Currencies: {}", tenant.getConfiguration().getSupportedCurrencies());
            log.info("   └─ Test with Header: X-Tenant-ID: {}", tenant.getTenantId());
        }
        
        log.info("\n");
        log.info("🔗 TEST ENDPOINTS");
        log.info("==================");
        log.info("Swagger UI: http://localhost:8081/api/swagger-ui.html");
        log.info("Health Check: http://localhost:8081/api/actuator/health");
        log.info("\n");
        log.info("📝 SAMPLE API CALLS");
        log.info("===================");
        log.info("1. Get all customers for FIRST_BANK:");
        log.info("   curl -X GET http://localhost:8081/api/v1/customers -H \"X-Tenant-ID: FIRST_BANK\"");
        log.info("\n2. Search customers in PAYFAST:");
        log.info("   curl -X GET \"http://localhost:8081/api/v1/customers/search?searchTerm=John\" -H \"X-Tenant-ID: PAYFAST\"");
        log.info("\n3. Onboard new customer to GLOBALPAY:");
        log.info("   curl -X POST http://localhost:8081/api/v1/customers \\");
        log.info("     -H \"X-Tenant-ID: GLOBALPAY\" \\");
        log.info("     -H \"Content-Type: application/json\" \\");
        log.info("     -d '{\"firstName\":\"Test\",\"lastName\":\"User\",\"email\":\"test@globalpay.com\",\"customerType\":\"INDIVIDUAL\"}'");
    }
}
