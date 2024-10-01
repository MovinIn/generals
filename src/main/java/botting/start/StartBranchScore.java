package botting.start;

import botting.generals.Map;

public class StartBranchScore implements BranchScore {
  private static final double GENERAL_DEVIATION_WEIGHT=2d;
  private static final double CENTER_PROXIMITY_WEIGHT=-10d;
  private static final double LAND_WEIGHT=100d;

  /**
   * Scores based on deviation from general, proximity to center, and land gained.
   * */
  @Override
  public double score(Map map, Branch branch) {
    int general=map.generals[map.generalme];
    return branch.getBranch().stream().mapToDouble(pos ->
            map.dist(pos,general)*GENERAL_DEVIATION_WEIGHT
                + map.dist(pos,map.center())*CENTER_PROXIMITY_WEIGHT).sum()/branch.getBranch().size()
        + LAND_WEIGHT*branch.getBranch().size();
  }
}
