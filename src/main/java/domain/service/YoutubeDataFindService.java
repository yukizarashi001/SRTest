package domain.service;

import java.io.IOException;
import java.util.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.VideoListResponse;

import org.apache.commons.lang3.StringUtils;

import config.YoutubeDataApiConfig;
import infra.client.YoutubeDataApiClient;
import infra.client.YoutubeDataApiClient.ExtractJapaneseVideoSetting;

public class YoutubeDataFindService {
    private static final String YOUTUBE_VIDEO_BASE_URL = "https://www.youtube.com/watch?v=";
    private static final int REQUIRED_SHOWROOM_VIDEO_COUNT = 100;
    private static final int REQUIRED_APEX_LEGENDS_VIDEO_COUNT = 10;

    private final YoutubeDataApiConfig config;

    public YoutubeDataFindService() {
        this.config = new YoutubeDataApiConfig();
    }

    /**
     * 動画探索種別に応じた動画URLリストを返却する.
     * 
     * note:
     * 何度か実行してみたが、WEBサイト上との検索結果と一致しない場合があった.
     * Youtube側でAPIのレスポンスとして返す結果を一定時間ごとに保存されたデータとしている可能性がある？（要検証）
     * 
     * @param type 動画探索種別
     * @return 動画URLリスト
     */
    public List<String> findVideoUrlsByType(final YoutubeVideoFindType type) {
        final Optional<String> mayBeApiKey = config.getApiKey();

        if(mayBeApiKey.isEmpty()) {
            System.err.println("connot find api key.");
            System.exit(1);
        }
        final String apiKey = mayBeApiKey.get();

        switch(type) {
            case SHOWROOM:
                return this.recursiveFindShowRoomVideoList(apiKey, new ArrayList<>(), StringUtils.EMPTY, REQUIRED_SHOWROOM_VIDEO_COUNT)
                .stream()
                .map(id -> YOUTUBE_VIDEO_BASE_URL + id)
                .collect(Collectors.toList());
            case APEX_LEGENDS:
                return this.recursiveFindApexLegendsVideoList(apiKey, new ArrayList<>(), StringUtils.EMPTY, REQUIRED_APEX_LEGENDS_VIDEO_COUNT)
                .stream()
                .map(id -> YOUTUBE_VIDEO_BASE_URL + id)
                .collect(Collectors.toList());
            default:
                System.err.println("not exist findType.");
                return List.of();
        }
    }

