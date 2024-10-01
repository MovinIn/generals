package botting.generals;

import org.apache.commons.collections4.list.SetUniqueList;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class Attack {
  public int start,end,turn;
  public boolean is50;
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

  public static List<Attack> fromNodes(List<Integer> nodes,int turn){
    List<Attack> attacks=new ArrayList<>();
    for(int i=0; i<nodes.size()-1; i++){
      attacks.add(new Attack(nodes.get(i),nodes.get(i+1),false,turn++));
    }
    return attacks;
  }

  public static List<Integer> asNodes(Collection<Attack> attacks){
    SetUniqueList<Integer> nodes=SetUniqueList.setUniqueList(new ArrayList<>());
    for(Attack a:attacks.stream().sorted(Comparator.comparingInt(attack->attack.turn)).toList()){
      nodes.add(a.start);
      nodes.add(a.end);
    }
    return nodes;
  }
}
