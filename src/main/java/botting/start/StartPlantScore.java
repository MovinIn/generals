package botting.start;

import botting.generals.Map;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StartPlantScore implements PlantScore {
  private static final double BRANCH_DEVIATION_WEIGHT=0.3d;
  private static final BranchScore branchScore=new StartBranchScore();
  private static PrintWriter out;

  static{
    try {
      out=new PrintWriter(new BufferedWriter(new FileWriter("out/plant.out")));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Scores based on StartBranchScore and branch deviation.
   * */
  @Override
  public double score(Map map, Plant plant) {
    List<Branch> branches=plant.getBranches();
    Set<Integer> nSet=new HashSet<>();
    branches.forEach(branch->nSet.addAll(branch.getBranch()));
    List<Integer> nodes=new ArrayList<>(nSet);

    if(nodes.isEmpty()) return 0;

    double branchDeviation=0;//we want spread out branches
    int count=0; //this is cause i dont know math
    for(int i=0; i<nodes.size(); i++){
      for(int j=i+1; j<nodes.size(); j++){
        branchDeviation+=map.dist(nodes.get(i),nodes.get(j));
        count++;
      }
    }
    double branchScoreTotal=branches.stream().mapToDouble(b->branchScore.score(map,b)).sum();
    //normalizing
    branchDeviation/=count;
    out.println(plant);
    out.println("===breakdown===");
    out.println("branch deviation: "+branchDeviation);
    out.println("branch deviation with weight: "+BRANCH_DEVIATION_WEIGHT*branchDeviation);
    out.println("branchScoreTotal: "+branchScoreTotal);
    out.println("=======================");
    return branchScoreTotal + BRANCH_DEVIATION_WEIGHT*branchDeviation;
  }
}
