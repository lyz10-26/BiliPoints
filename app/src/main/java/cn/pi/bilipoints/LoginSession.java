package cn.pi.bilipoints;
import org.json.*;import java.net.URI;
/** A cookie or redirect alone is not proof of a successful login. */
public final class LoginSession {
 public static boolean shouldProbe(String initial,String current,String page){
  BiliApi session=new BiliApi(current);if(!session.authenticated())return false;
  String before=BiliApi.value(initial==null?"":initial,"SESSDATA"),after=BiliApi.value(current,"SESSDATA");
  if(before.isEmpty()||!before.equals(after))return true;
  try{URI u=new URI(page==null?"":page);String host=u.getHost(),path=u.getPath();
   return "https".equals(u.getScheme())&&host!=null&&(host.equals("bilibili.com")||host.endsWith(".bilibili.com"))&&!host.startsWith("passport.")&&(path==null||!path.toLowerCase(java.util.Locale.ROOT).contains("login"));
  }catch(Exception e){return false;}
 }
 public static boolean verified(JSONObject response){
  if(response==null||response.optInt("code",-1)!=0)return false;JSONObject data=response.optJSONObject("data");
  return data!=null&&data.optBoolean("isLogin",false)&&data.optLong("mid",0)>0;
 }
}
