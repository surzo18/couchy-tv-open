import org.junit.Test;

/** Existing regression scenarios are now part of the standard Gradle test task. */
public class RegressionHarnessTest {
    @Test public void crossingCollisionsAndProgression() throws Exception { CrossingGameCheck.main(new String[0]); }
    @Test public void scooterPhysicsAndProgression() throws Exception { ScooterGameCheck.main(new String[0]); }
    @Test public void diagnosticRetention() { DiagnosticRetentionCheck.main(new String[0]); }
    @Test public void mediaClock() throws Exception { MediaClockCheck.main(new String[0]); }
    @Test public void photoRotation() throws Exception { MemoryRotationCheck.main(new String[0]); }
}
