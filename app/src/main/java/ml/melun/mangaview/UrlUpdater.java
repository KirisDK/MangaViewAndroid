package ml.melun.mangaview;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;

public class UrlUpdater extends AsyncTask<Void, Void, Boolean> {
    String result;
    String fetchUrl;
    boolean silent = false;
    Context c;
    UrlUpdaterCallback callback;
    public UrlUpdater(Context c){
        this.c = c;
        this.fetchUrl = p.getDefUrl();
    }
    public UrlUpdater(Context c, boolean silent, UrlUpdaterCallback callback, String defUrl){
        this.c = c;
        this.silent = silent;
        this.callback = callback;
        this.fetchUrl = defUrl;
    }
    protected void onPreExecute() {
        if(!silent) Toast.makeText(c, "자동 URL 설정중...", Toast.LENGTH_SHORT).show();
    }
    protected Boolean doInBackground(Void... params) {
        return fetch();
    }

    protected Boolean fetch() {
        try {
            // 1. Jsoup으로 HTML 페이지를 가져옵니다.
            String url = "https://www.xn--h10b2b940bwzy.store/";
            Document document = Jsoup.connect(url).get();

            // 2. 정확히 <a> 태그에서 data-testid="linkElement"와 aria-label="마나토끼 바로가기"를 만족하는 엘리먼트 선택
            Element linkElement = document.selectFirst("a[data-testid=\"linkElement\"][aria-label=\"마나토끼 바로가기\"]");

            // 3. href 속성을 반환
            if (linkElement != null) {
                String href = linkElement.attr("href");

                // 4. href가 "/" 뒤에 URI를 포함하는지 확인
                if (href != null && href.matches("http[s]?://[^/]+/.+")) {

                    // 5. 리다이렉트되는 최종 URL을 확인
                    href = resolveRedirectUrl(href);
                }

                result = href; // 최종 URL 저장
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 리다이렉트를 확인해 최종 URL 반환하는 메서드
    private String resolveRedirectUrl(String url) {
        OkHttpClient client = new OkHttpClient(); // OkHttpClient 클라이언트 초기화

        try {
            Request request = new Request.Builder()
                    .url(url) // 요청한 URL
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                return response.networkResponse().request().url().toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 리다이렉션이 없거나 예외 발생 시 원래 URL 반환
        return url;
    }

    @Deprecated
    protected Boolean fetch_(){
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "Mozilla/5.0 (Linux; U; Android 4.0.2; en-us; Galaxy Nexus Build/ICL53F) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");
            Response r = httpClient.get(fetchUrl, headers);
            if (r.code() == 302) {
                result = r.header("Location");
                r.close();
                return true;
            } else{
                r.close();
                return false;
            }

        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    protected void onPostExecute(Boolean r) {
        if(r && result !=null){
            p.setUrl(result);
            if(!silent)Toast.makeText(c, "자동 URL 설정 완료!", Toast.LENGTH_SHORT).show();
            if(callback!=null) callback.callback(true);
        }else{
            if(!silent)Toast.makeText(c, "자동 URL 설정 실패, 잠시후 다시 시도해 주세요", Toast.LENGTH_LONG).show();
            if(callback!=null) callback.callback(false);
        }


    }


    public interface UrlUpdaterCallback{
        void callback(boolean success);
    }
}
