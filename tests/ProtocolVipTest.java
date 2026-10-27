import cn.pi.bilipoints.*;import org.json.*;import java.net.*;
public class ProtocolVipTest {
 static void ok(boolean x,String m){if(!x)throw new AssertionError(m);}
 static class Capture extends BiliApi {String path,body;boolean json;Capture(){super("SESSDATA=test; bili_jct=a+b=c");}protected JSONObject call(String p,String b,boolean j)throws Exception{path=p;body=b;json=j;return new JSONObject("{\"code\":0}");}}
 public static void main(String[] args)throws Exception {
  Capture a=new Capture();TaskItem t=new TaskItem(new JSONObject("{\"task_code\":\"ogvwatchnew\",\"state\":0}"),"");a.receive(t);
  ok(a.path.startsWith("/pgc/activity/score/task/receive/v2?")&&!a.json,"receive endpoint and form encoding");
  java.util.Map<String,String> form=new java.util.HashMap<>();for(String p:a.body.split("&")){String[] v=p.split("=",2);form.put(URLDecoder.decode(v[0],"UTF-8"),URLDecoder.decode(v[1],"UTF-8"));}
  ok(form.get("taskCode").equals("ogvwatchnew")&&!form.containsKey("task_code"),"captured protocol uses camelCase taskCode");ok(form.get("csrf").equals("a+b=c"),"csrf must survive form encoding");
  ok(form.get("build").equals("8451100")&&a.path.contains("build=8451100"),"version gate gets build in query and form");ok(new JSONObject(form.get("statistics")).getString("version").equals("8.45.1"),"consistent statistics version");
  t.reward=true;a.claim(t);ok(a.path.contains("/complete/v2?")&&a.body.contains("taskCode="),"claim request naming");
  long now=1800000000000L;VipInfo active=VipInfo.parse(new JSONObject("{\"vipStatus\":1,\"vipType\":2,\"vipDueDate\":1800086400000,\"vip_label\":{\"text\":\"年度大会员\"}}"),now);
  ok(active.active&&active.state.equals("年度大会员")&&active.expiry.contains("剩余约 1 天"),"active annual VIP with real expiry");
  VipInfo expired=VipInfo.parse(new JSONObject("{\"vip\":{\"status\":0,\"type\":2,\"due_date\":1799913600}}"),now);ok(!expired.active&&expired.state.equals("大会员已到期")&&expired.due==1799913600000L,"expired nested VIP seconds conversion");
  VipInfo missing=VipInfo.parse(new JSONObject(),now);ok(!missing.active&&missing.state.equals("会员状态未知"),"missing data never reports membership");
  VipInfo conflicting=VipInfo.parse(new JSONObject("{\"vipStatus\":1,\"vipDueDate\":1799913600000}"),now);ok(!conflicting.active,"past expiry cannot display active VIP");
  VipInfo none=VipInfo.parse(new JSONObject("{\"vipStatus\":0,\"vipType\":0}"),now);ok(none.state.equals("当前未开通大会员"),"non member state");
  System.out.println("PASS: captured receive/claim protocol, encoded csrf, active/expired/unknown/missing VIP fields");
 }
}
