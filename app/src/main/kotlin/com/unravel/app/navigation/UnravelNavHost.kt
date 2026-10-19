package com.unravel.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.unravel.app.di.AppContainer
import com.unravel.core.database.dao.SkillNodeDao
import com.unravel.core.database.repository.SessionRepository
import com.unravel.core.engine.model.AlgorithmId
import com.unravel.core.engine.model.SandboxCategory
import com.unravel.feature.racemode.RaceModeScreen
import com.unravel.feature.racemode.RaceModeViewModel
import com.unravel.feature.sandbox.SandboxScreen
import com.unravel.feature.sandbox.SandboxViewModel
import com.unravel.feature.skillmap.SkillMapScreen
import com.unravel.feature.skillmap.SkillMapViewModel

/**
 * Root Navigation 3 Host orchestrating destinations, single-top backstack behavior,
 * predictive back gestures, and SharedTransitionLayout shared bounds.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun UnravelNavHost(
    modifier: Modifier = Modifier,
    initialRoute: UnravelRoute = SkillMapRoute,
    skillNodeDao: SkillNodeDao? = null,
    sessionRepository: SessionRepository? = null
) {
    val context = LocalContext.current
    val effectiveDao: SkillNodeDao = skillNodeDao ?: remember(context) { AppContainer.getSkillNodeDao(context) }
    val effectiveSessionRepo: SessionRepository = sessionRepository ?: remember(context) { AppContainer.getSessionRepository(context) }

    val backStack = rememberNavBackStack(initialRoute)

    // Handle back button / predictive back gesture
    val canPop = backStack.size > 1
    BackHandler(enabled = canPop) {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    fun navigateTo(route: UnravelRoute) {
        // Enforce single-top behavior
        if (backStack.lastOrNull() == route) return
        backStack.add(route)
    }

    fun popBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        val entryProvider = entryProvider<NavKey> {
            entry<SkillMapRoute> {
                val skillMapViewModel: SkillMapViewModel = viewModel(
                    key = "SkillMapViewModel",
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return SkillMapViewModel(
                                skillNodeDao = effectiveDao,
                                sessionRepository = effectiveSessionRepo
                            ) as T
                        }
                    }
                )

                val uiState by skillMapViewModel.uiState.collectAsStateWithLifecycle()

                SkillMapScreen(
                    uiState = uiState,
                    onIntent = skillMapViewModel::onIntent,
                    onNavigateToSandbox = { algorithmId, resumeSession ->
                        navigateTo(SandboxRoute(algorithmId = algorithmId, resumeSession = resumeSession))
                    },
                    onNavigateToRaceMode = {
                        navigateTo(RaceModeRoute())
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            entry<SandboxRoute> { route ->
                val algorithmIdEnum: AlgorithmId = remember(route.algorithmId) {
                    AlgorithmId.entries.firstOrNull {
                        it.id == route.algorithmId || it.name == route.algorithmId
                    } ?: AlgorithmId.TWO_POINTERS
                }

                val sandboxViewModel: SandboxViewModel = viewModel(
                    key = "sandbox-${route.algorithmId}-${route.resumeSession}",
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return SandboxViewModel(
                                initialAlgorithmId = algorithmIdEnum,
                                sessionRepository = effectiveSessionRepo
                            ) as T
                        }
                    }
                )

                SandboxScreen(
                    viewModel = sandboxViewModel,
                    onNavigateBack = { popBack() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            entry<RaceModeRoute> { route ->
                val raceCategory: SandboxCategory = remember(route.category) {
                    try {
                        SandboxCategory.valueOf(route.category)
                    } catch (_: Exception) {
                        SandboxCategory.ARRAY_POINTER
                    }
                }

                val raceViewModel: RaceModeViewModel = viewModel(
                    key = "racemode-${route.category}"
                )

                val uiState by raceViewModel.uiState.collectAsStateWithLifecycle()

                RaceModeScreen(
                    uiState = uiState,
                    onIntent = raceViewModel::onIntent,
                    onNavigateBack = { popBack() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        NavDisplay(
            backStack = backStack,
            onBack = { popBack() },
            entryProvider = entryProvider,
            sharedTransitionScope = this
        )
    }
}
