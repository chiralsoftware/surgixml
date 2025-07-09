package chiralsoftware.surgixml;

record ElementInsert(String xpath, String value) {

    static ElementInsert parse(String param) {
        final int idx = param.indexOf(':');
        if (idx == -1) throw new IllegalArgumentException("Invalid param: " + param);
        return new ElementInsert(param.substring(0, idx), param.substring(idx + 1));
    }
}
