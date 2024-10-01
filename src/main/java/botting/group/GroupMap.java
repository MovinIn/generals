package botting.group;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class GroupMap {
  private final Map<Integer, Integer> paths;
  private final botting.generals.Map map;
  private int destination;

  public GroupMap(botting.generals.Map map,int destination){
    paths =new HashMap<>();
    this.map=map;
    this.destination=destination;
    addNode(destination);
  }

  public void addNode(int nodeToAdd){
    //Do not process anything if the node is already added.
    if(paths.containsKey(nodeToAdd)){
      return;
    }
    //get the node with the least cost to destination
    int shortest=-1,steps=Integer.MAX_VALUE,challengerSteps;
    for(int n:map.dxs(nodeToAdd)){
      if (paths.containsKey(n)&&(challengerSteps=steps(n))<steps) {
        shortest=n;
        steps=challengerSteps+1;
      }
    }
    //    if a path to destination does not exist, append null.
    if(shortest==-1){
      paths.put(nodeToAdd,null);
      return;
    }
    //Otherwise, append the node with the shortest path. We also see if
    //the added node can shorten preexisting paths.
    paths.put(nodeToAdd,shortest);
    fixNodes(nodeToAdd,steps);
  }

  /**
   * @param steps not entirely needed, but speeds up runtime significantly.
   * */
  private void fixNodes(Integer node,int steps) {
    Queue<Pair<Integer,Integer>> affected=new PriorityQueue<>();
    affected.add(Pair.of(node,steps));
    while(!affected.isEmpty()) {
      Pair<Integer,Integer> p=affected.remove();
      node=p.getLeft();
      steps=p.getRight();
      for (int n : map.dxs(node)) {
        if (!paths.containsKey(n)|| Objects.equals(paths.get(n),node) || steps + 1 >= steps(n)) {
          continue;
        }
        affected.add(Pair.of(n,steps+1));
        paths.put(n,node);
      }
    }
  }

  /**moves the destination by a specified dx. This must be a legal move.*/
  public void moveDestination(int dx){
    paths.put(destination,destination+dx);
    destination=destination+dx;
    paths.put(destination,null);
    fixNodes(destination,0);
  }

  public void setDestination(int dposition){
    paths.put(dposition,null);
    for(int adj:map.dxs(destination)){
      if(paths.containsKey(adj)){
        paths.put(destination,adj);
        break;
      }
    }
    fixNodes(dposition,0);
  }

  public Optional<List<Integer>> path(Integer node) {
    List<Integer> path=new ArrayList<>();
    path.add(node);
    while(node!=destination){
      if((node= paths.get(node))==null) return Optional.empty();
      path.add(node);
    }
    return Optional.of(path);
  }

  private int steps(Integer node) {
    int steps=0;
    while(node!=destination){
      node= paths.get(node);
      if(node==null) return Integer.MAX_VALUE;
      steps++;
    }
    return steps;
  }

  public Map<Integer,Integer> getPaths(){
    return new HashMap<>(paths);
  }

  public int getDestination(){
    return destination;
  }
}
