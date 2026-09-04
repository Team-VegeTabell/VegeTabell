# DB設計

VegeTabell（フードロス削減マッチングアプリ）のデータベース設計案。
DB：Supabase（PostgreSQL）を想定。

## 設計方針

- 各テーブルの主キー(id)は **BIGSERIAL**（連番の数値）で統一する
- タイムスタンプは **TIMESTAMPTZ** を使用する
- ステータス等の区分値は、PostgreSQLのENUM型ではなく `VARCHAR + CHECK制約` で統一する（後から選択肢を追加しやすいため）

## テーブル一覧

- users
- shops
- categories
- products
- reservations
- notifications

## ER概要（関連）

- users(seller) 1 — 1 shops
- shops 1 — N products
- categories 1 — N products
- users(buyer) 1 — N reservations
- products 1 — N reservations
- users 1 — N notifications

---

## users（共通・認証）

| カラム | 型 | NULL | デフォルト | 制約・インデックス | 備考 |
|---|---|---|---|---|---|
| id | BIGSERIAL | NOT NULL | 自動採番 | PRIMARY KEY | |
| email | VARCHAR(255) | NOT NULL | - | UNIQUE（idx_users_email） | ログインID |
| password_hash | VARCHAR(255) | NOT NULL | - | - | bcryptハッシュを想定 |
| role | VARCHAR(10) | NOT NULL | - | CHECK (role IN ('buyer','seller')) | |
| display_name | VARCHAR(50) | NOT NULL | - | - | |
| default_latitude | NUMERIC(9,6) | NULL可 | - | - | 買い手のみ使用（新着出品通知の基準位置） |
| default_longitude | NUMERIC(9,6) | NULL可 | - | - | 買い手のみ使用 |
| notification_radius_m | INTEGER | NULL可 | 1000 | - | 買い手のみ使用 |
| created_at | TIMESTAMPTZ | NOT NULL | now() | - | |
| updated_at | TIMESTAMPTZ | NOT NULL | now() | - | 更新時にアプリ側で更新 |

## shops（売り手の店舗情報。role='seller'のuserに1対1で紐づく）

| カラム | 型 | NULL | デフォルト | 制約・インデックス | 備考 |
|---|---|---|---|---|---|
| id | BIGSERIAL | NOT NULL | 自動採番 | PRIMARY KEY | |
| user_id | BIGINT | NOT NULL | - | UNIQUE（idx_shops_user_id）, FK → users.id ON DELETE CASCADE | |
| shop_name | VARCHAR(100) | NOT NULL | - | - | 例：「大地の恵み 八百屋」 |
| address | VARCHAR(255) | NOT NULL | - | - | 予約確認画面の受取先住所 |
| latitude | NUMERIC(9,6) | NOT NULL | - | idx_shops_location（latitude, longitude） | ホーム画面の距離表示（例：250m）に使用 |
| longitude | NUMERIC(9,6) | NOT NULL | - | 同上 | |
| pickup_note | TEXT | NULL可 | - | - | 例：「世田谷駅北口から徒歩3分、角の緑の看板が目印」 |
| created_at | TIMESTAMPTZ | NOT NULL | now() | - | |
| updated_at | TIMESTAMPTZ | NOT NULL | now() | - | |

## categories（マスタ）

| カラム | 型 | NULL | デフォルト | 制約・インデックス | 備考 |
|---|---|---|---|---|---|
| id | BIGSERIAL | NOT NULL | 自動採番 | PRIMARY KEY | |
| name | VARCHAR(30) | NOT NULL | - | UNIQUE | 野菜／果物／パン／惣菜 |
| icon | VARCHAR(10) | NULL可 | - | - | 絵文字を想定 |

初期投入データ：野菜🥦／果物🍎／パン🥖／惣菜🍱（4件のみ、マスタデータのためインデックス追加は不要）

## products（出品商品）

| カラム | 型 | NULL | デフォルト | 制約・インデックス | 備考 |
|---|---|---|---|---|---|
| id | BIGSERIAL | NOT NULL | 自動採番 | PRIMARY KEY | |
| shop_id | BIGINT | NOT NULL | - | FK → shops.id ON DELETE CASCADE, idx_products_shop_id | |
| category_id | BIGINT | NOT NULL | - | FK → categories.id, idx_products_category_id | |
| name | VARCHAR(100) | NOT NULL | - | - | 商品名 |
| description | TEXT | NULL可 | - | - | 商品情報欄 |
| image_url | VARCHAR(500) | NULL可 | - | - | |
| normal_price | INTEGER | NOT NULL | - | CHECK (normal_price >= 0) | 通常価格 |
| rescue_price | INTEGER | NOT NULL | - | CHECK (rescue_price >= 0 AND rescue_price <= normal_price) | レスキュー価格 |
| total_quantity | INTEGER | NOT NULL | - | CHECK (total_quantity >= 0) | 出品個数 |
| remaining_quantity | INTEGER | NOT NULL | - | CHECK (remaining_quantity >= 0) | 残り在庫数量（登録時は total_quantity と同値） |
| expiry_at | TIMESTAMPTZ | NOT NULL | - | idx_products_expiry_at | 引取期限・消費期限 |
| status | VARCHAR(10) | NOT NULL | 'on_sale' | CHECK (status IN ('on_sale','sold_out','expired')) | |
| created_at | TIMESTAMPTZ | NOT NULL | now() | - | |
| updated_at | TIMESTAMPTZ | NOT NULL | now() | - | |

