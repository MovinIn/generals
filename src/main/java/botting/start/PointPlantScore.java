package botting.start;

import botting.generals.Map;

import java.util.*;

public final class PointPlantScore implements PlantScore{
  private static final double LAND_GAINED_WEIGHT=100d;
  private static final double GENERAL_DEVIATION_WEIGHT=5d;
  private static final double CENTER_PRESENCE_WEIGHT=50d;
  private static final double CLOSEST_CORNER_WEIGHT=25d;
  private static final double ADJACENT_SPACES=100d;
  private static final double IMPOSSIBLE_25_START=10000d;

  @Override
  public double score(Map map, Plant plant) {
    List<Branch> branches=plant.getBranches();
    Set<Integer> nSet=new HashSet<>();
    int generalDist=0;
    for(Branch branch:branches){
      nSet.addAll(branch.getBranch());
      for(int node:branch.getBranch()){
        generalDist+=map.manhat(map.generals[map.generalme],node);
      }
    }

    if(nSet.isEmpty()) return Integer.MIN_VALUE;
    double centerDist=nSet.stream().min(Comparator.comparingInt(node->map.manhat(node,map.center()))).get();
    int closestCorner=0;
    int shortestDist=Integer.MAX_VALUE;
    for(int corner:map.getEmptyCorners()){
      int dist=map.manhat(corner,map.generals[map.generalme]);
      if(dist<=Map.MINIMUM_GENERAL_DISTANCE) continue;
      if(shortestDist>dist){
        closestCorner=corner;
      }
    }
    int finalClosestCorner = closestCorner;
    double cornerDist=nSet.stream().min(Comparator.comparingInt(x->map.manhat(x, finalClosestCorner))).get();

    //Normalization
    generalDist/=nSet.size(); //average deviation from general
    cornerDist=1-cornerDist/shortestDist; //proportion covered until destination
    centerDist=1-centerDist/map.manhat(map.center(),map.generals[map.generalme]); // ^^
    boolean imp25=false;

    if(branches.size()==2){//TODO: calculate if we can actually do a 25 start or not
      imp25=branches.getFirst().overlap(branches.getLast())>2;
    }

    int adj=0;
    for(int dx:map.dxs(map.generals[map.generalme])){
      if(map.legalMove(dx,map.generals[map.generalme])&&!nSet.contains(dx)) {
        adj++;
      }
    }

    return LAND_GAINED_WEIGHT*nSet.size() +
        GENERAL_DEVIATION_WEIGHT*generalDist +
        CENTER_PRESENCE_WEIGHT*centerDist +
        CLOSEST_CORNER_WEIGHT*cornerDist +
        ADJACENT_SPACES*adj -
        IMPOSSIBLE_25_START*(imp25?1:0);
  }
}
