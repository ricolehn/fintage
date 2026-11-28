package io.github.ricolehn.fintage.ui.person

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.github.ricolehn.fintage.data.jellyfin.BaseItem
import io.github.ricolehn.fintage.ui.Load
import io.github.ricolehn.fintage.ui.LocalAppActions
import io.github.ricolehn.fintage.ui.LocalAppContainer
import io.github.ricolehn.fintage.ui.LocalImages
import io.github.ricolehn.fintage.ui.LocalNavigationPadding
import io.github.ricolehn.fintage.ui.components.ErrorBox
import io.github.ricolehn.fintage.ui.components.FintageImage
import io.github.ricolehn.fintage.ui.components.FrostedTopBar
import io.github.ricolehn.fintage.ui.components.LoadingBox
import io.github.ricolehn.fintage.ui.components.PosterCard
import io.github.ricolehn.fintage.ui.components.TopBarHeight
import io.github.ricolehn.fintage.ui.components.rememberBlurHash
import io.github.ricolehn.fintage.ui.detail.DetailViewModel
import io.github.ricolehn.fintage.ui.theme.Fintage
import io.github.ricolehn.fintage.ui.theme.softGlow
import io.github.ricolehn.fintage.util.Format

@Composable
fun PersonScreen(personId: String) {
    val container = LocalAppContainer.current
    val viewModel: DetailViewModel = viewModel { DetailViewModel(personId, container.repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val hazeState = rememberHazeState()
    val gridState = rememberLazyGridState()
    val frosted by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 12 } }
    val showTitle by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 420 } }
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TopBarHeight

    Box(Modifier.fillMaxSize()) {
        when (val load = state.load) {
            Load.Loading -> LoadingBox()
            is Load.Failed -> ErrorBox(load.message, onRetry = viewModel::load)
            is Load.Ready -> {
                val person = load.value.item
                val credits = load.value.children
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 106.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(hazeState),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = topPadding,
                        bottom = LocalNavigationPadding.current.calculateBottomPadding(),
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    item(key = "header", span = { GridItemSpan(maxLineSpan) }) { PersonHeader(person) }
                    if (credits.isNotEmpty()) {
                        item(key = "known", span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "Known for",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                        items(credits, key = { it.id }) { item ->
                            PosterCard(item, onClick = { actions.openItem(item) }, width = null)
                        }
                    }
                }
            }
        }
        FrostedTopBar(hazeState = hazeState, frosted = frosted, title = state.item?.name, showTitle = showTitle, onBack = actions.back)
    }
}

@Composable
private fun PersonHeader(person: BaseItem) {
    val images = LocalImages.current
    val photo = images.primary(person, 500)
    val glow = rememberBlurHash(photo?.blurHash, 24, 24)
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(240.dp)
                .softGlow(glow, alpha = 0.75f),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(176.dp)
                    .shadow(20.dp, CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (photo != null) {
                    FintageImage(photo, contentDescription = person.name, modifier = Modifier.fillMaxSize())
                } else {
                    Box(Modifier.fillMaxSize().background(Fintage.colors.cardGradient))
                    Text(
                        person.name.orEmpty().split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString(""),
                        style = MaterialTheme.typography.displaySmall,
                        color = Fintage.colors.dimText,
                    )
                }
            }
        }
        Text(person.name.orEmpty(), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        val born = Format.date(person.premiereDate)
        val died = Format.date(person.endDate)
        val place = person.productionLocations.firstOrNull()
        val facts = listOfNotNull(
            born?.let { "Born $it" },
            died?.let { "Died $it" },
            place,
        ).joinToString(" · ")
        if (facts.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(facts, style = MaterialTheme.typography.bodyMedium, color = Fintage.colors.dimText, textAlign = TextAlign.Center)
        }
        val overview = person.overview
        if (!overview.isNullOrBlank()) {
            Spacer(Modifier.height(14.dp))
            Text(
                overview,
                style = MaterialTheme.typography.bodyMedium,
                color = Fintage.colors.dimText,
                maxLines = if (expanded) Int.MAX_VALUE else 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .animateContentSize()
                    .clickable { expanded = !expanded },
            )
        }
    }
}
