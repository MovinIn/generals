package botting.map;

import botting.generals.Map;

import java.util.HashSet;
import java.util.Set;

public final class EmptyNodeMap {
  private EmptyNodeMap(){}
  public static Set<Integer> nodes(Map map){
    Set<Integer> nodes=new HashSet<>();
    for(int i=0; i<map.terrain.length; i++){
      if(!map.cities.contains(i)&&map.terrain[i]==Map.EMPTY||map.terrain[i]==Map.FOG){
        nodes.add(i);
      }
    }
    return nodes;
  }
}
