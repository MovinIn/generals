package botting.generals;

import org.json.JSONObject;

import java.io.*;

public class SimpleBot extends Bot{
  private final Map map;
  public SimpleBot() {
    map=new Map();
  }

  @Override
  public void processSocketMessage(SocketMessage message) {
    if(message.type==SocketMessageType.CONNECTED) {
      try(BufferedReader br=new BufferedReader(new FileReader("room.in"))) {
        joinCustomRoom(br.readLine());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    else if(message.type==SocketMessageType.MESSAGE) {
      String mode=message.data.getString(0);
      JSONObject gamedata;
      switch(mode) {
        case "queue_update":
          gamedata=message.data.getJSONObject(1);
          if(!gamedata.getBoolean("isForcing")&&gamedata.getInt("numPlayers")>1)
            socket.forceStart(custom);
          break;
        case "game_update":
          map.update(message.data.getJSONObject(1));
          break;
        case "game_start":
          map.start(message.data.getJSONObject(1));
          break;
        case "game_over":
          joinCustomRoom(custom);
      }
    }
  }
}
