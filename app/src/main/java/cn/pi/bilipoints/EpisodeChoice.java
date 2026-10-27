package cn.pi.bilipoints;
import org.json.*;
/** Chooses a published main episode, not an appended trailer or future entry. */
public final class EpisodeChoice {
 public static final long SEASON=28747;public static final String SEASON_URL="https://www.bilibili.com/bangumi/play/ss28747";
 public long id,published;public double number;public String title,longTitle;
 public String url(){return "https://www.bilibili.com/bangumi/play/ep"+id;}
 public String appUrl(){return "bilibili://bangumi/play/ep"+id;}
 public String label(){return "凡人修仙传 · 第"+title+"集"+(longTitle.isEmpty()?"":" · "+longTitle);}
 public static EpisodeChoice latest(JSONObject root,long now)throws JSONException{
  JSONObject r=root.optJSONObject("result");if(r==null||!r.optString("title").contains("凡人修仙传"))throw new JSONException("未获取到凡人修仙传剧集信息");JSONArray eps=r.optJSONArray("episodes");if(eps==null)throw new JSONException("正片列表缺失");EpisodeChoice best=null;
  for(int i=0;i<eps.length();i++){JSONObject ep=eps.optJSONObject(i);if(ep==null||ep.optInt("section_type",0)!=0)continue;String badge=ep.optString("badge"),title=ep.optString("title").trim();if(badge.contains("预告")||badge.equalsIgnoreCase("PV")||!title.matches("[0-9]+(?:\\.[0-9]+)?"))continue;long id=ep.optLong("id",ep.optLong("ep_id",0)),pub=ep.optLong("pub_time",0);if(id<=0||pub>now/1000||ep.optBoolean("is_view_hide",false))continue;double n;try{n=Double.parseDouble(title);}catch(Exception e){continue;}if(n<=0)continue;
   if(best==null||n>best.number||(n==best.number&&pub>best.published)){best=new EpisodeChoice();best.id=id;best.number=n;best.title=title;best.longTitle=ep.optString("long_title","");best.published=pub;}
  }if(best==null)throw new JSONException("没有可确认的已更新正片");return best;
 }
}
