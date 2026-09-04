# アプリ企画概要

授業課題として作成する、フードロス削減マッチングアプリの企画まとめ。
詳細な機能要件は [functional-requirements.md](./functional-requirements.md)、DB設計は [db-design.md](./db-design.md)、API設計は [api-design.md](./api-design.md)、認証設計は [auth-design.md](./auth-design.md) を参照。

---

## アプリ概要

個人経営の食品店（八百屋など）の余剰生鮮食品を、近隣の節約志向ユーザーへ格安通知販売するマッチングアプリ。

---

## ペルソナ

**売り手：角川太郎（36歳）**
住宅街の八百屋オーナー。野菜が余るたびに廃棄しており、届ける手段がない。

**買い手：バンタン次郎（20歳）**
一人暮らしの大学生。生活費を全額自己負担、給料日前は食費を削りたい。

---

## コンセプト設計

| 項目 | 内容 |
|---|---|
| ターゲット | 個人経営食品店 ／ 一人暮らし大学生 |
| 課題 | 売れ残りが廃棄される ／ 安く食材を買いたい |
| 解決 | 期限が近い商品を近隣ユーザーへ通知販売 |
| 価値 | 食品ロス削減・売上向上 ／ 節約・地域活性化 |

---

## 競合との差別化

| サービス | 弱点 | 自分たちの優位点 |
|---|---|---|
| TABETE | 飲食店・調理済み食品中心 | 生鮮食品・個人経営店に特化 |
| タベスケ | 自治体依存 | 店舗が自由に使える |
| HELAS | 特定地域中心 | エリア制限なし |

---

## MVP機能

| 機能 | 対象 |
|---|---|
| アカウント登録・ログイン（売り手／買い手） | 両者 |
| 商品出品（商品名・価格・期限・写真） | 売り手 |
| 商品一覧表示 | 買い手 |
| 予約機能（店頭払い） | 買い手 |
| 通知機能（予約確定・新着出品・期限間近など） | 両者 |

※通知機能は必須機能とする（詳細は[functional-requirements.mdの「6. 通知」](./functional-requirements.md#6-通知)、テーブル定義は[db-design.mdのnotifications](./db-design.md#notifications通知)を参照）

---

## 開発環境

| 項目 | 内容 |
|---|---|
| 言語 | Java 17（LTS。Spring Boot 3.x系の最低要件） |
| フレームワーク | Spring Boot |
| Editor | Eclipse |
| DB | Supabase（PostgreSQL） |
| 画面 | Thymeleaf等でレスポンシブWeb |
| プロジェクト作成 | Spring Initializrで作成したフォルダを、この`VegeTabell`直下に配置してEclipseで開く |

### Spring Initializr 依存関係

**必須**

| 依存関係 | 理由 |
|---|---|
| Spring Web | Thymeleafでの画面遷移・コントローラー実装のベース（[api-design.md](./api-design.md)のURL設計） |
| Thymeleaf | レスポンシブWebをThymeleafで構築するため |
| Spring Data JPA | 各テーブル（users/shops/products等）のCRUD。[auth-design.md](./auth-design.md)で「Spring Data JPA（プレースホルダ経由）でSQLインジェクション対策」と明記 |
| PostgreSQL Driver | DBはSupabase（PostgreSQL） |
| Spring Security | セッションベース認証、ROLE_BUYER/ROLE_SELLER、CSRF対策、所有者チェックの土台（[auth-design.md](./auth-design.md)）。パスワードハッシュ化（BCryptPasswordEncoder）もここに含まれる |
| Validation（spring-boot-starter-validation） | [functional-requirements.md](./functional-requirements.md)の業務ルール（レスキュー価格≤通常価格、引取期限は未来必須など）をフォーム入力時にバリデーションするため |

**あると便利（任意）**

| 依存関係 | 理由 |
|---|---|
| Lombok | Entityのgetter/setterを自動生成し記述量を減らす |
| Spring Boot DevTools | Eclipseでの開発時、ホットリロードで確認が楽になる |

**不要と判断したもの**

- Spring Boot Actuator：MVPの監視要件なし
- OAuth2 Client：ソーシャルログイン言及なし（メール＋パスワードのみ）
- WebSocket／Kafka：リアルタイム通信の要件なし
- Spring Mail：メール認証・パスワードリセットはMVP対象外
- バッチ用の追加ライブラリ（Quartz等）：期限切れ処理・リマインド通知は`@Scheduled`（標準搭載）で対応可能

**申し送り事項**

- 商品写真（`image_url`）のアップロード方式（ローカル保存 or Supabase Storage等）は未確定。外部ストレージを使う場合は別途SDKの検討が必要

---

## 補足

- 本アプリは授業の課題として作成する。
- このフォルダ（`VegeTabell`）では要件定義までを行い、実際のアプリ開発は別フォルダで行う。
