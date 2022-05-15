package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;


public class YoutubeDataApiConfig {
    private static final String PROPERTIES_FILENAME = "youtube.properties";
    private Properties properties = new Properties();

    /**
     * プロパティファイルから設定を読み込んで変数に格納する.
     */
    public YoutubeDataApiConfig() {
        try(InputStream inputStream = YoutubeDataApiConfig.class.getClassLoader().getResourceAsStream(PROPERTIES_FILENAME)) {
            properties.load(inputStream);
        } catch (IOException e) {
            System.err.println(PROPERTIES_FILENAME + " not found.");
            System.exit(1);
        }
    }

    /**
     * APIキーを返却する.
     * 環境変数に設定がある場合は環境変数の値が優先される.
     * 次点でプロパティファイルの設定が優先される.
     * どちらも存在しない場合はOptional#emptyを返す.
     * 
     * @return Youtube APIキー
     */
    public Optional<String> getApiKey() {
        final String environmentVariableApiKey = System.getenv("YOUTUBE_API_KEY");
        final String configFileApiKey = this.properties.getProperty("youtube.apikey");
        if(!StringUtils.isEmpty(environmentVariableApiKey)) {
            return Optional.of(environmentVariableApiKey);
        } else if(!StringUtils.isEmpty(configFileApiKey)) {
            return Optional.of(configFileApiKey);
        } else {
            return Optional.empty();
        }
    }
}
