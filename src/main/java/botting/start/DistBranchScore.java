package botting.start;

import botting.generals.Map;

public class DistBranchScore implements BranchScore {
  @Override
  public double score(Map map, Branch branch) {
    int general=map.generals[0];
    return branch.getBranch().stream().mapToDouble(pos -> map.dist(pos,general)).sum();
  }
}
