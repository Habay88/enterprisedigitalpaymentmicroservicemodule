package com.edpp.merchant.enums;

public enum MerchantCategory {
    RETAIL_GOODS(5411, "Retail - Goods"),
    RETAIL_SERVICES(5412, "Retail - Services"),
    ECOMMERCE(5734, "E-commerce"),
    TRAVEL(4722, "Travel & Hospitality"),
    RESTAURANT(5812, "Restaurants & Dining"),
    DIGITAL_GOODS(5816, "Digital Goods & Software"),
    FINANCIAL_SERVICES(6012, "Financial Services"),
    GROCERY(5411, "Grocery & Supermarkets"),
    HEALTHCARE(8090, "Healthcare"),
    EDUCATION(8299, "Education"),
    UTILITIES(4900, "Utilities"),
    GOVERNMENT(9399, "Government Services");

    private final int code;
    private final String description;

    MerchantCategory(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }
}
