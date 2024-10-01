package botting.map;

import java.util.Arrays;
import java.util.Optional;

public enum LandType {
  MOUNTAIN(-2),FOG_OBSTACLE(-4),CITY(431),EMPTY(-1),ENEMY(1),FRIENDLY(0);

  private int value;
  LandType(int value){
    this.value=value;
  }

  public static LandType as(int terrain){
    Optional<LandType> type=Arrays.stream(LandType.values()).filter(l->l.value==terrain).findAny();
    if(type.isEmpty()) throw new IllegalArgumentException("terrain does not exist");
    return type.get();
  }
}
