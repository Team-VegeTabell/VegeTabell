# API・URL設計

Spring MVC + Thymeleafによるサーバーサイドレンダリングを前提とした、コントローラー単位のURL一覧。
機能要件は [functional-requirements.md](./functional-requirements.md)、テーブル定義は [db-design.md](./db-design.md) を参照。

## 設計方針

- Thymeleafの標準`<form>`はGET/POSTのみ対応のため、更新・削除も含めて **POSTで統一**する（`HiddenHttpMethodFilter`は使わず、MVPのシンプルさを優先）
- POST処理成功後はPRGパターン（Post/Redirect/Get）でGETにリダイレクトし、ブラウザ再読み込みによる二重送信を防ぐ
- 認可はSpring Securityを想定。`/seller/**`は`ROLE_SELLER`のみアクセス可能。予約・通知など個人に紐づくリソースは、コントローラー/サービス層で「本人（または本人の店舗）のデータか」の所有者チェックを行う

---

## 認証

| Method | URL | 内容 | 対象 |
|---|---|---|---|
| GET | /login | ログイン画面表示 | 全員 |
| POST | /login | ログイン処理（Spring Security標準の認証処理） | 全員 |
| POST | /logout | ログアウト | 要ログイン |
| GET | /signup | 新規登録画面表示 | 全員 |
| POST | /signup | 登録処理（roleに応じて`users`＋`shops`を作成） | 全員 |

## 買い手向け

| Method | URL | 内容 | 対象 |
|---|---|---|---|
| GET | / | トップ。ログイン後、roleに応じて`/products`または`/seller/dashboard`へリダイレクト | 要ログイン |
| GET | /products | 商品一覧（買い手ホーム）。クエリ：`category`, `keyword`, `area` | 買い手 |
| GET | /products/{id} | 商品詳細 | 買い手 |
| POST | /products/{id}/reservations | 予約作成（数量をフォームで送信）→成功時 `/reservations/{id}` へリダイレクト | 買い手 |
| GET | /reservations/{id} | 予約内容確認（受け取り情報） | 買い手本人の予約のみ |
| POST | /reservations/{id}/cancel | 予約キャンセル | 買い手本人 |
| GET | /mypage | マイページ（予約履歴など） | 買い手 ※現行ワイヤーフレーム未反映、要追加 |

## 売り手向け

| Method | URL | 内容 | 対象 |
|---|---|---|---|
| GET | /seller/dashboard | 売り手ダッシュボード（本日の状況・出品状況） | 売り手 |
| GET | /seller/products | 出品リスト一覧 | 売り手（自店舗分のみ） |
| GET | /seller/products/new | 出品フォーム表示 | 売り手 |
| POST | /seller/products | 出品登録 | 売り手 |
| POST | /seller/products/{id}/sold-out | 完売にする | 売り手（自店舗の商品のみ） |
| POST | /seller/products/{id}/delete | 出品削除（予約が無い商品のみ） | 売り手（自店舗の商品のみ） |
| POST | /reservations/{id}/cancel | 予約キャンセル（買い手向けと共通エンドポイント） | 売り手（自店舗の予約のみ） |
| GET | /seller/settings | 設定画面 | 売り手 ※現行ワイヤーフレーム未反映、要追加 |

## 通知（買い手・売り手共通）

| Method | URL | 内容 | 対象 |
|---|---|---|---|
| GET | /notifications | 通知一覧 | 要ログイン（本人宛のみ表示） |
| POST | /notifications/{id}/read | 既読化 | 要ログイン（本人宛のみ） |

---

## 申し送り事項

- `/mypage`（買い手マイページ）と`/seller/settings`（売り手設定）は現行ワイヤーフレームに未反映。画面遷移図・ワイヤーフレーム側での追加要否をチームで確認
- 予約作成・キャンセル時の在庫整合性（同時アクセス）は、コントローラーではなくサービス層のトランザクション制御で担保する（[db-design.mdの未確定事項](./db-design.md#未確定今後の検討事項)参照）
