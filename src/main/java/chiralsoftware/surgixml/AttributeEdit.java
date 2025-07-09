package chiralsoftware.surgixml;

record AttributeEdit(String xpath, String attribute, String newValue) {

    static AttributeEdit parse(String param) {
        final int atIndex = param.lastIndexOf('@');
        final int eqIndex = param.indexOf('=', atIndex);
        if(atIndex == -1 || eqIndex == -1) {
            throw new IllegalArgumentException("invalid attribute: " + param);
        }
        return new AttributeEdit(param.substring(0, atIndex),
                param.substring(atIndex + 1, eqIndex),
                param.substring(eqIndex + 1));
    }

    @Override
    public String toString() {
        return "AttributeEdit{" +
                "xpath='" + xpath + '\'' +
                ", attribute='" + attribute + '\'' +
                ", newValue='" + newValue + '\'' +
                '}';
    }
}
