package infra.client;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.youtube.YouTube;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;

import java.io.IOException;

public class YoutubeDataApiClient {
  /**
   * 一度にリクエストする動画数（最大50）
   */
  public static final long ONCE_REQUIRED_VIDEO_COUNT = 50;
  /**
   * アプリケーション名
   */
  private static final String APPLICATION_NAME = "SR-Test";
  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
  private static final JsonFactory JSON_FACTORY = new GsonFactory();

  /**
   * テスト①の実行時設定
   */
  public static class Test1Setting {
    public static final String PART = "snippet";
    public static final String QUERY = "SHOWROOM";
    public static final String RESOURCE_TYPE = "video";
    public static final String SORT_ORDER = "date";
  }

  /**
   * テスト②の実行時設定
   */
  public static class Test2Setting {
    public static final String PART = "id";
    public static final String QUERY = "Apex Legends";
    public static final String REGION_CODE = "JP";
    public static final String RESOURCE_TYPE = "video";
    public static final String SORT_ORDER = "viewCount";
  }

  public static class ExtractJapaneseVideoSetting {
    public static final String PART = "snippet";
  }

  /**
   * Youtube Data APIクライアントを返却する.
   * @return APIクライアント
   */
  public static YouTube getClient() {
    return new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpRequestInitializer() {
        public void initialize(HttpRequest request) throws IOException {}
      })
      .setApplicationName(APPLICATION_NAME)
      .build();
  }
}
