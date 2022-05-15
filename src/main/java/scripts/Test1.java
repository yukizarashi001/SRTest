package scripts;

import application.service.YoutubeService;

public class Test1 {
  public static void main(String[] args) {
    final YoutubeService service = new YoutubeService();
    service.printShowRoomVideoUrl();
  }
}