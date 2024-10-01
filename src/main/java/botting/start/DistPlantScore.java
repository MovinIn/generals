package botting.start;

import botting.generals.Map;

public class DistPlantScore implements PlantScore {
  @Override
  public double score(Map map, Plant plant) {
    BranchScore s=new StartBranchScore();
    return plant.getBranches().stream().mapToDouble(branch->s.score(map,branch)).sum();
  }
}
