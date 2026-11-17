import com.conreo.couchytv.MemoryRotation;
import com.conreo.couchytv.MemoryChoice;
import java.util.*;
public class MemoryRotationCheck {
  static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
  public static void main(String[] args){
    var ids=new LinkedHashSet<String>(); for(int i=0;i<191;i++)ids.add("photo-"+i);
    Set<String> seen=new HashSet<>();String last="";
    for(int round=0;round<4;round++){
      Set<String> drawn=new HashSet<>();
      for(int i=0;i<ids.size();i++){
        MemoryChoice c=MemoryRotation.INSTANCE.next(ids,seen,last);
        check(!c.getSelected().equals(last),"Adjacent repeat");
        check(drawn.add(c.getSelected()),"Repeat inside cycle");
        check(ids.containsAll(c.getSeen()),"Stale ID retained");
        // A new copied history simulates restoring persisted state at every step.
        seen=new HashSet<>(c.getSeen());last=c.getSelected();
      }
      check(drawn.equals(ids),"Incomplete cycle");
    }
    check(MemoryRotation.INSTANCE.next(Set.of(),seen,last)==null,"Empty album");
    var one=MemoryRotation.INSTANCE.next(Set.of("only"),seen,last);
    check(one.getSelected().equals("only")&&one.getSeen().equals(Set.of("only")),"Single photo or stale cleanup");
    var changed=MemoryRotation.INSTANCE.next(Set.of("old","new"),Set.of("old","removed"),"old");
    check(changed.getSelected().equals("new")&&!changed.getSeen().contains("removed"),"Changed album");
    System.out.println("PASS: 4 complete 191-photo cycles, persisted-history copies, no adjacent repeats, empty/single/changed album");
  }
}
