package botting.generals;

public enum GameSpeed {
  SLOW(0.5),NORMAL(1),FAST(2),SUPER(4);
  public final double speed;
  GameSpeed(double speed){
    this.speed=speed;
  }
}
