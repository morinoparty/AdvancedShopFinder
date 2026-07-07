package dev.nikomaru.advancedshopfinder.search

import arrow.core.Either
import com.ghostchu.quickshop.api.shop.Shop
import dev.nikomaru.advancedshopfinder.search.error.ShopSearchError
import org.koin.core.component.KoinComponent

interface Searcher<T> : KoinComponent {
    fun search(query: T): Either<ShopSearchError, List<Shop>>
}
