import cn.pi.bilipoints.*;
public class TapBurstTest {
 static void ok(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args){
  TapBurst t=new TapBurst();for(int i=0;i<4;i++)ok(!t.tap(i*400),"four taps cannot trigger");ok(t.tap(2000),"fifth at inclusive two second boundary triggers");ok(!t.tap(2001),"trigger resets burst");
  t=new TapBurst();for(int i=0;i<5;i++)ok(!t.tap(i*600),"pairwise fast taps spanning over two seconds cannot trigger");
  ok(t.tap(2500),"sliding last five taps within two seconds can trigger");
  t=new TapBurst();t.tap(5000);t.tap(5100);t.tap(100);ok(!t.tap(101)&&!t.tap(102)&&!t.tap(103)&&t.tap(104),"clock rollback starts a new burst");
  System.out.println("PASS: five taps within two seconds, boundary, sliding window, reset and monotonic rollback");
 }
}
