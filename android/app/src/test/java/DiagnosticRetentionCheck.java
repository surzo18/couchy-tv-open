import com.conreo.couchytv.DiagnosticEvent;
import com.conreo.couchytv.DiagnosticRetention;
import java.util.*;

public class DiagnosticRetentionCheck {
    static int checks;
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
    static DiagnosticEvent event(long at){return new DiagnosticEvent(at,"Test","INFO","Synthetic retention check","");}
    public static void main(String[] args){
        long now=1800000000000L, age=DiagnosticRetention.MAX_AGE;
        var policy=DiagnosticRetention.INSTANCE;
        var result=policy.retain(List.of(event(now-age-1),event(now-age),event(now),event(now+1)),now);
        check(result.size()==2,"Expired and future entries removed");
        check(result.get(0).getAt()==now-age,"Exact 30-day boundary retained");
        check(result.get(1).getAt()==now,"Current entry retained");
        var many=new ArrayList<DiagnosticEvent>();
        for(int i=999;i>=0;i--)many.add(event(now-i));
        var capped=policy.retain(many,now);
        check(capped.size()==400,"Hard count cap");
        check(capped.get(0).getAt()==now-399,"Newest 400 selected from unordered input");
        check(capped.get(399).getAt()==now,"Newest retained");
        check(policy.retain(capped,now+age+1).isEmpty(),"All events expire after 30 days");
        check(policy.retain(List.of(),now).isEmpty(),"Empty input safe");
        System.out.println("PASS "+checks+" retention checks (no TV data changed)");
    }
}
