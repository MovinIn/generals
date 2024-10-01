package botting.map;

public class Node {
  public int position;
  int troops;//TODO: maybe make this a negative # if enemy, but seems too troll
  LandType type;
  public Node(int position,int troops,LandType type){
    this.type=type;
    this.troops=troops;
    this.position=position;
  }
  public Node(int position,int troops,int type){
    this(position,troops,LandType.as(type));
  }
}
