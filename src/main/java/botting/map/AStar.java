package botting.map;

import java.util.*;

public final class AStar {
  private AStar(){}

  public static Optional<List<Integer>> getPath(int start,int end,
      Set<Integer> nodes,botting.generals.Map map,Set<Integer> restricted){
    Map<Integer,List<Integer>> paths=new HashMap<>();
    Map<Integer,Integer> openList=new HashMap<>();
    openList.put(start,0);
    paths.put(start,new ArrayList<>(List.of(start)));
    if(start==end){
      return Optional.of(paths.get(start));
    }
    while(!openList.isEmpty()) {
      int q=openList.keySet().stream().min(Comparator.comparingDouble(node->openList.get(node) +
          map.manhat(node,end))).get();
      int cost=openList.remove(q);
      List<Integer> currpath;

      //add successors
      for(int n:Arrays.stream(map.dxs(q))
          .filter(dx->map.legalMove(q,dx)&&nodes.contains(dx)&&!restricted.contains(dx)).toArray()) {
        currpath=new ArrayList<>(paths.get(q));
        currpath.add(n);

        if(n==end) {
          return Optional.of(currpath);
        }

        openList.put(n,cost+1);
        paths.put(n,currpath);
      }
      paths.remove(q);
      restricted.add(q);
    }
    return Optional.empty();
  }
}
