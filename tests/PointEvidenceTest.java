import cn.pi.bilipoints.*;import org.json.*;
public class PointEvidenceTest {
 static void ok(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  TaskItem t=new TaskItem(new JSONObject("{\"task_code\":\"vipmallview\",\"title\":\"浏览会员购页面10秒\"}"),"");
  JSONArray before=new JSONArray("[{\"order_no\":\"old\",\"point\":10,\"remark\":\"浏览会员购页面10秒\"}]");
  JSONArray after=new JSONArray("[{\"order_no\":\"new\",\"point\":10,\"remark\":\"浏览会员购页面10秒\"},{\"order_no\":\"old\",\"point\":10,\"remark\":\"浏览会员购页面10秒\"}]");
  ok(PointEvidence.credited(before,after,t)==10,"credit must come from a new matching ledger entry");
  ok(PointEvidence.credited(before,before,t)==0,"timer or successful response with no new entry is never credited");
  ok(PointEvidence.credited(null,after,t)==0,"missing baseline cannot prove credit");
  JSONArray unrelated=new JSONArray("[{\"order_no\":\"buy\",\"point\":100,\"remark\":\"购买会员购商品\"},{\"order_no\":\"other\",\"point\":10,\"remark\":\"浏览影视频道\"}]");
  ok(PointEvidence.credited(before,unrelated,t)==0,"another purchase or another task cannot count");
  JSONArray duplicate=new JSONArray("[{\"order_no\":\"new\",\"point\":10,\"remark\":\"浏览会员购页面10秒\"},{\"order_no\":\"new\",\"point\":10,\"remark\":\"浏览会员购页面10秒\"}]");
  ok(PointEvidence.credited(before,duplicate,t)==10,"duplicate ledger IDs count once");
  ok(PointEvidence.list(new JSONObject("{\"data\":{\"big_point_list\":null}}")).length()==0,"explicit null is an empty ledger");
  try{PointEvidence.list(new JSONObject("{\"data\":{}}"));throw new AssertionError("changed schema");}catch(JSONException expected){}
  ProtocolVipTest.Capture a=new ProtocolVipTest.Capture();a.completeBrowse(t);ok(a.path.equals("https://show.bilibili.com/api/activity/fire/common/event/dispatch")&&a.json,"mall browse must use dedicated event dispatch");JSONObject body=new JSONObject(a.body);ok(body.getString("eventId").equals("hevent_oy4b7h3epeb")&&body.getString("csrf").equals("a+b=c"),"event payload and csrf");
  TaskItem dress=new TaskItem(new JSONObject("{\"task_code\":\"dressview\",\"title\":\"浏览装扮商城主页\"}"),"");ok(PointEvidence.credited(new JSONArray(),new JSONArray(),dress)==0,"opening dress page alone is never credited");ok(PointEvidence.credited(new JSONArray(),new JSONArray("[{\"order_no\":\"dress\",\"point\":10,\"remark\":\"浏览装扮商城主页\"}]"),dress)==10,"new official dress ledger entry proves credit");ok(PointEvidence.credited(new JSONArray(),after,dress)==0,"another parallel task cannot prove dress credit");a.completeBrowse(dress);ok(a.path.contains("complete/v2")&&a.body.contains("taskCode=dressview"),"dress browser must submit completion");
  TaskItem buy=new TaskItem(new JSONObject("{\"task_code\":\"vipmallbuy\",\"title\":\"购买商品\"}"),"");try{a.completeBrowse(buy);throw new AssertionError("purchase execution");}catch(Exception expected){}
  System.out.println("PASS: genuine credit evidence, unchanged/unrelated/missing ledger, duplicates, mall/dress protocols and purchase exclusion");
 }
}
