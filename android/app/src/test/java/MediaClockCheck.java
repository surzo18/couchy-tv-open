import com.conreo.couchytv.MediaClock;
public class MediaClockCheck {
 static int checks=0;
 static void test(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
 public static void main(String[]args){
  var playing=new MediaClock(84000,250000,1000,1f,true);
  test(playing.label(1000).equals("1:24 / 4:10"),"elapsed/total");
  test(playing.positionAt(5000)==88000,"advances locally");
  test(playing.positionAt(0)==84000,"future anchor safe");
  test(playing.positionAt(999999)==250000,"duration clamp");
  test(new MediaClock(84000,250000,1000,1f,false).positionAt(5000)==84000,"paused or buffering freezes");
  test(new MediaClock(1000,10000,1000,2f,true).positionAt(2000)==3000,"speed");
  test(new MediaClock(-1,10000,1000,1f,true).label(2000).isEmpty(),"unknown position hidden");
  test(new MediaClock(84000,0,1000,1f,false).label(1000).equals("1:24"),"unknown duration");
  test(new MediaClock(0,250000,5000,1f,true).positionAt(5000)==0,"new track resets");
  test(new MediaClock(1000,10000,1000,Float.NaN,true).positionAt(2000)==1000,"invalid speed");
  test(MediaClock.Companion.format(3661000).equals("1:01:01"),"hours");
  test(new MediaClock(60000,250000,5000,1f,true).positionAt(6000)==61000,"seek anchor");
  System.out.println("PASS "+checks+" media clock checks");
 }
}
