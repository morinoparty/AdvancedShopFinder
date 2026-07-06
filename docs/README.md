# Advanced Shop Finder Documentation

[Fumadocs](https://fumadocs.dev) (Next.js) 製のドキュメントサイトです。日本語 (`ja`, デフォルト) と英語 (`en`) のバイリンガル構成です。

## 開発

```bash
pnpm install
pnpm dev
```

`http://localhost:3000` を開くと `/ja/docs` へリダイレクトされます。

## ビルド

```bash
pnpm build
```

静的サイトが `out/` に出力されます (`output: "export"`)。`scripts/postbuild.mjs` が検索インデックスの配置とルートリダイレクト (`/` → `/ja/docs/`) を生成します。

## コンテンツ

- `content/docs/**` … MDX ドキュメント本体
  - `*.mdx` … 日本語 (デフォルト)
  - `*.en.mdx` … 英語
  - `meta.json` / `meta.en.json` … サイドバーの並び・カテゴリ