    /**
     * Youtube Data APIを実行し要求数に応じた「SHOWROOM」関連動画IDリストを返却する.
     * 1度に取得できる動画数の上限を超えた場合は再帰処理される.
     * （nextPageTokenが存在し、かつ次ページに50件存在しない場合は、要求数以下で返却される場合もある）
     * 
     * @param request APIリクエスト
     * @param videoIdList 動画IDリスト
     * @param requiredVideoCount 要求動画数
     * @return 動画IDリスト
     */
    private List<String> recursiveFindShowRoomVideoList(final String apiKey, final List<String> videoIdList, final String pageToken, final long requiredVideoCount) {
        try {
            final YouTube apiClient = YoutubeDataApiClient.getClient();

            final SearchListResponse searchResponse = apiClient
            .search()
            .list(YoutubeDataApiClient.Test1Setting.PART)
            .setKey(apiKey)
            .setQ(YoutubeDataApiClient.Test1Setting.QUERY)
            .setType(YoutubeDataApiClient.Test1Setting.RESOURCE_TYPE)
            .setMaxResults(YoutubeDataApiClient.ONCE_REQUIRED_VIDEO_COUNT)
            .setOrder(YoutubeDataApiClient.Test1Setting.SORT_ORDER)
            .setPageToken(pageToken)
            .execute();

            final List<SearchResult> searchResultList = searchResponse.getItems();

            // API実行結果が空であった場合はプログラムの実行を停止する.
            if(searchResultList.isEmpty()) {
                System.err.println("can not get API result.");
                System.exit(1);
            }

            for(SearchResult result: searchResultList) {
                if(videoIdList.size() < requiredVideoCount) {
                    videoIdList.add(result.getId().getVideoId());
                }
            }

            final String nextPageToken = searchResponse.getNextPageToken();

            if(!nextPageToken.isEmpty() && videoIdList.size() < requiredVideoCount) {
                recursiveFindShowRoomVideoList(apiKey, videoIdList, nextPageToken, requiredVideoCount);
            }
        } catch (IOException e) {
            System.err.println("IO error: " + e.getCause() + " : " + e.getMessage());
            System.exit(1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
        return videoIdList;
    }

    /**
     * Youtube Data APIを実行し要求数に応じた「Apex Legends」関連動画IDリストを返却する.
     * 1度に取得できる動画数の上限を超えた場合は再帰処理される.
     * （nextPageTokenが存在し、かつ次ページに50件存在しない場合は、要求数以下で返却される場合もある）
     * 
     * @param request APIリクエスト
     * @param videoIdList 動画IDリスト
     * @param requiredVideoCount 要求動画数
     * @return 動画IDリスト
     */
    private List<String> recursiveFindApexLegendsVideoList(final String apiKey, final List<String> videoIdList, final String pageToken, final long requiredVideoCount) {
        Date now = new Date();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_MONTH, -3);

        // 3日前の日時を取得
        Date threeDaysAgo = calendar.getTime();
        
        try {
            final YouTube apiClient = YoutubeDataApiClient.getClient();

            final SearchListResponse searchResponse = apiClient
            .search()
            .list(YoutubeDataApiClient.Test2Setting.PART)
            .setKey(apiKey)
            .setQ(YoutubeDataApiClient.Test2Setting.QUERY)
            .setRegionCode(YoutubeDataApiClient.Test2Setting.REGION_CODE)
            .setType(YoutubeDataApiClient.Test2Setting.RESOURCE_TYPE)
            .setMaxResults(YoutubeDataApiClient.ONCE_REQUIRED_VIDEO_COUNT)
            .setOrder(YoutubeDataApiClient.Test2Setting.SORT_ORDER)
            .setPublishedAfter(new DateTime(threeDaysAgo))
            .execute();

            final List<SearchResult> searchResultList = searchResponse.getItems();

            // API実行結果が空であった場合はプログラムの実行を停止する.
            if(searchResultList.isEmpty()) {
                System.err.println("can not get API result.");
                System.exit(1);
            }

            final List<String> resultVideoIdList = searchResultList
            .stream()
            .map(r -> r.getId().getVideoId())
            .collect(Collectors.toList());

            // 3日以内の再生回数が多い順の動画IDリストから日本人の動画のみを抽出する
            final List<String> japaneseVideoIdList = this.extractJapaneseVideoIdList(apiKey, resultVideoIdList);

            for(String videoId: japaneseVideoIdList) {
                if(videoIdList.size() < requiredVideoCount) {
                    videoIdList.add(videoId);
                }
            }

            final String nextPageToken = searchResponse.getNextPageToken();

            if(!nextPageToken.isEmpty() && videoIdList.size() < requiredVideoCount) {
                recursiveFindApexLegendsVideoList(apiKey, videoIdList, nextPageToken, requiredVideoCount);
            }
        } catch (IOException e) {
            System.err.println("IO error: " + e.getCause() + " : " + e.getMessage());
            System.exit(1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
        return videoIdList;
    }

    /**
     * 動画IDリストから日本人投稿者の動画IDを抽出した動画IDリストを返却する.
     * 存在しない場合は空の動画IDリストを返却する.
     * 
     * note:
     * 「日本人の動画」の定義が難しく、API仕様書を確認し、動画投稿者の国籍を取得することも考えたが現状はできなさそうで、
     * 動画撮影場所から日本の地域で撮影されたものとして絞り込んだとしてもそれが日本人の動画かどうかは判別がつかず、
     * 指定した国の検索結果として日本を指定したとしても同様に日本人の動画かどうか判定できないと判断した.
     * そのため、今回は「デフォルトの音声が日本語の動画 = 日本人の動画」と見做して抽出を行った.
     * 
     * @param apiKey APIキー
     * @param videoIdList 動画IDリスト
     * @return 日本人投稿者の動画IDリスト
     */
    private List<String> extractJapaneseVideoIdList(final String apiKey, final List<String> videoIdList) {
        final YouTube apiClient = YoutubeDataApiClient.getClient();

        // APIクライアントの引数に動画IDのリストを文字列として渡すために変換する.
        final String videoIds = videoIdList
        .stream()
        .collect(Collectors.joining(","));

        try {
            final VideoListResponse searchResponse = apiClient
            .videos()
            .list(ExtractJapaneseVideoSetting.PART)
            .setKey(apiKey)
            .setId(videoIds)
            .execute();

            return searchResponse.getItems()
            .stream()
            .filter(r -> r.getSnippet().getDefaultAudioLanguage() != null && r.getSnippet().getDefaultAudioLanguage().equals("ja"))
            .map(it -> it.getId())
            .collect(Collectors.toList());

        } catch (IOException e) {
            System.err.println("IO error: " + e.getCause() + " : " + e.getMessage());
            System.exit(1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }

        return List.of();
    }
}
