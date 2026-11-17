import com.conreo.couchytv.game.*;
import java.time.LocalDate;
public class ScooterGameCheck {
 static int checks=0;
 static void check(boolean v,String s){if(!v)throw new AssertionError(s);checks++;}
 static void field(Object g,String n,Object v)throws Exception{var f=g.getClass().getDeclaredField(n);f.setAccessible(true);f.set(g,v);}
 static void tick(ScooterGame g,int n){for(int i=0;i<n;i++)g.update(1f/120);}
 static ScooterGame start(){var g=new ScooterGame(123L);g.setPaused(false);return g;}
 public static void main(String[]args)throws Exception{
  var g=new ScooterGame(123L);check(!g.jump(),"paused jump");g.update(.05f);check(g.getX()==160,"paused physics");
  g.setPaused(false);tick(g,30);check(g.getX()>220 && g.getGrounded(),"automatic forward and grounded");
  check(g.jump(),"first jump");check(!g.jump(),"repeat suppression");tick(g,25);check(g.jump() && g.getTricks()==1,"second jump trick");tick(g,25);check(!g.jump(),"no third jump");
  tick(g,180);check(g.getGrounded()&&!g.getDead()&&g.getLandings()==1,"swept landing");
  g.setPaused(true);float x=g.getX(),t=g.getTime();tick(g,120);check(g.getX()==x&&g.getTime()==t,"pause freezes all");
  g.restart(123);check(g.getX()==160&&g.getCoins()==0&&g.getTricks()==0&&!g.getDead()&&!g.getPaused(),"restart resets");
  var slow=start();slow.setBrake(true);tick(slow,100);var fast=start();fast.setBoost(true);tick(fast,100);check(slow.getX()<g.getX()+200&&fast.getSpeed()>slow.getSpeed(),"brake and boost");
  g=start();field(g,"coins",120);field(g,"x",20000f);check(g.getDifficulty()==10,"difficulty capped");
  for(int seed=1;seed<=50;seed++){
   g=new ScooterGame(seed);g.setPaused(false);
   // Independent of player skill: inspect generated future pieces at widely spaced positions.
   for(int pos=160;pos<30000;pos+=800){field(g,"x",(float)pos);field(g,"dead",false);field(g,"paused",false);field(g,"y",500f);field(g,"vy",0f);g.update(0);
    check(g.getPlatforms().size()<12,"track memory bound");
    for(var p:g.getPlatforms()){check(p.getEnd()-p.getStart()>=440 && p.getY()>=650&&p.getY()<=810,"platform dimensions");}
   }
  }
  g=start();g.getPlatforms().clear();g.getPlatforms().add(new ScooterGame.Platform(0,0,5000,760,1,190,new float[0],0));tick(g,20);check(g.getDead(),"cone collision");
  g=start();g.getPlatforms().clear();g.getPlatforms().add(new ScooterGame.Platform(0,0,5000,760,3,210,new float[0],0));g.duck();tick(g,60);check(!g.getDead(),"duck clears overhead barrier");
  g=start();g.getPlatforms().clear();g.getPlatforms().add(new ScooterGame.Platform(0,0,5000,760,3,210,new float[0],0));tick(g,60);check(g.getDead(),"standing hits overhead barrier");
  g=start();g.getPlatforms().clear();g.getPlatforms().add(new ScooterGame.Platform(0,0,5000,760,0,0,new float[]{190},0));tick(g,30);check(g.getCoins()==1,"coin pickup once");field(g,"x",160f);tick(g,30);check(g.getCoins()==1,"coin cannot repeat");
  g=start();g.getPlatforms().clear();tick(g,160);check(g.getDead(),"gap fall");
  var a=start();var b=start();a.jump();b.jump();for(int i=0;i<40;i++)a.update(1f/60);for(int i=0;i<20;i++)b.update(1f/30);check(Math.abs(a.getX()-b.getX())<.01&&Math.abs(a.getY()-b.getY())<.01,"frame-rate independent substeps");
  var date=LocalDate.of(2026,11,14);var d=CrossingDaily.INSTANCE.forDate(date);
  check(d.equals(CrossingDaily.INSTANCE.forDate(date)),"daily stable");
  check(d.getSeed()!=CrossingDaily.INSTANCE.forDate(date.plusDays(1)).getSeed(),"daily rotates");
  check(!CrossingDaily.INSTANCE.eligible(d,Long.MIN_VALUE,d.getRows(),d.getCoins()-1),"daily needs both goals");
  check(CrossingDaily.INSTANCE.eligible(d,Long.MIN_VALUE,d.getRows(),d.getCoins()),"daily reward available");
  check(!CrossingDaily.INSTANCE.eligible(d,d.getEpoch(),999,999),"daily reward once");
  check(!CrossingDaily.INSTANCE.eligible(d,d.getEpoch()+1,999,999),"clock rollback no duplicate reward");
  System.out.println("PASS "+checks+" scooter/daily checks");
 }
}
