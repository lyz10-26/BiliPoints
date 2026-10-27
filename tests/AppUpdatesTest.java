import cn.pi.bilipoints.*;import org.json.*;
public class AppUpdatesTest {
 public static void main(String[] args)throws Exception{
  String prefix="{\"version_code\":16,\"version_name\":\"0.4.5-beta\",\"apk_url\":\"";
  AppUpdates a=new AppUpdates(new JSONObject(prefix+"https://github.com/lyz10-26/BiliPoints/raw/refs/heads/main/BiliPoints-0.4.5-beta.apk\"}"));
  if(a.code!=16)throw new AssertionError();
  for(String url:new String[]{"https://evil.example/a.apk","https://github.com/other/repo/a.apk","https://github.com/lyz10-26/BiliPoints/raw/refs/heads/main/../evil.apk"}){boolean bad=false;try{new AppUpdates(new JSONObject(prefix+url+"\"}"));}catch(Exception e){bad=true;}if(!bad)throw new AssertionError("Untrusted URL accepted");}
  System.out.println("PASS: update version fields and repository-only APK URLs");
 }
}
