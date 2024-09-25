package botting.generals;

public abstract class Bot {
  protected GeneralSocket socket;
  protected String custom;
  public Bot() {
    socket=GeneralSocket.createGeneralSocket(this::processSocketMessage);
  }

  public void joinCustomRoom(String custom) {
    this.custom=custom;
    socket.joinCustom(custom);
  }

  public abstract void processSocketMessage(SocketMessage message);
  }
