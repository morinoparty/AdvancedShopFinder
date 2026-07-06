import type { BaseLayoutProps } from "fumadocs-ui/layouts/shared";

/**
 * Shared layout options for the home and docs layouts.
 * `locale` keeps navigation links inside the active language.
 */
export function baseOptions(locale: string): BaseLayoutProps {
	return {
		i18n: true,
		nav: {
			title: (
				<div className="flex items-center gap-2">
					<span className="text-lg font-bold">Advanced Shop Finder</span>
				</div>
			),
			url: `/${locale}`,
			transparentMode: "top",
		},
		// ライトテーマのみのため、テーマ切り替えスイッチを非表示にする
		themeSwitch: {
			enabled: false,
		},
		githubUrl: "https://github.com/morinoparty/AdvancedShopFinder",
		links: [
			{
				text: "Modrinth",
				url: "https://modrinth.com/plugin/advancedshopfinder",
				external: true,
			},
		],
	};
}
