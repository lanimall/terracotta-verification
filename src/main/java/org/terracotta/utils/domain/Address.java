package org.terracotta.utils.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.utils.RandomUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Address implements Serializable {
    public static final List<String> listStates = new ArrayList<String>();
    public static final List<String> listStateCodes = new ArrayList<String>();
    static {
        listStates.add("Alabama");
        listStates.add("Alaska");
        listStates.add("Arizona");
        listStates.add("Arkansas");
        listStates.add("California");
        listStates.add("Colorado");
        listStates.add("Connecticut");
        listStates.add("Delaware");
        listStates.add("Florida");
        listStates.add("Georgia");
        listStates.add("Hawaii");
        listStates.add("Idaho");
        listStates.add("Illinois");
        listStates.add("Indiana");
        listStates.add("Iowa");
        listStates.add("Kansas");
        listStates.add("Kentucky");
        listStates.add("Louisiana");
        listStates.add("Maine");
        listStates.add("Maryland");
        listStates.add("Massachusetts");
        listStates.add("Michigan");
        listStates.add("Minnesota");
        listStates.add("Mississippi");
        listStates.add("Missouri");
        listStates.add("Montana");
        listStates.add("Nebraska");
        listStates.add("Nevada");
        listStates.add("New Hampshire");
        listStates.add("New Jersey");
        listStates.add("New Mexico");
        listStates.add("New York");
        listStates.add("North Carolina");
        listStates.add("North Dakota");
        listStates.add("Ohio");
        listStates.add("Oklahoma");
        listStates.add("Oregon");
        listStates.add("Pennsylvania");
        listStates.add("Rhode Island");
        listStates.add("South Carolina");
        listStates.add("South Dakota");
        listStates.add("Tennessee");
        listStates.add("Texas");
        listStates.add("Utah");
        listStates.add("Vermont");
        listStates.add("Virginia");
        listStates.add("Washington");
        listStates.add("West Virginia");
        listStates.add("Wisconsin");
        listStates.add("Wyoming");

        listStateCodes.add("AL");
        listStateCodes.add("AK");
        listStateCodes.add("AZ");
        listStateCodes.add("AR");
        listStateCodes.add("CA");
        listStateCodes.add("CO");
        listStateCodes.add("CT");
        listStateCodes.add("DE");
        listStateCodes.add("FL");
        listStateCodes.add("GA");
        listStateCodes.add("HI");
        listStateCodes.add("ID");
        listStateCodes.add("IL");
        listStateCodes.add("IN");
        listStateCodes.add("IA");
        listStateCodes.add("KS");
        listStateCodes.add("KY");
        listStateCodes.add("LA");
        listStateCodes.add("ME");
        listStateCodes.add("MD");
        listStateCodes.add("MA");
        listStateCodes.add("MI");
        listStateCodes.add("MN");
        listStateCodes.add("MS");
        listStateCodes.add("MO");
        listStateCodes.add("MT");
        listStateCodes.add("NE");
        listStateCodes.add("NV");
        listStateCodes.add("NH");
        listStateCodes.add("NJ");
        listStateCodes.add("NM");
        listStateCodes.add("NY");
        listStateCodes.add("NC");
        listStateCodes.add("ND");
        listStateCodes.add("OH");
        listStateCodes.add("OK");
        listStateCodes.add("OR");
        listStateCodes.add("PA");
        listStateCodes.add("RI");
        listStateCodes.add("SC");
        listStateCodes.add("SD");
        listStateCodes.add("TN");
        listStateCodes.add("TX");
        listStateCodes.add("UT");
        listStateCodes.add("VT");
        listStateCodes.add("VA");
        listStateCodes.add("WA");
        listStateCodes.add("WV");
        listStateCodes.add("WI");
        listStateCodes.add("WY");
    }
    private static final long serialVersionUID = -6705540997879341956L;
    private static Logger log = LoggerFactory.getLogger(Address.class);
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String stateCode;
    private String zip;
    private AddressCategory addressCategory;

    public Address() {
        super();
    }

    public Address(String line1, String line2, String city, String state, String zip, AddressCategory addressCategory) {
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.state = state;
        this.zip = zip;
        this.addressCategory = addressCategory;
    }

    public static Address createRandom() {
        RandomUtil randomUtil = new RandomUtil();

        Address address = new Address();
        address.setLine1(randomUtil.generateRandomNumericString(5) + " " + randomUtil.generateRandomText(30) + " Avenue");
        address.setLine2("Suite " + randomUtil.generateRandomNumericString(4));
        address.setCity("city_" + randomUtil.generateRandomNumericString(5));
        int stateIndex = randomUtil.generateRandomInt(listStates.size());
        address.setState(listStates.get(stateIndex));
        address.setState(listStateCodes.get(stateIndex));
        address.setZip(randomUtil.generateRandomNumericString(5));
        address.setAddressCategory(AddressCategory.createRandom());

        return address;
    }

    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getLine2() {
        return line2;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStateCode() {
        return stateCode;
    }

    public void setStateCode(String stateCode) {
        this.stateCode = stateCode;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public AddressCategory getAddressCategory() {
        return addressCategory;
    }

    public void setAddressCategory(AddressCategory addressCategory) {
        this.addressCategory = addressCategory;
    }

    @Override
    public String toString() {
        return "Address{" +
                "line1='" + line1 + '\'' +
                ", line2='" + line2 + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", stateCode='" + stateCode + '\'' +
                ", zip='" + zip + '\'' +
                ", addressCategory=" + addressCategory +
                '}';
    }
}