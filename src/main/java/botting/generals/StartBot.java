package botting.generals;

import botting.start.Branch;
import botting.start.BranchScore;
import botting.start.DistBranchScore;
import botting.start.Plant;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

import static botting.generals.GeneralSocket.FAST_GAME_SPEED;

public class StartBot extends Bot {
  private Map map;
  private boolean findStart;
  private PrintWriter out;
  private Plant bestPlant;
  private BranchScore branchScore;
  private java.util.Map<Integer,Attack> turnBasedAttacks;
  private PrintWriter bashout;
  // 13-long branch including general. 7-long branch including general, padded by delays with stems.
  // there is no point in doing a 14 long branch because turn stalling exists.
  private static final int LONG_BRANCH=13,SHORT_BRANCH=7;
  public StartBot() {
    reset();
  }

  @Override
  public void processSocketMessage(SocketMessage message) {
    if(message.type==SocketMessageType.CONNECTED) {
      try(BufferedReader br=new BufferedReader(new FileReader("room.in"))) {
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
          if(!gamedata.getJSONObject("options").has("game_speed") ||
              gamedata.getJSONObject("options").getInt("game_speed")!=FAST_GAME_SPEED){
            socket.fast(custom);
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
      socket.attack(a);
    }
  }

  private void reset(){
    map=new Map();
    branchScore=new DistBranchScore();
    turnBasedAttacks=new HashMap<>();
    bestPlant=new Plant();
    findStart=true;
    try {
      out=new PrintWriter(new BufferedWriter(new FileWriter("branch.out")));
      bashout=new PrintWriter(new BufferedWriter(new FileWriter("bash.out")));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void findStart() {
    //well, lets first try to find the possible branch routes.
    int general=map.generals[map.generalme];
    System.out.println(general);
    int adjpermeable=0;
    List<Branch> branch13s=new ArrayList<>();

    //We restrict all adjacent tiles from the general because:
    // let tilex be an adjacent tile.
    // we can simply start from tilex instead of stepping on it using another branch.
    Set<Integer> initialRestrictions=new HashSet<>();
    initialRestrictions.add(general);
    for(int dx:map.dxs()){
      initialRestrictions.add(general+dx);
    }
    //We will try to compute all the 13 branches.
    for(int dx:map.dxs()){
      if(map.legalMove(general,general+dx)){
        adjpermeable++;
        getBranch(List.of(general,general+dx),LONG_BRANCH,initialRestrictions)
            .ifPresent(branch13s::add);
      }
    }

    //Printing the `13` branches.
    System.out.println(branch13s.size());
    for(Branch branch:branch13s){
      out.println(branch);
    }
    out.close();

    if(branch13s.isEmpty()){
      System.err.println("something went really wrong... we did not get a single branch13!");
      socket.surrender();
      return;
    }
    //try to find a short stem from long branch with 1-2 delay
    if(stemBranchBash(branch13s,SHORT_BRANCH,1,2)){
      return;
    }
    //cave spawn, give up
    if(adjpermeable==1){
      socket.surrender();
      return;
    }
    // with 3 or more adjacent permeable tiles, try to find a double branch solve
    if(adjpermeable!=2) {
      if (!twoBranchBash(branch13s, general, initialRestrictions)){
        socket.surrender();
      }
      return;
    }
    // with 2 adjacent permeable tiles, try to find a short stem from long branch with 3-5 delay
    if(stemBranchBash(branch13s,SHORT_BRANCH,3,5)){
      return;
    }
    // with 2 adjacent permeable tiles, try to find a short branch not stemming from long branch
    if(!twoBranchBash(branch13s,general,initialRestrictions)){
      socket.surrender();
    }
  }

  /**
   * @param branchsize the length of the branch AFTER the delays.
   * */
  private boolean stemBranchBash(List<Branch> branch13s,int branchsize,
      int mindelay,int maxdelay){
    for(Branch b13:branch13s) {
      updateWithStem(b13,branchsize,mindelay,maxdelay);
    }
    return bash();
  }

  private boolean twoBranchBash(List<Branch>branch13s,int general,
      Set<Integer> initialRestrictions) {
    best2Branch(branch13s,general,initialRestrictions);
    return bash();
  }

  private void best2Branch(List<Branch>branch13s,int general,Set<Integer> initialRestrictions){
    for(Branch b13:branch13s) {
      for(int dx:map.dxs()){
        if(!map.legalMove(general,general+dx)||b13.getBranch().contains(general+dx)) {
          continue;
        }
        getBranch(List.of(general,general+dx),SHORT_BRANCH,initialRestrictions).ifPresent(b6 ->
            updateBestPlant(b13,b6));
      }
    }
  }

  private void updateBestPlant(Branch b13,Branch b6) {
    Plant challenger=new Plant(branchScore.score(map,b13)+ branchScore.score(map,b6),b13,b6);
    updateBestPlant(challenger);
  }

  private void updateBestPlant(Plant challenger){
    if(challenger.compare(bestPlant)>0){
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
    bestPlant=new Plant(bestPlant.getScore(),sortedBranches.toArray(new Branch[0]));
    System.out.println(bestPlant);
    int turn=0;
    Set<Integer> owned=new HashSet<>();
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

      owned.addAll(sortedBranches.get(i).getBranch());
    }
    int lastatksize=sortedBranches.getLast().getBranch().size();
    int generaltroops=1+income(turn,turn+lastatksize-1);
    turn+=lastatksize;
    System.out.println("bash starting at "+turn);
    java.util.Map<List<Attack>,Integer> all=new HashMap<>();
    simulate(turn,50,generaltroops,owned,new ArrayList<>(),all);
    all.keySet().stream().max(Comparator.comparingInt(all::get)).get()
        .forEach(atk->turnBasedAttacks.put(atk.turn,atk));
    System.out.println("====================");
    System.out.println(turnBasedAttacks);
    System.out.println("estimated start land: "+
        all.get(all.keySet().stream().max(Comparator.comparingInt(all::get)).get()));
    return true;
  }

  private void simulate(int start,int end,int generaltroops,Set<Integer> owned,List<Attack> atk,
      java.util.Map<List<Attack>,Integer>all) {
    for(int turn=start; turn<=end; turn++){
      //add code
      if(generaltroops>1){
        Set<List<Attack>> attacks=new HashSet<>();
        allAttacks(map.generals[map.generalme],turn,generaltroops,owned,
            new ArrayList<>(),attacks);
        for(List<Attack> attack:attacks){
          Set<Integer> ownedCopy=new HashSet<>(owned);
          int turnsElapsed=Math.min(attack.size(),end-start+1);
          generaltroops=1+income(turn,turn+turnsElapsed);
          for(int i=0; i<turnsElapsed; i++){
            ownedCopy.add(attack.get(i).end);
            atk.add(attack.get(i));
          }
          simulate(turn+turnsElapsed,end,generaltroops,ownedCopy,
              new ArrayList<>(atk),all);
        }
      }
      if(turn%2==0){
        generaltroops++;
      }
    }
//    bashout.println("putting atk: "+atk);
//    bashout.flush();
    all.put(atk,owned.size());//TODO: make better scorefunc than owned land
  }
/**
 * generates general income from start (inclusive) to end (exclusive)
 * */
  private int income(int start,int end){
    int income=0;
    for(int i=start; i<end; i++){
      if(i%2==0){
        income++;
      }
    }
    return income;
  }

  private void allAttacks(int position,int turn,int troops,
      Set<Integer> owned,List<Attack> attack,Set<List<Attack>> attacks){
    if(troops==1||turn==51){
      attacks.add(attack);
      return;
    }
    List<Attack> possibleAttacks=possibleAttacks(position,turn);
    if(possibleAttacks.isEmpty()){
      attacks.add(attack);
      return;
    }
    for(Attack a:possibleAttacks){
      Set<Integer> ownedCopy=new HashSet<>(owned);
      ownedCopy.add(a.end);
      List<Attack> atkCopy=new ArrayList<>(attack);
      atkCopy.add(a);
      allAttacks(a.end,turn+1,troops-(owned.contains(a.end)?0:1)
          ,ownedCopy,atkCopy,attacks);
    }
  }

  private List<Attack> possibleAttacks(int position,int turn) {
    List<Attack> attacks=new ArrayList<>();
    for(int dx:map.dxs()){
      if(map.legalMove(position,position+dx)){
        attacks.add(new Attack(position,position+dx,false,turn));
      }
    }
    return attacks;
  }

  private void updateWithStem(Branch branch, int branchsize, int mindelay,int maxdelay){
    List<Integer> branchList=branch.getBranch();
    if(mindelay<1) mindelay=1;
    if(maxdelay>branchList.size()-1) maxdelay=branchList.size()-1;
    for(int i=mindelay; i<maxdelay+1; i++) {
      //adds the delay to branchsize
      findStemBranch(branch,new ArrayList<>(branchList.subList(0,i+1)),
          branchsize+i,branchScore).ifPresent(this::updateBestPlant);
    }
  }

  private Optional<Branch> getBranch(List<Integer> start,int branchsize,Set<Integer> restrictions) {
    // don't plug in start because it uses list.of()
    List<Integer> branch=new ArrayList<>(start);
    return branch(branch,branchsize,new HashSet<>(restrictions));
  }

  private Optional<Plant> findStemBranch(Branch b13,List<Integer> start,int branchsize,
      BranchScore scoreFunc){
    Set<Integer> restricted=new HashSet<>(b13.getBranch());
    List<Integer> branch = new ArrayList<>(start);
    restricted.addAll(start);
    Optional<Branch> obranchn=branch(branch,branchsize,restricted);
    if(obranchn.isEmpty()|| !new HashSet<>(obranchn.get().getBranch()).containsAll(start)){
      return Optional.empty();
    }
    return obranchn.map(branchn -> new Plant(
        scoreFunc.score(map, b13) + scoreFunc.score(map, branchn),b13,branchn));
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
    int general=map.generals[map.generalme];
    double max=-Integer.MAX_VALUE;
    for(int dx:map.dxs()){
      if(restricted.contains(currnode+dx)||!map.legalMove(currnode,currnode+dx)){
        continue;
      }
      max=Math.max(max,map.dist(general,currnode+dx));
    }
    if(max==-Integer.MAX_VALUE){
      restricted.add(branch.removeLast());
      return branch(branch,maxbranchsize,new HashSet<>(restricted));
    }

    List<Branch> competitionBranches=new ArrayList<>();
    for(int dx:map.dxs()){
      if(restricted.contains(currnode+dx)||!map.legalMove(currnode,currnode+dx)
          || Double.compare(map.dist(general,currnode+dx),max)!=0){
        continue;
      }
      List<Integer> b=new ArrayList<>(branch);
      Set<Integer> r=new HashSet<>(restricted);
      b.add(currnode+dx);
      r.add(currnode+dx);
      branch(b,maxbranchsize,r).ifPresent(competitionBranches::add);
    }

    return competitionBranches.stream().max(Comparator.comparingDouble(challenger->
        branchScore.score(map,challenger)));
  }
}
