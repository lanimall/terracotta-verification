package org.terracotta.utils.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.utils.RandomUtil;

import java.io.Serializable;

public class AddressCategory implements Serializable {
    public static final String[] valuesCategoryType = {"commercial", "residential"};
    public static final String[] valuesCategorySubType = {"apt", "house", "condo", "high rise"};
    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(AddressCategory.class);
    private String type;
    private String subType;

    public AddressCategory() {
        super();
    }

    public AddressCategory(String type, String subType) {
        super();
        this.type = type;
        this.subType = subType;
    }

    public static AddressCategory createRandom() {
        RandomUtil randomUtil = new RandomUtil();

        AddressCategory addressCategory = new AddressCategory();
        addressCategory.setType(randomUtil.getRandomObjectFromArray(valuesCategoryType));
        addressCategory.setSubType(randomUtil.getRandomObjectFromArray(valuesCategorySubType));
        return addressCategory;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    @Override
    public String toString() {
        return "AddressCategory{" +
                "type='" + type + '\'' +
                ", subType='" + subType + '\'' +
                '}';
    }
}