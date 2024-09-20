package botting.generals;

public enum SocketMessageType {
  /**Warning: you cannot send messages at OPEN; must be after CONNECTED*/
  OPEN,CONNECTED,CLOSED,MESSAGE,PROBE;
}
