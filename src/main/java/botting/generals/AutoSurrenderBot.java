package botting.generals;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class AutoSurrenderBot extends Bot {

  @Override
  public void processSocketMessage(SocketMessage message) {
    if(message.type==SocketMessageType.CONNECTED) {
      try(BufferedReader br=new BufferedReader(new FileReader("in/room.in"))) {
        joinCustomRoom(br.readLine());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    else if(message.type==SocketMessageType.CLOSED) {

    }
    else if(message.type==SocketMessageType.MESSAGE) {
      String mode=message.data.getString(0);
      switch(mode) {
        case "queue_update":
          JSONObject gamedata=message.data.getJSONObject(1);

          if(!gamedata.getBoolean("isForcing")&&gamedata.getInt("numPlayers")>1)
            socket.forceStart(custom);
          break;
        case "game_start":
          socket.surrender();
          break;
        case "game_over":
          joinCustomRoom(custom);
      }
    }
  }
}
