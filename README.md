# SRTest

## ①

### 環境
- Java: `11.0`
- Gradle: `7.4`

### エンドポイントファイルURL
> https://github.com/yukizarashi001/SRTest/blob/main/src/main/java/scripts/Test1.java

### 実行手順

```sh
$ ./gradlew build
$ YOUTUBE_API_KEY={APIキー} ./gradlew run -DmainClass=scripts.Test1
```

## ②

### 環境
- Java: `11.0`
- Gradle: `7.4`

### エンドポイントファイルURL
> https://github.com/yukizarashi001/SRTest/blob/main/src/main/java/scripts/Test2.java

### 実行手順

```sh
$ ./gradlew build
$ YOUTUBE_API_KEY={APIキー} ./gradlew run -DmainClass=scripts.Test2
```

## ③

### アーキテクチャ図

![Test3_SystemArchitecture](https://user-images.githubusercontent.com/48287235/168465512-7298a629-7212-4eb8-a86b-1fbc975fe9da.png)

#### 各部概略
- `Client`: 
  - アプリケーション実行者（home画面へアクセスするユーザ）
- `Application Load Balancer`: 
  - 各EC2インスタンスへリクエストを振り分け、負荷分散するためのロードバランサー
- `フロントサーバ`: 
  - `Amazon EC2`で構成され、静的なページを返却するコンポーネント
  - `APIサーバ`にリクエストを行い、画面の生成に必要なデータの参照・永続化を行う
- `APIサーバ`:
  - `Amazon EC2`で構成され、リクエストを元に永続化層へアクセスし、その結果を返却するコンポーネント
  - 永続化層のデータを返却したり、永続化層にデータを保存したりするフローを担う
- `キャッシュサーバ`:
  - `Amazon ElastiCache for Memcached`あるいは`Amazon ElastiCache for Redis`で構成されるコンポーネント
  - `APIサーバ`からのリクエストを元に、SQLを実行した結果をキャッシュとして保持していた場合はその結果を、そうでない場合はDBにアクセスして取得した結果を返却する
- `Amazon Aurora`:
  - 永続化が必要なデータを保持するDBサーバ
  - 今回はデータモデル（後述）を元にDBに適用し、`動画投稿者名`、`動画視聴URL`、`動画サムネイル保存先URL`を格納できるようにしておく
- `Amazon Cloudfront`:
  - 動画や画像コンテンツをキャッシュし、各サーバに保存するコンポーネント
  - home画面から動画や画像をリクエストされた場合にはまずここにキャッシュされているかを確認し、キャッシュされていればそのコンテンツを、そうでなければ`Amazon S3`からコンテンツを取得して返却する
- `Amazon S3`:
  - 動画や画像を保存しておくコンポーネント
  - 今回はDBに保存している`動画サムネイル保存先URL`に基づいた位置に画像ファイルを配置する

#### 処理フロー
①`Client`からインターネット経由でリクエストが送信される。<br>
②リクエストは最初の`Application Load Balancer`に送られ、`フロントサーバ`を構成するいずれかの`Amazon EC2`に割り振られる。<br>
③`フロントサーバ`から次の`Application Load Balancer`にリクエストを送り、`APIサーバ`から永続化層のデータの参照・永続化を行う。<br>
④`APIサーバ`から永続化層へのアクセスが発生する場合、まずは`キャッシュサーバ`に保存されたSQLの実行結果がないか確認する。<br>`キャッシュサーバ`に実行結果が残っていればその結果を、実行結果がなければ`Amazon Aurora`に向けてSQLを実行し、その結果を返却する。<br>
⑤`APIサーバ`の結果を元に`フロントサーバ`でhome画面を生成する。<br>
⑥home画面を生成する際に`動画サムネイル保存先URL`を元にサムネイル画像を`Amazon Cloudfront`に要求する。<br>`Amazon Cloudfront`に画像がキャッシュされていればその画像を、そうでなければ`Amazon S3`にアクセスして取得した画像を返却するようにし、返却された結果を元にhome画面のレンダリングを行う。<br>
⑦`Client`に生成したhome画面を返却する。<br>

### データモデル図（ER図）

![Test3_DataModel](https://user-images.githubusercontent.com/48287235/168468087-b47b291a-3c88-4e53-b126-96701d5e203e.png)

#### データモデル概略

- `CONTRIBUTERS(投稿者)`テーブルの`CONTRIBUTER_ID(投稿者ID)`を元に`VIDEOS(動画)`テーブルから動画に関するデータをリレーションして参照・永続化できるような構成とする
- `VIDEOS(動画)`テーブルでは`CONTRIBUTER_ID`と`VIDEO_ID(動画ID)`を複合ユニークキーとして持ち、投稿者と動画情報の組み合わせが重複しないようにする
- `VIDEOS(動画)`テーブルでは`CONTRIBUTER_ID`は`CONTRIBUTERS(投稿者)`テーブルの`CONTRIBUTER_ID`と外部キー制約を貼っておくことにより、2つのテーブルのデータ整合性を保つようにする
- `動画視聴URL`、`動画サムネイル保存先URL`は、実コンテンツの配置されている`Amazon S3`のURLを保存するようにする
- 今回は`動画投稿者名`、`動画サムネイル`、`動画視聴URL`を要求されているため、下記のようなクエリを実行し、必要なデータを取得する

```sql
SELECT 
CONTRIBUTER_NAME,
VIDEO_URL,
THUMNAIL_SAVED_URL
FROM
CONTRIBUTERS,
VIDEOS
WHERE
CONTRIBUTERS.CONTRIBUTER_ID = VIDEOS.CONTRIBUTER_ID
ORDER BY VIDEOS.VIDEO_ID DESC;
```

## ④
### Webサーバのチューニング案

Webサーバが`Nginx`であれば`worker_conections（1プロセスで処理できる最大接続数）`の値を増加させる。<br>
`worker_conections`の値は

```
worker_conections < worker_rlimit_nofile（1プロセスで扱える最大ファイル数） * 4
```

となるように`nginx.conf`に`worker_conections`の値を指定する。<br>
1プロセスあたり2ファイルディスクリプタを消費するので、

```
worker_conections < worker_rlimit_nofile（1プロセスで扱える最大ファイル数） * 2
```

となるようにしても良いが、バッファを持たせる場合は前者の案を採用する。<br>

また、`worker_rlimit_nofile`の値はOSのファイルディスクリプタ上限と`worker_process（Nginxのプロセス数で、auto指定でCPUのコア数）`に依存するため、各々確認する。<br>
確認した後、

```
worker_rlimit_nofile < OSのファイルディスクリプタ上限（OS全体で扱えるファイル総数） / worker_process
```

となるように`nginx.conf`に`worker_rlimit_nofile`および`worker_process`の値を指定する。<br>

ここまででも同時接続数が足りない場合は、メモリを増設した上でNginxの設定を変更して同時接続数をさらに増加させたり、<br>
サーバを増やした上でロードバランサー経由のアクセスができるようにし、ラウンドロビンでアクセスを分散処理できるようにする。<br>

※ 参考文献
> https://qiita.com/mikene_koko/items/85fbe6a342f89bf53e89
> https://www.a-frontier.jp/technology/performance02/

### サーバサイドのチューニング案

リクエストの増加に伴い、処理速度の改善が求められると思うので、1プロセスで捌ける処理の量を小さくし、非同期処理などで並列で処理を行えるようにする。<br>
また、1度だけDBからデータを取得すれば良い処理結果などはキャッシュとして保存し、DBへのアクセス回数を減らす。

### フロントエンドのチューニング案

画像などはcssやJavaScriptで変更するのではなく、なるべく表示させるままの画像を用意する。<br>
不要なリソースや空白を除外する。<br>
最低限必要な要素のみファーストビューとして表示し、以降の要素は遅延表示させるようにする。<br>

※ 参考文献
> https://www.prime-strategy.co.jp/resource/pdf/DevelopersSummit2020.pdf