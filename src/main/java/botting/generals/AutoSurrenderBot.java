package botting.generals;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class AutoSurrenderBot {
  private GeneralSocket socket;
  private String custom;
  public AutoSurrenderBot() {
    socket=GeneralSocket.createGeneralSocket(this::processSocketMessage);
  }

  public void joinCustomRoom(String custom) {
    this.custom=custom;
    socket.joinCustom(custom);
  }

  public void processSocketMessage(SocketMessage message) {
    if(message.type==SocketMessageType.CONNECTED) {
      try(BufferedReader br=new BufferedReader(new FileReader("room.in"))) {
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
        case "game_over":
          joinCustomRoom(custom);
      }
    }
  }
}
