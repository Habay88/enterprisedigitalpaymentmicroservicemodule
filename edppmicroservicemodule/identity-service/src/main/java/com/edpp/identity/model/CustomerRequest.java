package com.edpp.identity.model;

private Customer mapToEntity(CustomerRequest request){return Customer.builder().firstName(request.getFirstName()).lastName(request.getLastName()).email(request.getEmail()).phoneNumber(request.getPhoneNumber()).customerType(request.getCustomerType()).dateOfBirth(request.getDateOfBirth()).taxId(request.getTaxId()).residentialAddress(request.getAddress()).build();}
