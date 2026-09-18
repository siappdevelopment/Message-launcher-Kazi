package com.messages.smart.sms.models;

public class ContactsModel {
    public String photo;
    public String name;
    public String number;
    public boolean isExpanded;

    public ContactsModel(String photo, String name, String number) {
        this.photo = photo;
        this.name = name;
        this.number = number;
    }

    public String getPhoto() {
        return photo;
    }

    public String getName() {
        return name;
    }

    public String getNumber() {
        return number;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public boolean isExpanded() {
        return isExpanded;
    }
}