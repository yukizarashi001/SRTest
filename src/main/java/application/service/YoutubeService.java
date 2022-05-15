package application.service;

import java.util.List;

import domain.service.YoutubeDataFindService;
import domain.service.YoutubeVideoFindType;

public class YoutubeService {
    /**
     * 「SHOWROOM」というキーワードを持つ動画を出力する.
     */
    public void printShowRoomVideoUrl() {
        final YoutubeDataFindService findService = new YoutubeDataFindService();

        final List<String> showRoomVideoUrlList = findService.findVideoUrlsByType(YoutubeVideoFindType.SHOWROOM);

        if(showRoomVideoUrlList.isEmpty()) {
            System.err.println("showRoomVideoList is not found.");
            System.exit(1);
        }

        showRoomVideoUrlList
        .stream()
        .forEach(System.out::println);
    }

    /**
     * 「Apex Legends」というキーワードを持つ動画を出力する.
     */
    public void printApexLegendsVideoUrl() {
        final YoutubeDataFindService findService = new YoutubeDataFindService();

        final List<String> apexLegendsVideoUrlList = findService.findVideoUrlsByType(YoutubeVideoFindType.APEX_LEGENDS);

        if(apexLegendsVideoUrlList.isEmpty()) {
            System.err.println("showRoomVideoList is not found.");
            System.exit(1);
        }

        apexLegendsVideoUrlList
        .stream()
        .forEach(System.out::println);
    }
}
