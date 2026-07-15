package com.yatayat.backend.dto;

public class OperatorApplicationRequest {

    private Long userId;
    private String organizationName;
    private String operatorType;
    private String registrationNumber;
    private String permitNumber;
    private String contactPerson;
    private String phone;
    private String address;

    public OperatorApplicationRequest() {
    }

    public Long getUserId() {
        return userId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public String getOperatorType() {
        return operatorType;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getPermitNumber() {
        return permitNumber;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setOrganizationName(
            String organizationName
    ) {
        this.organizationName = organizationName;
    }

    public void setOperatorType(String operatorType) {
        this.operatorType = operatorType;
    }

    public void setRegistrationNumber(
            String registrationNumber
    ) {
        this.registrationNumber = registrationNumber;
    }

    public void setPermitNumber(String permitNumber) {
        this.permitNumber = permitNumber;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}