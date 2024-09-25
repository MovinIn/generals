package botting.map;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static botting.generals.Map.FOG_OBSTACLE;
import static botting.generals.Map.MOUNTAIN;

public class NodeMap {
  private final Set<Integer> nodes;
  private final Map<Integer,Set<Integer>> connections;

  public NodeMap(botting.generals.Map m) {
    int[] dxs={1,-1,m.height(),-m.height()};
    connections=new HashMap<>();
    nodes=new HashSet<>();
    for(int i=0; i<m.terrain.length;i++){
      if(m.terrain[i]==MOUNTAIN||m.terrain[i]==FOG_OBSTACLE){
        continue;
      }
      nodes.add(i);
      HashSet<Integer> connection=new HashSet<>();
      for(int dx:dxs){
        if(m.legalMove(i,i+dx)){
          connection.add(i+dx);
        }
      }
      if(connection.isEmpty()){
        nodes.remove(i);
        continue;
      }
      connections.put(i,connection);
    }
  }
}
