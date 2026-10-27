package cn.pi.bilipoints;
import org.json.*;
import java.util.*;
public class TaskItem {
 public String code,title,subtitle,link,group,button; public int state,done,max; public boolean reward;
 public TaskItem(JSONObject j,String g){
  code=j.optString("task_code");title=j.optString("title",code);subtitle=j.optString("subtitle");
  link=j.optString("link");group=g;state=j.optInt("state",-1);done=j.optInt("complete_times");max=j.optInt("max_times",1);
  button=j.optString("button_text");JSONObject b=j.optJSONObject("button");if(b!=null)button=b.optString("text",button);
  // No unverified mapping of state=2 to 'claimable'. Require an explicit server flag/action label.
  reward=state!=3 && (j.optBoolean("can_receive_reward",false)||j.optBoolean("can_claim",false)||button.equals("领取积分")||button.equals("领取奖励"));
 }
 public boolean purchase(){return code.toLowerCase(Locale.ROOT).contains("buy")||title.contains("购买")||title.contains("消费");}
 public boolean watch(){return !purchase()&&(code.startsWith("ogvwatch")||title.contains("观看剧集")||title.contains("观看任一剧集"));}
 public String browseKind(){
  if(purchase())return "";
  String t=title.replaceAll("<[^>]*>", "").replaceAll("\\s+", "");
  if(code.equals("animatetab")||t.contains("浏览追番频道"))return "animatetab";
  if(code.equals("filmtab")||t.contains("浏览影视频道"))return "filmtab";
  if(code.equals("vipmallview")||code.equals("vipmallviewnew")||t.contains("浏览会员购页面"))return "vipmallview";
  if(t.contains("浏览装扮商城主页"))return "dressview";
  return "";
 }
 public boolean browse(){return !browseKind().isEmpty();}
 public String label(){if(reward)return "领取积分";if(state==0)return "领取任务";if(state==1)return purchase()?"查看提醒":"去完成";if(state==3)return "已完成";return "官方核验";}
 public static ArrayList<TaskItem> parse(JSONObject data)throws JSONException {
  ArrayList<TaskItem> out=new ArrayList<>(); HashSet<String> seen=new HashSet<>();
  JSONObject info=data.optJSONObject("task_info");if(info==null)info=data.optJSONObject("task");
  if(info==null)throw new JSONException("任务字段已变化，请在官方积分页核验");
  JSONArray mods=info.optJSONArray("modules");
  if(mods!=null){for(int i=0;i<mods.length();i++){JSONObject m=mods.getJSONObject(i);append(out,seen,m.optJSONArray("common_task_item"),m.optString("module_title"));}}
  else append(out,seen,info.optJSONArray("task_item"),"日常任务");
  return out;
 }
 private static void append(ArrayList<TaskItem> out,HashSet<String> seen,JSONArray list,String group)throws JSONException {
  if(list==null)return;for(int i=0;i<list.length();i++){TaskItem t=new TaskItem(list.getJSONObject(i),group);if(!t.code.isEmpty()&&seen.add(t.code))out.add(t);}
 }
}
