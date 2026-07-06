import { HomeLayout } from "fumadocs-ui/layouts/home";
import Link from "next/link";
import { baseOptions } from "@/app/layout.config";

const content = {
	ja: {
		tagline: "QuickShop-Hikari 対応のショップ検索プラグイン",
		description:
			"Minecraft サーバー上のショップを、アイテム・エンチャント本・あいまい検索で素早く見つけられます。",
		cta: "ドキュメントを読む",
	},
	en: {
		tagline: "A shop finder plugin for QuickShop-Hikari",
		description:
			"Quickly locate shops on your Minecraft server by item, enchanted book, or fuzzy search.",
		cta: "Read the docs",
	},
} as const;

export default async function HomePage({
	params,
}: {
	params: Promise<{ lang: string }>;
}) {
	const { lang } = await params;
	const t = content[lang as keyof typeof content] ?? content.ja;

	return (
		<HomeLayout {...baseOptions(lang)}>
			<main className="flex flex-1 flex-col items-center justify-center gap-6 px-4 py-24 text-center">
				<h1 className="text-4xl font-bold sm:text-5xl">Advanced Shop Finder</h1>
				<p className="text-lg text-fd-muted-foreground">{t.tagline}</p>
				<p className="max-w-xl text-fd-muted-foreground">{t.description}</p>
				<Link
					href={`/${lang}/docs`}
					className="rounded-full bg-fd-primary px-6 py-2.5 font-semibold text-fd-primary-foreground transition-opacity hover:opacity-90"
				>
					{t.cta}
				</Link>
			</main>
		</HomeLayout>
	);
}

export function generateStaticParams() {
	return [{ lang: "ja" }, { lang: "en" }];
}
