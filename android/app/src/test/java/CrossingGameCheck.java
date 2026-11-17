import com.conreo.couchytv.game.*;
public class CrossingGameCheck {
 static int checks=0;
 static void check(boolean v,String s){if(!v)throw new AssertionError(s);checks++;}
 static void tick(CrossingGame g){for(int i=0;i<4;i++)g.update(.05f);}
 static void field(CrossingGame g,String n,Object v)throws Exception{var f=CrossingGame.class.getDeclaredField(n);f.setAccessible(true);f.set(g,v);}
 public static void main(String[]args)throws Exception{
  var g=new CrossingGame(123L);
  check(!g.move(0,1),"paused movement");g.update(1f);check(g.getTime()==0,"paused clock");
  g.setPaused(false);check(!g.move(1,1),"diagonal rejected");check(!g.move(0,-1),"bottom bound");
  check(g.move(0,1)&&g.getScore()==1&&g.getCoins()==1,"first coin and score");
  check(!g.move(0,-1),"repeat cooldown");tick(g);check(g.move(0,-1),"backwards movement");tick(g);
  check(g.move(0,1)&&g.getCoins()==1&&g.getScore()==1,"no repeated coin or score");
  double t=g.getTime();g.setPaused(true);tick(g);check(g.getTime()==t,"pause freezes hazards");
  var g2=new CrossingGame(123L);check(g.lane(9).equals(g2.lane(9)),"stable lane generation");
  g.restart(99L);check(!g.getDead()&&g.getCoins()==0&&g.getScore()==0&&g.getX()==4f,"restart reset");
  var road=g.lane(2);float car=road.start(0)+road.getGap()+road.getLength()/2;
  field(g,"row",2);field(g,"x",car-.5f);g.update(0f);check(g.getDead(),"car collision");
  g.restart(99L);var river=g.lane(4);float log=river.start(0)+river.getGap()+river.getLength()/2;
  field(g,"row",4);field(g,"x",log-.5f);float before=g.getX();g.update(.05f);
  check(!g.getDead()&&Math.abs(g.getX()-before-river.getDirection()*river.getSpeed()*.05f)<.0001,"river carries player");
  g.restart(99L);field(g,"x",4.37f);check(g.move(0,1)&&Math.abs(g.getX()-4.37f)<.0001,"vertical hop preserves horizontal position");
  g.restart(99L);field(g,"row",4);field(g,"x",river.start(0)+river.getGap()+river.getLength()+.3f-.5f);g.update(0f);check(g.getDead(),"water collision");
  g.restart(5L);for(int i=0;i<1000;i++)g.lane(i);g.move(0,1);check(g.getCachedLaneCount()<=26,"lane pruning");
  g.restart(10L);field(g,"coins",5);check(g.getDifficulty()==2,"coins increase difficulty");
  g.restart(10L);field(g,"x",.37f);check(g.move(-1,0)&&g.getX()==0f,"fractional left edge reachable");tick(g);check(!g.move(-1,0),"left boundary stays bounded");
  g.restart(10L);field(g,"x",7.63f);check(g.move(1,0)&&g.getX()==8f,"fractional right edge reachable");tick(g);check(!g.move(1,0),"right boundary stays bounded");
  field(g,"score",1000);check(g.getDifficulty()==10,"difficulty capped");
  for(int i=0;i<100;i++)g.update(.05f);check(g.getTime()>6,"pace increases continuously");
  g.restart(10L);var rail=g.lane(42);check(rail.getKind()==3&&g.lane(41).getKind()==0&&g.lane(43).getKind()==0,"railway safe banks");
  double railStart=(8.8-rail.getPhase())%8.8;
  check(rail.warning(railStart+7)&&!rail.warning(railStart+3),"train warning window");
  float train=rail.trainX(railStart+.7);field(g,"row",42);field(g,"time",railStart+.7);field(g,"x",train+4f);g.update(0f);check(g.getDead(),"train collision");
  g.restart(10L);field(g,"row",42);field(g,"time",railStart+3);g.update(0f);check(!g.getDead(),"inactive railway safe");
  boolean balanced=true;for(int i=1;i<10000;i++){var lane=g.lane(i);balanced &= lane.getGap()>lane.getLength()+1f && lane.getLength()>=1f && lane.getSpeed()<1.5f;}
  check(balanced,"bounded traversable lane spacing");
  var skins=CrossingCollection.INSTANCE.getSkins();check(skins.size()==20&&skins.stream().map(x->x.getId()).distinct().count()==20,"20 unique skins");
  check(skins.get(1).getPrice()==12&&skins.get(4).getPrice()==75,"old skin prices preserved");
  var challenges=CrossingChallenges.INSTANCE.getAll();check(challenges.size()==14&&challenges.stream().map(x->x.getId()).distinct().count()==14,"14 unique achievements");
  System.out.println("PASS "+checks+" game simulation checks");
 }
}
