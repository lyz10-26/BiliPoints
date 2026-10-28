import cn.pi.bilipoints.*;import org.json.*;
public class LoginSessionTest {
 static void ok(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  String a="SESSDATA=old; bili_jct=csrf",b="SESSDATA=new; bili_jct=csrf";
  ok(!LoginSession.shouldProbe("","bili_jct=csrf","https://www.bilibili.com"),"redirect without session cannot complete login");
  ok(!LoginSession.shouldProbe("","SESSDATA=new","https://www.bilibili.com"),"wait for complete session cookies");
  ok(!LoginSession.shouldProbe(a,a,"https://passport.bilibili.com/login"),"existing session must not dismiss switch-account page");
  ok(LoginSession.shouldProbe(a,b,"https://passport.bilibili.com/login"),"SPA login with changed session works without navigation");
  ok(LoginSession.shouldProbe("",b,"https://passport.bilibili.com/login"),"new login can be verified while callback page remains");
  ok(LoginSession.shouldProbe(a,a,"https://www.bilibili.com/"),"existing valid session redirected by official login can return");
  ok(!LoginSession.shouldProbe(a,a,"https://bilibili.com.evil.test/"),"untrusted redirect cannot trigger old-session return");
  ok(!LoginSession.shouldProbe(a,a,"https://www.bilibili.com/login"),"login page alone cannot trigger old-session return");
  ok(LoginSession.verified(new JSONObject("{\"code\":0,\"data\":{\"isLogin\":true,\"mid\":123}}")),"authenticated server profile is required");
  for(String s:new String[]{"{}","{\"code\":-101}","{\"code\":0,\"data\":{\"isLogin\":false,\"mid\":123}}","{\"code\":0,\"data\":{\"isLogin\":true}}"})ok(!LoginSession.verified(new JSONObject(s)),"failed or incomplete response must stay on login");
  ok(!new BiliApi(a).sameSession(new BiliApi(b)),"late old-session response cannot close a newer login");
  System.out.println("PASS: SPA and redirect login, old-session switch guard, incomplete cookies, server verification and stale-session rejection");
 }
}
