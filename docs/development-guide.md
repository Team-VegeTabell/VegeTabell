# アプリ開発手順書

企画・要件定義（[overview.md](./overview.md) / [functional-requirements.md](./functional-requirements.md) / [db-design.md](./db-design.md) / [api-design.md](./api-design.md) / [auth-design.md](./auth-design.md)）を踏まえ、`VegeTabell-app`（Eclipse + Gradle + Spring Boot）での実装手順をまとめたもの。

## このドキュメントの運用ルール（進捗管理）

本ドキュメントは手順書であると同時に、**進捗管理チェックリストを兼ねる**。

- 会話（セッション）を再開する際は、まず本ドキュメントの「進捗チェックリスト」を読み込み、チェック済み（`[x]`）の項目は完了済みとして扱い、未チェック（`[ ]`）の項目から作業を再開・提案すること
- 作業を1つ完了するごとに、該当項目を `- [ ]` から `- [x]` に変更し、行末に `(完了: YYYY-MM-DD)` の形式で完了日を追記する
- チェックリストの項目粒度を変えたり、新しい作業が発生した場合はチェックリスト自体に項目を追加してよい
- 各チェック項目の詳細な手順は、下部の「詳細手順」内の対応する章を参照する

## 進捗チェックリスト

### 環境構築（各メンバーのPC）
- [ ] JDK 17インストール確認
- [ ] EclipseにBuildship / Spring Tools 4 (STS) プラグイン導入
- [ ] リポジトリをclone
- [ ] EclipseへGradleプロジェクトとしてインポート（[§1](#1-プロジェクトをeclipseへ取り込む)）

### プロジェクト初期設定
- [x] `build.gradle`にSpring Data JPAを追加（[§2](#2-依存関係の追加spring-data-jpa)） (完了: 2026-09-07)
- [x] Supabaseプロジェクト作成・接続情報取得（[§3](#3-supabasepostgresql接続設定)） (完了: 2026-09-07)
- [x] `application.properties` / `application-local.properties`にDB接続設定を追加 (完了: 2026-09-07)
- [x] Supabase（Session pooler）への実際の接続確認（`bootRun`起動、Hikari接続成功、`Started VegeTabellAppApplication`を確認） (完了: 2026-09-07)

### 実装（機能単位・[§5の推奨順序](#5-実装の推奨順序)に対応）
- [x] Step1: Supabase上に`db-design.md`通りのテーブルをSQLで作成（Supabase MCPの`apply_migration`で適用済み） (完了: 2026-09-07)
- [x] Step2: Entity・Repository実装（6 Entity + 6 Repository + enum/Converter、`ddl-auto=validate`通過、`CategoryRepositoryTest`で実DBからのデータ取得を確認） (完了: 2026-09-07)
- [x] Step3: Spring Security設定（`SecurityConfig`/`CustomUserDetailsService`/ログイン時ロール一致チェック、auth-design.md §2の認可マトリクス実装、テスト6クラス16件で検証） (完了: 2026-09-07)
- [x] Step4: 会員登録・ログイン画面（`AuthController`/`SignupForm`/`SignupService`、`/login`・`/signup`テンプレート、テスト8クラス28件で検証。実DB(Supabase)への登録・ログイン・ロール不一致・ログアウトも手動確認済み） (完了: 2026-09-07)
- [x] Step5: 売り手：商品出品機能（`SellerController`/`ProductForm`、`/seller/dashboard`・`/seller/products`・`/seller/products/new`とsold-out/delete操作を実装、テスト1クラス10件で検証。実DB(Supabase)への出品・完売・削除も手動確認済み） (完了: 2026-09-07)
- [x] Step6: 買い手：商品一覧・詳細（`ProductController`/`ProductSummary`/`ProductDetail`、`/`・`/products`・`/products/{id}`を実装。カテゴリ・キーワード・エリア（店舗住所）絞り込み、割引率・残り時間・完売時の予約ボタン無効化に対応。テスト1クラス7件＋実DBへの手動確認で検証） (完了: 2026-09-07)
- [x] Step7: 予約機能・キャンセル（`ReservationController`/`ReservationService`/`ReservationForm`、`/products/{id}/reservations`・`/reservations/{id}`・`/reservations/{id}/cancel`を実装。数量選択UI、在庫減算/復元、sold_out自動化、売り手ダッシュボードへの予約一覧＋キャンセル追加。テスト2クラス17件＋実DBへの手動確認で検証） (完了: 2026-09-07)
- [x] Step8: 通知機能（`NotificationController`/`NotificationView`、`/notifications`・`/notifications/{id}/read`を実装。予約作成/キャンセル時に`ReservationService`から通知を生成、買い手ホーム・売り手ダッシュボードに未読バッジ付きベルアイコンを追加。`NotificationControllerTest`6件を新規追加、`ReservationServiceTest`に通知生成の検証3件を追加＋実DBへの手動確認で検証） (完了: 2026-09-08)
- [x] Step9: バッチ処理（`@Scheduled`）（`BatchService`/`SchedulingConfig`、5分おきに①期限切れ商品の`expired`化②受取期限超過予約の`completed`化③`pickup_reminder`④`stock_expiring_warning`通知生成を実装。通知生成ロジックは`ReservationService`から`NotificationService`に切り出して共通化。テスト2クラス7件を新規追加＋`ReservationServiceTest`を`NotificationService`利用に更新） (完了: 2026-09-08)
- [ ] Step10: ワイヤーフレーム・画面遷移図との突合せ、レスポンシブ調整

---

## 現状の棚卸し

`VegeTabell-app`はSpring Initializrで初期化済み。

| 項目 | 現状 |
|---|---|
| `build.gradle` | Web, Thymeleaf, Security, Validation, Lombok, DevTools, PostgreSQLドライバ、**Spring Data JPA**（追加済み） |
| `application.properties` | `spring.application.name`＋Supabase接続設定（host/username、**password除く**）を追加済み |
| ソースコード | `VegeTabellAppApplication.java`のみ。Entity/Repository/Service/Controller/SecurityConfigは未実装 |
| テンプレート | `src/main/resources/templates`・`static`は空 |
| Eclipseプロジェクト設定 | `.project`/`.classpath`/`.settings`はBuildshipにより生成済み（`.gitignore`で除外設定済み・正しく運用されている） |

---

## 詳細手順

## 0. 前提環境（各メンバーのPC）

| ツール | バージョン目安 | 備考 |
|---|---|---|
| JDK | 17（LTS） | `build.gradle`のtoolchainで指定済み |
| Eclipse | Eclipse IDE for Enterprise Java and Web Developers | **Spring Tools 4 (STS)** と **Buildship (Gradle)** プラグインが必要。Marketplaceから追加インストール可 |
| Git | 任意の最新版 | |
| Supabaseアカウント | - | PostgreSQL接続情報（ホスト・DB名・ユーザー・パスワード）を各自取得 |

---

## 1. プロジェクトをEclipseへ取り込む

1. `git clone` （または既存のクローン済みフォルダを使用）
2. Eclipse: `File > Import... > Gradle > Existing Gradle Project`
3. `VegeTabell` 直下ではなく **`VegeTabell-app`フォルダ**を選択（[overview.md](./overview.md#開発環境)の指示通り、appフォルダをこの直下に配置する構成のため）
4. インポート後、Gradleの依存解決が走るのを待つ（初回は時間がかかる）
5. Package Explorerで`VegeTabell-app`が認識され、`VegeTabellAppApplication.java`が見えればOK

---

## 2. 依存関係の追加（Spring Data JPA）✅完了

`build.gradle`の`dependencies`ブロックに以下を追加済み：

```groovy
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
```

まだEclipseにインポートしていないメンバーは、インポート時に自動で依存解決される。既にインポート済みのメンバーは、`VegeTabell-app`プロジェクトを右クリック → `Gradle > Refresh Gradle Project` を実行し、依存関係を再同期すること。

---

## 3. Supabase（PostgreSQL）接続設定 ✅完了

チーム共通のSupabaseプロジェクト（project ref: `vrrrxhmpwscxsbwarhne`）を作成済み。接続情報（Supabaseダッシュボード `Project Settings > Database > Connect` の**Session pooler**タブで確認したもの）は以下の通り：

- Host: `aws-0-ap-south-1.pooler.supabase.com`
- Port: `5432`（Session pooler）
- Database: `postgres`
- Username: `postgres.vrrrxhmpwscxsbwarhne`
- Password: 各自Supabaseダッシュボードで確認し、**Gitにコミットしない**

Session poolerを使う理由：直接接続（`db.<ref>.supabase.co`）はIPv6必須のため、大学のネットワークなどIPv4しか使えない環境だと繋がらないことがある。Session poolerはIPv4互換なので、こちらを共通設定として採用した。

1. パスワードは`src/main/resources/application-local.properties`（`application-local.properties.example`をコピーして作成、`.gitignore`で除外済み）か、環境変数（`SPRING_DATASOURCE_PASSWORD`等）で渡す
2. `application.properties`本体には上記の共通設定（host/username）を記述済み。パスワードは空欄のまま
3. ローカルプロファイルを使う場合はEclipseの実行構成（Run Configurations）で `--spring.profiles.active=local` を追加するか、`application-local.properties`をそのまま`application.properties`と同階層に置き自動読込させる

`db-design.md`通りのテーブル（users/shops/categories/products/reservations/notifications）はSupabase MCPの`apply_migration`で作成済み（Step1完了）。`ddl-auto`は`validate`（JPAのEntity定義とDBスキーマの不一致を検知するのみ）にしてあるため、Entity実装時にスキーマとズレがあればここでエラーになる。`update`にすると意図しないスキーマ変更が起きうるため非推奨。

なお、このアプリはSupabase Auth/PostgRESTを使わずSpring Boot側で直接JDBC接続・独自認証を行う構成のため、全テーブルでRLS（Row Level Security）を有効化（ポリシーは追加せずデフォルト拒否）し、anon/publishableキー経由でのPostgREST公開を塞いである。バックエンドの直接接続（`postgres`ユーザー）はRLSの影響を受けずアクセス可能。

### Step3（Spring Security）からStep4（会員登録・ログイン画面）への申し送り

- ログインフォームは`username`（＝email）・`password`に加えて、買い手/売り手トグル用に**`role`という名前**のフィールドを送信すること（`buyer`または`seller`の小文字。`RoleMatchAuthenticationSuccessHandler`が参照する）
- 通常のメール/パスワード間違い（`/login?error`）と、ロール不一致（`/login?error=role`）は**パラメータ値に関わらず表示文言を完全に同じ**にすること（「メールアドレス、パスワード、またはアカウント種別が正しくありません」）。どちらが誤りかをUI側で分岐して教えない
- `SecurityConfig`は`.loginPage("/login")`を明示指定しているため、Spring Securityの自動生成ログインページは使われない。Step4で`/login`にGETで表示用のController/テンプレートを実装するまでは`/login`は404になる（意図した状態）

### Step4実装時に発見・対処した点

- `auth-design.md`の認可表は`/`を`ROLE_BUYER`限定としているが、`api-design.md`は「`/`はログイン後roleに応じて`/products`または`/seller/dashboard`へリダイレクト」としており、売り手が直接ログインすると（保存済みリクエストが無い場合のデフォルト遷移先が`/`のため）即403になってしまう矛盾があった。`SecurityConfig`の認可設定（`/`はBUYER限定のまま）には手を入れず、`RoleMatchAuthenticationSuccessHandler`のログイン成功後デフォルト遷移先をロールに応じて`/`（買い手）／`/seller/dashboard`（売り手）に振り分けるよう修正して解決した（売り手を`/`に送らないようにすることで両ドキュメントを両立）
- 上記修正にあたり、`RoleMatchAuthenticationSuccessHandler`（シングルトンBean）が`SavedRequestAwareAuthenticationSuccessHandler`をインスタンスフィールドとして使い回していたため、同時ログイン時にリダイレクト先設定が競合するおそれがあった。リクエストごとにローカルインスタンスを生成する形に修正済み
- `/`・`/products`・`/seller/dashboard`はまだControllerが無いため、ログイン直後のリダイレクトは404になる（Step5/6でController実装まで想定通り）

### Step5実装時の判断・申し送り事項

- **商品写真**：overview.mdで「アップロード方式は未確定」と申し送りされていた項目。Step5ではファイルアップロードは実装せず、`image_url`にURLを直接入力する簡易フォームとした。未入力時はCSSで🥬プレースホルダーを表示。実ファイルアップロード（ローカル保存 or Supabase Storage）は引き続き未確定のため、対応する場合は別Stepで検討すること
- **商品説明（description）欄**：`functional-requirements.md §2`の入力項目一覧・ワイヤーフレーム（seller-listing.png）のどちらにも記載が無いため、出品フォームには含めていない（DBカラムはNULLのまま）。商品詳細画面（Step6）で説明文が必要になった場合は、出品フォームへの項目追加を検討
- **ダッシュボードの売上集計**：ワイヤーフレーム（seller-dashboard.png）には「レスキュー完了（売上）」カードがあるが、予約機能（Step7）が未実装のため常に0円表示になり実態と合わない。Step5では「本日出品アイテム」件数と出品状況一覧のみを表示し、売上カードはStep7実装後に追加する
- **削除時の予約チェック**：`ReservationRepository.existsByProductId`で予約有無を確認し、予約が1件でもあれば削除せず`/seller/products`にリダイレクトしてエラーメッセージ（flash attribute）を表示する方式にした
- **実DBでの動作確認**：マージ後に`--spring.profiles.active=local`でローカル起動し、実際のSupabaseに対して出品→一覧反映→完売/削除操作をブラウザで手動確認済み（確認後にテストデータは削除済み）。なお`VegeTabellAppApplicationTests#contextLoads`はこのセッションでは元々（main上でも）失敗する状態だったが、これはローカルのパスワード未設定が原因で、Step5の変更とは無関係

### Step6実装時の判断・申し送り事項

- **エリア・距離表示**：`db-design.md`の未確定事項（距離表示：リアルタイム計算か簡易固定値か）およびチーム確認の結果、Step6では店舗住所（`shops.address`）への部分一致テキスト検索のみを実装し、ワイヤーフレーム（buyer-home.png/product-detail.png）にある距離「○m」表示・現在地取得（Geolocation）は見送った。買い手の位置情報を保存する仕組み自体も現状無い。実装する場合は別途チームで方針を決めること
- **予約ボタン**：商品詳細画面に「受け取りを予約する」ボタン（`POST /products/{id}/reservations`宛）は表示するが、Step7で未実装のため押すと404になる（Step4→5と同様の前方参照）。数量選択UIも予約フォームの一部としてStep7で実装予定のため、Step6では「残り在庫数量」の表示のみ
- **一覧・詳細のsold_out商品の扱い**：一覧（`/products`）は`status=on_sale AND remaining_quantity>0`のみ表示。詳細（`/products/{id}`）はステータスを問わず表示し、`on_sale`以外は予約ボタンを無効化して「完売しました」等のラベルを表示する方式にした（`functional-requirements.md §4`の「在庫切れ表示」要件に対応）
- **期限が迫っている警告バナーのしきい値**：「残り6時間未満」をMVP暫定値として採用（要件・ワイヤーフレームに具体的な時間指定が無いため）。運用してみて長さが合わなければ調整すること
- **バグ発見・修正**：キーワード/エリアが未入力（null）の場合、JPQLの`LOWER(CONCAT('%', :keyword, '%'))`をPostgreSQLが型推論できず`function lower(bytea) does not exist`で500エラーになる不具合を実機確認中に発見。`CONCAT`をJPQLから外し、Java側で`"%" + value.toLowerCase() + "%"`のLIKEパターン文字列を組み立ててから渡す方式に修正して解消した
- **ドキュメント不具合の修正**：Step6のコミットで進捗チェックリストの「Step7」行が誤って2行重複していたため、本Step7の作業と合わせて修正した

### Step7実装時の判断・申し送り事項

- **受取可能時間帯（pickup_start_at/pickup_end_at）**：db-design.md/functional-requirements.mdに決定ルールが無いため、`pickup_start_at=予約時刻`・`pickup_end_at=商品のexpiry_at`と定義した（「引取期限」をそのまま受取終了時刻として扱うMVP暫定ルール）。この定義だと予約時刻が深夜近くの場合は受取時間帯が日をまたぐため、表示文言では終了時刻側に「翌日」ラベルを付けて明確化している（`PickupWindowPresenter`）
- **売り手の予約一覧**：api-design.mdには売り手向けの予約一覧GETエンドポイントが定義されておらず、ワイヤーフレームにも対応画面が無かった（売り手がキャンセルするには予約IDを知る手段が必要なため、実装上のギャップだった）。`/seller/dashboard`に「予約一覧」セクションを追加する形で解消した（チームに確認済み）
- **通知（reservation_confirmed等）の生成**：db-design.mdでは予約作成・キャンセル時に通知を生成する設計だが、通知機能自体はStep8のため、Step7では通知レコードの作成は行っていない（Step8でまとめて実装する）
- **同時予約の整合性**：db-design.mdの未確定事項の通り、Step7では単純な読み取り→更新のトランザクション処理のみとし、悲観ロック等の対策は行っていない
- **`SecurityConfig`の認可調整**：auth-design.md §2の補足通り、`POST /reservations/{id}/cancel`のみ買い手・売り手どちらもアクセスしうるため、`/reservations/**`のBUYER限定ルールより先に`authenticated()`のみのルールを追加し、所有者チェック（本人の予約 or 自店舗の商品の予約）はコントローラー側で行う方式にした
- **バグ発見・修正**：実機確認で、受取可能時間帯が日をまたぐ場合に「本日21:37〜00:35まで」のように終了時刻が翌日であることが分からない表示になっていたのを発見。終了時刻の日付が開始日と異なる場合は「翌日」または日付ラベルを付けるよう`PickupWindowPresenter`を修正した

### Step8実装時の判断・申し送り事項

- **生成する通知の範囲**：db-design.mdが定義する6種類の通知のうち、既存イベントに紐づく`reservation_confirmed`／`new_reservation`／`reservation_canceled`の3種類のみをStep8で実装した。残り3種類は生成元イベントが無いため見送り：`pickup_reminder`と`stock_expiring_warning`はStep9のバッチ処理待ち、`new_product_nearby`は買い手の位置情報を保存する仕組みが無い（Step6から続く未対応事項）ため実装できない
- **キャンセル通知の宛先**：`reservation_canceled`はキャンセルした側ではなく、**相手側にのみ**送るようにした（自分の操作について自分に通知するのは冗長なため）。買い手がキャンセルすれば売り手へ、売り手がキャンセルすれば買い手へ1件ずつ生成する
- **既読化時の遷移先**：通知をタップすると既読化し、関連予約があれば買い手は`/reservations/{id}`へ、売り手は（個別の予約閲覧画面が無いため）`/seller/dashboard`へ遷移する。関連予約が無い通知は`/notifications`に留まる
- **未読バッジの表示範囲**：ワイヤーフレームに合わせて買い手ホーム（`/products`）と売り手ダッシュボード（`/seller/dashboard`）のみに表示し、共通ヘッダーが無い他の画面（商品詳細・出品フォーム等）には追加していない。全画面共通のナビ構成はStep10で整理する想定
- **通知一覧の「戻る」リンク**：`/`は買い手専用（`SecurityConfig`でROLE_BUYER限定）のため、売り手が`/notifications`から戻る際に`/`へのリンクだと403になる。`NotificationController`側でログインユーザーのロールに応じた戻り先（`/products`または`/seller/dashboard`）をModelに渡す方式で解決した

### Step9実装時の判断・申し送り事項

- **`pickup_reminder`のタイミング解釈**：Step7の実装で`pickup_start_at`＝予約した瞬間の時刻としているため、db-design.mdの文言通り「受取開始時刻の一定時間前」にリマインドすると実質的に予約直後にしか送られず意味がない。そのためStep9では**「受取期限（`pickup_end_at`）の一定時間前」**に、まだ受け取っていない予約（`status='reserved'`）の買い手へ送る、という実務的な解釈に変更した（チーム確認済み）
- **しきい値・実行間隔**：`pickup_reminder`は受取期限の1時間前、`stock_expiring_warning`は期限の2時間前（db-design.mdの例示値を採用）をMVP暫定値とした。4つのバッチ（商品期限切れ化・予約完了化・受取リマインド・在庫警告）はいずれも`@Scheduled(cron = "0 */5 * * * *")`で5分おきに実行する。具体的な数値の指定がdocsに無いため、いずれも運用してみて調整可能な暫定値
- **重複通知防止**：同一の予約／商品に対して同じ通知タイプを二重生成しないよう、`NotificationRepository`に`existsByReservationIdAndType`/`existsByProductIdAndType`を追加し、バッチ実行のたびに既存通知の有無を確認してからのみ生成する方式にした
- **通知生成ロジックの共通化**：`ReservationService`内にあった通知生成の`private notify(...)`メソッドを`NotificationService.create(...)`として切り出し、`ReservationService`とバッチ処理の両方から利用する形にリファクタリングした
- **`new_product_nearby`は引き続き対象外**：買い手の位置情報を保存する仕組みが無い（Step6から続く未対応事項）ため、Step9でも実装していない
- **画面側の変更なし**：`notifications/list.html`は通知タイプに依らずtitle/bodyを表示する汎用UIのため、新しい通知タイプ追加に伴うテンプレート変更は不要だった

---

## 4. パッケージ構成（提案）

`com.example.VegeTabell.app` 配下を役割ごとに分割する：

```
com.example.VegeTabell.app
├── entity/          … User, Shop, Category, Product, Reservation, Notification（db-design.md準拠）
├── repository/      … Spring Data JPAのRepositoryインターフェース
├── service/         … 業務ロジック（在庫更新・予約整合性チェックなど）
├── controller/       … AuthController, ProductController, ReservationController,
│                        SellerController, NotificationController（api-design.md準拠）
├── security/        … SecurityConfig, CustomUserDetailsService, ロール一致チェック処理（auth-design.md準拠）
├── form/ (or dto/)  … 画面入力用フォームクラス＋Validationアノテーション
└── config/          … @Scheduledバッチ設定など
```

テンプレートは`src/main/resources/templates`配下を画面グループごとに分ける：

```
templates/
├── auth/        (login.html, signup.html)
├── products/    (list.html, detail.html)
├── reservations/(confirm.html)
├── seller/      (dashboard.html, product-list.html, product-form.html)
└── notifications/(list.html)
```

[design/wireframes](../design/wireframes/) の各画像とファイル名を対応させると迷いにくい（例：`buyer-home.png` → `products/list.html`）。

---

## 5. 実装の推奨順序

| Step | 内容 | 参照ドキュメント |
|---|---|---|
| 1 | Supabase上に`db-design.md`通りのテーブルをSQLで作成 | [db-design.md](./db-design.md) |
| 2 | Entity・Repositoryを実装し、起動確認（`ddl-auto=validate`が通ることを確認） | [db-design.md](./db-design.md) |
| 3 | Spring Securityの設定（セッション認証、ROLE_BUYER/ROLE_SELLER、ログイン時のロール一致チェック） | [auth-design.md](./auth-design.md) |
| 4 | 会員登録・ログイン画面（`/signup`, `/login`） | [functional-requirements.md §1](./functional-requirements.md#1-アカウント登録ログイン) |
| 5 | 売り手：商品出品機能（`/seller/products/new`等） | [functional-requirements.md §2](./functional-requirements.md#2-商品出品) |
| 6 | 買い手：商品一覧・詳細（`/products`, `/products/{id}`） | [functional-requirements.md §3-4](./functional-requirements.md#3-商品一覧表示買い手ホーム) |
| 7 | 予約機能・キャンセル（`/products/{id}/reservations`等） | [functional-requirements.md §5](./functional-requirements.md#5-予約機能店頭払い) |
| 8 | 通知機能（一覧・既読化） | [functional-requirements.md §6](./functional-requirements.md#6-通知) |
| 9 | バッチ処理（`@Scheduled`：期限切れ商品の`expired`化、`pickup_reminder`等） | [db-design.md](./db-design.md#通知が生成されるタイミング実装フェーズで詳細化) |
| 10 | ワイヤーフレーム・画面遷移図との突合せ、レスポンシブ調整 | [design/wireframes](../design/wireframes/), [VegeTabell.drawio](./VegeTabell.drawio) |

各Stepの間で一度Eclipseから起動して画面確認しながら進めると手戻りが少ない。

---

## 6. Eclipseでの起動・確認方法

- Package Explorerで`VegeTabellAppApplication.java`を右クリック → `Run As > Spring Boot App`（STSプラグイン導入時）または`Run As > Java Application`
- もしくはGradle Tasksビューから `VegeTabell-app > application > bootRun` をダブルクリック
- 起動後、ブラウザで `http://localhost:8080` にアクセスして確認
- コード変更時、`spring-boot-devtools`導入済みのため保存すると自動リロードされる

---

## 7. テスト

- `src/test/java`配下にJUnit 5でテストクラスを作成（既存の`VegeTabellAppApplicationTests.java`がテンプレート）
- Eclipseでテストクラスを右クリック → `Run As > JUnit Test`
- コマンドラインからは `./gradlew test`

---

## 8. Git運用

- 過去の履歴（`feature/something`ブランチ→PRマージ）に倣い、機能ごとに`feature/xxx`ブランチを切ってPRを出す運用とする
- コミット前に`git status`で`.gradle/`・`build/`・`bin/`・`.classpath`等のEclipse/Gradle生成物が含まれていないか確認（`VegeTabell-app/.gitignore`で除外設定済みだが、誤って`git add -f`しないよう注意）
- 個人のDBパスワードを含む`application-local.properties`等は絶対にコミットしない

---

## 9. 申し送り事項との関係

実装を進める中で、[docs内の「チーム要確認」事項](./functional-requirements.md#申し送り事項チームで要確認)（パスワードリセットの要否、予約済み商品の削除禁止運用、数量選択UI追加など）に該当する画面・機能に着手する前に、チームで結論を出しておくこと。
