package cz.zcu.kiv.dbm2.sp.util;

public final class Utils {

    private Utils(){}

    /**
     * Return part of uri that follows last occurrence of '/' or '#'
     * @param uri Complete URI
     * @return part of uri that follows last occurrence of '/' or '#'
     */
    public static String getLastPartFromURI(String uri) {
        String s1 = uri.substring(uri.lastIndexOf("/") + 1);
        String s2 = uri.substring(uri.lastIndexOf("#") + 1);
        return s1.length() < s2.length() ? s1 : s2;
    }

    /**
     * Return base part of uri - before last occurrence of '/'
     * @param uri Complete URI
     * @return base part of uri - before last occurrence of '/'
     */
    public static String getBaseFromURI(String uri) { return uri.substring(0, uri.lastIndexOf("/") + 1);}

    /**
     * Return well formatted object name - either last part of uri
     * or part the value before schema specification.
     * If name of object is empty, do nothing
     * @param object String representation of object
     * @return well formatted object name
     */
    public static String getFormattedObjectName(String object) {
        if (object.isEmpty()) {
            return "";
        }
        else if (object.startsWith("http")) {
            return getLastPartFromURI(object);
        }
        else if (object.contains("^^")){
            return object.substring(0, object.indexOf('^'));
        }
        else {
            return object;
        }
    }
}
