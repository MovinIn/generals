package botting.start;

import botting.generals.Map;

public interface PlantScore {
  double score(Map map,Plant plant);
  default double compare(Map map,Plant p1,Plant p2){
    return Double.compare(score(map,p1),score(map,p2));
  }
}
