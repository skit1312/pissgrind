package ch.pissgrind.ui.elements

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import ch.pissgrind.data.local.entity.CollectionItem
import ch.pissgrind.ui.screens.CollectionViewModel
import compose.icons.FeatherIcons
import compose.icons.feathericons.*


@Composable
fun AddToCollectionButton(
    item: CollectionItem,
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val isInCollection by viewModel.isInCollection(item.id)
        .collectAsState(initial = false)

    IconButton(onClick = { viewModel.toggleInCollection(item) }) {
        Icon(
            imageVector = if (isInCollection) FeatherIcons.Heart
                          else FeatherIcons.Heart,
            contentDescription = if (isInCollection) "Remove from Collection"
                                 else "Add to Collection",
            tint = if (isInCollection) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurface
        )
    }
}
