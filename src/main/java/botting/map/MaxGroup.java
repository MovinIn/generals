package botting.map;

import botting.generals.Attack;
import botting.group.GroupMap;

import java.util.*;

public abstract class MaxGroup {
  public static List<Attack> groupAll(int general,int startTurn,GroupMap destinationMap){
    Map<Integer,Attack> nodes=new HashMap<>();
    int turn=0;
    List<Integer> gpath=destinationMap.path(general).orElseThrow();
    for(int node=0; node<gpath.size()-1; node++){
      nodes.put(gpath.get(node),new Attack(gpath.get(node),gpath.get(node+1),false,turn++));
    }
    nodes.put(gpath.getLast(),null);
    turn=0;
    for(var entry:destinationMap.getPaths().entrySet()){
      Optional<List<Integer>> o=destinationMap.path(entry.getKey());
      if(o.isEmpty()) continue;
      List<Integer> path=o.get();
      int i;
      for(i=0; i<path.size(); i++){
        if(nodes.containsKey(path.get(i))){
          break;
        }
      }
      for(int j=i-1; j>=0; j--){
        nodes.put(path.get(j),new Attack(path.get(j),path.get(j+1),false,--turn));
      }
    }
    nodes.remove(gpath.getLast());
    for(Attack atk:nodes.values()){
      atk.turn+=turn*-1+startTurn;
    }
    return nodes.values().stream().sorted(Comparator.comparingInt(atk->atk.turn)).toList();
  }
}
