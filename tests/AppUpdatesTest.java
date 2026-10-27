import cn.pi.bilipoints.*;import org.json.*;
public class AppUpdatesTest {
 public static void main(String[] args)throws Exception{
  String prefix="{\"version_code\":16,\"version_name\":\"0.4.5-beta\",\"apk_url\":\"";
  AppUpdates a=new AppUpdates(new JSONObject(prefix+"https://github.com/lyz10-26/BiliPoints/raw/refs/heads/main/BiliPoints-0.4.5-beta.apk\"}"));
  if(a.code!=16)throw new AssertionError();
  for(String url:new String[]{"https://evil.example/a.apk","https://github.com/other/repo/a.apk","https://github.com/lyz10-26/BiliPoints/raw/refs/heads/main/../evil.apk"}){boolean bad=false;try{new AppUpdates(new JSONObject(prefix+url+"\"}"));}catch(Exception e){bad=true;}if(!bad)throw new AssertionError("Untrusted URL accepted");}
  String manifest=prefix+"https://github.com/lyz10-26/BiliPoints/raw/refs/heads/main/BiliPoints-0.4.5-beta.apk\"}";
  int[] count={0};AppUpdates fallback=AppUpdates.fetch(url->{count[0]++;if(url.equals(AppUpdates.INDEX))throw new java.net.SocketTimeoutException();return new JSONObject().put("encoding","base64").put("content",java.util.Base64.getEncoder().encodeToString(manifest.getBytes("UTF-8"))).toString();});
  if(count[0]!=2||fallback.code!=16)throw new AssertionError("API fallback must decode official contents response");
  count[0]=0;AppUpdates.fetch(url->{count[0]++;return manifest;});if(count[0]!=1)throw new AssertionError("healthy primary must not issue extra requests");
  boolean failed=false;try{AppUpdates.fetch(url->{throw new java.net.UnknownHostException();});}catch(Exception e){failed=e.getMessage().contains("主更新源")&&e.getMessage().contains("备用更新源")&&e.getMessage().contains("域名解析");}if(!failed)throw new AssertionError("both endpoint failures retain diagnostic causes");
  if(!AppUpdates.describe(new java.net.SocketTimeoutException()).contains("超时"))throw new AssertionError();
  System.out.println("PASS: update version fields and repository-only APK URLs");
 }
}
