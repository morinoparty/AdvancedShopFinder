import { createTokenizer } from "@orama/tokenizers/japanese";
import { createFromSource } from "fumadocs-core/search/server";
import { source } from "@/lib/source";

export const revalidate = false;

// 日本語は Orama の標準トークナイザに対応していないため、専用トークナイザを使う。
export const { staticGET: GET } = createFromSource(source, {
	localeMap: {
		ja: {
			components: {
				tokenizer: createTokenizer(),
			},
			search: {
				threshold: 0,
				tolerance: 0,
			},
		},
		en: "english",
	},
});