補足：ホーム画面の一覧（出品中のみ・期限が近い順）表示のため、複合インデックス `idx_products_status_expiry (status, expiry_at)` も付与する。

## reservations（予約）

| カラム | 型 | NULL | デフォルト | 制約・インデックス | 備考 |
|---|---|---|---|---|---|
| id | BIGSERIAL | NOT NULL | 自動採番 | PRIMARY KEY | |
| product_id | BIGINT | NOT NULL | - | FK → products.id, idx_reservations_product_id | |
| buyer_id | BIGINT | NOT NULL | - | FK → users.id, idx_reservations_buyer_id | |
| quantity | INTEGER | NOT NULL | - | CHECK (quantity > 0) | |
| total_price | INTEGER | NOT NULL | - | CHECK (total_price >= 0) | 予約時点の価格を保存 |
| pickup_start_at | TIMESTAMPTZ | NOT NULL | - | - | 例：「本日16:00〜」 |
| pickup_end_at | TIMESTAMPTZ | NOT NULL | - | - | 例：「〜19:00まで」 |
| status | VARCHAR(10) | NOT NULL | 'reserved' | CHECK (status IN ('reserved','completed','canceled')) | |
| canceled_by | VARCHAR(10) | NULL可 | - | CHECK (canceled_by IN ('buyer','seller')) | |
| canceled_at | TIMESTAMPTZ | NULL可 | - | - | |
| reserved_at | TIMESTAMPTZ | NOT NULL | now() | - | |

## notifications（通知）

| カラム | 型 | NULL | デフォルト | 制約・インデックス | 備考 |
|---|---|---|---|---|---|
| id | BIGSERIAL | NOT NULL | 自動採番 | PRIMARY KEY | |
| user_id | BIGINT | NOT NULL | - | FK → users.id, idx_notifications_user_id_created_at（user_id, created_at DESC） | 通知の受信者（買い手 or 売り手） |
| type | VARCHAR(30) | NOT NULL | - | CHECK（下記参照） | |
| title | VARCHAR(100) | NOT NULL | - | - | |
| body | TEXT | NULL可 | - | - | |
| product_id | BIGINT | NULL可 | - | FK → products.id | 関連商品 |
| reservation_id | BIGINT | NULL可 | - | FK → reservations.id | 関連予約 |
| is_read | BOOLEAN | NOT NULL | false | idx_notifications_user_id_is_read（user_id, is_read） | 未読バッジ数の取得に使用 |
| created_at | TIMESTAMPTZ | NOT NULL | now() | - | |

### notifications.type の値（CHECK制約）

| type | 対象 | 内容 |
|---|---|---|
| reservation_confirmed | 買い手 | 予約確定 |
| pickup_reminder | 買い手 | 受け取り時間のリマインド |
| new_product_nearby | 買い手 | 近くの新着出品 |
| reservation_canceled | 買い手・売り手 | 予約キャンセル |
| new_reservation | 売り手 | 新規予約 |
| stock_expiring_warning | 売り手 | 売れ残り・期限間近の警告 |

### 通知が生成されるタイミング（実装フェーズで詳細化）

- `reservation_confirmed` / `new_reservation` / `reservation_canceled`：予約作成・キャンセル時にイベントとして生成（買い手・売り手それぞれに1件ずつ）
- `pickup_reminder`：受取開始時刻の一定時間前にバッチ処理で生成
- `new_product_nearby`：商品登録時に、範囲内（`notification_radius_m`）の買い手を検索して生成
- `stock_expiring_warning`：期限が近い（例：2時間前）かつ在庫ありの商品を定期チェックして生成

---

## 未確定・今後の検討事項

- 距離表示：リアルタイム計算か簡易固定値か
- 在庫の同時予約制御（複数買い手が同時予約した場合の整合性）
- 緯度経度を使った近傍検索は、件数が増えた場合PostGIS等の導入も検討
