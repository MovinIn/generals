package botting.generals;

import org.json.JSONArray;

public class XHRUtils {
  public static final String q="\"";
  public static final String defaultPrefix="42";
  public static String buildXHR(String prefix,JSONArray arr){
    return prefix+arr;
  }
  public static String buildXHR(JSONArray arr){
    return buildXHR(defaultPrefix,arr);
  }
}
