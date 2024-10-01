package botting.map;

import botting.generals.Map;
import java.util.*;
import java.util.stream.Collectors;

public abstract class EmptySpaces {
  public static int findNearestEmptyNode(int node,botting.generals.Map map) {
    int terrain=map.terrain[node];
    Set<Integer> visited=new HashSet<>();
    Queue<Integer> queue=new LinkedList<>();
    queue.add(node);

    while((terrain!=Map.EMPTY&&terrain!=Map.FOG)||map.cities.contains(node)||!has20(queue.peek(),map)){
      node=queue.remove();
      visited.add(node);
      int finalNode = node;
      Arrays.stream(map.dxs(node))
          .filter(dx->map.adjacent(finalNode,dx)&&!visited.contains(dx))
          .forEach(queue::add);
      terrain=map.terrain[queue.peek()];
    }
    return queue.peek();
  }

  public static Set<Integer> emptyCorners(botting.generals.Map map){
    int[] corners={0,map.w-1,map.w*(map.h-1),map.w*map.h-1};
    return Arrays.stream(corners).map(corner->findNearestEmptyNode(corner,map)).boxed()
        .collect(Collectors.toSet());
  }

  /**
   * returns if there is at least 20 tiles that the node can reach.
   * */
  public static boolean has20(int node,botting.generals.Map map){
    Queue<Integer> queue=new LinkedList<>();
    Set<Integer> visited=new HashSet<>();
    queue.add(node);
    while(!queue.isEmpty()){
      if(visited.size()>=20){
        return true;
      }
      node=queue.remove();
      visited.add(node);
      for(int dx:map.dxs(node)){
        if(map.legalMove(node,dx)&&!visited.contains(dx)&&!map.cities.contains(dx)){
          queue.add(dx);
          visited.add(dx);
        }
      }
    }
    return false;
  }
}
