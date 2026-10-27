package cn.pi.bilipoints;
import org.json.*;import java.util.*;
/** Only new matching positive ledger entries are evidence of credited points. */
public final class PointEvidence {
 static String key(JSONObject r){String id=r.optString("order_no","");return id.isEmpty()?r.optLong("change_time")+"|"+r.optInt("point")+"|"+r.optString("remark"):id;}
 public static int credited(JSONArray before,JSONArray after,TaskItem task){
  if(before==null||after==null||task.purchase())return 0;
  HashSet<String> seen=new HashSet<>();for(int i=0;i<before.length();i++){JSONObject r=before.optJSONObject(i);if(r!=null)seen.add(key(r));}
  int sum=0;for(int i=0;i<after.length();i++){JSONObject r=after.optJSONObject(i);if(r==null||!seen.add(key(r))||r.optInt("point")<=0)continue;String remark=r.optString("remark","");if(matches(remark,task))sum+=r.optInt("point");}return sum;
 }
 static boolean matches(String r,TaskItem t){if(r.contains("购买")||r.contains("消费"))return false;switch(t.browseKind()){case "animatetab":return r.contains("追番")&&r.contains("浏览");case "filmtab":return r.contains("影视")&&r.contains("浏览");case "vipmallview":return r.contains("会员购")&&r.contains("浏览");case "dressview":return r.contains("装扮")&&r.contains("浏览");default:return t.watch()&&r.contains("观看")&&(r.contains("剧集")||r.contains("正片"));}}
 public static JSONArray list(JSONObject response)throws JSONException{JSONObject d=response.optJSONObject("data");if(d==null)throw new JSONException("积分记录缺失");JSONArray a=d.optJSONArray("big_point_list");if(a==null&&d.has("big_point_list")&&d.isNull("big_point_list"))return new JSONArray();if(a==null)throw new JSONException("积分记录格式变化");return a;}
}
