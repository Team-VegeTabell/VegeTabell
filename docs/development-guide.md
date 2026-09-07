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
- [ ] Step4: 会員登録・ログイン画面
- [ ] Step5: 売り手：商品出品機能
- [ ] Step6: 買い手：商品一覧・詳細
- [ ] Step7: 予約機能・キャンセル
- [ ] Step8: 通知機能
- [ ] Step9: バッチ処理（`@Scheduled`）
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
