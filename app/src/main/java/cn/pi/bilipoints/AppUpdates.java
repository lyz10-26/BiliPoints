package cn.pi.bilipoints;
import org.json.*;import java.net.*;import java.io.*;
/** Public repository metadata; Bilibili credentials are never attached. */
public final class AppUpdates {
 public static final String REPO="https://github.com/lyz10-26/BiliPoints";
 public static final String INDEX="https://raw.githubusercontent.com/lyz10-26/BiliPoints/main/latest.json";
 public final int code;public final String name,apk;
 public AppUpdates(JSONObject j)throws Exception{
  code=j.getInt("version_code");name=j.getString("version_name");apk=j.getString("apk_url");
  if(code<=0||name.isEmpty()||!apk.matches("https://github\\.com/lyz10-26/BiliPoints/raw/refs/heads/main/BiliPoints-[A-Za-z0-9.\\-]+\\.apk"))throw new IOException("更新数据或下载地址不合法");
 }
 public static AppUpdates fetch()throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL(INDEX).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setInstanceFollowRedirects(false);c.setUseCaches(false);c.setRequestProperty("Accept","application/json");
  try{if(c.getResponseCode()!=200)throw new IOException("更新源响应异常 HTTP "+c.getResponseCode());ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[2048];int n;while((n=in.read(b))!=-1){if(out.size()+n>16384)throw new IOException("更新数据过大");out.write(b,0,n);}}return new AppUpdates(new JSONObject(new String(out.toByteArray(),"UTF-8")));}finally{c.disconnect();}
 }
}
