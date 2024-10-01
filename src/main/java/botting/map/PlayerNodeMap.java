package botting.map;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerNodeMap {
  private final Map<Integer,Node> nodes;
  private final Map<Node,Set<Node>> connections;

  public PlayerNodeMap(botting.generals.Map map) {
    connections=new HashMap<>();
    nodes=new HashMap<>();
    update(map);
  }

  //TODO: we only need to check the patches, but its painful to find out how it works
  public void update(botting.generals.Map map){
    connections.clear();
    nodes.clear();
    for(int i=0; i<map.terrain.length;i++){
      if(map.terrain[i]==0){
        nodes.put(i,new Node(i,map.troops[i],LandType.FRIENDLY));
        connections.put(nodes.get(i),new HashSet<>());
      }
    }
    for(Integer i:nodes.keySet()){
      for(int dx:map.dxs()){
        Node n=nodes.get(i+dx);
        if(n!=null){
          connections.get(nodes.get(i)).add(n);
        }
      }
    }
  }
}
