package botting.generals;

import org.json.JSONArray;

public class Attack {
  int start,end,turn;
  boolean is50;
  private static int counter=1;
  public Attack(int start,int end,boolean is50,int turn) {
    this.start=start;
    this.end=end;
    this.is50=is50;
    this.turn=turn;
  }

  public JSONArray asData(){
    return new JSONArray(new Object[]{"attack",start,end,is50,counter++});
  }
  public String toString(){
    return asData().toString();
  }
}
