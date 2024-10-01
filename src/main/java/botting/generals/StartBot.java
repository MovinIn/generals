package botting.generals;

import botting.group.GroupMap;
import botting.map.*;
import botting.start.*;
import org.json.JSONObject;

import javax.swing.text.html.Option;
import java.io.*;
import java.util.*;

public class StartBot extends Bot {
  private Map map;
  private boolean findStart;
  private Plant bestPlant;
  private BranchScore branchScore;
  private PlantScore plantScore;
  private java.util.Map<Integer,Attack> turnBasedAttacks;
  // 13-long branch including general. 7-long branch including general, padded by delays with stems.
  // there is no point in doing a 14 long branch because turn stalling exists.
  private static final int LONG_BRANCH=13,SHORT_BRANCH=7;
  private GroupMap generalMap,centerMap;
  private java.util.Map<Integer,GroupMap> cornerMaps;
  private PrintWriter out;
  private Set<Integer> emptyNodes;
  private List<Integer> unvisitedCorners;
  private Queue<Integer> unattemptedCorners;
  public StartBot() {
    resetOuts();
    reset();
  }

  @Override
  public void processSocketMessage(SocketMessage message) {
    if(message.type==SocketMessageType.CONNECTED) {
      try(BufferedReader br=new BufferedReader(new FileReader("in/room.in"))) {
        joinCustomRoom(br.readLine());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    else if(message.type==SocketMessageType.CLOSED) {
      System.out.println("Socket Closed.");
      System.exit(1);
    }
    else if(message.type==SocketMessageType.MESSAGE) {
      if(message.data.isEmpty()) return;
      String mode=message.data.getString(0);
      JSONObject gamedata;
      switch(mode) {
        case "queue_update":
          gamedata=message.data.getJSONObject(1);

          if(!gamedata.getBoolean("isForcing")&&gamedata.getInt("numPlayers")>1)
            socket.forceStart(custom);
          //          if(!gamedata.getJSONObject("options").has("game_speed") ||
          //              gamedata.getJSONObject("options").getInt("game_speed")!=FAST_GAME_SPEED){
          //            socket.fast(custom,GameSpeed.FAST);
          //          }
          if(!gamedata.getJSONObject("options").has("modifiers") ||
              !gamedata.getJSONObject("options").getJSONArray("modifiers").toList().contains(3)){
            socket.nofog(custom);
          }
          break;
        case "game_start":
          map.start(message.data.getJSONObject(1));
          break;
        case "game_update":
          gamedata=message.data.getJSONObject(1);
          // System.out.println(gamedata);
          map.update(gamedata);
          if(findStart) {
            findStart=false;
            findStart();
          }
          tick();
          break;
        case "game_over":
          reset();
          joinCustomRoom(custom);
      }
    }
  }

  private void tick() {
    Attack a=turnBasedAttacks.remove(map.turn+1);
    if(a!=null){
      System.out.println(a.turn+" "+a);
      socket.attack(a);
    }
  }

  private void reset(){
    map=new Map();
    branchScore=new StartBranchScore();
    plantScore=new PointPlantScore();
    turnBasedAttacks=new HashMap<>();
    bestPlant=new Plant();
    emptyNodes=new HashSet<>();
    unvisitedCorners =new ArrayList<>();
    unattemptedCorners=new PriorityQueue<>();
    cornerMaps=new HashMap<>();
    findStart=true;
  }
  private void resetOuts(){
    try {
      out=new PrintWriter(new BufferedWriter(new FileWriter("out/log.out")));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void findStart() {
    int general=map.generals[map.generalme];
    initCorners();
    emptyNodes= EmptyNodeMap.nodes(map);
    Set<Integer> mainPoints=new HashSet<>(unvisitedCorners);
    mainPoints.add(EmptySpaces.findNearestEmptyNode(map.center(),map));
    //well, lets first try to find the possible branch routes.
    System.out.println(general);
    List<Branch> branch13s=new ArrayList<>();

    //We restrict all adjacent tiles from the general because:
    // let tilex be an adjacent tile.
    // we can simply start from tilex instead of stepping on it using another branch.
    Set<Integer> initialRestrictions=new HashSet<>();
    initialRestrictions.add(general);
    for(int dx:map.dxs(general)){
      initialRestrictions.add(dx);
    }
    //We will try to compute all the 13 branches.
    for(int dx:map.dxs(general)){
      if(map.legalMove(general,dx)){
        for(int corner:mainPoints){
          branchTo(new ArrayList<>(List.of(general,dx)),corner,LONG_BRANCH,initialRestrictions)
              .ifPresent(branch13s::add);
        }
      }
    }

    System.out.println(branch13s.size());
    System.out.println(branch13s);

    for(Branch branch13:branch13s){
      for(int corner:mainPoints){
        updateWithStemV2(branch13,corner,SHORT_BRANCH,1,5);
      }
    }
    if(bash()) return;

    for(Branch b13:branch13s) {
      for(int dx:map.dxs(general)){
        if(!map.legalMove(general,dx)||b13.getBranch().contains(dx)) {
          continue;
        }
        for(int corner:mainPoints) {
          branchTo(new ArrayList<>(List.of(general, dx)),SHORT_BRANCH,corner,initialRestrictions)
              .ifPresent(b6 -> updateBestPlant(b13, b6));
        }
      }
    }
    if(bash()) return;
    socket.surrender();
  }

  private void updateBestPlant(Branch b13,Branch b6) {
    Plant challenger=new Plant(b13,b6);
    updateBestPlant(challenger);
  }

  private void updateBestPlant(Plant challenger){
    if(plantScore.compare(map,challenger,bestPlant)>0){
      System.out.println("updating plant to: "+challenger);
      bestPlant=challenger;
    }
  }
  /**
   * bashing will take the range from 6-8 turns.
   * */
  private boolean bash() {
    if(bestPlant.getBranches().isEmpty() ||
        bestPlant.getBranches().stream().map(Branch::getBranch).anyMatch(b->b==null||b.isEmpty())) {
      return false;
    }
    //sorting from biggest to shortest.
    List<Branch> sortedBranches=bestPlant.getBranches().stream()
        .sorted(Comparator.comparingInt(b->b.getBranch().size()*-1)).toList();
    bestPlant=new Plant(sortedBranches.toArray(new Branch[0]));
    System.out.println(bestPlant);
    int turn=0;
    for(int i=0; i<sortedBranches.size();i++){
      int delay=0;
      for(int j=i-1; j>=0; j--){
        delay+=sortedBranches.get(i).overlap(sortedBranches.get(j));
      }
      // (-1): general should not count for overlap
      delay=Math.max(delay-1,0);
      // (size-delay): troops needed in general.
      // (-1): we start with 1 troop in general.
      // (*2): takes 2 turns to get 1 troop.
      // turn = 25+13+9
      int troops=sortedBranches.get(i).getBranch().size()-1-delay;

      turn+=troops*2;
      sortedBranches.get(i).toAttackList(turn+1).forEach(attack->
          turnBasedAttacks.put(attack.turn,attack));
    }
    turn=turnBasedAttacks.values().stream().mapToInt(att->att.turn).max().getAsInt()+1;
    SimResult result=Simulator.sim(turnBasedAttacks.values(),map,turn);

    System.out.println(Arrays.toString(result.terrain));
    System.out.println(Arrays.toString(result.troops));
    System.out.println(result.land);

    System.out.println("bash starting at "+turn);
    java.util.Map<List<Attack>,Integer> all=new HashMap<>();
    simulate(turn,50,result,new ArrayList<>(),all);
    all.keySet().stream().max(Comparator.comparingInt(all::get)).get()
        .forEach(atk->turnBasedAttacks.put(atk.turn,atk));
    System.out.println("====================");
    System.out.println(turnBasedAttacks);
    System.out.println("estimated start land: "+
        all.get(all.keySet().stream().max(Comparator.comparingInt(all::get)).get()));

    initGroupMaps();

    return true;
  }

  private void initGroupMaps() {
    initCornerMaps();
    int centerDestination = turnBasedAttacks.values().stream()
        .min(Comparator.comparingDouble(att->map.manhat(att.end,map.center()))).orElseThrow().end;
    centerMap=new GroupMap(map,centerDestination);
    generalMap = new GroupMap(map, map.generals[map.generalme]);

    Attack.asNodes(turnBasedAttacks.values()).forEach(node->{
      generalMap.addNode(node);
      centerMap.addNode(node);
      cornerMaps.values().forEach(gmap->gmap.addNode(node));
    });
    System.out.println("center destination: "+centerDestination);
    System.out.println(centerMap.getPaths());
    groupTowardsCorner().ifPresent(this::astarToCorner);
  }

  private void initCorners() {
    unvisitedCorners =new ArrayList<>(map.getEmptyCorners().stream().sorted().toList());
    unattemptedCorners=new PriorityQueue<>(unvisitedCorners);
  }

  private void initCornerMaps(){
    int[] corners={0,map.w-1,map.w*(map.h-1),map.w*map.h-1};

    List<Integer> nodes=Attack.asNodes(turnBasedAttacks.values());
    for(int corner:unvisitedCorners){
      System.out.println(corner);
      int destination=nodes.stream().min(Comparator.comparingInt(node->
          AStar.getPath(node,corner,emptyNodes,map,new HashSet<>()).orElseThrow().size()))
          .orElse(map.generals[map.generalme]);
      cornerMaps.put(corner,new GroupMap(map,destination));
    }
  }

  private Optional<Integer> groupTowardsCorner() {
    Optional<Integer> corner=attemptNearestCorner();
    corner.ifPresent(c->MaxGroup.groupAll(map.generals[map.generalme],
            availableTurn(),cornerMaps.get(c))
        .forEach(attack -> turnBasedAttacks.put(attack.turn,attack)));
    return corner;
  }

  private Optional<Integer> attemptNearestCorner(){
    int corner;
    do{
      if(unattemptedCorners.isEmpty()){
        return Optional.empty();
      }
      corner=unattemptedCorners.remove();
    }
    while(map.manhat(map.generals[map.generalme],corner)<=Map.MINIMUM_GENERAL_DISTANCE);
    System.out.println("2try: "+corner);
    return Optional.of(corner);
  }

  private void astarToCorner(int corner){
    emptyNodes= EmptyNodeMap.nodes(map);
    List<Integer> pathnodes=AStar.getPath(cornerMaps.get(corner).getDestination()
        ,corner,emptyNodes,map,new HashSet<>()).orElse(new ArrayList<>());
    for(int i=0; i<pathnodes.size(); i++){
      if(map.manhat(pathnodes.get(i),corner)<=8){
        //TODO: then go find another corner by restricting these pathnodes.
        // if we cannot find a path, simply unrestricted the last node and restrict all
        // tiles traversed previously. Repeat this recursively until we can find another path.
        //TODO: we should also count how many troops we have in order to see if we can make
        // it to the corner.
        pathnodes=pathnodes.subList(0,i+1);
        break;
      }
    }
    Attack.fromNodes(pathnodes,availableTurn()).forEach(att->turnBasedAttacks.put(att.turn,att));
  }

  private int availableTurn() {
    return Collections.max(turnBasedAttacks.keySet())+1;
  }

  private void simulate(int start,int end, SimResult result,List<Attack> atk,
      java.util.Map<List<Attack>,Integer>all) {
    for(int turn=start; turn<=end; turn++){
      simulate(turn+1,end,Simulator.sim(new ArrayList<>(),result,turn,map,turn+1),atk,all);
      if(result.troops[map.generals[map.generalme]]<=1){
        return;
      }
      //add code
      Set<List<Attack>> attacks=new HashSet<>();
      allAttacks(map.generals[map.generalme],turn,result,new ArrayList<>(),attacks,end);

      for(List<Attack> attack:attacks){
        int turnsElapsed=Math.min(attack.size(),end-start+1);
        for(int i=0; i<turnsElapsed; i++){
          atk.add(attack.get(i));
        }
        simulate(turn+Math.max(turnsElapsed,1),end,
            Simulator.sim(attack,result,turn,map,turn+turnsElapsed),new ArrayList<>(atk),all);
      }
    }

    all.put(atk,result.land);//TODO: make better scorefunc than owned land
  }

  private void allAttacks(int position,int turn,SimResult result,
      List<Attack> attack,Set<List<Attack>> attacks,int end){
    if(result.troops[position]==1||turn==end+1){
      attacks.add(attack);
      return;
    }
    List<Attack> possibleAttacks=possibleAttacks(position,turn);
    if(possibleAttacks.isEmpty()){
      attacks.add(attack);
      return;
    }
    SimResult initial=result;
    for(Attack a:possibleAttacks){
      result=Simulator.sim(List.of(a),initial,turn,map,turn+1);
      //      System.out.println("land!"+result.land);
      List<Attack> atkCopy=new ArrayList<>(attack);
      atkCopy.add(a);
      allAttacks(a.end,turn+1,result,atkCopy,attacks,end);
    }
  }

  private List<Attack> possibleAttacks(int position,int turn) {
    List<Attack> attacks=new ArrayList<>();
    for(int dx:map.dxs(position)){
      if(map.legalMove(position,dx)){
        attacks.add(new Attack(position,dx,false,turn));
      }
    }
    return attacks;
  }

  private void updateWithStemV2(Branch branch, int destination,int maxbranchsize,
      int mindelay,int maxdelay){
    List<Integer> branchList=branch.getBranch();
    if(mindelay<1) mindelay=1;
    if(maxdelay>branchList.size()-1) maxdelay=branchList.size()-1;
    for(int i=mindelay; i<maxdelay+1; i++) {
      //adds the delay to branchsize
      branchTo(new ArrayList<>(branchList.subList(0,i+1)),destination,maxbranchsize+i,
          new HashSet<>(branchList)).ifPresent(stem->updateBestPlant(branch,stem));
    }
  }

  private Optional<Branch> branch(List<Integer> branch, final int maxbranchsize,
      Set<Integer> restricted) {
    if(branch.isEmpty()) {
      return Optional.empty();
    }
    if(branch.size()>=maxbranchsize) {
      return Optional.of(new Branch(branch));
    }

    int currnode=branch.getLast();
    double max=-Integer.MAX_VALUE;
    for(int dx:map.dxs(currnode)){
      if(restricted.contains(dx)||!map.legalMove(currnode,dx)){
        continue;
      }
      max=Math.max(max,nodeScore(dx));
    }
    if(max==-Integer.MAX_VALUE){
      restricted.add(branch.removeLast());
      return branch(branch,maxbranchsize,new HashSet<>(restricted));
    }

    List<Branch> competitionBranches=new ArrayList<>();
    for(int dx:map.dxs(currnode)){
      if(restricted.contains(dx)||!map.legalMove(currnode,dx)
          || Double.compare(nodeScore(dx),max)!=0){
        continue;
      }
      List<Integer> b=new ArrayList<>(branch);
      Set<Integer> r=new HashSet<>(restricted);
      b.add(dx);
      r.add(dx);
      branch(b,maxbranchsize,r).ifPresent(competitionBranches::add);
    }

    return competitionBranches.stream().max(Comparator.comparingDouble(challenger->
        branchScore.score(map,challenger)));
  }

  private double nodeScore(int node) {
    return branchScore.score(map,new Branch(List.of(node)));
  }

  private Optional<Branch> branchTo(List<Integer> start,int destination,int maxbranchsize,
      Set<Integer> restricted){
    if(start.isEmpty()) return Optional.empty();
    restricted=new HashSet<>(restricted);
    Optional<List<Integer>> olist=AStar.getPath(start.getLast(),destination,emptyNodes,map,
        restricted);
    if(olist.isEmpty()) return Optional.empty();
    List<Integer> branch=olist.get();
    branch.addAll(0,start.subList(0,start.size()-1));
    if(branch.size()>maxbranchsize) return Optional.of(new Branch(branch.subList(0,maxbranchsize)));
    restricted.addAll(branch);
    return branch(branch,maxbranchsize,restricted);
  }
}
