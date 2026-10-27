package cn.pi.bilipoints;
import org.json.*;
import java.net.*;
import java.io.*;
public class BiliApi {
 public static final String CENTER="https://big.bilibili.com/mobile/bigPoint/task";
 private final String cookie,csrf;
 public BiliApi(String cookie){this.cookie=cookie==null?"":cookie;csrf=value(this.cookie,"bili_jct");}
 public static String value(String ck,String key){for(String p:ck.split(";")){String[] s=p.trim().split("=",2);if(s.length==2&&s[0].equals(key))return s[1];}return "";}
 public String csrf(){return csrf;}
 public boolean authenticated(){return !value(cookie,"SESSDATA").isEmpty()&&!csrf.isEmpty();}
 public static String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
 public static final String CLIENT_BUILD="8451100",CLIENT_VERSION="8.45.1";
 public String clientQuery()throws Exception{return "build="+CLIENT_BUILD+"&mobi_app=android&platform=android&statistics="+enc("{\"appId\":1,\"platform\":3,\"version\":\""+CLIENT_VERSION+"\",\"abtest\":\"\"}");}
 String clientPath(String p)throws Exception{return p+(p.contains("?")?"&":"?")+clientQuery();}
 public JSONObject get(String path)throws Exception{return call(clientPath(path),null,false);}
 public JSONObject form(String path,String body)throws Exception{return call(clientPath(path),body+"&"+clientQuery(),false);}
 public JSONObject json(String path,JSONObject body)throws Exception{return call(clientPath(path),body.toString(),true);}
 public JSONObject combine()throws Exception{return get("/x/vip_point/task/combine?csrf="+enc(csrf)+"");}
 public EpisodeChoice latestFanren()throws Exception{return EpisodeChoice.latest(get("/pgc/view/web/season?season_id="+EpisodeChoice.SEASON),System.currentTimeMillis());}

 public JSONObject exchangeSkus(int page)throws Exception{return get("/x/vip_point/sku/list?pn="+page+"&ps=20&in_review=0");}
 public JSONObject records()throws Exception{return get("/x/vip_point/list?change_type=1&pn=1&ps=50");}
 public JSONObject completeBrowse(TaskItem t)throws Exception {String kind=t.browseKind();if(t.purchase())throw new Exception("消费任务不能自动执行");if(kind.equals("animatetab")||kind.equals("filmtab"))return browseDone(kind);if(kind.equals("dressview"))return form("/pgc/activity/score/task/complete/v2",taskBody(t.code));if(kind.equals("vipmallview")){JSONObject body=new JSONObject();body.put("csrf",csrf);body.put("eventId","hevent_oy4b7h3epeb");return call("https://show.bilibili.com/api/activity/fire/common/event/dispatch",body.toString(),true);}throw new Exception("此类任务暂不支持自动完成");}
 public JSONObject profile()throws Exception{return get("/x/web-interface/nav");}
 public String taskBody(String code)throws Exception {if(code==null||code.isEmpty())throw new Exception("任务标识缺失，请重新同步");return "taskCode="+enc(code)+"&csrf="+enc(csrf)+"";}
 public JSONObject receive(TaskItem t)throws Exception{return form("/pgc/activity/score/task/receive/v2",taskBody(t.code));}
 public JSONObject claim(TaskItem t)throws Exception{if(!t.reward)throw new Exception("服务端未标记为可领取奖励");return form("/pgc/activity/score/task/complete/v2",taskBody(t.code));}
 public JSONObject browseDone(String code)throws Exception{
  if(code.equals("animatetab")||code.equals("filmtab"))return form("/pgc/activity/deliver/task/complete","position="+(code.equals("animatetab")?"jp_channel":"tv_channel")+"&csrf="+enc(csrf)+"");
  throw new Exception("此任务需在官方页面完成；已返回同步最新状态");
 }
 public JSONObject sign()throws Exception {
  JSONObject j=new JSONObject();long now=System.currentTimeMillis();j.put("device","phone");j.put("t",now);j.put("ts",now/1000);
  return json("/pgc/activity/score/task/sign2?csrf="+enc(csrf)+"",j);
 }
 protected JSONObject call(String path,String body,boolean json)throws Exception{
  if(!authenticated())throw new ApiError(-101,"请先通过官方页面登录");
  boolean mall=path.equals("https://show.bilibili.com/api/activity/fire/common/event/dispatch");
  if(!path.startsWith("/")&&!mall)throw new Exception("非法请求路径");
  HttpURLConnection c=(HttpURLConnection)new URL(mall?path:"https://api.bilibili.com"+path).openConnection();
  c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(20000);
  c.setRequestProperty("Cookie",cookie);c.setRequestProperty("Referer",mall?"https://show.bilibili.com/":CENTER);c.setRequestProperty("Accept","application/json");c.setRequestProperty("native_api_from","h5");String buvid=value(cookie,"buvid3");if(!buvid.isEmpty())c.setRequestProperty("buvid",buvid);
  c.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0.0.0 Mobile Safari/537.36 BiliApp/"+CLIENT_BUILD+" mobi_app/android build/"+CLIENT_BUILD);
  try{
   if(body!=null){c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type",json?"application/json; charset=utf-8":"application/x-www-form-urlencoded; charset=utf-8");try(OutputStream os=c.getOutputStream()){os.write(body.getBytes("UTF-8"));}}
   int status=c.getResponseCode();if(status!=200)throw new ApiError(status,"网络或风控响应 HTTP "+status+"；请到官方页面核验");
   StringBuilder b=new StringBuilder();try(BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),"UTF-8"))){String s;while((s=r.readLine())!=null){b.append(s);if(b.length()>2000000)throw new Exception("响应超出限制");}}
   JSONObject response=new JSONObject(b.toString());int code=response.optInt("code",-999);if(code!=0)throw new ApiError(code,operation(path)+"："+response.optString("message","服务端拒绝请求"));
   return response;
  }finally{c.disconnect();}
 }
 static String operation(String p){if(p.contains("receive/v2"))return "领取任务失败";if(p.contains("complete"))return "任务上报失败";if(p.contains("sign2"))return "签到失败";return "同步失败";}
 public static class ApiError extends Exception {public final int code;ApiError(int c,String s){super("["+c+"] "+s);code=c;}}
}
