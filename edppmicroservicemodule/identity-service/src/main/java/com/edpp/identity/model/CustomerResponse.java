package com.edpp.identity.model;

private CustomerResponse mapToResponse(Customer customer){return CustomerResponse.builder().id(customer.getId()).cifNumber(customer.getCifNumber()).fullName(customer.getFirstName()+" "+customer.getLastName()).email(customer.getEmail()).customerType(customer.getCustomerType()).status(customer.getStatus()).riskRating(customer.getRiskRating()).kycCompleted(customer.getKycDetails()!=null&&customer.getKycDetails().isKycCompleted()).createdAt(customer.getCreatedAt()).build();}
