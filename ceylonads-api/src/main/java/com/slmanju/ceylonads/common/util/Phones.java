package com.slmanju.ceylonads.common.util;

public final class Phones {

    // Accepts blank (optional field, not required), local "0XXXXXXXXX", or international
    // "+94XXXXXXXXX" Sri Lankan mobile/landline numbers - not a general international phone
    // validator, matching the project's existing local-market scope.
    public static final String SRI_LANKAN_PHONE_PATTERN = "^$|^(0[1-9][0-9]{8}|\\+94[1-9][0-9]{8})$";

    private Phones() {
    }
}
