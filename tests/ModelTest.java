import cn.pi.bilipoints.*;
import org.json.*;
public class ModelTest {
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 public static void main(String[] args)throws Exception{
  JSONObject base=new JSONObject("{\"task_info\":{\"modules\":[{\"module_title\":\"日常任务\",\"common_task_item\":[{\"task_code\":\"animatetab\",\"state\":0,\"title\":\"浏览追番\"},{\"task_code\":\"vipmallbuy\",\"state\":1,\"title\":\"购买会员购商品\"},{\"task_code\":\"unknown\",\"state\":2},{\"task_code\":\"animatetab\",\"state\":0}]}]}}");
  java.util.ArrayList<TaskItem> a=TaskItem.parse(base);check(a.size()==3,"duplicate task codes must not run twice");check(a.get(0).browse()&&!a.get(0).purchase(),"browser classification");check(a.get(1).purchase(),"purchase must be reminder-only");check(!a.get(2).reward&&a.get(2).label().equals("官方核验"),"unknown state must never become a claim");
  TaskItem r=new TaskItem(new JSONObject("{\"task_code\":\"x\",\"state\":1,\"can_claim\":true}"),"");check(r.reward,"explicit claim flag");
  TaskItem done=new TaskItem(new JSONObject("{\"task_code\":\"x\",\"state\":3,\"can_claim\":true}"),"");check(!done.reward&&done.label().equals("已完成"),"completed task not claimed twice");
  TaskItem absent=new TaskItem(new JSONObject("{\"task_code\":\"x\"}"),"");check(absent.state==-1&&!absent.reward,"missing state not treated as accepted");
  check(!new BiliApi("").authenticated(),"empty cookie");check(!new BiliApi("SESSDATA=x").authenticated(),"missing csrf");check(new BiliApi("SESSDATA=x; bili_jct=y").authenticated(),"valid-shaped cookie");check(BiliApi.value("foo=x=y; bili_jct=csrf", "foo").equals("x=y"),"preserve cookie value equals");
  try{TaskItem.parse(new JSONObject());throw new AssertionError("missing task schema must fail");}catch(JSONException expected){}
  System.out.println("PASS: task classification, duplicate protection, unknown/claimed states, missing schema, session parsing");
 }
}
