package botting.generals;

import org.json.JSONArray;

public class SocketMessage {
  SocketMessageType type;
  String raw;
  JSONArray data;
  public SocketMessage(SocketMessageType type){
    this(type,"");
  }
  public SocketMessage(SocketMessageType type,String message) {
    this.type=type;
    this.raw=message;
    if(raw.contains("[")){
      data=new JSONArray(raw.substring(raw.indexOf("[")));
    }
  }
}
